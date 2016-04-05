package com.jackfrosch.camel.inventory.domain

import groovy.transform.Canonical

@Canonical
public class StockItem {
    String itemSku;
    int quantityOnHand;
    BigDecimal markedPrice;
    boolean taxable;
    BigDecimal itemValuation;

    boolean isNotTaxable() {
        !taxable;
    }

    BigDecimal getItemValuation() {
        if(!itemValuation) {
            itemValuation = quantityOnHand * markedPrice
        }
        itemValuation;
    }
}
