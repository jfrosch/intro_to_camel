package com.jackfrosch.camel.inventory

import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InventoryReportingApp {
    final static Logger logger = LoggerFactory.getLogger(InventoryReportingApp.class);

    static void main(String[] args) throws Exception {

        CamelContext ctx = new DefaultCamelContext();

        // Use a timer component to generate message events
        ctx.addRoutes(new InventoryReporterRouteBuilder());

        ctx.start();
        Thread.sleep(20000);
        ctx.stop();
    }
}
