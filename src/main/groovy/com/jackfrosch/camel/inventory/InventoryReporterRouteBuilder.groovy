package com.jackfrosch.camel.inventory

import com.jackfrosch.camel.inventory.processors.InventoryProcessor
import com.jackfrosch.camel.inventory.processors.InventoryReportGenerator
import com.jackfrosch.camel.inventory.processors.InventoryValuator
import org.apache.camel.builder.RouteBuilder

class InventoryReporterRouteBuilder extends RouteBuilder {
    @Override
    void configure() throws Exception {
        getContext().setTracing(true);

//        from("jetty:http://localhost:8080/inventory")
//            .to("direct:valueInventory");

        from("file:/tmp/camel-demo/inventory/in?delay=1s&move=../archive") // polling consumer
                .routeId("inventoryReporterEntry")
                .convertBodyTo(String.class)
                .process(new InventoryProcessor())
                .to("direct:valueInventory");

        from("direct:valueInventory")
                .routeId("valueInventory")
                .process(new InventoryValuator())
                .to("direct:reportInventory");

        from("direct:reportInventory")
                .routeId("reportInventory")
                .process(new InventoryReportGenerator())
                .to("file:///tmp/camel-demo/inventory/out");                   //  producer

    }
}
