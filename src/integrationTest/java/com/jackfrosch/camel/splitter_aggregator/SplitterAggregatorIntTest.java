package com.jackfrosch.camel.splitter_aggregator;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SplitterAggregatorIntTest extends CamelTestSupport {
    private static final int NUMBER_OF_LINE_ITEMS = 10; // make it a 100 to see effect of concurrency

    @Before
    public void setup() throws Exception {
        context.addRoutes(new SplitterAggregatorRouteBuilder());
    }

    @Test
    public void testFullRouting() {
        List<Order> orders = new ArrayList<>();
        orders.add(createOrder("order-1", "12345"));
//        orders.add(createOrder("order-2", "67890"));
//        orders.add(createOrder("order-3", "99999"));

        NotifyBuilder notifier = new NotifyBuilder(context)
                                    .fromRoute("orderEntry")
                                    .wereSentTo("direct:finished")
                                    .whenDone(orders.size())
                                    .create();

        for(Order order : orders) {
            template.sendBody("direct:orderEntry", order);
        }

        assertTrue("We should have completed", notifier.matches(10, TimeUnit.SECONDS));
    }

    private Order createOrder(String orderId, String postalCode) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setPostalCode(postalCode);

        for(int i = 1; i <= NUMBER_OF_LINE_ITEMS; i++) {
            LineItem item = new LineItem();
            item.setItemNo(i);
            item.setProductNo("P" + i);
            item.setPrice(BigDecimal.ONE);
            item.setQty(i);
            order.addLineItem(item);
        }
        return order;
    }

}
