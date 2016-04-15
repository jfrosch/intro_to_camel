package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.Exchange
import org.apache.camel.Message

class OrderHandler {

    void calculateTax(Exchange exchange) {
        String postalCode = exchange.in.headers.postalCode
        LineItem item = exchange.in.body as LineItem

        BigDecimal taxRate
        switch(postalCode) {
            case '12345':
                taxRate = 0.05
                break
            case '67890':
                taxRate = 0.08
                break
            default:
                taxRate = 0.06
        }
        item.taxRate = taxRate

        Thread.sleep(250) // simulating time to calculate it
    }

    void prepareForSplit(Exchange exchange) {
        Message msg = exchange.in
        Order order = msg.body as Order

        msg.headers.orderId = order.orderId
        msg.headers.postalCode = order.postalCode
        msg.headers.lineItemCount = order.lineItems.size()

        exchange.in.body = order.lineItems
    }

    void rebuildOrder(Exchange exchange) {
        Message msg = exchange.in
        List<LineItem> items = msg.body as List<LineItem>
        items.sort { it.itemNo }

        Order order = new Order(orderId: msg.headers.orderId,
                postalCode: msg.headers.postalCode,
                lineItems: items)

        msg.body = order
    }
}
