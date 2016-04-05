package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

class InventoryValuatorSpec extends Specification {
    InventoryValuator processor
    Message inMsg
    Exchange exchange

    void setup() {
        processor = new InventoryValuator()

        inMsg = Mock(Message)
        exchange = Mock(Exchange)
        exchange.getIn() >> inMsg
    }

    void "Verify calculateInventoryValuation for the StockItem inventory"() {
        given:
            List<StockItem> items = createInput()
        when:
            BigDecimal taxableValue = processor.calculateInventoryValuation(items, { it.taxable })
            BigDecimal nontaxableValue = processor.calculateInventoryValuation(items, { it.notTaxable })
        then:
            taxableValue == 1100.00
            nontaxableValue == 400.00
    }

    void "Verify processor sets INVENTORY_TAXABLE_VALUATION and INVENTORY_NONTAXABLE_VALUATION headers"() {
        given:
            List<StockItem> items = createInput()
            exchange.in.getBody() >> items
        when:
            processor.process(exchange)
        then:
            1 * exchange.in.setHeader('INVENTORY_TAXABLE_VALUATION', 1100.00)
            1 * exchange.in.setHeader('INVENTORY_NONTAXABLE_VALUATION', 400.00)
    }

    private List<StockItem> createInput() {
        [ new StockItem('101', 30, 20.00, true),
          new StockItem('102', 40, 10.00, false),
          new StockItem('103', 1000, 0.50,true) ]
    }
}
