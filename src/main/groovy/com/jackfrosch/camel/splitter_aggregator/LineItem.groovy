package com.jackfrosch.camel.splitter_aggregator

import groovy.transform.ToString

@ToString(includeNames = true, includePackage = false, ignoreNulls = true)
class LineItem {
    int itemNo
    String productNo
    BigDecimal price = 0.00
    int qty = 1
    BigDecimal taxRate = 0.00
}
