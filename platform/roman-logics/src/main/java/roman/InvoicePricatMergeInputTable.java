package roman;

import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.context.ThreadLocalContext;
import platform.server.data.Modify;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.Type;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcPropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.SingleKeyTableUsage;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

/**
 * User: DAle
 * Date: 22.04.11
 * Time: 15:03
 */

public class InvoicePricatMergeInputTable implements ImportInputTable {
    private ImportInputTable invoiceTable;
    private RomanLogicsModule RomanLM;
    private List<List<String>> data = new ArrayList<List<String>>();

    public static enum ResultField {BARCODE, ARTICLE, INVOICE, BOXNUMBER, COLORCODE, COLOR, SIZE, ORIGINALNAME,
        COUNTRY, NETWEIGHT, COMPOSITION, PRICE, DATE, RRP, QUANTITY, NUMBERSKU, CUSTOMCODE, CUSTOMCODE6,
        SEASON, GENDER, BRANDCODE, BRANDNAME, THEMECODE, THEMENAME, SUBCATEGORYCODE, SUBCATEGORYNAME, DESTINATION}

    public InvoicePricatMergeInputTable(RomanLogicsModule RomanLM, ImportInputTable invoiceTable, ResultField... invoiceFields) {
        this.RomanLM = RomanLM;
        this.invoiceTable = invoiceTable;

        List<ResultField> invoiceFieldsList = Arrays.asList(invoiceFields);
        Map<ResultField, Integer> invoiceFieldsPos = new HashMap<ResultField, Integer>();
        for (ResultField field : invoiceFieldsList) {
            invoiceFieldsPos.put(field, invoiceFieldsList.indexOf(field));
        }
        assert invoiceFieldsPos.containsKey(ResultField.BARCODE) && invoiceFieldsPos.containsKey(ResultField.INVOICE) &&
               invoiceFieldsPos.containsKey(ResultField.BOXNUMBER);

        Map<ResultField, LCP> propertyMap = createPricatFieldsMap();

        try {
            Map<String, Map<ResultField, Object>> pricatData = getDataFromPricat(invoiceFieldsList, propertyMap);
            int barcodeInvoiceIndex = invoiceFieldsList.indexOf(ResultField.BARCODE);

            //сливаем таблицу инвойса и данные из прайса
            for (int i = 0; i < invoiceTable.rowsCnt(); i++) {
                List<String> row = new ArrayList<String>();
                String barcode = transformBarcode(invoiceTable.getCellString(i, barcodeInvoiceIndex).trim());
                boolean pricatContainsBarcode = pricatData.containsKey(barcode);

                for (ResultField field : ResultField.values()) {
                    String value = "";
                    if (invoiceFieldsPos.containsKey(field) && !invoiceTable.getCellString(i, invoiceFieldsPos.get(field)).trim().equals("")) {
                        value = invoiceTable.getCellString(i, invoiceFieldsPos.get(field));
                    } else if (pricatContainsBarcode && pricatData.get(barcode).containsKey(field)) {
                        Object data = pricatData.get(barcode).get(field);
                        value = (data == null ? "" : data.toString());
                    }
                    row.add(value);
                }

                data.add(row);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<ResultField, LCP> createPricatFieldsMap() {
        Map<ResultField, LCP> propertyMap = new OrderedMap<ResultField, LCP>();

        propertyMap.put(ResultField.BARCODE, RomanLM.barcodePricat);
        propertyMap.put(ResultField.ARTICLE, RomanLM.articleNumberPricat);
        propertyMap.put(ResultField.CUSTOMCODE, RomanLM.customCategoryOriginalPricat);
        propertyMap.put(ResultField.COLORCODE, RomanLM.colorCodePricat);
        propertyMap.put(ResultField.COLOR, RomanLM.colorNamePricat);
        propertyMap.put(ResultField.SIZE, RomanLM.sizePricat);
        propertyMap.put(ResultField.ORIGINALNAME, RomanLM.originalNamePricat);
        propertyMap.put(ResultField.COUNTRY, RomanLM.countryPricat);
        propertyMap.put(ResultField.NETWEIGHT, RomanLM.netWeightPricat);
        propertyMap.put(ResultField.COMPOSITION, RomanLM.compositionPricat);
        propertyMap.put(ResultField.PRICE, RomanLM.pricePricat);
        propertyMap.put(ResultField.RRP, RomanLM.rrpPricat);
        propertyMap.put(ResultField.SEASON, RomanLM.seasonPricat);
        propertyMap.put(ResultField.GENDER, RomanLM.genderPricat);
        propertyMap.put(ResultField.BRANDNAME, RomanLM.brandNamePricat);
        propertyMap.put(ResultField.BRANDCODE, RomanLM.brandNamePricat);
        propertyMap.put(ResultField.THEMECODE, RomanLM.themeCodePricat);
        propertyMap.put(ResultField.THEMENAME, RomanLM.themeNamePricat);
        propertyMap.put(ResultField.SUBCATEGORYCODE, RomanLM.subCategoryCodePricat);
        propertyMap.put(ResultField.SUBCATEGORYNAME, RomanLM.subCategoryNamePricat);
        propertyMap.put(ResultField.DESTINATION, RomanLM.destinationPricat);

        return propertyMap;
    }

    private String transformBarcode(String barcode) {
        if (barcode.length() > 13) {
            return barcode.substring(barcode.length() - 13);
        }
        return barcode;
    }

    private Map<String, Map<ResultField, Object>> getDataFromPricat(List<ResultField> invoiceFields,
                                                                     Map<ResultField, LCP> propertyMap) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        // вообще этот метод вызывается в стеке ActionProperty.execute(ExecutionContext context),
        // поэтому можно бы сделать просто context.getDBManager(), просто надо зарефакторить
        SQLSession sqlSession = ThreadLocalContext.getDbManager().createSQL();

        //читаем данные из прайса по импортированным из инвойса штрихкодам
        Type keyType = RomanLM.barcode.property.getType();
        SingleKeyTableUsage<ResultField> table = new SingleKeyTableUsage<ResultField>(keyType, SetFact.<ResultField>EMPTYORDER(), null);

        for (int i = 0; i < invoiceTable.rowsCnt(); i++) {
            String barcodeStr = transformBarcode(invoiceTable.getCellString(i, invoiceFields.indexOf(ResultField.BARCODE)));
            table.modifyRecord(sqlSession, new DataObject(barcodeStr), MapFact.<ResultField, ObjectValue>EMPTY(), Modify.MODIFY);
        }

        ImRevMap<PropertyInterface, KeyExpr> mapKeys = RomanLM.barcodePricat.getMapKeys();
        QueryBuilder<PropertyInterface, ResultField> query = new QueryBuilder<PropertyInterface, ResultField>(mapKeys);
        query.and(table.join(RomanLM.barcodePricat.property.getExpr(mapKeys)).getWhere());

        for (ResultField propertyName : propertyMap.keySet()) {
            CalcPropertyImplement propertyImplement = propertyMap.get(propertyName).getMapping(mapKeys.singleValue());
            query.addProperty(propertyName, ((LCP<PropertyInterface>) propertyMap.get(propertyName)).property.getExpr(propertyImplement.mapping));
        }

        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<ResultField, Object>> result = query.execute(sqlSession);
        Map<String, Map<ResultField, Object>> pricatResult = new HashMap<String, Map<ResultField, Object>>();
        for (ImMap<PropertyInterface, Object> key : result.keyIt()) {
            ImMap<ResultField, Object> value = result.get(key);
            pricatResult.put((String) value.get(ResultField.BARCODE), value.toJavaMap());
        }
        return pricatResult;
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        if (data.isEmpty()) return 0;
        return data.get(0).size();
    }
}
