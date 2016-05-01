package com.jackfrosch.camel.pingpong;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPong {
    final static Logger logger = LoggerFactory.getLogger(PingPong.class);

    public static void main(String[] args) throws Exception {
        Processor proc = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                System.out.println(exchange.getIn().getBody());
                Thread.sleep(500);
            }
        };

        RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                getContext().setTracing(false);

                from("direct:start")
                  .routeId("start")
                  .to("http://localhost:8080/app/ping");

                from("jetty:http://localhost:8080/app/ping")
                  .routeId("ping")
                  .setBody(constant("Ping!"))
                  .process(proc)
                  .to("http://localhost:8081/app/pong?bridgeEndpoint=true");

                from("jetty:http://localhost:8081/app/pong")
                  .routeId("pong")
                  .setBody(constant("Pong!"))
                  .process(proc);

            }
        };

        Main main = new Main();
        main.addRouteBuilder(rb);
        main.start();

        Route route = main.getCamelContexts().get(0).getRoute("start");
        Exchange exchange =  route.getEndpoint().createExchange();
        ProducerTemplate template = exchange.getContext().createProducerTemplate();

        String msg = "start";
        for(int i = 1; i <= 3; i++) {
            template.sendBody("http://localhost:8080/app/ping", msg);
        }

        main.stop();
    }

}