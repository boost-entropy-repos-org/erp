package lsfusion.erp.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ImportReceiptsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface zReportInterface;

    public ImportReceiptsActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("ZReport")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        zReportInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        DataSession session = context.getSession();
        try {
            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы XML", "xml");
            DataObject zReportObject = context.getDataKeyValue(zReportInterface);
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {

                List<List<Object>> dataSale = new ArrayList<List<Object>>();
                List<List<Object>> dataReturn = new ArrayList<List<Object>>();
                List<List<Object>> dataPayment = new ArrayList<List<Object>>();

                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());
                for (byte[] file : fileList) {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(new ByteArrayInputStream(file));
                    doc.getDocumentElement().normalize();

                    String numberZReport = (String) LM.findLCPByCompoundName("numberZReport").read(session, zReportObject);
                    String numberCashRegisterZReport = (String) LM.findLCPByCompoundName("numberCashRegisterZReport").read(session, zReportObject);

                    Integer receiptNumber = 0;
                    KeyExpr receiptExpr = new KeyExpr("receipt");
                    ImRevMap<Object, KeyExpr> receiptKeys = MapFact.singletonRev((Object)"receipt", receiptExpr);
                    QueryBuilder<Object, Object> receiptQuery = new QueryBuilder<Object, Object>(receiptKeys);
                    receiptQuery.addProperty("numberReceipt", getLCP("numberReceipt").getExpr(context.getModifier(), receiptExpr));
                    receiptQuery.and(getLCP("zReportReceipt").getExpr(context.getModifier(), receiptQuery.getMapExprs().get("receipt")).compare(zReportObject.getExpr(), Compare.EQUALS));
                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptResult = receiptQuery.execute(session.sql);
                    for (ImMap<Object, Object> receiptRows : receiptResult.valueIt()) {
                        Integer number = (Integer) receiptRows.get("numberReceipt");
                        if (number != null && number > receiptNumber)
                            receiptNumber = number;
                    }

                    NodeList receiptList = doc.getElementsByTagName("receipt");

                    for (int receiptIndex = 0; receiptIndex < receiptList.getLength(); receiptIndex++) {

                        Node receipt = receiptList.item(receiptIndex);
                        if (receipt.getNodeType() == Node.ELEMENT_NODE) {
                            Element receiptElement = (Element) receipt;

                            receiptNumber++;
                            Double discountSumReceipt = (Double) getTagValue("discountSumReceipt", receiptElement, 2);
                            String seriesNumberDiscountCard = (String) getTagValue("numberDiscountCardReceipt", receiptElement, 0);

                            Long dateTimeValue = (Long) getTagValue("dateTimeReceipt", receiptElement, 3);
                            Date dateReceipt = dateTimeValue == null ? null : new Date(dateTimeValue);
                            Time timeReceipt = dateTimeValue == null ? null : new Time(dateTimeValue);

                            NodeList paymentList = receiptElement.getElementsByTagName("payment");
                            for (int paymentIndex = 0; paymentIndex < paymentList.getLength(); paymentIndex++) {
                                Node payment = paymentList.item(paymentIndex);
                                if (payment.getNodeType() == Node.ELEMENT_NODE) {
                                    Element paymentElement = (Element) payment;
                                    Double sumPayment = (Double) getTagValue("sumPayment", paymentElement, 2);
                                    String namePaymentMeansPayment = (String) getTagValue("namePaymentMeansPayment", paymentElement, 0);
                                    String sidPaymentTypePayment = (String) getTagValue("sidPaymentTypePayment", paymentElement, 0);
                                    dataPayment.add(Arrays.<Object>asList(numberZReport, receiptNumber, numberCashRegisterZReport,
                                            sumPayment, namePaymentMeansPayment, sidPaymentTypePayment, paymentIndex + 1));
                                }
                            }
                            NodeList receiptDetailList = receiptElement.getElementsByTagName("receiptDetail");
                            for (int receiptDetailIndex = 0; receiptDetailIndex < receiptDetailList.getLength(); receiptDetailIndex++) {
                                Node receiptDetail = receiptDetailList.item(receiptDetailIndex);
                                if (receiptDetail.getNodeType() == Node.ELEMENT_NODE) {
                                    Element receiptDetailElement = (Element) receiptDetail;
                                    Double priceReceiptDetail = (Double) getTagValue("priceReceiptDetail", receiptDetailElement, 2);
                                    Double quantityReceiptSaleDetail = (Double) getTagValue("quantityReceiptSaleDetail", receiptDetailElement, 2);
                                    Double quantityReceiptReturnDetail = (Double) getTagValue("quantityReceiptReturnDetail", receiptDetailElement, 2);
                                    String idBarcodeReceiptDetail = (String) getTagValue("idBarcodeReceiptDetail", receiptDetailElement, 0);
                                    Double sumReceiptDetail = (Double) getTagValue("sumReceiptDetail", receiptDetailElement, 2);
                                    Double discountSumReceiptDetail = (Double) getTagValue("discountSumReceiptDetail", receiptDetailElement, 2);
                                    Integer numberReceiptDetail = (Integer) getTagValue("numberReceiptDetail", receiptDetailElement, 1);

                                    if (quantityReceiptReturnDetail != null)
                                        dataReturn.add(Arrays.<Object>asList(numberCashRegisterZReport, numberZReport, dateReceipt, timeReceipt, receiptNumber,
                                                numberReceiptDetail, idBarcodeReceiptDetail, quantityReceiptReturnDetail, priceReceiptDetail, sumReceiptDetail,
                                                discountSumReceiptDetail, discountSumReceipt, seriesNumberDiscountCard));
                                    else
                                        dataSale.add(Arrays.<Object>asList(numberCashRegisterZReport, numberZReport, dateReceipt, timeReceipt, receiptNumber,
                                                numberReceiptDetail, idBarcodeReceiptDetail, quantityReceiptSaleDetail, priceReceiptDetail, sumReceiptDetail,
                                                discountSumReceiptDetail, discountSumReceipt, seriesNumberDiscountCard));
                                }
                            }
                        }
                    }
                }

                ImportField cashRegisterField = new ImportField(LM.findLCPByCompoundName("numberCashRegister"));
                ImportField zReportNumberField = new ImportField(LM.findLCPByCompoundName("numberZReport"));
                ImportField numberReceiptField = new ImportField(LM.findLCPByCompoundName("numberReceipt"));
                ImportField dateField = new ImportField(LM.findLCPByCompoundName("dateReceipt"));
                ImportField timeField = new ImportField(LM.findLCPByCompoundName("timeReceipt"));
                ImportField numberReceiptDetailField = new ImportField(LM.findLCPByCompoundName("numberReceiptDetail"));
                ImportField idBarcodeReceiptDetailField = new ImportField(LM.findLCPByCompoundName("idBarcodeReceiptDetail"));

                ImportField quantityReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("quantityReceiptSaleDetail"));
                ImportField priceReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("priceReceiptSaleDetail"));
                ImportField sumReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("sumReceiptSaleDetail"));
                ImportField discountSumReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("discountSumReceiptSaleDetail"));
                ImportField discountSumSaleReceiptField = new ImportField(LM.findLCPByCompoundName("discountSumSaleReceipt"));

                ImportField quantityReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("quantityReceiptReturnDetail"));
                ImportField priceReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("priceReceiptReturnDetail"));
                ImportField retailSumReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("sumReceiptReturnDetail"));
                ImportField discountSumReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("discountSumReceiptReturnDetail"));
                ImportField discountSumReturnReceiptField = new ImportField(LM.findLCPByCompoundName("discountSumReturnReceipt"));

                ImportField sidTypePaymentField = new ImportField(LM.findLCPByCompoundName("sidPaymentType"));
                ImportField sumPaymentField = new ImportField(LM.findLCPByCompoundName("POS.sumPayment"));
                ImportField numberPaymentField = new ImportField(LM.findLCPByCompoundName("POS.numberPayment"));
                ImportField paymentMeansPaymentField = new ImportField(LM.baseLM.staticCaption);
                ImportField seriesNumberDiscountCardField = new ImportField(LM.findLCPByCompoundName("seriesNumberDiscountCard"));

                List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
                List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
                List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

                ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ZReport"), LM.findLCPByCompoundName("zReportNumberCashRegister").getMapping(zReportNumberField, cashRegisterField));
                ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CashRegister"), LM.findLCPByCompoundName("cashRegisterNumber").getMapping(cashRegisterField));
                ImportKey<?> receiptKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Receipt"), LM.findLCPByCompoundName("receiptZReportNumberCashRegister").getMapping(zReportNumberField, numberReceiptField, cashRegisterField));
                ImportKey<?> skuKey = new ImportKey((CustomClass) LM.findClassByCompoundName("Sku"), LM.findLCPByCompoundName("skuBarcodeIdDate").getMapping(idBarcodeReceiptDetailField, dateField));
                ImportKey<?> discountCardKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DiscountCard"), LM.findLCPByCompoundName("discountCardSeriesNumber").getMapping(seriesNumberDiscountCardField, dateField));

                saleProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
                saleProperties.add(new ImportProperty(cashRegisterField, LM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                        LM.baseLM.object(LM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
                saleProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
                saleProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

                saleProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
                saleProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
                saleProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
                saleProperties.add(new ImportProperty(discountSumSaleReceiptField, LM.findLCPByCompoundName("discountSumSaleReceipt").getMapping(receiptKey)));
                saleProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                        LM.baseLM.object(LM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
                saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("seriesNumberDiscountCard").getMapping(discountCardKey)));
                saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                        LM.baseLM.object(LM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

                ImportKey<?> receiptSaleDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ReceiptSaleDetail"), LM.findLCPByCompoundName("receiptDetailZReportReceiptNumberCashRegister").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
                saleProperties.add(new ImportProperty(numberReceiptDetailField, LM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(quantityReceiptSaleDetailField, LM.findLCPByCompoundName("quantityReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(priceReceiptSaleDetailField, LM.findLCPByCompoundName("priceReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(sumReceiptSaleDetailField, LM.findLCPByCompoundName("sumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(discountSumReceiptSaleDetailField, LM.findLCPByCompoundName("discountSumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
                saleProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptSaleDetailKey),
                        LM.baseLM.object(LM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

                saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("skuReceiptSaleDetail").getMapping(receiptSaleDetailKey),
                        LM.baseLM.object(LM.findClassByCompoundName("Sku")).getMapping(skuKey)));

                returnProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
                returnProperties.add(new ImportProperty(cashRegisterField, LM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                        LM.baseLM.object(LM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
                returnProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
                returnProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

                returnProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
                returnProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
                returnProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
                returnProperties.add(new ImportProperty(discountSumReturnReceiptField, LM.findLCPByCompoundName("discountSumReturnReceipt").getMapping(receiptKey)));
                returnProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                        LM.baseLM.object(LM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
                returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("seriesNumberDiscountCard").getMapping(discountCardKey)));
                returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                        LM.baseLM.object(LM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

                ImportKey<?> receiptReturnDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("ReceiptReturnDetail"), LM.findLCPByCompoundName("receiptDetailZReportReceiptNumberCashRegister").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
                returnProperties.add(new ImportProperty(numberReceiptDetailField, LM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(quantityReceiptReturnDetailField, LM.findLCPByCompoundName("quantityReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(priceReceiptReturnDetailField, LM.findLCPByCompoundName("priceReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(retailSumReceiptReturnDetailField, LM.findLCPByCompoundName("sumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(discountSumReceiptReturnDetailField, LM.findLCPByCompoundName("discountSumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
                returnProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptReturnDetailKey),
                        LM.baseLM.object(LM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

                returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("skuReceiptReturnDetail").getMapping(receiptReturnDetailKey),
                        LM.baseLM.object(LM.findClassByCompoundName("Sku")).getMapping(skuKey)));

                List<ImportField> saleImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                        numberReceiptField, numberReceiptDetailField, idBarcodeReceiptDetailField, quantityReceiptSaleDetailField,
                        priceReceiptSaleDetailField, sumReceiptSaleDetailField, discountSumReceiptSaleDetailField,
                        discountSumSaleReceiptField, seriesNumberDiscountCardField);

                List<ImportField> returnImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                        numberReceiptField, numberReceiptDetailField, idBarcodeReceiptDetailField, quantityReceiptReturnDetailField,
                        priceReceiptReturnDetailField, retailSumReceiptReturnDetailField, discountSumReceiptReturnDetailField,
                        discountSumReturnReceiptField, seriesNumberDiscountCardField);

                new IntegrationService(session, new ImportTable(saleImportFields, dataSale), Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptSaleDetailKey, skuKey, discountCardKey),
                        saleProperties).synchronize(true);

                new IntegrationService(session, new ImportTable(returnImportFields, dataReturn), Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptReturnDetailKey, skuKey, discountCardKey),
                        returnProperties).synchronize(true);

                ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("POS.Payment"), LM.findLCPByCompoundName("paymentZReportReceiptNumberCashRegister").getMapping(zReportNumberField, numberReceiptField, numberPaymentField, cashRegisterField));
                ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("PaymentType"), LM.findLCPByCompoundName("typePaymentSID").getMapping(sidTypePaymentField));
                paymentProperties.add(new ImportProperty(sumPaymentField, LM.findLCPByCompoundName("POS.sumPayment").getMapping(paymentKey)));
                paymentProperties.add(new ImportProperty(numberPaymentField, LM.findLCPByCompoundName("numberPayment").getMapping(paymentKey)));
                paymentProperties.add(new ImportProperty(sidTypePaymentField, LM.findLCPByCompoundName("paymentTypePayment").getMapping(paymentKey),
                        LM.baseLM.object(LM.findClassByCompoundName("PaymentType")).getMapping(paymentTypeKey)));
                paymentProperties.add(new ImportProperty(paymentMeansPaymentField, LM.findLCPByCompoundName("POS.paymentMeansPayment").getMapping(paymentKey)));
                paymentProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptPayment").getMapping(paymentKey),
                        LM.baseLM.object(LM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

                List<ImportField> paymentImportFields = Arrays.asList(zReportNumberField, numberReceiptField, cashRegisterField, sumPaymentField,
                        paymentMeansPaymentField, sidTypePaymentField, numberPaymentField);

                new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, receiptKey, cashRegisterKey),
                        paymentProperties).synchronize(true);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static Object getTagValue(String sTag, Element eElement, int type) {
        Node elem = eElement.getElementsByTagName(sTag).item(0);
        if (elem == null)
            return null;
        else {
            NodeList nlList = elem.getChildNodes();
            Node nValue = nlList.item(0);
            String value = nValue.getNodeValue();
            switch (type) {
                case 0:
                    return value;
                case 1:
                    return Integer.parseInt(value);
                case 2:
                    return Double.parseDouble(value);
                case 3:
                    return Long.parseLong(value);
                default:
                    return value.trim();
            }
        }
    }
}