package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext

class Application {
    static void main(String[] args) {
        CamelContext ctx = new DefaultCamelContext()

        OrderHandler orderHandler = new OrderHandler()
        ctx.addRoutes(new RouteBuilder() {
            void configure() {
                from("direct:orderEntry")
                    .routeId("orderEntry")
                    .bean(orderHandler, 'breakOutLineItems')
                    .split(body())//.parallelProcessing()
                        .to('direct:calculateTax')

                from("direct:calculateTax")
                    .bean(orderHandler, 'calculateTax')
                    .to("log:com.jackfrosch.camel.splitter_aggregator")
            }
        })
    }
}
