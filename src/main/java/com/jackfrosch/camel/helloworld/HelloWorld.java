package com.jackfrosch.camel.helloworld;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {
    final static Logger logger = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String[] args) throws Exception {

        CamelContext ctx = new DefaultCamelContext();

        // Use a timer component to generate message events
        ctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("timer:sayHello?period=1s")
                    .log("Hello");

            }
        });

        ctx.start();
        Thread.sleep(10000);
        ctx.stop();
    }

}
