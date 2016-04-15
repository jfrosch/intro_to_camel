package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.Exchange

class OrderHandler {

    void breakOutLineItems(Exchange exchange) {
        Order order = exchange.in.body as Order
        exchange.in.setHeader('order', order)   // stash order in a header
        exchange.in.body = order.lineItems      // send line items as body
    }
}
