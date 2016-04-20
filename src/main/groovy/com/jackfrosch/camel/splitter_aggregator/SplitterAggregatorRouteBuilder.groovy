package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.builder.RouteBuilder

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SplitterAggregatorRouteBuilder extends RouteBuilder {
    OrderHandler orderHandler = new OrderHandler()

    ExecutorService cachedThreadPool = Executors.newCachedThreadPool()
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100) // same size as number of line items

    void configure() {
        from("direct:orderEntry")
                .routeId("orderEntry")
                .bean(orderHandler, 'prepareForSplit')
                .split(body())                                      // one thread
//                .split(body()).parallelProcessing()                 // 10 threads
//                .split(body()).executorService(cachedThreadPool)    // variable threads as load changes
//                .split(body()).executorService(fixedThreadPool)     // same # of threads as line items
                .to('direct:calculateTax')

        from("direct:calculateTax")
                .routeId("calculateTax")
                .log('Received at calculateTax: ${body}')
                .bean(orderHandler, 'calculateTax')
                .to("direct:aggregator")

        from("direct:aggregator")
                .routeId("aggregator")
                .log('Received at aggregator: ${body}')
                .aggregate(header('orderId'), new LineItemAggregatorStrategy())
                    .completionSize(header('lineItemCount'))
                .bean(orderHandler, 'rebuildOrder')
                .to('direct:finished')

        from("direct:finished")
                .routeId("finished")
                .log('Received at finished: Order: ${body} with ${body.lineItems.size()} lineItems and total value = ${body.orderTotal}')
                .log('LineItem order: ${body.itemNumbers}')

    }
}
