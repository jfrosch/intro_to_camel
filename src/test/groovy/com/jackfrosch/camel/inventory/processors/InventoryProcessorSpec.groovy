package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

class InventoryProcessorSpec extends Specification {
    InventoryProcessor processor
    Message inMsg
    Exchange exchange

    void setup() {
        processor = new InventoryProcessor()

        inMsg = Mock(Message)
        exchange = Mock(Exchange)
        exchange.getIn() >> inMsg
    }

    void "verify parseInput yields three StockItems"() {
        given:
            String input = createInput()

        when:
            List<StockItem> items = processor.parseInput(input)

        then:
            items?.size() == 3
            items[0] == new StockItem('101', 30, new BigDecimal("20.00"), true)
            items[1] == new StockItem('102', 40, new BigDecimal("10.00"), false)
            items[2] == new StockItem('103', 1000, new BigDecimal("0.50"), true)
    }

    void "verify process sets the count header and the body holds three StockItems"() {
        given:
            String input = createInput()
            List<StockItem> items = processor.parseInput(input)
            inMsg.getBody() >> input
            inMsg.getHeader("INVENTORY_COUNT")

        when:
            processor.process(exchange)

        then:
            1 * exchange.in.setHeader("INVENTORY_COUNT", items.size())
            1 * exchange.in.setBody(items)
    }

    private String createInput() {
'''SKU,Qty,Price,Taxable
101,30,20.00,Y
102,40,10.00,N
103,100,0.50,Y'''
    }
}