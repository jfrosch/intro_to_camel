package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.Processor

class InventoryProcessor implements Processor {
    @Override
    void process(Exchange exchange) throws Exception {
        Message inMsg = exchange.getIn();
        String input = (String) inMsg.getBody();

        List<StockItem> items = parseInput(input);
        inMsg.setBody(items);
        inMsg.setHeader("INVENTORY_COUNT", items.size());
    }

    protected List<StockItem> parseInput(String input) {
        List<StockItem> items = new ArrayList<>();

        String[] lines = input.split("\n");
        for(int i = 1; i < lines.length; i++) {
            items.add(createStockItem(lines[i].split(",")));
        }

        items;
    }

    private StockItem createStockItem(String[] fields) {
        new StockItem(fields[0], Integer.valueOf(fields[1]), new BigDecimal(fields[2]), "Y".equals(fields[3]));
    }
}
