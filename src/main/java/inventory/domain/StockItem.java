package inventory.domain;

import java.math.BigDecimal;

public class StockItem {
    private final String itemSku;
    private final int quantityOnHand;
    private final BigDecimal markedPrice;
    private final boolean taxable;
    private BigDecimal itemValuation;

    public StockItem(String itemSku, int quantityOnHand, BigDecimal markedPrice, boolean taxable) {
        this.itemSku = itemSku;
        this.quantityOnHand = quantityOnHand;
        this.markedPrice = markedPrice;
        this.taxable = taxable;
        this.itemValuation = new BigDecimal(quantityOnHand).multiply(markedPrice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StockItem stockItem = (StockItem) o;

        return itemSku.equals(stockItem.itemSku);
    }

    public String getItemSku() {
        return itemSku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getMarkedPrice() {
        return markedPrice;
    }

    @Override
    public int hashCode() {
        return itemSku.hashCode();
    }

    public boolean isTaxable() {
        return taxable;
    }

    public boolean isNotTaxable() {
        return !taxable;
    }

    public BigDecimal getItemValuation() {
        return itemValuation;
    }
}
