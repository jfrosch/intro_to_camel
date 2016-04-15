package com.jackfrosch.camel.inventory;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryReportingApp {
    final static Logger logger = LoggerFactory.getLogger(InventoryReportingApp.class);

    public static void main(String[] args) throws Exception {

        CamelContext ctx = new DefaultCamelContext();

        // Use a timer component to generate message events
        ctx.addRoutes(new InventoryReporterRouteBuilder());

        ctx.start();
        Thread.sleep(150000);
        ctx.stop();
    }
}
