package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.Processor

class InventoryProcessor implements Processor {
    @Override
    void process(Exchange exchange) throws Exception {
        Message inMsg = exchange.getIn()
        List<List<String>> records = (List<List<String>>) inMsg.body

        List<StockItem> items = parseInput(records)
        inMsg.setBody(items)
        inMsg.setHeader("INVENTORY_COUNT", items.size())
    }

    protected List<StockItem> parseInput(List<List<String>> records) {
        records.collect { List<String> fields -> createStockItem(fields) }
    }

    private StockItem createStockItem(List<String> fields) {
        new StockItem(fields[0], Integer.valueOf(fields[1]), new BigDecimal(fields[2]), "Y".equals(fields[3]))
    }
}
