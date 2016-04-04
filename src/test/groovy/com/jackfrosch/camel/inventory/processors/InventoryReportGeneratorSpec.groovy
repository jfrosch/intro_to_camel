package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultMessage
import spock.lang.Specification

class InventoryReportGeneratorSpec extends Specification {
    InventoryReportGenerator processor
    def inMsg
    Exchange exchange

    void setup() {
        processor = new InventoryReportGenerator()

        inMsg = new DefaultMessage() // will "cheat" and use real Message just to hold test data
        exchange = Mock(Exchange)
        exchange.getIn() >> inMsg
    }

    void "Verify report generated as expected"() {
        given:
            List<StockItem> items = createInput()
            inMsg.setHeader(Exchange.FILE_NAME, 'Store123_Inventory_2016-04-01_01-35-45.csv')
            inMsg.setHeader('INVENTORY_COUNT', items.size())
            inMsg.setHeader('INVENTORY_TAXABLE_VALUATION', 650.00)
            inMsg.setHeader('INVENTORY_NONTAXABLE_VALUATION', 400.00)
            inMsg.setBody(items)
        when:
            processor.process(exchange)
        then:
            exchange.in.body == createExpectedReport()
    }

    private String createExpectedReport() {
"""Inventory Report - ${processor.createReportDate()}

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

    private List<StockItem> createInput() {
        [ new StockItem('101', 30, 20.00, true),
          new StockItem('102', 40, 10.00, false),
          new StockItem('103', 100, 0.50, true) ]
    }
}
