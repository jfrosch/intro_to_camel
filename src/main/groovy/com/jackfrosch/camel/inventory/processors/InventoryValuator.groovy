package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import groovy.transform.CompileStatic
import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.Processor

import java.util.function.Predicate

class InventoryValuator implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message inMsg = exchange.in

        @SuppressWarnings("unchecked")
        List<StockItem> items = (List<StockItem>) inMsg.getBody()
        inMsg.setHeader("INVENTORY_TAXABLE_VALUATION", calculateInventoryValuation(items, {it.taxable} ))
        inMsg.setHeader("INVENTORY_NONTAXABLE_VALUATION", calculateInventoryValuation(items, {it.notTaxable} ))
    }

    // Note: The Predicate here is a Java 8 Predicate functional interface, not a Camel Predicate!
    @CompileStatic
    protected BigDecimal calculateInventoryValuation(List<StockItem> items, Predicate<StockItem> selector) {
        // can do it using Groovy functional way
//        items.findAll { selector.test(it) }
//             .collect {it.itemValuation}
//             .inject(BigDecimal.ZERO) { BigDecimal a, BigDecimal b -> a + b } as BigDecimal

        // or, leverage Java's stream
        StreamBasedInventoryValuator.calculateInventoryValuation(items, selector)
    }
}
