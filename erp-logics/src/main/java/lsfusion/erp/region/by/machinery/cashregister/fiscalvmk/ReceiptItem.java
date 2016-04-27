package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import java.io.Serializable;

public class ReceiptItem implements Serializable {
    public boolean isGiftCard;
    public double price;
    public double quantity;
    public String barcode;
    public String name;
    public double sumPos;
    public double articleDiscSum;
    public double bonusSum;
    public double bonusPaid;

    public ReceiptItem(boolean isGiftCard, double price, double quantity, String barcode, String name, double sumPos,
                       double articleDiscSum, double bonusSum, double bonusPaid) {
        this.isGiftCard = isGiftCard;
        this.price = price;
        this.quantity = quantity;
        this.barcode = barcode;
        this.name = name;
        this.sumPos = sumPos;
        this.articleDiscSum = articleDiscSum;
        this.bonusSum = bonusSum;
        this.bonusPaid = bonusPaid;
    }
}
