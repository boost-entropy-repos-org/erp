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

    String logPath;
    String ip;
    int comPort;
    int baudRate;
    int placeNumber;
    int operatorNumber;
    ReceiptInstance receipt;
    String receiptTop;
    String receiptBottom;
    boolean giftCardAsNotPayment;
    String giftCardAsNotPaymentText;
    String UNP;
    String regNumber;
    String machineryNumber;

    public FiscalVMKPrintReceiptClientAction(String logPath, String ip, Integer comPort, Integer baudRate, Integer placeNumber, Integer operatorNumber,
                                             ReceiptInstance receipt, String receiptTop, String receiptBottom, boolean giftCardAsNotPayment,
                                             String giftCardAsNotPaymentText, String UNP, String regNumber, String machineryNumber) {
        this.logPath = logPath;
        this.ip = ip;
        this.comPort = comPort == null ? 0 : comPort;
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.placeNumber = placeNumber == null ? 1 : placeNumber;
        this.operatorNumber = operatorNumber == null ? 1 : operatorNumber;
        this.receipt = receipt;
        this.receiptTop = receiptTop;
        this.receiptBottom = receiptBottom;
        this.giftCardAsNotPayment = giftCardAsNotPayment;
        this.giftCardAsNotPaymentText = giftCardAsNotPaymentText;
        this.UNP = UNP;
        this.regNumber = regNumber;
        this.machineryNumber = machineryNumber;
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

                FiscalVMK.openPort(logPath, ip, comPort, baudRate);
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

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal discountSum = BigDecimal.ZERO;
            DecimalFormat formatter = getFormatter();
            FiscalVMK.printFiscalText(" \n      ТОВАРНЫЙ ЧЕК      \n ");
            for (ReceiptItem item : receiptList) {
                BigDecimal discount = BigDecimal.valueOf(item.articleDiscSum).subtract(BigDecimal.valueOf(item.bonusPaid));
                sum = sum.add(BigDecimal.valueOf(item.sumPos).subtract(discount));
                discountSum = discountSum.add(discount);
                FiscalVMK.printMultilineFiscalText(item.name);
                FiscalVMK.printFiscalText(getFiscalString("Код", item.barcode));
                FiscalVMK.printFiscalText(getFiscalString("Цена",
                        new DecimalFormat("#,###.##").format(item.quantity) + "x" + formatter.format(item.price)));
                if(!discountSum.equals(BigDecimal.ZERO)) {
                    FiscalVMK.printFiscalText(getFiscalString("Скидка", formatter.format(discount)));
                    FiscalVMK.printFiscalText(getFiscalString("Цена со скидкой", formatter.format(item.sumPos)));
                }
            }
            discountSum = discountSum.add(receipt.sumDisc == null ? BigDecimal.ZERO : receipt.sumDisc);
            sum = sum.subtract(receipt.sumGiftCard).max(BigDecimal.ZERO/*discountSum.abs()*/);

            FiscalVMK.printFiscalText(getFiscalString("Сертификат", formatter.format(receipt.sumGiftCard.negate())));
            if(receipt.numberDiscountCard != null)
                FiscalVMK.simpleLogAction("Дисконтная карта: " + receipt.numberDiscountCard);
            FiscalVMK.printFiscalText(getFiscalString("", " \n( _______ ____________ )"));
            FiscalVMK.printFiscalText(getFiscalString("", " (подпись)     ФИО      \n "));

            FiscalVMK.printMultilineFiscalText(giftCardAsNotPaymentText);

            FiscalVMK.printFiscalText(" \n      КАССОВЫЙ ЧЕК      \n ");
            if(UNP != null)
                FiscalVMK.printFiscalText(getFiscalString("УНП", UNP));
            if(regNumber != null)
            FiscalVMK.printFiscalText(getFiscalString("РЕГ N", regNumber));
            if(machineryNumber != null)
                FiscalVMK.printFiscalText(getFiscalString("N КСА", machineryNumber));

            if (!FiscalVMK.registerAndDiscountItem(sum.doubleValue(), discountSum.doubleValue()))
                return null;

            if (!FiscalVMK.subtotal())
                return null;

            FiscalVMK.printFiscalText(receiptBottom);

            if (!FiscalVMK.totalCard(receipt.sumCard))
                return null;
            //касса выдаёт ошибку, если пробивается ненулевая оплата безналом, а потом нулевая наличными
            if (receipt.sumCard != null && receipt.sumCash != null && receipt.sumCash.equals(BigDecimal.ZERO))
                receipt.sumCash = null;
            if (!FiscalVMK.totalCash(receipt.sumCash))
                return null;
            if(receipt.sumCard == null && receipt.sumCash == null && sum.doubleValue() == discountSum.abs().doubleValue())
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
