package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.MessageClientAction;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;


public class FiscalVMKPrintReceiptClientAction implements ClientAction {

    String ip;
    int comPort;
    int baudRate;
    int placeNumber;
    int operatorNumber;
    ReceiptInstance receipt;
    String receiptTop;
    String receiptBottom;
    boolean giftCardAsNotPayment;
    String denominationStage;

    public FiscalVMKPrintReceiptClientAction(String ip, Integer comPort, Integer baudRate, Integer placeNumber, Integer operatorNumber,
                                             ReceiptInstance receipt, String receiptTop, String receiptBottom, boolean giftCardAsNotPayment,
                                             String denominationStage) {
        this.ip = ip;
        this.comPort = comPort == null ? 0 : comPort;
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.placeNumber = placeNumber == null ? 1 : placeNumber;
        this.operatorNumber = operatorNumber == null ? 1 : operatorNumber;
        this.receipt = receipt;
        this.receiptTop = receiptTop;
        this.receiptBottom = receiptBottom;
        this.giftCardAsNotPayment = giftCardAsNotPayment;
        this.denominationStage = denominationStage;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        if (receipt.receiptSaleList.size() != 0 && receipt.receiptReturnList.size() != 0) {
            new MessageClientAction("В одном чеке обнаружены продажи и возврат одновременно", "Ошибка!");
            return "В одном чеке обнаружены продажи и возврат одновременно";
        }

        //защита от случая, когда сумма сертификата + сумма карточкой больше общей суммы.
        else if (receipt.sumGiftCard != null && receipt.sumCard != null && receipt.sumTotal != null && receipt.sumGiftCard.add(receipt.sumCard).doubleValue() > receipt.sumTotal.doubleValue()) {
            new MessageClientAction("Сумма сертификата и сумма оплаты по карточке больше общей суммы чека", "Ошибка!");
            return "Сумма сертификата и сумма оплаты по карточке больше общей суммы чека";
        } else {
            try {
                FiscalVMK.init();

                FiscalVMK.openPort(ip, comPort, baudRate);
                FiscalVMK.opensmIfClose();
                
                Integer numberReceipt = null;
                
                if (receipt.receiptSaleList.size() != 0) {
                    numberReceipt = printReceipt(receipt.receiptSaleList, true);
                    if (numberReceipt == null) {
                        String error = FiscalVMK.getError(false);
                        FiscalVMK.cancelReceipt();
                        return error;
                    }
                }
                    
                if (receipt.receiptReturnList.size() != 0) {
                    numberReceipt = printReceipt(receipt.receiptReturnList, false);
                    if (numberReceipt == null) {
                        String error = FiscalVMK.getError(false);
                        FiscalVMK.cancelReceipt();
                        return error;
                    }
                }
                    

                FiscalVMK.closePort();
                FiscalVMK.logReceipt(receipt, numberReceipt);

                return numberReceipt;
            } catch (RuntimeException e) {
                FiscalVMK.cancelReceipt();
                return FiscalVMK.getError(true);
            }
        }
    }

    private Integer printReceipt(List<ReceiptItem> receiptList, boolean sale) {

        if (!FiscalVMK.getFiscalClosureStatus())
            return null;
        if (!FiscalVMK.openReceipt(sale ? 0 : 1))
            return null;

        Integer receiptNumber = FiscalVMK.getReceiptNumber();

        FiscalVMK.printFiscalText(receiptTop);

        if (giftCardAsNotPayment && receipt.sumGiftCard != null) {

            double sum = 0;
            double discountSum = 0;
            DecimalFormat formatter = getFormatter();
            for (ReceiptItem item : receiptList) {
                double discount = item.articleDiscSum - item.bonusPaid;
                sum += item.sumPos - discount;
                discountSum += discount;
                FiscalVMK.printMultilineFiscalText(item.name);
                FiscalVMK.printFiscalText(getFiscalString("Код", item.barcode));
                FiscalVMK.printFiscalText(getFiscalString("Цена",
                        new DecimalFormat("#,###.##").format(item.quantity) + "x" + formatter.format(item.price)));
                if(discountSum != 0.0)
                FiscalVMK.printFiscalText(getFiscalString("Скидка",
                        formatter.format(discount)));
            }
            discountSum += receipt.sumDisc == null ? 0 : receipt.sumDisc.doubleValue();
            sum = Math.max(sum - receipt.sumGiftCard.doubleValue(), 0);

            FiscalVMK.printFiscalText(getFiscalString("Сертификат", formatter.format(receipt.sumGiftCard)));
            FiscalVMK.printFiscalText(getFiscalString("", " \n( _______ ____________ )"));
            FiscalVMK.printFiscalText(getFiscalString("", " (подпись)     ФИО      \n "));

            if (!FiscalVMK.registerAndDiscountItem(sum, discountSum))
                return null;

            if (!FiscalVMK.subtotal())
                return null;

            FiscalVMK.printFiscalText(receiptBottom);

            if (!FiscalVMK.totalCard(receipt.sumCard))
                return null;
            if (!FiscalVMK.totalCash(receipt.sumCash))
                return null;
            if(receipt.sumCard == null && receipt.sumCash == null && sum == 0.0)
                if(!FiscalVMK.totalCash(BigDecimal.ZERO))
                    return null;

        } else {

            for (ReceiptItem item : receiptList) {
                if (!FiscalVMK.registerItem(item))
                    return null;
                if (!FiscalVMK.discountItem(item, receipt.numberDiscountCard))
                    return null;
                DecimalFormat formatter = getFormatter();
                if (item.bonusSum != 0.0) {
                    FiscalVMK.simpleLogAction("Дисконтная карта: " + receipt.numberDiscountCard);
                    FiscalVMK.printFiscalText("Начислено бонусных баллов:\n" + formatter.format(item.bonusSum));
                }
                if (item.bonusPaid != 0.0) {
                    FiscalVMK.simpleLogAction("Дисконтная карта: " + receipt.numberDiscountCard);
                    FiscalVMK.printFiscalText("Оплачено бонусными баллами:\n" + formatter.format(item.bonusPaid));
                }
            }

            if (!FiscalVMK.subtotal())
                return null;
            if (!FiscalVMK.discountReceipt(receipt))
                return null;

            FiscalVMK.printFiscalText(receiptBottom);

            if (!FiscalVMK.totalGiftCard(receipt.sumGiftCard))
                return null;
            if (!FiscalVMK.totalCard(receipt.sumCard))
                return null;
            if (!FiscalVMK.totalCash(receipt.sumCash))
                return null;

        }

        return receiptNumber;
    }

    private String getFiscalString(String prefix, String value) {
        while(value.length() < 23 - prefix.length())
            value = " " + value;
        return prefix + " " + value;
    }

    private DecimalFormat getFormatter() {
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        formatter.setMinimumFractionDigits(2);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator('`');
        formatter.setDecimalFormatSymbols(symbols);
        return formatter;
    }
}
