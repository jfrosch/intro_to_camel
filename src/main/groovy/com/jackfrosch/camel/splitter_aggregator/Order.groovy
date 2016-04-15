package com.jackfrosch.camel.splitter_aggregator

import groovy.transform.ToString

@ToString(includeNames = true, includePackage = false, ignoreNulls = true, excludes = 'lineItems')
class Order {
    String orderId                  // this will be our split item "correlation id"
    String postalCode               // drives tax on LineItems
    List<LineItem> lineItems = []

    void addLineItem(LineItem item) {
        lineItems << item
    }

    BigDecimal getOrderTotal() {
        lineItems.collect { LineItem item ->  item.price * item.qty * (1.00 + item.taxRate) }
                 .inject(0.00) { sum, value -> sum + value }
    }
}