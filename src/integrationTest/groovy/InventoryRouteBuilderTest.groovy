import com.jackfrosch.camel.inventory.InventoryReporterRouteBuilder
import com.jackfrosch.camel.inventory.domain.StockItem
import com.jackfrosch.camel.inventory.processors.InventoryReportGenerator
import org.apache.camel.Exchange
import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.builder.NotifyBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.model.RouteDefinition
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Before
import org.junit.Test

/**
 * With these system tests, we'll focus on testing routing
 */
class InventoryRouteBuilderTest extends CamelTestSupport {
    private static final String TEST_INPUT_FILE_NAME = 'Store123_Inventory_2016-04-01_01-35-45.csv'
    private static final String TEST_OUTPUT_FILE_NAME = 'Store123_Inventory_2016-04-01_01-35-45_Report.txt'

    List<StockItem> items

    @Before
    public void setUp() {
        super.setUp()
        items = createStockItems()
    }

    @Test
    void "test inventoryReporterEntry route"() {
        mockToRoute('inventoryReporterEntry', 'direct:valueInventory', 'mock:valueInventory');
        NotifyBuilder notifier = createNotifier('inventoryReporterEntry', 'mock:valueInventory')

        MockEndpoint mockEp = getMockEndpoint('mock:valueInventory')
        mockEp.expectedHeaderReceived(Exchange.FILE_NAME, TEST_INPUT_FILE_NAME)
        mockEp.expectedHeaderReceived('INVENTORY_COUNT', items.size())
        mockEp.expectedBodiesReceived([items])  // Notice this is a list of the bodies expected. We expect one message, so 1 body
        mockEp.resultMinimumWaitTime = 500

        // This will actually write the file out to the inbound directory
        template.sendBodyAndHeader('file:/tmp/camel-demo/inventory/in?delay=1s&move=../archive', createInput(),
                                    Exchange.FILE_NAME, TEST_INPUT_FILE_NAME)

        assert notifier.matchesMockWaitTime()
        assertMockEndpointsSatisfied()

        List skus = mockEp.exchanges[0].in.body.collect { StockItem item -> item.itemSku }
        assert skus == ['101', '102', '103']
    }

    @Test
    void "test valueInventory route"() {
        mockToRoute('valueInventory', 'direct:reportInventory', 'mock:reportInventory');
        NotifyBuilder notifier = createNotifier('valueInventory', 'mock:reportInventory')

        MockEndpoint mockEp = getMockEndpoint('mock:reportInventory')
        mockEp.expectedHeaderReceived(Exchange.FILE_NAME, TEST_INPUT_FILE_NAME)
        mockEp.expectedHeaderReceived('INVENTORY_COUNT', this.items.size())
        mockEp.expectedHeaderReceived('INVENTORY_TAXABLE_VALUATION', new BigDecimal("650.00"))
        mockEp.expectedHeaderReceived('INVENTORY_NONTAXABLE_VALUATION', new BigDecimal("400.00"))
        mockEp.expectedBodiesReceived([this.items])  // Notice this is a list of the bodies expected. We expect one message, so 1 body
        mockEp.resultMinimumWaitTime = 500

        // This will actually write the file out to the inbound directory
        template.sendBodyAndHeaders('direct:valueInventory', this.items,
                                    [(Exchange.FILE_NAME) : TEST_INPUT_FILE_NAME, 'INVENTORY_COUNT' : this.items.size()])

        assert notifier.matchesMockWaitTime()
        assertMockEndpointsSatisfied()

        List skus = mockEp.exchanges[0].in.body.collect { StockItem item -> item.itemSku }
        assert skus == ['101', '102', '103']
    }

    @Test
    void "test reportInventory route"() {
        mockToRoute('reportInventory', 'file:/tmp/camel-demo/inventory/out', 'mock:reportDir')
        NotifyBuilder notifier = createNotifier('reportInventory', 'mock:reportDir')

        MockEndpoint mockEp = getMockEndpoint('mock:reportDir')
        mockEp.expectedHeaderReceived(Exchange.FILE_NAME, TEST_OUTPUT_FILE_NAME)
        mockEp.expectedBodiesReceived(createExpectedReport())
        mockEp.resultMinimumWaitTime = 500

        // This will actually write the file out to the inbound directory
        template.sendBodyAndHeaders('direct:reportInventory', this.items,
                [(Exchange.FILE_NAME) : TEST_INPUT_FILE_NAME,
                 'INVENTORY_COUNT' : this.items.size(),
                 'INVENTORY_TAXABLE_VALUATION' :new BigDecimal("650.00"),
                 'INVENTORY_NONTAXABLE_VALUATION' : new BigDecimal("400.00")])

        assert notifier.matchesMockWaitTime()
        assertMockEndpointsSatisfied()
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        new InventoryReporterRouteBuilder()
    }

    private String createInput() {
        '''SKU,Qty,Price,Taxable
101,30,20.00,Y
102,40,10.00,N
103,100,0.50,Y'''
    }

    private NotifyBuilder createNotifier(String fromRouteId, String toRouteUri) {
        new NotifyBuilder(context).fromRoute(fromRouteId)
                .wereSentTo(toRouteUri)
                .whenCompleted(1)
                .create()
    }

    private String createExpectedReport() {
"""Inventory Report - ${InventoryReportGenerator.createReportDate()}

Summary
----------------------------------------------

Total SKUs in inventory: 3
Total Taxable Value in inventory: 650.00
Total Non-taxable Value in inventory: 400.00
----------------------------------------------

Inventory Details
----------------------------------------------
Item #        SKU   Qty   Price   Value   Tax?
------ ---------- ----- ------- -------   ----
     1        101    30   20.00  600.00      Y
     2        102    40   10.00  400.00      N
     3        103   100    0.50   50.00      Y
----------------------------------------------

--End Report--"""
    }

    private List<StockItem> createStockItems() {
        [
                new StockItem('101', 30, new BigDecimal("20.00"), true),
                new StockItem('102', 40, new BigDecimal("10.00"), false),
                new StockItem('103', 100, new BigDecimal("0.50"), true)
        ]
    }

    private RouteDefinition mockToRoute(final String fromRouteId, final String oldToRouteUri, final String mockToRouteUri) {
        context.getRouteDefinition(fromRouteId).adviceWith(context, new AdviceWithRouteBuilder() {
            void configure() throws Exception {
                interceptSendToEndpoint(oldToRouteUri)
                        .skipSendToOriginalEndpoint()
                        .to(mockToRouteUri)
            }
        })
    }
}
