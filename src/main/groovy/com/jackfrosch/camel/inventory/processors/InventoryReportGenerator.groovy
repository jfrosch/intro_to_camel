package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.Processor

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InventoryReportGenerator implements Processor {
    static String createReportDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd / yyyy"))
    }

    @Override
    void process(Exchange exchange) throws Exception {
        Message inMsg = exchange.getIn()
        inMsg.setHeader(Exchange.FILE_NAME, createReportFileName(inMsg.getHeader(Exchange.FILE_NAME, String.class)))
        inMsg.setBody(buildReport(inMsg))
    }

    private String createReportFileName(String inputFileName) {
        return inputFileName.substring(0, inputFileName.indexOf('.')) + "_Report.txt"
    }

    private String buildReport(Message inMsg) {
        StringBuilder sb = new StringBuilder()
        sb.append("Inventory Report - ").append(createReportDate()).append("\n\n")
                .append("Summary")
                .append("\n----------------------------------------------\n\n")
                .append("Total SKUs in inventory: ").append(inMsg.getHeader("INVENTORY_COUNT")).append("\n")
                .append("Total Taxable Value in inventory: ").append(inMsg.getHeader("INVENTORY_TAXABLE_VALUATION")).append("\n")
                .append("Total Non-taxable Value in inventory: ").append(inMsg.getHeader("INVENTORY_NONTAXABLE_VALUATION"))
                .append("\n----------------------------------------------\n\n")
                .append("Inventory Details")
                .append("\n----------------------------------------------\n")
                .append(String.format("%6s %10s %5s %7s %7s %6s", "Item #", "SKU", "Qty", "Price", "Value", "Tax?")).append("\n")
                .append(String.format("%6s %10s %5s %7s %7s %6s", "------", "----------", "-----", "-------", "-------", "----"))
                .append("\n")


        // @formatter:off
        @SuppressWarnings("unchecked")
        List<StockItem> items = (List<StockItem>) inMsg.getBody()
        int index = 1
        for (StockItem item in items) {
            sb.append(String.format("%6d %10s %5d %7.2f %7.2f %6s", index, item.itemSku, item.quantityOnHand,
                                                                    item.markedPrice, item.itemValuation, item.taxable ? "Y" : "N"))
              .append("\n")
            index++
        }
        sb.append("----------------------------------------------\n\n--End Report--")

        return sb.toString()
    }
}
