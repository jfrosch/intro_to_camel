package com.jackfrosch.camel.inventory.processors

import com.jackfrosch.camel.inventory.domain.StockItem
import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.Processor

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InventoryReportGenerator implements Processor {
    static String createReportDate() {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd / yyyy"))
    }

    @Override
    void process(Exchange exchange) throws Exception {
        Message inMsg = exchange.getIn()
        inMsg.setHeader(Exchange.FILE_NAME, createReportFileName(inMsg.getHeader(Exchange.FILE_NAME, String.class)))
        inMsg.setBody(buildReport(inMsg))
    }

    private String createReportFileName(String inputFileName) {
        inputFileName.substring(0, inputFileName.indexOf('.')) + "_Report.txt"
    }

    private String buildReport(Message inMsg) {
"""Inventory Report - ${createReportDate()}

Summary
----------------------------------------------

Total SKUs in inventory: ${inMsg.getHeader("INVENTORY_COUNT")}
Total Taxable Value in inventory: ${inMsg.getHeader("INVENTORY_TAXABLE_VALUATION")}
Total Non-taxable Value in inventory: ${inMsg.getHeader("INVENTORY_NONTAXABLE_VALUATION")}
----------------------------------------------

Inventory Details
----------------------------------------------
Item #        SKU   Qty   Price   Value   Tax?
------ ---------- ----- ------- -------   ----
${generateDetailLines(inMsg.body as List<StockItem>)}
----------------------------------------------

--End Report--"""
    }

    String generateDetailLines(List<StockItem> items) {
        List lines = []
        int index = 1
        for (StockItem item in items) {
            lines << String.format("%6d %10s %5d %7.2f %7.2f %6s", index, item.itemSku, item.quantityOnHand,
                                    item.markedPrice, item.itemValuation, item.taxable ? "Y" : "N")
            index++
        }
        lines.join('\n')
    }
}
