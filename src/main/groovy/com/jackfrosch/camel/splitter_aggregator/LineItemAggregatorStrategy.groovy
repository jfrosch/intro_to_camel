package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.Exchange
import org.apache.camel.processor.aggregate.AggregationStrategy

class LineItemAggregatorStrategy implements AggregationStrategy {
    @Override
    Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        LineItem item = newExchange.in.body as LineItem

        if (oldExchange == null) {
            newExchange.in.body = [item]
            newExchange
        } else {
            List<LineItem> list = oldExchange.in.body as List<LineItem>
            list << item
            return oldExchange
        }
    }
}
