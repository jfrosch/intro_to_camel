package com.jackfrosch.camel.inventory.processors;

import com.jackfrosch.camel.inventory.domain.StockItem;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Input:
 *
 * Expect a CSV file in the Exchange body of this form:
 *
 *      SKU,Qty,Price,Taxable
 *      101,10,19.99,Y
 *      102,5,9.99,N
 *      103,200,0.49,Y
 *
 * The header line is mandatory, even if zero inventory exists
 *
 * Output:
 * Exchange in body will be List of StockItems
 * Exchange in header INVENTORY_COUNT will be set to number of items in inventory input
 */
public class InventoryProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String input = (String) in.getBody();

        List<StockItem> items = parseInput(input);
        in.setBody(items);
        in.setHeader("INVENTORY_COUNT", items.size());
    }

    protected List<StockItem> parseInput(String input) {
        List<StockItem> items = new ArrayList<>();

        String[] lines = input.split("\n");
        for(int i = 1; i < lines.length; i++) {
            items.add(createStockItem(lines[i].split(",")));
        }

        return items;
    }

    private StockItem createStockItem(String[] fields) {
        return new StockItem(fields[0], Integer.valueOf(fields[1]), new BigDecimal(fields[2]), "Y".equals(fields[3]));
    }
}
