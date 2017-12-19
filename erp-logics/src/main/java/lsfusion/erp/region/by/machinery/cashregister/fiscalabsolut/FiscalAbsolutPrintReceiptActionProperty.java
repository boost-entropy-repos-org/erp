package lsfusion.erp.region.by.machinery.cashregister.fiscalabsolut;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FiscalAbsolutPrintReceiptActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface receiptInterface;

    public FiscalAbsolutPrintReceiptActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
      
        try {
            DataObject receiptObject = context.getDataKeyValue(receiptInterface);

            String fiscalAbsolutReceiptTop = (String) findProperty("fiscalAbsolutTop[Receipt]").read(context, receiptObject);
            String fiscalAbsolutReceiptBottom = (String) findProperty("fiscalAbsolutBottom[Receipt]").read(context, receiptObject);
            String numberDiscountCard = (String) findProperty("numberDiscountCard[Receipt]").read(context, receiptObject);

            ScriptingLogicsModule giftCardLM = context.getBL().getModule("GiftCard");

            boolean skipReceipt = findProperty("fiscalSkip[Receipt]").read(context, receiptObject) != null;
            if (skipReceipt) {
                if (context.apply())
                    findAction("createCurrentReceipt[]").execute(context);
                else
                    ServerLoggers.systemLogger.error("FiscalAbsolutPrintReceipt Apply Error (Not Fiscal)");
            } else {
                String logPath = (String) findProperty("logPathCurrentCashRegister[]").read(context);
                Integer comPort = (Integer) findProperty("comPortCurrentCashRegister[]").read(context);
                Integer baudRate = (Integer) findProperty("baudRateCurrentCashRegister[]").read(context);
                Integer placeNumber = (Integer) findProperty("nppMachineryCurrentCashRegister[]").read(context);
                ObjectValue userObject = findProperty("employee[Receipt]").readClasses(context, receiptObject);
                Object operatorNumber = userObject.isNull() ? 0 : findProperty("operatorNumberCurrentCashRegister[CustomUser]").read(context, (DataObject) userObject);
                BigDecimal sumTotal = (BigDecimal) findProperty("sumReceiptDetail[Receipt]").read(context, receiptObject);
                BigDecimal maxSum = (BigDecimal) findProperty("maxSumCurrentCashRegister[]").read(context);
                boolean saveCommentOnFiscalTape = findProperty("saveCommentOnFiscalTapeAbsolut[]").read(context) != null;
                boolean groupPaymentsByVAT = findProperty("groupPaymentsByVAT[]").read(context) != null;
                boolean sumPaymentAbsolut = findProperty("sumPaymentAbsolut[]").read(context) != null;
                Integer maxLinesAbsolut = (Integer) findProperty("maxLinesAbsolut[]").read(context);
                boolean printSumWithDiscount = findProperty("printSumWithDiscountAbsolut[]").read(context) != null;
                if (sumTotal != null && maxSum != null && sumTotal.compareTo(maxSum) > 0) {
                    context.requestUserInteraction(new MessageClientAction("Сумма чека превышает " + maxSum.intValue() + " рублей", "Ошибка!"));
                    return;
                }

                ScriptingLogicsModule posGiftCardLM = context.getBL().getModule("POSGiftCard");
                boolean giftCardAsNotPayment = posGiftCardLM != null && (posGiftCardLM.findProperty("giftCardAsNotPaymentCurrentCashRegister[]").read(context) != null);

                BigDecimal sumDisc = null;
                BigDecimal sumCard = null;
                BigDecimal sumCash = null;
                BigDecimal sumGiftCard = null;

                KeyExpr paymentExpr = new KeyExpr("payment");
                ImRevMap<Object, KeyExpr> paymentKeys = MapFact.singletonRev((Object) "payment", paymentExpr);

                QueryBuilder<Object, Object> paymentQuery = new QueryBuilder<>(paymentKeys);
                paymentQuery.addProperty("sumPayment", findProperty("sum[Payment]").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.addProperty("paymentMeansPayment", findProperty("paymentMeans[Payment]").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.and(findProperty("receipt[Payment]").getExpr(context.getModifier(), paymentQuery.getMapExprs().get("payment")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> paymentResult = paymentQuery.execute(context);
                for (ImMap<Object, Object> paymentValues : paymentResult.valueIt()) {
                    DataObject paymentMeansCashObject = ((ConcreteCustomClass) findClass("PaymentMeans")).getDataObject("paymentMeansCash");
                    DataObject paymentMeansCardObject = ((ConcreteCustomClass) findClass("PaymentMeans")).getDataObject("paymentMeansCard");
                    BigDecimal sumPayment = (BigDecimal) paymentValues.get("sumPayment");
                    if(sumPayment != null) {
                        //DataObject paymentMeansGiftCardObject = giftCardLM == null ? null : ((ConcreteCustomClass) giftCardLM.findClass("PaymentMeans")).getDataObject("paymentMeansGiftCard");
                        if (paymentMeansCashObject.getValue().equals(paymentValues.get("paymentMeansPayment"))) {
                            sumCash = sumCash == null ? sumPayment : sumCash.add(sumPayment);
                        } else if (paymentMeansCardObject.getValue().equals(paymentValues.get("paymentMeansPayment"))) {
                            sumCard = sumCard == null ? sumPayment : sumCard.add(sumPayment);
                        } else if (giftCardLM != null) {
                            sumGiftCard = sumGiftCard == null ? sumPayment : sumGiftCard.add(sumPayment);
                        } else
                            sumDisc = sumDisc == null ? sumPayment : sumDisc.add(sumPayment);
                    }
                }

                KeyExpr receiptDetailExpr = new KeyExpr("receiptDetail");
                ImRevMap<Object, KeyExpr> receiptDetailKeys = MapFact.singletonRev((Object) "receiptDetail", receiptDetailExpr);

                QueryBuilder<Object, Object> receiptDetailQuery = new QueryBuilder<>(receiptDetailKeys);
                String[] receiptDetailNames = new String[]{"nameSkuReceiptDetail", "quantityReceiptDetail", "quantityReceiptSaleDetail",
                        "quantityReceiptReturnDetail", "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                        "discountPercentReceiptSaleDetail", "discountSumReceiptDetail", "valueVATReceiptDetail", "typeReceiptDetail",
                        "skuReceiptDetail", "boardNameSkuReceiptDetail", "bonusSumReceiptDetail", "bonusPaidReceiptDetail"};
                LCP[] receiptDetailProperties = findProperties("nameCashRegisterSku[ReceiptDetail]", "quantity[ReceiptDetail]", "quantity[ReceiptSaleDetail]",
                        "quantity[ReceiptReturnDetail]", "price[ReceiptDetail]", "idBarcode[ReceiptDetail]", "sum[ReceiptDetail]",
                        "discountPercent[ReceiptSaleDetail]", "discountSum[ReceiptDetail]", "valueVAT[ReceiptDetail]", "type[ReceiptDetail]",
                        "sku[ReceiptDetail]", "boardNameSku[ReceiptDetail]", "bonusSum[ReceiptDetail]", "bonusPaid[ReceiptDetail]");
                for (int j = 0; j < receiptDetailProperties.length; j++) {
                    receiptDetailQuery.addProperty(receiptDetailNames[j], receiptDetailProperties[j].getExpr(context.getModifier(), receiptDetailExpr));
                }
                receiptDetailQuery.and(findProperty("receipt[ReceiptDetail]").getExpr(context.getModifier(), receiptDetailQuery.getMapExprs().get("receiptDetail")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = receiptDetailQuery.execute(context);
                List<ReceiptItem> receiptSaleItemList = new ArrayList<>();
                List<ReceiptItem> receiptReturnItemList = new ArrayList<>();
                for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
                    String typeReceiptDetail = (String) receiptDetailValues.get("typeReceiptDetail");
                    Boolean isGiftCard = typeReceiptDetail != null && typeReceiptDetail.equals("Сертификат");
                    BigDecimal price = (BigDecimal) receiptDetailValues.get("priceReceiptDetail");
                    BigDecimal quantitySaleValue = (BigDecimal) receiptDetailValues.get("quantityReceiptSaleDetail");
                    double quantitySale = quantitySaleValue == null ? 0.0 : quantitySaleValue.doubleValue();
                    BigDecimal quantityReturnValue = (BigDecimal) receiptDetailValues.get("quantityReceiptReturnDetail");
                    double quantityReturn = quantityReturnValue == null ? 0.0 : quantityReturnValue.doubleValue();
                    BigDecimal quantityValue = (BigDecimal) receiptDetailValues.get("quantityReceiptDetail");
                    double quantity = quantityValue == null ? 0.0 : quantityValue.doubleValue();
                    String barcode = (String) receiptDetailValues.get("idBarcodeReceiptDetail");
                    if(barcode == null)
                        barcode = String.valueOf(receiptDetailValues.get("skuReceiptDetail"));
                    String boardName = (String) receiptDetailValues.get("boardNameSkuReceiptDetail");
                    String name = boardName != null ? boardName : (String) receiptDetailValues.get("nameSkuReceiptDetail");
                    name = name == null ? "" : name.trim();
                    double sumReceiptDetail = getDouble((BigDecimal) receiptDetailValues.get("sumReceiptDetail"), false);
                    double bonusSumReceiptDetail = getDouble((BigDecimal) receiptDetailValues.get("bonusSumReceiptDetail"), quantityReturn > 0);
                    double bonusPaidReceiptDetail = getDouble((BigDecimal) receiptDetailValues.get("bonusPaidReceiptDetail"), quantityReturn > 0);
                    double discountSumReceiptDetail = getDouble((BigDecimal) receiptDetailValues.get("discountSumReceiptDetail"), true);
                    double valueVATReceiptDetail = getDouble((BigDecimal) receiptDetailValues.get("valueVATReceiptDetail"), false);
                    if (quantitySale > 0 && !isGiftCard)
                        receiptSaleItemList.add(new ReceiptItem(isGiftCard, price, quantitySale, barcode, name, sumReceiptDetail,
                                discountSumReceiptDetail, bonusSumReceiptDetail, bonusPaidReceiptDetail, valueVATReceiptDetail));
                    if (quantity > 0 && isGiftCard)
                        receiptSaleItemList.add(new ReceiptItem(isGiftCard, price, quantity, barcode, "Подарочный сертификат",
                                sumReceiptDetail, discountSumReceiptDetail, bonusSumReceiptDetail, bonusPaidReceiptDetail, valueVATReceiptDetail));
                    if (quantityReturn > 0)
                        receiptReturnItemList.add(new ReceiptItem(isGiftCard, price, quantityReturn, barcode, name, sumReceiptDetail,
                                discountSumReceiptDetail, bonusSumReceiptDetail, -bonusPaidReceiptDetail, valueVATReceiptDetail));
                }

                String prefix = (String) findProperty("fiscalAbsolutPrefixCode128[]").read(context);
                String receiptCode128 = prefix == null ? null : (prefix + receiptObject.getValue());

                boolean useSKNO = findProperty("useSKNOAbsolutCurrentCashRegister[]").read(context) != null;

                if (context.checkApply()) {
                    Object result = context.requestUserInteraction(new FiscalAbsolutPrintReceiptClientAction(logPath, comPort, baudRate, placeNumber,
                            operatorNumber == null ? 1 : (Integer) operatorNumber, new ReceiptInstance(sumDisc, sumCard, sumCash,
                            sumGiftCard == null ? null : sumGiftCard.abs(), sumTotal, numberDiscountCard, receiptSaleItemList, receiptReturnItemList),
                            fiscalAbsolutReceiptTop, fiscalAbsolutReceiptBottom, receiptCode128, saveCommentOnFiscalTape, groupPaymentsByVAT,
                            giftCardAsNotPayment, sumPaymentAbsolut, maxLinesAbsolut, printSumWithDiscount, useSKNO));
                    if (result != null) {
                        ServerLoggers.systemLogger.error("FiscalAbsolutPrintReceipt Error: " + result);
                        context.requestUserInteraction(new MessageClientAction((String) result, "Ошибка"));
                    } else {
                        if (context.apply())
                            findAction("createCurrentReceipt[]").execute(context);
                        else
                            ServerLoggers.systemLogger.error("FiscalAbsolutPrintReceipt Apply Error");
                    }
                }
            }
        } catch (SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private double getDouble(BigDecimal value, boolean negate) {
        return value == null ? 0 : (negate ? value.negate() : value).doubleValue();
    }
}
