package com.jackfrosch.camel.splitter_aggregator

class Order {
    String orderId                  // this will be our split item "correlation id"
    String postalCode               // drives tax on LineItems
    List<LineItem> lineItems = []

    BigDecimal getOrderTotal() {
        BigDecimal sum = 0.00
        lineItems.collect { LineItem item ->  sum += (item.price * item.qty) + item.tax }
                 .inject(sum) { sum += it }
    }
}