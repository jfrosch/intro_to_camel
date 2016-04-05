package com.jackfrosch.camel.inventory.domain

import groovy.transform.Canonical

@Canonical
class StockItem {
    String itemSku;
    Integer quantityOnHand;
    BigDecimal markedPrice;
    Boolean taxable;
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
