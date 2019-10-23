package lsfusion.erp.region.by.machinery.cashregister.fiscalsento;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class FiscalSentoPrintReceiptAction extends InternalAction {
    private final ClassPropertyInterface receiptInterface;

    public FiscalSentoPrintReceiptAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
      
        try {
            DataObject receiptObject = context.getDataKeyValue(receiptInterface);

            String fiscalSentoReceiptTop = (String) findProperty("fiscalSentoTop[Receipt]").read(context, receiptObject);
            String fiscalSentoReceiptBottom = (String) findProperty("fiscalSentoBottom[Receipt]").read(context, receiptObject);
            String numberDiscountCard = (String) findProperty("numberDiscountCard[Receipt]").read(context, receiptObject);

            ScriptingLogicsModule giftCardLM = context.getBL().getModule("GiftCard");

            boolean skipReceipt = findProperty("fiscalSkip[Receipt]").read(context, receiptObject) != null;
            if (skipReceipt) {
                if (context.apply())
                    findAction("createCurrentReceipt[]").execute(context);
                else
                    ServerLoggers.systemLogger.error("FiscalSentoPrintReceipt Apply Error (Not Fiscal)");
            } else {
                String logPath = (String) findProperty("logPathCurrentCashRegister[]").read(context);
                String comPort = (String) findProperty("stringComPortCurrentCashRegister[]").read(context);
                Integer baudRate = (Integer) findProperty("baudRateCurrentCashRegister[]").read(context);
                BigDecimal sumTotal = (BigDecimal) findProperty("sumReceiptDetail[Receipt]").read(context, receiptObject);
                BigDecimal maxSum = (BigDecimal) findProperty("maxSumCurrentCashRegister[]").read(context);

                if (sumTotal != null && maxSum != null && sumTotal.compareTo(maxSum) > 0) {
                    context.requestUserInteraction(new MessageClientAction("Сумма чека превышает " + maxSum.intValue() + " рублей", "Ошибка!"));
                    return;
                }

                BigDecimal sumDisc = null;

                TreeMap<Integer, BigDecimal> paymentSumMap = new TreeMap<>();
                BigDecimal sumCard = null;
                BigDecimal sumCash = null;
                BigDecimal sumGiftCard = null;

                KeyExpr paymentExpr = new KeyExpr("payment");
                ImRevMap<Object, KeyExpr> paymentKeys = MapFact.singletonRev("payment", paymentExpr);

                QueryBuilder<Object, Object> paymentQuery = new QueryBuilder<>(paymentKeys);
                paymentQuery.addProperty("sumPayment", findProperty("sum[Payment]").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.addProperty("paymentMeansPayment", findProperty("paymentMeans[Payment]").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.addProperty("idTypeRegister", findProperty("idTypeRegister[Payment]").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.and(findProperty("receipt[Payment]").getExpr(context.getModifier(), paymentQuery.getMapExprs().get("payment")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> paymentResult = paymentQuery.execute(context);
                for (ImMap<Object, Object> paymentValues : paymentResult.valueIt()) {
                    DataObject paymentMeansCashObject = ((ConcreteCustomClass) findClass("PaymentMeans")).getDataObject("paymentMeansCash");
                    DataObject paymentMeansCardObject = ((ConcreteCustomClass) findClass("PaymentMeans")).getDataObject("paymentMeansCard");
                    Integer idTypeRegister = (Integer) paymentValues.get("idTypeRegister");
                    BigDecimal sumPayment = (BigDecimal) paymentValues.get("sumPayment");
                    if(sumPayment != null) {
                        if(idTypeRegister != null) {
                            BigDecimal sum = paymentSumMap.get(idTypeRegister);
                            paymentSumMap.put(idTypeRegister, safeAdd(sum, sumPayment));
                        } else if (paymentMeansCashObject.getValue().equals(paymentValues.get("paymentMeansPayment"))) {
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
                ImRevMap<Object, KeyExpr> receiptDetailKeys = MapFact.singletonRev("receiptDetail", receiptDetailExpr);

                QueryBuilder<Object, Object> receiptDetailQuery = new QueryBuilder<>(receiptDetailKeys);
                String[] receiptDetailNames = new String[]{"nameSkuReceiptDetail", "quantityReceiptSaleDetail",
                        "quantityReceiptReturnDetail", "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                        "discountPercentReceiptSaleDetail", "discountSumReceiptDetail", "numberVATReceiptDetail",
                        "skuReceiptDetail", "boardNameSkuReceiptDetail"};
                LP[] receiptDetailProperties = findProperties("nameSku[ReceiptDetail]", "quantity[ReceiptSaleDetail]",
                        "quantity[ReceiptReturnDetail]", "price[ReceiptDetail]", "idBarcode[ReceiptDetail]", "sum[ReceiptDetail]",
                        "discountPercent[ReceiptSaleDetail]", "discountSum[ReceiptDetail]", "numberVAT[ReceiptDetail]",
                        "sku[ReceiptDetail]", "boardNameSku[ReceiptDetail]");
                for (int j = 0; j < receiptDetailProperties.length; j++) {
                    receiptDetailQuery.addProperty(receiptDetailNames[j], receiptDetailProperties[j].getExpr(context.getModifier(), receiptDetailExpr));
                }
                receiptDetailQuery.and(findProperty("receipt[ReceiptDetail]").getExpr(context.getModifier(), receiptDetailQuery.getMapExprs().get("receiptDetail")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = receiptDetailQuery.execute(context);
                List<ReceiptItem> receiptSaleItemList = new ArrayList<>();
                List<ReceiptItem> receiptReturnItemList = new ArrayList<>();
                for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
                    BigDecimal price = (BigDecimal) receiptDetailValues.get("priceReceiptDetail");
                    BigDecimal quantitySaleValue = (BigDecimal) receiptDetailValues.get("quantityReceiptSaleDetail");
                    double quantitySale = quantitySaleValue == null ? 0.0 : quantitySaleValue.doubleValue();
                    BigDecimal quantityReturnValue = (BigDecimal) receiptDetailValues.get("quantityReceiptReturnDetail");
                    double quantityReturn = quantityReturnValue == null ? 0.0 : quantityReturnValue.doubleValue();
                    String barcode = (String) receiptDetailValues.get("idBarcodeReceiptDetail");
                    if(barcode == null)
                        barcode = String.valueOf(receiptDetailValues.get("skuReceiptDetail"));
                    String boardName = (String) receiptDetailValues.get("boardNameSkuReceiptDetail");
                    String name = boardName != null ? boardName : (String) receiptDetailValues.get("nameSkuReceiptDetail");
                    name = name == null ? "" : name.trim();
                    BigDecimal sumReceiptDetailValue = (BigDecimal) receiptDetailValues.get("sumReceiptDetail");
                    double sumReceiptDetail = sumReceiptDetailValue == null ? 0 : sumReceiptDetailValue.doubleValue();
                    BigDecimal discountSumReceiptDetailValue = (BigDecimal) receiptDetailValues.get("discountSumReceiptDetail");
                    double discountSumReceiptDetail = discountSumReceiptDetailValue == null ? 0 : discountSumReceiptDetailValue.negate().doubleValue();
                    if (quantitySale > 0)
                        receiptSaleItemList.add(new ReceiptItem( price, quantitySale, barcode, name, sumReceiptDetail, discountSumReceiptDetail));
                    if (quantityReturn > 0)
                        receiptReturnItemList.add(new ReceiptItem(price, quantityReturn, barcode, name, sumReceiptDetail, discountSumReceiptDetail));
                }

                if (context.checkApply()) {
                    Object result = context.requestUserInteraction(new FiscalSentoPrintReceiptClientAction(false, logPath, comPort, baudRate,
                            new ReceiptInstance(sumDisc, paymentSumMap, sumCard, sumCash,
                            sumGiftCard == null ? null : sumGiftCard.abs(), sumTotal, numberDiscountCard, receiptSaleItemList, receiptReturnItemList),
                            fiscalSentoReceiptTop, fiscalSentoReceiptBottom));
                    if (result instanceof Integer) {
                        findProperty("number[Receipt]").change((Integer)result, context, receiptObject);
                        if (context.apply())
                            findAction("createCurrentReceipt[]").execute(context);
                        else
                            ServerLoggers.systemLogger.error("FiscalSentoPrintReceipt Apply Error");
                    } else {
                        ServerLoggers.systemLogger.error("FiscalSentoPrintReceipt Error: " + result);
                        context.requestUserInteraction(new MessageClientAction((String) result, "Ошибка"));
                    }
                }
            }
        } catch (SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    protected BigDecimal safeAdd(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else return (operand1 == null ? operand2 : (operand2 == null ? operand1 : operand1.add(operand2)));
    }
}