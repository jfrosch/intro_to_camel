package com.jackfrosch.camel.splitter_aggregator

import org.apache.camel.builder.NotifyBuilder
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Before
import org.junit.Test

import java.util.concurrent.TimeUnit

/*
 A groovy version of the integration test
 */
class SplitterAggregatorRouteTest extends CamelTestSupport {
    private static final int NUMBER_OF_LINE_ITEMS = 10 // make it a 100 to see effect of concurrency

    @Before
    void setup() throws Exception {
        context.addRoutes(new SplitterAggregatorRouteBuilder())
    }

    @Test
    void testFullRouting() {
        List<Order> orders = new ArrayList<>()
        orders.add(createOrder("order-1", "12345"))
//        orders.add(createOrder("order-2", "67890"))
//        orders.add(createOrder("order-3", "99999"))

        NotifyBuilder notifier = new NotifyBuilder(context)
                .fromRoute("orderEntry")
                .wereSentTo("direct:finished")
                .whenDone(orders.size())
                .create()

        orders.each { Order order ->
            template.sendBody("direct:orderEntry", order)
        }

        assert notifier.matches(10, TimeUnit.SECONDS)
    }

    private Order createOrder(String orderId, String postalCode) {
        Order order = new Order(orderId: orderId, postalCode: postalCode)

        (1..NUMBER_OF_LINE_ITEMS).each { i ->
            order += new LineItem(itemNo: i, productNo: "P${i}", price: 1.00, qty: 1)
        }
        order
    }

}