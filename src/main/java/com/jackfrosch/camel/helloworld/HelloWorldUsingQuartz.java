package com.jackfrosch.camel.helloworld;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldUsingQuartz {
    final static Logger logger = LoggerFactory.getLogger(HelloWorldUsingQuartz.class);

    public static void main(String[] args) throws Exception {

        Main main = new Main();

        // Use a timer component to generate message events
        main.addRouteBuilder(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("quartz:myGroup/sayHelloTimer?cron=0/2+*+*+*+*+?")
                    .log("Hello from Quartz");

            }
        });

        main.start();
    }

}
