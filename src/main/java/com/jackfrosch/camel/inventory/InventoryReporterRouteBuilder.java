package com.jackfrosch.camel.inventory;

import com.jackfrosch.camel.inventory.processors.InventoryProcessor;
import com.jackfrosch.camel.inventory.processors.InventoryReportGenerator;
import com.jackfrosch.camel.inventory.processors.InventoryValuator;
import org.apache.camel.builder.RouteBuilder;

/**
 * These routes are a bit contrived, but illustrate some fundamental Camel
 * routing and processing ideas
 */
public class InventoryReporterRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        getContext().setTracing(true);

        from("file:/tmp/camel-demo/inventory/in?delay=1s&move=../archive") // endpoint is a consumer
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
            .to("file:///tmp/camel-demo/inventory/out");                    //  endpoint is a producer

    }
}
