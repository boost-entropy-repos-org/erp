package lsfusion.erp.region.by.integration.excel;

import com.google.common.base.Throwables;
import jxl.write.WriteException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.DateClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportExcelUserInvoicesActionProperty extends ExportExcelActionProperty {
    private final ClassPropertyInterface dateFromInterface;
    private final ClassPropertyInterface dateToInterface;

    public ExportExcelUserInvoicesActionProperty(ScriptingLogicsModule LM) {
        super(LM, DateClass.instance, DateClass.instance);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        dateFromInterface = i.next();
        dateToInterface = i.next();
    }

    public ExportExcelUserInvoicesActionProperty(ScriptingLogicsModule LM, ClassPropertyInterface dateFrom, ClassPropertyInterface dateTo) {
        super(LM, DateClass.instance, DateClass.instance);

        dateFromInterface = dateFrom;
        dateToInterface = dateTo;
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportUserInvoices", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Серия", "Номер", "Дата", "Код товара", "Кол-во", "Поставщик", "Склад покупателя",
                "Склад поставщика", "Цена", "Цена услуг", "Розничная цена", "Розничная надбавка",
                "Оптовая цена", "Оптовая надбавка", "Сертификат");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        ScriptingLogicsModule pricingPurchaseLM = (ScriptingLogicsModule) context.getBL().getModule("PricingPurchase");
        ScriptingLogicsModule purchaseInvoiceWholesaleLM = (ScriptingLogicsModule) context.getBL().getModule("PurchaseInvoiceWholesalePrice");

        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {

            DataObject dateFromObject = context.getDataKeyValue(dateFromInterface);
            DataObject dateToObject = context.getDataKeyValue(dateToInterface);

            KeyExpr userInvoiceExpr = new KeyExpr("UserInvoice");
            ImRevMap<Object, KeyExpr> userInvoiceKeys = MapFact.singletonRev((Object) "UserInvoice", userInvoiceExpr);

            String[] userInvoiceProperties = new String[]{"seriesUserInvoice", "numberUserInvoice",
                    "Purchase.dateUserInvoice", "supplierUserInvoice", "Purchase.customerStockInvoice", "Purchase.supplierStockInvoice"};
            QueryBuilder<Object, Object> userInvoiceQuery = new QueryBuilder<Object, Object>(userInvoiceKeys);
            for (String uiProperty : userInvoiceProperties) {
                userInvoiceQuery.addProperty(uiProperty, getLCP(uiProperty).getExpr(context.getModifier(), userInvoiceExpr));
            }
            userInvoiceQuery.and(getLCP("numberUserInvoice").getExpr(context.getModifier(), userInvoiceQuery.getMapExprs().get("UserInvoice")).getWhere());
            userInvoiceQuery.and(getLCP("Purchase.dateUserInvoice").getExpr(context.getModifier(), userInvoiceQuery.getMapExprs().get("UserInvoice")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> userInvoiceResult = userInvoiceQuery.execute(session);

            for (int i = 0, size = userInvoiceResult.size(); i < size; i++) {
                DataObject userInvoiceObject = new DataObject(userInvoiceResult.getKey(i).get("UserInvoice"), (ConcreteClass) LM.findClassByCompoundName("UserInvoice"));

                Date date = (Date) userInvoiceResult.getValue(i).get("Purchase.dateUserInvoice");

                if ((dateFromObject == null || date.after((Date) dateFromObject.object)) && (dateToObject == null || date.before((Date) dateToObject.object))) {
                    ImMap<Object, Object> userInvoiceValue = userInvoiceResult.getValue(i);

                    String seriesUserInvoice = trim((String) userInvoiceValue.get("seriesUserInvoice"), "");
                    String numberUserInvoice = trim((String) userInvoiceValue.get("numberUserInvoice"), "");
                    String dateInvoice = date == null ? null : new SimpleDateFormat("dd.MM.yyyy").format(date);

                    Integer supplierID = (Integer) userInvoiceValue.get("supplierUserInvoice");
                    Integer customerStockID = (Integer) userInvoiceValue.get("Purchase.customerStockInvoice");
                    Integer supplierStockID = (Integer) userInvoiceValue.get("Purchase.supplierStockInvoice");

                    KeyExpr userInvoiceDetailExpr = new KeyExpr("UserInvoiceDetail");
                    ImRevMap<Object, KeyExpr> userInvoiceDetailKeys = MapFact.singletonRev((Object) "UserInvoiceDetail", userInvoiceDetailExpr);

                    QueryBuilder<Object, Object> userInvoiceDetailQuery = new QueryBuilder<Object, Object>(userInvoiceDetailKeys);
                    String[] userInvoiceDetailProperties = new String[]{"Purchase.idBarcodeSkuInvoiceDetail", "quantityUserInvoiceDetail",
                            "priceUserInvoiceDetail", "Purchase.chargePriceUserInvoiceDetail", "certificateTextInvoiceDetail"};
                    for (String uidProperty : userInvoiceDetailProperties) {
                        userInvoiceDetailQuery.addProperty(uidProperty, getLCP(uidProperty).getExpr(context.getModifier(), userInvoiceDetailExpr));
                    }

                    if (purchaseInvoiceWholesaleLM != null) {
                        String[] purchaseInvoiceWholesaleUserInvoiceDetailProperties = new String[]{"Purchase.wholesalePriceUserInvoiceDetail", "Purchase.wholesaleMarkupUserInvoiceDetail"};
                        for (String uidProperty : purchaseInvoiceWholesaleUserInvoiceDetailProperties) {
                            userInvoiceDetailQuery.addProperty(uidProperty, purchaseInvoiceWholesaleLM.findLCPByCompoundOldName(uidProperty).getExpr(context.getModifier(), userInvoiceDetailExpr));
                        }
                    }

                    if (pricingPurchaseLM != null) {
                        String[] pricingPurchaseUserInvoiceDetailProperties = new String[]{"Purchase.retailPriceUserInvoiceDetail", "Purchase.retailMarkupUserInvoiceDetail"};
                        for (String uidProperty : pricingPurchaseUserInvoiceDetailProperties) {
                            userInvoiceDetailQuery.addProperty(uidProperty, pricingPurchaseLM.findLCPByCompoundOldName(uidProperty).getExpr(context.getModifier(), userInvoiceDetailExpr));
                        }
                    }

                    userInvoiceDetailQuery.and(getLCP("userInvoiceUserInvoiceDetail").getExpr(context.getModifier(), userInvoiceDetailQuery.getMapExprs().get("UserInvoiceDetail")).compare(userInvoiceObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> userInvoiceDetailResult = userInvoiceDetailQuery.execute(context);

                    for (ImMap<Object, Object> userInvoiceDetailValues : userInvoiceDetailResult.valueIt()) {

                        String idBarcodeSkuInvoiceDetail = trim((String) userInvoiceDetailValues.get("Purchase.idBarcodeSkuInvoiceDetail"), "");
                        BigDecimal quantityUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("quantityUserInvoiceDetail");
                        BigDecimal priceUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("priceUserInvoiceDetail");
                        BigDecimal chargePriceUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("Purchase.chargePriceUserInvoiceDetail");
                        BigDecimal retailPriceUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("Purchase.retailPriceUserInvoiceDetail");
                        BigDecimal retailMarkupUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("Purchase.retailMarkupUserInvoiceDetail");
                        BigDecimal wholesalePriceUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("Purchase.wholesalePriceUserInvoiceDetail");
                        BigDecimal wholesaleMarkupUserInvoiceDetail = (BigDecimal) userInvoiceDetailValues.get("Purchase.wholesaleMarkupUserInvoiceDetail");
                        String certificateTextInvoiceDetail = trim((String) userInvoiceDetailValues.get("certificateTextInvoiceDetail"), "");

                        data.add(Arrays.asList(seriesUserInvoice, numberUserInvoice, dateInvoice, idBarcodeSkuInvoiceDetail, 
                                formatValue(quantityUserInvoiceDetail), formatValue(supplierID), formatValue(customerStockID),
                                formatValue(supplierStockID), formatValue(priceUserInvoiceDetail), formatValue(chargePriceUserInvoiceDetail),
                                formatValue(retailPriceUserInvoiceDetail), formatValue(retailMarkupUserInvoiceDetail),
                                formatValue(wholesalePriceUserInvoiceDetail), formatValue(wholesaleMarkupUserInvoiceDetail),
                                certificateTextInvoiceDetail));
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return data;
    }

    /*
            Arrays.asList(Arrays.asList("AA", "12345678", "12.12.2012", "1111", "150", "ПС0010325", "4444", "3333",
            "5000", "300", "7000", "30", "№123456789")));
            */



}