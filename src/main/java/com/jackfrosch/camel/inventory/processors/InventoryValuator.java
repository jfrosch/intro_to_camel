package com.jackfrosch.camel.inventory.processors;

import com.jackfrosch.camel.inventory.domain.StockItem;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/**
 * Input:
 *
 * List of StockItems in inventory
 *
 * Output:
 *
 * Exchange in header "INVENTORY_TAXABLE_VALUATION" will contain the total value of the Inventory subject to sales tax
 * Exchange in header "INVENTORY_NONTAXABLE_VALUATION" will contain the total value of the Inventory not subject to sales tax
 *
 */
public class InventoryValuator implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        @SuppressWarnings("unchecked")
        List<StockItem> items = (List<StockItem>) in.getBody();
        in.setHeader("INVENTORY_TAXABLE_VALUATION", calculateInventoryValuation(items, StockItem::isTaxable));
        in.setHeader("INVENTORY_NONTAXABLE_VALUATION", calculateInventoryValuation(items, StockItem::isNotTaxable));
    }

    // Note: The Predicate here is a Java 8 Predicate functional interface, not a Camel Predicate!
    protected BigDecimal calculateInventoryValuation(List<StockItem> items, Predicate<StockItem> selector) {
        return items.stream()                       // we could use a parallelStream() here
                    .filter(selector)
                    .map(StockItem::getItemValuation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
