package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.builder.NotifyBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Before
import org.junit.Test

import java.util.concurrent.TimeUnit

/*
 A groovy version of the integration test
 */
class SplitterAggregatorRouteTest extends CamelTestSupport {
    private static final int NUMBER_OF_LINE_ITEMS = 100 // make it a 100 to see effect of concurrency

    @Before
    void setup() throws Exception {
        context.addRoutes(new SplitterAggregatorRouteBuilder())
    }

    @Test
    void "verify line items are split"() {
        int numberOfLineItems = 5
        Order order = createOrder("testOrder", "12345", numberOfLineItems)
        
        context.getRouteDefinition("orderEntry").adviceWith(context,
                new AdviceWithRouteBuilder() {
                    void configure() throws Exception {
                        interceptSendToEndpoint('direct:calculateTax')
                                .skipSendToOriginalEndpoint()
                                .to('mock:calculateTax')
                    }
                })

        NotifyBuilder notifier = new NotifyBuilder(context).fromRoute('orderEntry')
                .wereSentTo('mock:calculateTax')
                .whenCompleted(1)
                .create()

        MockEndpoint mockEp = getMockEndpoint('mock:calculateTax')
        mockEp.expectedHeaderReceived('orderId', order.orderId)
        mockEp.expectedHeaderReceived('postalCode', order.postalCode)
        mockEp.expectedHeaderReceived('lineItemCount', numberOfLineItems)
        mockEp.expectedBodiesReceivedInAnyOrder(order.lineItems) // one or more bodies Note InAnyOrder
        mockEp.resultMinimumWaitTime = 500

        // now send the inbound message to the route we're testing
        // template is a ProducerTemplate for sending messages, usually in tests
        template.sendBody('direct:orderEntry', order)


        // Now do the assertions. Suppose we expect route A to send
        // In Java use JUnit assertMethods. With Groovy we can use power assert
        assert notifier.matchesMockWaitTime() // Groovy power assert
        assertMockEndpointsSatisfied()
    }

    @Test
    void "verify line items are aggregated in order and order reconstituted"() {
        int numberOfLineItems = 10
        Order order = createOrder("testOrder", "12345", numberOfLineItems)
        order.lineItems*.taxRate = 0.05
        BigDecimal orderTotal = order.orderTotal

        context.getRouteDefinition("aggregator").adviceWith(context,
                new AdviceWithRouteBuilder() {
                    void configure() throws Exception {
                        interceptSendToEndpoint('direct:finished')
                                .skipSendToOriginalEndpoint()
                                .to('mock:finished')
                    }
                })

        NotifyBuilder notifier = new NotifyBuilder(context).fromRoute('aggregator')
                .wereSentTo('mock:finished')
                .whenDone(1)
                .create()

        MockEndpoint mockEp = getMockEndpoint('mock:finished')
        mockEp.expectedBodiesReceived(order)
        mockEp.resultMinimumWaitTime = 1000

        // now send the inbound message to the route we're testing
        // template is a ProducerTemplate for sending messages, usually in tests
        order.lineItems.each { LineItem lineItem ->
                                template.asyncRequestBodyAndHeaders('direct:aggregator', lineItem,
                                                                        [ orderId: order.orderId,
                                                                          postalCode: order.postalCode,
                                                                          lineItemCount: numberOfLineItems
                                                                        ]) }


        // Now do the assertions. Suppose we expect route A to send
        // In Java use JUnit assertMethods. With Groovy we can use power assert
        assert notifier.matchesMockWaitTime() // Groovy power assert
        assertMockEndpointsSatisfied()
        assert orderTotal == (mockEp.exchanges[0].in.body as Order).orderTotal
    }

    @Test
    void "exercise all routes in order splitting and aggregation"() {
        List<Order> orders = []
        orders << createOrder("order-1", "12345")
//        orders << createOrder("order-2", "67890")
//        orders << createOrder("order-3", "99999")

        NotifyBuilder notifier = new NotifyBuilder(context)
                .fromRoute("orderEntry")
                .wereSentTo("direct:finished")
                .whenDone(orders.size())
                .create()

        orders.each { Order order ->
            template.asyncRequestBody("direct:orderEntry", order)
        }

        assert notifier.matches(NUMBER_OF_LINE_ITEMS, TimeUnit.SECONDS)
    }

    private Order createOrder(String orderId, String postalCode, int numberOfLineItems = NUMBER_OF_LINE_ITEMS) {
        Order order = new Order(orderId: orderId, postalCode: postalCode)

        (1..numberOfLineItems).each { i ->
            order += new LineItem(itemNo: i, productNo: "P${i}", price: 1.00, qty: 1)
        }
        order
    }

}