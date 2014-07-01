package lsfusion.erp.retail;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public abstract class ExportReceiptsZReportActionProperty extends ScriptingActionProperty {

    // Опциональные модули
    ScriptingLogicsModule POSVostrovLM;
    ScriptingLogicsModule itemArticleLM;
    ScriptingLogicsModule zReportDiscountCardLM;
    ScriptingLogicsModule zReportRetailCRMLM;

    protected final ClassPropertyInterface zReportInterface;

    public ExportReceiptsZReportActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClassByCompoundName("ZReport"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        zReportInterface = i.next();
    }

    public void export(ExecutionContext<ClassPropertyInterface> context, DataObject zReportObject, String filePath, boolean customPath) {

        this.POSVostrovLM = (ScriptingLogicsModule) context.getBL().getModule("POSVostrov");
        this.itemArticleLM = (ScriptingLogicsModule) context.getBL().getModule("ItemArticle");
        this.zReportDiscountCardLM = (ScriptingLogicsModule) context.getBL().getModule("ZReportDiscountCard");
        this.zReportRetailCRMLM = (ScriptingLogicsModule) context.getBL().getModule("ZReportRetailCRM");
        
        if (filePath == null && !customPath)
            return;

        try {

            List<DataObject> receiptObjectsList = new ArrayList<DataObject>();

            DataSession session = context.getSession();

            String numberZReport = (String) getLCP("numberZReport").read(session, zReportObject);
            numberZReport = numberZReport == null ? "" : numberZReport.trim();
            
            KeyExpr receiptExpr = new KeyExpr("receipt");
            ImRevMap<Object, KeyExpr> receiptKeys = MapFact.singletonRev((Object) "receipt", receiptExpr);

            String[] receiptNames = new String[]{"numberReceipt", "dateTimeReceipt", "discountSumReceipt", "noteReceipt"};
            LCP<?>[] receiptProperties = getLCPs("numberReceipt", "dateTimeReceipt", "discountSumReceipt", "noteReceipt");
            QueryBuilder<Object, Object> receiptQuery = new QueryBuilder<Object, Object>(receiptKeys);
            for (int j = 0; j < receiptProperties.length; j++) {
                receiptQuery.addProperty(receiptNames[j], receiptProperties[j].getExpr(session.getModifier(), receiptExpr));
            }
            if (zReportDiscountCardLM != null)
                receiptQuery.addProperty("numberDiscountCardReceipt", zReportDiscountCardLM.findLCPByCompoundOldName("numberDiscountCardReceipt").getExpr(session.getModifier(), receiptExpr));

            if (POSVostrovLM != null)
                receiptQuery.addProperty("isInvoiceReceipt", POSVostrovLM.findLCPByCompoundOldName("isInvoiceReceipt").getExpr(session.getModifier(), receiptExpr));

            receiptQuery.and(getLCP("zReportReceipt").getExpr(session.getModifier(), receiptQuery.getMapExprs().get("receipt")).compare(zReportObject.getExpr(), Compare.EQUALS));
            receiptQuery.and(getLCP("exportReceipt").getExpr(session.getModifier(), receiptQuery.getMapExprs().get("receipt")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptResult = receiptQuery.execute(session);

            if (receiptResult.size() == 0)
                return;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("zReport");
            doc.appendChild(rootElement);

            for (int i = 0, size = receiptResult.size(); i < size; i++) {
                DataObject receiptObject = new DataObject(receiptResult.getKey(i).get("receipt"), (ConcreteClass) LM.findClassByCompoundName("Receipt"));
                getLCP("exportReceipt").change((Object) null, session.getSession(), receiptObject);

                Element receipt = doc.createElement("receipt");
                rootElement.appendChild(receipt);

                KeyExpr receiptDetailExpr = new KeyExpr("receiptDetail");
                ImRevMap<Object, KeyExpr> receiptDetailKeys = MapFact.singletonRev((Object) "receiptDetail", receiptDetailExpr);

                String[] receiptDetailNames = new String[]{"typeReceiptDetail", "quantityReceiptSaleDetail",
                        "quantityReceiptReturnDetail", "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                        "discountSumReceiptDetail", "discountPercentReceiptSaleDetail", "skuReceiptDetail"};
                LCP[] receiptDetailProperties = getLCPs("typeReceiptDetail", "quantityReceiptSaleDetail",
                        "quantityReceiptReturnDetail", "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                        "discountSumReceiptDetail", "discountPercentReceiptSaleDetail", "skuReceiptDetail");
                QueryBuilder<Object, Object> receiptDetailQuery = new QueryBuilder<Object, Object>(receiptDetailKeys);
                for (int j = 0; j < receiptDetailProperties.length; j++) {
                    receiptDetailQuery.addProperty(receiptDetailNames[j], receiptDetailProperties[j].getExpr(session.getModifier(), receiptDetailExpr));
                }
                receiptDetailQuery.and(getLCP("receiptReceiptDetail").getExpr(session.getModifier(), receiptDetailExpr).compare(receiptObject.getExpr(), Compare.EQUALS));
                ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> receiptDetailResult = receiptDetailQuery.executeClasses(session.getSession());

                int numberReceiptDetail = 1;
                for (int j = 0, sizeResult = receiptDetailResult.size(); j < sizeResult; j++) {
                    DataObject receiptDetailObject = receiptDetailResult.getKey(j).valueIt().iterator().next();
                    Element receiptDetail = doc.createElement("receiptDetail");
                    rootElement.appendChild(receiptDetail);

                    String[] fields = new String[]{"typeReceiptDetail", "priceReceiptDetail", "quantityReceiptSaleDetail",
                            "quantityReceiptReturnDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                            "discountSumReceiptDetail", "discountPercentReceiptSaleDetail", "numberReceiptDetail",
                            "articleReceiptDetail", "idItemReceiptDetail"};

                    ObjectValue skuReceiptDetail = receiptDetailResult.getValue(j).get("skuReceiptDetail");
                    for (String field : fields) {
                        Object value = null;
                        if (field.equals("numberReceiptDetail")) {
                            value = numberReceiptDetail;
                            numberReceiptDetail++;
                        } else if (field.equals("idItemReceiptDetail")) {
                            if(itemArticleLM != null) {
                                value = itemArticleLM.findLCPByCompoundOldName("idItem").read(context, skuReceiptDetail);
                            }
                        } else if (field.equals("articleReceiptDetail")) {
                            if (itemArticleLM != null) {
                                value = skuReceiptDetail instanceof NullValue ? null : itemArticleLM.findLCPByCompoundOldName("idArticle").read(context,
                                        itemArticleLM.findLCPByCompoundOldName("articleItem").readClasses(context, (DataObject) skuReceiptDetail));
                            }
                        } else value = receiptDetailResult.getValue(j).get(field).getValue();
                        if (value != null) {
                            Element element = doc.createElement(field.replace("Sale", "").replace("Return", ""));
                            element.appendChild(doc.createTextNode(String.valueOf(value).trim()));
                            receiptDetail.appendChild(element);
                        }
                    }

                    if (zReportRetailCRMLM != null) {

                        KeyExpr promotionConditionExpr = new KeyExpr("promotionCondition");
                        ImRevMap<Object, KeyExpr> promotionConditionKeys = MapFact.singletonRev((Object) "promotionCondition", promotionConditionExpr);
                        QueryBuilder<Object, Object> promotionConditionQuery = new QueryBuilder<Object, Object>(promotionConditionKeys);
                        String[] receiptDetailPromotionConditionProperties = new String[]{"quantityReceiptSaleDetailPromotionCondition",
                                "promotionSumReceiptSaleDetailPromotionCondition", "setUserPromotionReceiptSaleDetailPromotionCondition"};
                        for (String pcProperty : receiptDetailPromotionConditionProperties) {
                            promotionConditionQuery.addProperty(pcProperty, zReportRetailCRMLM.findLCPByCompoundOldName(pcProperty).getExpr(session.getModifier(), receiptDetailObject.getExpr(), promotionConditionExpr));
                        }
                        promotionConditionQuery.addProperty("idPromotionCondition", zReportRetailCRMLM.findLCPByCompoundOldName("idPromotionCondition").getExpr(session.getModifier(), promotionConditionExpr));
                        promotionConditionQuery.addProperty("namePromotionPromotionCondition", zReportRetailCRMLM.findLCPByCompoundOldName("namePromotionPromotionCondition").getExpr(session.getModifier(), promotionConditionExpr));
                        promotionConditionQuery.and(zReportRetailCRMLM.findLCPByCompoundOldName("receiptReceiptDetail").getExpr(session.getModifier(), receiptDetailObject.getExpr()).compare(receiptObject.getExpr(), Compare.EQUALS));
                        promotionConditionQuery.and(zReportRetailCRMLM.findLCPByCompoundOldName("quantityReceiptSaleDetailPromotionCondition").getExpr(receiptDetailObject.getExpr(), promotionConditionExpr).getWhere());
                        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> promotionConditionResult = promotionConditionQuery.execute(session);

                        for (int z = 0, sizeReceiptDetailResult = promotionConditionResult.size(); z < sizeReceiptDetailResult; z++) {
                            Element promotionCondition = doc.createElement("promotionCondition");

                            String[] promotionConditionFields = new String[]{"idPromotionCondition", "namePromotionPromotionCondition",
                                    "quantityReceiptSaleDetailPromotionCondition", "promotionSumReceiptSaleDetailPromotionCondition"};
                            for (String field : promotionConditionFields) {
                                Object value = promotionConditionResult.getValue(z).get(field);
                                if (value != null) {
                                    Element element = doc.createElement(field.replace("Sale", "").replace("Return", ""));
                                    element.appendChild(doc.createTextNode(String.valueOf(value).trim()));
                                    promotionCondition.appendChild(element);
                                }
                            }
                            receiptDetail.appendChild(promotionCondition);
                        }
                        receipt.appendChild(receiptDetail);
                        
                    }
                }

                KeyExpr paymentExpr = new KeyExpr("payment");
                ImRevMap<Object, KeyExpr> paymentKeys = MapFact.singletonRev((Object) "payment", paymentExpr);

                QueryBuilder<Object, Object> paymentQuery = new QueryBuilder<Object, Object>(paymentKeys);
                paymentQuery.addProperty("sumPayment", getLCP("sumPayment").getExpr(session.getModifier(), paymentExpr));
                paymentQuery.addProperty("paymentMeansPayment", getLCP("namePaymentMeansPayment").getExpr(session.getModifier(), paymentExpr));
                paymentQuery.addProperty("sidPaymentTypePayment", getLCP("sidPaymentTypePayment").getExpr(session.getModifier(), paymentExpr));

                paymentQuery.and(getLCP("receiptPayment").getExpr(session.getModifier(), paymentQuery.getMapExprs().get("payment")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> paymentResult = paymentQuery.execute(session);
                for (ImMap<Object, Object> paymentValues : paymentResult.valueIt()) {
                    Element payment = doc.createElement("payment");
                    rootElement.appendChild(payment);

                    String[] fields = new String[]{"sumPayment", "paymentMeansPayment", "sidPaymentTypePayment"};
                    for (String field : fields) {
                        Object value = paymentValues.get(field);
                        if (value != null) {
                            Element element = doc.createElement(field);
                            element.appendChild(doc.createTextNode(String.valueOf(value).trim()));
                            payment.appendChild(element);
                        }
                    }
                    receipt.appendChild(payment);
                }

                String[] fields = new String[]{"numberReceipt", "dateTimeReceipt", "discountSumReceipt", 
                        "numberDiscountCardReceipt", "noteReceipt", "isInvoiceReceipt"};
                for (String field : fields) {
                    Object value;
                    if (field.equals("isInvoiceReceipt"))
                        value = receiptResult.getValue(i).get(field) == null ? "НЕТ" : "ДА";
                    else
                        value = receiptResult.getValue(i).get(field);
                    if (value != null) {
                        Element element = doc.createElement(field);
                        if (value instanceof Timestamp)
                            element.appendChild(doc.createTextNode(String.valueOf(((Timestamp) value).getTime()).trim()));
                        else
                            element.appendChild(doc.createTextNode(String.valueOf(value).trim()));
                        receipt.appendChild(element);
                    }
                }
                rootElement.appendChild(receipt);

                receiptObjectsList.add(receiptObject);
            }

            // write the content into xml file
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            String xmlString = sw.toString();

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Calendar cal = Calendar.getInstance();
            String fileName = "export-" + numberZReport + "-" + dateFormat.format(cal.getTime());

            File file = filePath == null ? File.createTempFile(fileName, ".xml") : new File(filePath + "//" + fileName + ".xml");
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(file), "UTF8"));
            writer.println(xmlString);
            writer.close();
            if (customPath) {
                context.delayUserInterfaction(new ExportFileClientAction(fileName + ".xml", IOUtils.getFileBytes(file)));
            }

            for (DataObject receiptObject : receiptObjectsList)
                getLCP("exportedIncrementReceipt").change(true, context, receiptObject);

            session.apply(context);

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


}
