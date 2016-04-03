package com.jackfrosch.camel.inventory.processors;

import com.jackfrosch.camel.inventory.domain.StockItem;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Input:
 * <p>
 * Exchange in body has a the List of StockItems
 * <p>
 * Three headers should be set on the in Message:
 * <p>
 * INVENTORY_COUNT
 * INVENTORY_TAXABLE_VALUATION
 * INVENTORY_NONTAXABLE_VALUATION
 * Exchange.FILE_NAME containing input csv file name
 * <p>
 * Output:
 * <p>
 * A text report about the inventory results will be generated summarizing the
 * inventory in the top section and detailing the inventory in the details section.
 * <p>
 * This report will be stored in the exchange in body, replacing the List of StockItems
 */
public class InventoryReportGenerator implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        in.setHeader(Exchange.FILE_NAME, createReportFileName(in.getHeader(Exchange.FILE_NAME, String.class)));
        in.setBody(buildReport(in));
    }

    private String createReportFileName(String inputFileName) {
        return inputFileName.substring(0, inputFileName.indexOf('.')) + "_Report.txt";
    }

    private String buildReport(Message in) {
        StringBuilder sb = new StringBuilder();
        sb.append("Inventory Report - ").append(createReportDate()).append("\n\n")
            .append("Summary")
            .append("\n----------------------------------------------\n\n")
            .append("Total SKUs in inventory: ").append(in.getHeader("INVENTORY_COUNT")).append("\n")
            .append("Total Taxable Value in inventory: ").append(in.getHeader("INVENTORY_TAXABLE_VALUATION")).append("\n")
            .append("Total Non-taxable Value in inventory: ").append(in.getHeader("INVENTORY_NONTAXABLE_VALUATION"))
            .append("\n----------------------------------------------\n\n")
            .append("Inventory Details")
            .append("\n----------------------------------------------\n")
            .append(String.format("%6s %10s %5s %7s %7s %6s", "Item #", "SKU", "Qty", "Price", "Value", "Tax?")).append("\n")
            .append(String.format("%6s %10s %5s %7s %7s %6s", "------", "----------", "-----", "-------", "-------", "----"))
            .append("\n");


        // @formatter:off
        @SuppressWarnings("unchecked")
        List<StockItem> items = (List<StockItem>) in.getBody();
        int index = 1;
        for (StockItem item : items) {
            sb.append(String.format("%6d %10s %5d %7.2f %7.2f %6s", index, item.getItemSku(), item.getQuantityOnHand(),
                                                                    item.getMarkedPrice(), item.getItemValuation(), item.isTaxable() ? "Y" : "N"))
              .append("\n");
            index++;
        }
        sb.append("----------------------------------------------\n\n--End Report--");

        return sb.toString();
    }

    protected String createReportDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd / yyyy"));
    }
}
