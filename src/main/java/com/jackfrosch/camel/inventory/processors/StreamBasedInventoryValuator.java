package com.jackfrosch.camel.inventory.processors;

import com.jackfrosch.camel.inventory.domain.StockItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

public class StreamBasedInventoryValuator {
    protected static BigDecimal calculateInventoryValuation(List<StockItem> items, Predicate<StockItem> selector) {
        return items.stream()
                    .filter(selector)
                    .map(StockItem::getItemValuation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
