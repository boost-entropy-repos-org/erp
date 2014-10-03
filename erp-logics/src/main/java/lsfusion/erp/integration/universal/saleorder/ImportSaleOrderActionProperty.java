package lsfusion.erp.integration.universal.saleorder;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import lsfusion.base.IOUtils;
import lsfusion.erp.integration.universal.ImportColumnDetail;
import lsfusion.erp.integration.universal.ImportDocumentActionProperty;
import lsfusion.erp.integration.universal.ImportDocumentSettings;
import lsfusion.erp.integration.universal.UniversalImportException;
import lsfusion.erp.stock.BarcodeUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.integration.*;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ImportSaleOrderActionProperty extends ImportDocumentActionProperty {
    private final ClassPropertyInterface orderInterface;

    // Опциональные модули
    ScriptingLogicsModule saleManufacturingPriceLM;

    public ImportSaleOrderActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClass("Sale.Order"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        orderInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataSession session = context.getSession();

            DataObject orderObject = context.getDataKeyValue(orderInterface);

            ObjectValue importTypeObject = findProperty("importTypeOrder").readClasses(session, orderObject);

            if (!(importTypeObject instanceof NullValue)) {

                ObjectValue operationObject = findProperty("autoImportOperationImportType").readClasses(session, (DataObject) importTypeObject);
                ObjectValue supplierObject = findProperty("autoImportSupplierImportType").readClasses(session, (DataObject) importTypeObject);
                ObjectValue supplierStockObject = findProperty("autoImportSupplierStockImportType").readClasses(session, (DataObject) importTypeObject);
                ObjectValue customerObject = findProperty("autoImportCustomerImportType").readClasses(session, (DataObject) importTypeObject);
                ObjectValue customerStockObject = findProperty("autoImportCustomerStockImportType").readClasses(session, (DataObject) importTypeObject);

                Map<String, ImportColumnDetail> importColumns = readImportColumns(session, importTypeObject).get(0);
                ImportDocumentSettings settings = readImportDocumentSettings(session, importTypeObject);
                String fileExtension = settings.getFileExtension();

                if (importColumns != null && fileExtension != null) {

                    CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, fileExtension + " Files", fileExtension);
                    ObjectValue objectValue = context.requestUserData(valueClass, null);
                    if (objectValue != null) {
                        List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                        for (byte[] file : fileList) {

                            makeImport(context.getBL(), session, orderObject, importColumns, file, settings, fileExtension, 
                                    operationObject, supplierObject, supplierStockObject, customerObject, customerStockObject);

                            session.apply(context);
                            
                            findAction("formRefresh").execute(context);
                        }
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BiffException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (UniversalImportException e) {
            e.printStackTrace();
            context.requestUserInteraction(new MessageClientAction(e.getMessage(), e.getTitle()));
        }
    }

    public boolean makeImport(BusinessLogics BL, DataSession session, DataObject orderObject, Map<String, ImportColumnDetail> importColumns,
                              byte[] file, ImportDocumentSettings settings, String fileExtension, ObjectValue operationObject, ObjectValue supplierObject,
                              ObjectValue supplierStockObject, ObjectValue customerObject, ObjectValue customerStockObject)
            throws ParseException, IOException, SQLException, BiffException, xBaseJException, ScriptingErrorLog.SemanticErrorException, UniversalImportException, SQLHandledException {

        this.saleManufacturingPriceLM = BL.getModule("SaleManufacturingPrice");

        List<List<SaleOrderDetail>> orderDetailsList = importOrdersFromFile(session, (Integer) orderObject.object,
                importColumns, file, fileExtension, settings.getStartRow(), settings.isPosted(), settings.getSeparator(),
                settings.getPrimaryKeyType(), settings.isCheckExistence(), settings.getSecondaryKeyType(), settings.isKeyIsDigit());

        boolean importResult1 = (orderDetailsList != null && orderDetailsList.size() >= 1) && importOrders(orderDetailsList.get(0),
                BL, session, orderObject, importColumns, settings.getPrimaryKeyType(), operationObject, supplierObject, supplierStockObject,
                customerObject, customerStockObject);

        boolean importResult2 = (orderDetailsList != null && orderDetailsList.size() >= 2) && importOrders(orderDetailsList.get(1),
                BL, session, orderObject, importColumns, settings.getSecondaryKeyType(), operationObject, supplierObject, supplierStockObject,
                customerObject, customerStockObject);

        findAction("formRefresh").execute(session);

        return importResult1 && importResult2;
    }

    public boolean importOrders(List<SaleOrderDetail> orderDetailsList, BusinessLogics BL, DataSession session,
                                DataObject orderObject, Map<String, ImportColumnDetail> importColumns, String keyType,
                                ObjectValue operationObject, ObjectValue supplierObject, ObjectValue supplierStockObject,
                                ObjectValue customerObject, ObjectValue customerStockObject)
            throws SQLException, ScriptingErrorLog.SemanticErrorException, IOException, xBaseJException, ParseException, BiffException, SQLHandledException {

        if (orderDetailsList != null && !orderDetailsList.isEmpty()) {

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

            List<List<Object>> data = initData(orderDetailsList.size());

            if (showField(orderDetailsList, "numberDocument")) {
                addDataField(props, fields, importColumns, findProperty("Sale.numberUserOrder"), "numberDocument", orderObject);
                addDataField(props, fields, importColumns, findProperty("Sale.numberUserOrder"), "numberDocument", orderObject);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("numberDocument"));
            }

            if (showField(orderDetailsList, "dateDocument")) {
                addDataField(props, fields, importColumns, findProperty("Sale.dateUserOrder"), "dateDocument", orderObject);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("dateDocument"));
            }

            ImportField idUserOrderDetailField = new ImportField(findProperty("Sale.idUserOrderDetail"));
            ImportKey<?> orderDetailKey = new ImportKey((CustomClass) findClass("Sale.UserOrderDetail"),
                    findProperty("Sale.userOrderDetailId").getMapping(idUserOrderDetailField));
            keys.add(orderDetailKey);
            props.add(new ImportProperty(idUserOrderDetailField, findProperty("Sale.idUserOrderDetail").getMapping(orderDetailKey)));
            props.add(new ImportProperty(orderObject, findProperty("Sale.userOrderUserOrderDetail").getMapping(orderDetailKey)));
            fields.add(idUserOrderDetailField);
            for (int i = 0; i < orderDetailsList.size(); i++)
                data.get(i).add(orderDetailsList.get(i).getFieldValue("idDocumentDetail"));

            if (operationObject instanceof DataObject)
                props.add(new ImportProperty((DataObject) operationObject, findProperty("Sale.operationUserOrder").getMapping(orderObject)));

            if (supplierObject instanceof DataObject) {
                props.add(new ImportProperty((DataObject) supplierObject, findProperty("Sale.supplierUserOrderDetail").getMapping(orderDetailKey)));
                props.add(new ImportProperty((DataObject) supplierObject, findProperty("Sale.supplierUserOrder").getMapping(orderObject)));
            }

            if (supplierStockObject instanceof DataObject) {
                props.add(new ImportProperty((DataObject) supplierStockObject, findProperty("Sale.supplierStockUserOrderDetail").getMapping(orderDetailKey)));
                props.add(new ImportProperty((DataObject) supplierStockObject, findProperty("Sale.supplierStockUserOrder").getMapping(orderObject)));
            }

            if (customerObject instanceof DataObject) {
                props.add(new ImportProperty((DataObject) customerObject, findProperty("Sale.customerUserOrderDetail").getMapping(orderDetailKey)));
                props.add(new ImportProperty((DataObject) customerObject, findProperty("Sale.customerUserOrder").getMapping(orderObject)));
            }

            if (customerStockObject instanceof DataObject) {
                props.add(new ImportProperty((DataObject) customerStockObject, findProperty("Sale.customerStockUserOrderDetail").getMapping(orderDetailKey)));
                props.add(new ImportProperty((DataObject) customerStockObject, findProperty("Sale.customerStockUserOrder").getMapping(orderObject)));
            }

            ImportField idBarcodeSkuField = new ImportField(findProperty("idBarcodeSku"));
            ImportKey<?> barcodeKey = new ImportKey((ConcreteCustomClass) findClass("Barcode"),
                    findProperty("extBarcodeId").getMapping(idBarcodeSkuField));
            keys.add(barcodeKey);
            barcodeKey.skipKey = true;
            fields.add(idBarcodeSkuField);
            for (int i = 0; i < orderDetailsList.size(); i++)
                data.get(i).add(orderDetailsList.get(i).getFieldValue("barcodeItem"));

            ImportField idBatchField = new ImportField(findProperty("idBatch"));
            ImportKey<?> batchKey = new ImportKey((CustomClass) findClass("Batch"),
                    findProperty("batchId").getMapping(idBatchField));
            props.add(new ImportProperty(idBatchField, findProperty("Sale.batchUserOrderDetail").getMapping(orderDetailKey),
                    object(findClass("Batch")).getMapping(batchKey)));
            fields.add(idBatchField);
            for (int i = 0; i < orderDetailsList.size(); i++)
                data.get(i).add(orderDetailsList.get(i).getFieldValue("idBatch"));

            ImportField dataIndexOrderDetailField = new ImportField(findProperty("Sale.dataIndexUserOrderDetail"));
            props.add(new ImportProperty(dataIndexOrderDetailField, findProperty("Sale.dataIndexUserOrderDetail").getMapping(orderDetailKey)));
            fields.add(dataIndexOrderDetailField);
            for (int i = 0; i < orderDetailsList.size(); i++)
                data.get(i).add(orderDetailsList.get(i).getFieldValue("dataIndex"));

            ImportField idItemField = new ImportField(findProperty("idItem"));
            fields.add(idItemField);
            for (int i = 0; i < orderDetailsList.size(); i++)
                data.get(i).add(orderDetailsList.get(i).getFieldValue("idItem"));

            LCP iGroupAggr = getItemKeyGroupAggr(keyType);
            ImportField iField = (keyType == null || keyType.equals("item")) ? idItemField : keyType.equals("barcode") ? idBarcodeSkuField : idBatchField;
            ImportKey<?> itemKey = new ImportKey((CustomClass) findClass("Item"),
                    iGroupAggr.getMapping(iField));
            keys.add(itemKey);
            itemKey.skipKey = true;
            props.add(new ImportProperty(iField, findProperty("Sale.skuUserOrderDetail").getMapping(orderDetailKey),
                    object(findClass("Sku")).getMapping(itemKey)));
            props.add(new ImportProperty(iField, findProperty("skuBarcode").getMapping(barcodeKey),
                    object(findClass("Item")).getMapping(itemKey)));

            if (showField(orderDetailsList, "idManufacturer")) {
                ImportField idManufacturerField = new ImportField(findProperty("idManufacturer"));
                ImportKey<?> manufacturerKey = new ImportKey((ConcreteCustomClass) findClass("Manufacturer"),
                        findProperty("manufacturerId").getMapping(idManufacturerField));
                keys.add(manufacturerKey);
                props.add(new ImportProperty(idManufacturerField, findProperty("idManufacturer").getMapping(manufacturerKey)));
                props.add(new ImportProperty(idManufacturerField, findProperty("manufacturerItem").getMapping(itemKey),
                        object(findClass("Manufacturer")).getMapping(manufacturerKey), getReplaceOnlyNull(importColumns, "idManufacturer")));
                fields.add(idManufacturerField);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("idManufacturer"));
            }

            if (showField(orderDetailsList, "idCustomer")) {
                ImportField idCustomerField = new ImportField(findProperty("idLegalEntity"));
                ImportKey<?> customerKey = new ImportKey((ConcreteCustomClass) findClass("LegalEntity"),
                        findProperty("legalEntityId").getMapping(idCustomerField));
                keys.add(customerKey);
                props.add(new ImportProperty(idCustomerField, findProperty("Sale.customerUserOrder").getMapping(orderObject),
                        object(findClass("LegalEntity")).getMapping(customerKey), getReplaceOnlyNull(importColumns, "idCustomer")));
                fields.add(idCustomerField);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("idCustomer"));
            }

            if (showField(orderDetailsList, "idCustomerStock")) {
                ImportField idCustomerStockField = new ImportField(findProperty("idStock"));
                ImportKey<?> customerStockKey = new ImportKey((CustomClass) findClass("Stock"),
                        findProperty("stockId").getMapping(idCustomerStockField));
                keys.add(customerStockKey);
                props.add(new ImportProperty(idCustomerStockField, findProperty("Sale.customerStockUserOrder").getMapping(orderObject),
                        object(findClass("Stock")).getMapping(customerStockKey), getReplaceOnlyNull(importColumns, "idCustomerStock")));
                fields.add(idCustomerStockField);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("idCustomerStock"));
            }

            if (showField(orderDetailsList, "quantity")) {
                addDataField(props, fields, importColumns, findProperty("Sale.quantityUserOrderDetail"), "quantity", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("quantity"));
            }

            if (showField(orderDetailsList, "price")) {
                addDataField(props, fields, importColumns, findProperty("Sale.priceUserOrderDetail"), "price", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("price"));
            }

            if (showField(orderDetailsList, "sum")) {
                addDataField(props, fields, importColumns, findProperty("Sale.sumUserOrderDetail"), "sum", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("sum"));
            }

            if (showField(orderDetailsList, "valueVAT")) {
                ImportField valueVATOrderDetailField = new ImportField(findProperty("Sale.valueVATUserOrderDetail"));
                ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) findClass("Range"),
                        findProperty("valueCurrentVATDefaultValue").getMapping(valueVATOrderDetailField));
                keys.add(VATKey);
                props.add(new ImportProperty(valueVATOrderDetailField, findProperty("Sale.VATUserOrderDetail").getMapping(orderDetailKey),
                        object(findClass("Range")).getMapping(VATKey), getReplaceOnlyNull(importColumns, "valueVAT")));
                fields.add(valueVATOrderDetailField);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("valueVAT"));
            }

            if (showField(orderDetailsList, "sumVAT")) {
                addDataField(props, fields, importColumns, findProperty("Sale.VATSumUserOrderDetail"), "sumVAT", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("sumVAT"));
            }

            if (showField(orderDetailsList, "invoiceSum")) {
                addDataField(props, fields, importColumns, findProperty("Sale.invoiceSumUserOrderDetail"), "invoiceSum", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("invoiceSum"));
            }

            if ((saleManufacturingPriceLM != null) && showField(orderDetailsList, "manufacturingPrice")) {
                addDataField(props, fields, importColumns, saleManufacturingPriceLM.findProperty("Sale.manufacturingPriceUserOrderDetail"), "manufacturingPrice", orderDetailKey);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).getFieldValue("manufacturingPrice"));
            }

            if (showField(orderDetailsList, "isPosted")) {
                addDataField(props, fields, importColumns, findProperty("Sale.isPostedUserOrder"), "isPosted", orderObject);
                for (int i = 0; i < orderDetailsList.size(); i++)
                    data.get(i).add(orderDetailsList.get(i).isPosted);
            }

            ImportTable table = new ImportTable(fields, data);

            session.pushVolatileStats("SOA_OR");
            IntegrationService service = new IntegrationService(session, table, keys, props);
            service.synchronize(true, false);
            String result = session.applyMessage(BL);
            session.popVolatileStats();

            return result == null;
        }
        return false;
    }

    public List<List<SaleOrderDetail>> importOrdersFromFile(DataSession session, Integer orderObject, Map<String, ImportColumnDetail> importColumns,
                                                            byte[] file, String fileExtension, Integer startRow, Boolean isPosted, String separator,
                                                            String primaryKeyType, boolean checkExistence, String secondaryKeyType, boolean keyIsDigit)
            throws ParseException, UniversalImportException, IOException, SQLException, xBaseJException, ScriptingErrorLog.SemanticErrorException, BiffException, SQLHandledException {

        List<List<SaleOrderDetail>> orderDetailsList;

        List<String> stringFields = Arrays.asList("idDocumentDetail", "numberDocument", "idBatch", "barcodeItem", "idItem", "idManufacturer", "valueVAT", "idCustomerStock");
        
        List<String> bigDecimalFields = Arrays.asList("dataIndex", "quantity", "price", "sum", "sumVAT", "invoiceSum", "manufacturingPrice");

        List<String> dateFields = Arrays.asList("dateDocument");
        
        if (fileExtension.equals("DBF"))
            orderDetailsList = importOrdersFromDBF(session, file, importColumns, stringFields, bigDecimalFields, dateFields, 
                    primaryKeyType, checkExistence, secondaryKeyType, keyIsDigit, startRow, isPosted, orderObject);
        else if (fileExtension.equals("XLS"))
            orderDetailsList = importOrdersFromXLS(session, file, importColumns, stringFields, bigDecimalFields, dateFields,
                    primaryKeyType, checkExistence, secondaryKeyType, keyIsDigit, startRow, isPosted, orderObject);
        else if (fileExtension.equals("XLSX"))
            orderDetailsList = importOrdersFromXLSX(session, file, importColumns, stringFields, bigDecimalFields, dateFields,
                    primaryKeyType, checkExistence, secondaryKeyType, keyIsDigit, startRow, isPosted, orderObject);
        else if (fileExtension.equals("CSV") || fileExtension.equals("TXT"))
            orderDetailsList = importOrdersFromCSV(session, file, importColumns, stringFields, bigDecimalFields, dateFields,
                    primaryKeyType, checkExistence, secondaryKeyType, keyIsDigit, startRow, isPosted, separator, orderObject);
        else
            orderDetailsList = null;

        return orderDetailsList;
    }

    private List<List<SaleOrderDetail>> importOrdersFromXLS(DataSession session, byte[] importFile, Map<String, ImportColumnDetail> importColumns,
                                                            List<String> stringFields, List<String> bigDecimalFields, List<String> dateFields, 
                                                            String primaryKeyType, boolean checkExistence,  String secondaryKeyType, boolean keyIsDigit,
                                                            Integer startRow, Boolean isPosted, Integer orderObject)
            throws IOException, BiffException, UniversalImportException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<SaleOrderDetail> primaryList = new ArrayList<SaleOrderDetail>();
        List<SaleOrderDetail> secondaryList = new ArrayList<SaleOrderDetail>();

        String primaryKeyColumn = getItemKeyColumn(primaryKeyType);
        String secondaryKeyColumn = getItemKeyColumn(secondaryKeyType);
        
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding("cp1251");
        Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(importFile), ws);
        Sheet sheet = wb.getSheet(0);

        for (int i = startRow - 1; i < sheet.getRows(); i++) {
            Map<String, Object> fieldValues = new HashMap<String, Object>();
            for(String field : stringFields) {
                String value = getXLSFieldValue(sheet, i, importColumns.get(field));
                if(field.equals("idDocumentDetail"))
                    fieldValues.put(field, String.valueOf(orderObject) + i);
                else if(field.equals("valueVAT"))
                    fieldValues.put(field, VATifAllowed(parseVAT(value)));
                else if (field.equals("barcodeItem"))
                    fieldValues.put(field, BarcodeUtils.appendCheckDigitToBarcode(value, 7));
                else if(field.equals("idCustomerStock")) {
                    String idCustomer = readIdCustomer(session, value);
                    fieldValues.put(field, value);
                    fieldValues.put("idCustomer", idCustomer);
                } else
                    fieldValues.put(field, value);
            }
            for(String field : bigDecimalFields) {
                BigDecimal value = getXLSBigDecimalFieldValue(sheet, i, importColumns.get(field));
                if (field.equals("dataIndex"))
                    fieldValues.put(field, value == null ? (primaryList.size() + secondaryList.size() + 1) : value.intValue());
                else
                    fieldValues.put(field, value);
            }
            for(String field : dateFields) {
                fieldValues.put(field, getXLSDateFieldValue(sheet, i, importColumns.get(field)));
            }
            
            SaleOrderDetail saleOrderDetail = new SaleOrderDetail(fieldValues, isPosted);

            String primaryKeyColumnValue = getXLSFieldValue(sheet, i, importColumns.get(primaryKeyColumn));
            String secondaryKeyColumnValue = getXLSFieldValue(sheet, i, importColumns.get(secondaryKeyColumn));
            if (checkKeyColumnValue(primaryKeyColumn, primaryKeyColumnValue, keyIsDigit, session, primaryKeyType, checkExistence))
                primaryList.add(saleOrderDetail);
            else if (checkKeyColumnValue(secondaryKeyColumn, secondaryKeyColumnValue, keyIsDigit))
                primaryList.add(saleOrderDetail);
        }

        return Arrays.asList(primaryList, secondaryList);
    }

    private List<List<SaleOrderDetail>> importOrdersFromCSV(DataSession session, byte[] importFile, Map<String, ImportColumnDetail> importColumns,
                                                            List<String> stringFields, List<String> bigDecimalFields, List<String> dateFields, String primaryKeyType, boolean checkExistence, 
                                                            String secondaryKeyType, boolean keyIsDigit, Integer startRow, Boolean isPosted, String separator, Integer orderObject)
            throws UniversalImportException, ScriptingErrorLog.SemanticErrorException, SQLException, IOException, SQLHandledException {

        List<SaleOrderDetail> primaryList = new ArrayList<SaleOrderDetail>();
        List<SaleOrderDetail> secondaryList = new ArrayList<SaleOrderDetail>();

        String primaryKeyColumn = getItemKeyColumn(primaryKeyType);
        String secondaryKeyColumn = getItemKeyColumn(secondaryKeyType);
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(importFile)));
        String line;
        
        List<String[]> valuesList = new ArrayList<String[]>();
        while ((line = br.readLine()) != null) {
            valuesList.add(line.split(separator));              
        }

        for (int count = startRow; count <= valuesList.size(); count++) {
            Map<String, Object> fieldValues = new HashMap<String, Object>();
            for(String field : stringFields) {
                String value = getCSVFieldValue(valuesList, importColumns.get(field), count);
                if(field.equals("idDocumentDetail"))
                    fieldValues.put(field, String.valueOf(orderObject) + count);
                else if(field.equals("valueVAT"))
                    fieldValues.put(field, VATifAllowed(parseVAT(value)));
                else if (field.equals("barcodeItem"))
                    fieldValues.put(field, BarcodeUtils.appendCheckDigitToBarcode(value, 7));
                else if(field.equals("idCustomerStock")) {
                    String idCustomer = readIdCustomer(session, value);
                    fieldValues.put(field, value);
                    fieldValues.put("idCustomer", idCustomer);
                } else
                    fieldValues.put(field, value);
            }
            for(String field : bigDecimalFields) {
                BigDecimal value = getCSVBigDecimalFieldValue(valuesList, importColumns.get(field), count);
                if (field.equals("dataIndex"))
                    fieldValues.put(field, value == null ? (primaryList.size() + secondaryList.size() + 1) : value.intValue());
                else
                    fieldValues.put(field, value);
            }
            for(String field : dateFields) {
                fieldValues.put(field, getCSVDateFieldValue(valuesList, importColumns.get(field), count));
            }
            
            SaleOrderDetail saleOrderDetail = new SaleOrderDetail(fieldValues, isPosted);

            String primaryKeyColumnValue = getCSVFieldValue(valuesList, importColumns.get(primaryKeyColumn), count);
            String secondaryKeyColumnValue = getCSVFieldValue(valuesList, importColumns.get(secondaryKeyColumn), count);
            if (checkKeyColumnValue(primaryKeyColumn, primaryKeyColumnValue, keyIsDigit, session, primaryKeyType, checkExistence))
                primaryList.add(saleOrderDetail);
            else if (checkKeyColumnValue(secondaryKeyColumn, secondaryKeyColumnValue, keyIsDigit))
                secondaryList.add(saleOrderDetail);

        }

        return Arrays.asList(primaryList, secondaryList);
    }

    private List<List<SaleOrderDetail>> importOrdersFromXLSX(DataSession session, byte[] importFile, Map<String, ImportColumnDetail> importColumns,
                                                             List<String> stringFields, List<String> bigDecimalFields, List<String> dateFields, 
                                                             String primaryKeyType, boolean checkExistence, String secondaryKeyType, boolean keyIsDigit, 
                                                             Integer startRow, Boolean isPosted, Integer orderObject)
            throws IOException, UniversalImportException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<SaleOrderDetail> primaryList = new ArrayList<SaleOrderDetail>();
        List<SaleOrderDetail> secondaryList = new ArrayList<SaleOrderDetail>();

        String primaryKeyColumn = getItemKeyColumn(primaryKeyType);
        String secondaryKeyColumn = getItemKeyColumn(secondaryKeyType);
        
        XSSFWorkbook Wb = new XSSFWorkbook(new ByteArrayInputStream(importFile));
        XSSFSheet sheet = Wb.getSheetAt(0);

        for (int i = startRow - 1; i <= sheet.getLastRowNum(); i++) {
            Map<String, Object> fieldValues = new HashMap<String, Object>();
            for(String field : stringFields) {
                String value = getXLSXFieldValue(sheet, i, importColumns.get(field));
                if(field.equals("idDocumentDetail"))
                    fieldValues.put(field, String.valueOf(orderObject) + i);
                else if(field.equals("valueVAT"))
                    fieldValues.put(field, VATifAllowed(parseVAT(value)));
                else if (field.equals("barcodeItem"))
                    fieldValues.put(field, BarcodeUtils.appendCheckDigitToBarcode(value, 7));
                else if(field.equals("idCustomerStock")) {
                    String idCustomer = readIdCustomer(session, value);
                    fieldValues.put(field, value);
                    fieldValues.put("idCustomer", idCustomer);
                } else
                    fieldValues.put(field, value);
            }
            for(String field : bigDecimalFields) {
                BigDecimal value = getXLSXBigDecimalFieldValue(sheet, i, importColumns.get(field));
                if (field.equals("dataIndex"))
                    fieldValues.put(field, value == null ? (primaryList.size() + secondaryList.size() + 1) : value.intValue());
                else
                    fieldValues.put(field, value);
            }
            for(String field : dateFields) {
               fieldValues.put(field, getXLSXFieldValue(sheet, i, importColumns.get(field))); 
            }                       

            SaleOrderDetail saleOrderDetail = new SaleOrderDetail(fieldValues, isPosted);

            String primaryKeyColumnValue = getXLSXFieldValue(sheet, i, importColumns.get(primaryKeyColumn));
            String secondaryKeyColumnValue = getXLSXFieldValue(sheet, i, importColumns.get(secondaryKeyColumn));
            if (checkKeyColumnValue(primaryKeyColumn, primaryKeyColumnValue, keyIsDigit, session, primaryKeyType, checkExistence))
                primaryList.add(saleOrderDetail);
            else if (checkKeyColumnValue(secondaryKeyColumn, secondaryKeyColumnValue, keyIsDigit))
                secondaryList.add(saleOrderDetail);
        }

        return Arrays.asList(primaryList, secondaryList);
    }

    private List<List<SaleOrderDetail>> importOrdersFromDBF(DataSession session, byte[] importFile, Map<String, ImportColumnDetail> importColumns,
                                                            List<String> stringFields, List<String> bigDecimalFields, List<String> dateFields, 
                                                            String primaryKeyType, boolean checkExistence, String secondaryKeyType, boolean keyIsDigit, 
                                                            Integer startRow, Boolean isPosted, Integer orderObject)
            throws IOException, xBaseJException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException, UniversalImportException, SQLHandledException {

        List<SaleOrderDetail> primaryList = new ArrayList<SaleOrderDetail>();
        List<SaleOrderDetail> secondaryList = new ArrayList<SaleOrderDetail>();

        String primaryKeyColumn = getItemKeyColumn(primaryKeyType);
        String secondaryKeyColumn = getItemKeyColumn(secondaryKeyType);

        File tempFile = File.createTempFile("saleOrder", ".dbf");
        IOUtils.putFileBytes(tempFile, importFile);

        DBF file = new DBF(tempFile.getPath());
        String charset = getDBFCharset(tempFile);

        int totalRecordCount = file.getRecordCount();

        for (int i = 0; i < startRow - 1; i++) {
            file.read();
        }
        
        for (int i = startRow - 1; i < totalRecordCount; i++) {
            Map<String, Object> fieldValues = new HashMap<String, Object>();
            
            file.read();

            for(String field : stringFields) {
                String value = getDBFFieldValue(file, importColumns.get(field), i, charset);
                if(field.equals("idDocumentDetail"))
                    fieldValues.put(field, String.valueOf(orderObject) + i);
                else if(field.equals("valueVAT"))
                    fieldValues.put(field, VATifAllowed(parseVAT(value)));
                else if (field.equals("barcodeItem"))
                    fieldValues.put(field, BarcodeUtils.appendCheckDigitToBarcode(value, 7));
                else if(field.equals("idCustomerStock")) {
                    String idCustomer = readIdCustomer(session, value);
                    fieldValues.put(field, value);
                    fieldValues.put("idCustomer", idCustomer);
                } else
                    fieldValues.put(field, value);
            }
            for(String field : bigDecimalFields) {
                BigDecimal value = getDBFBigDecimalFieldValue(file, importColumns.get(field), i, charset);
                if (field.equals("dataIndex"))
                    fieldValues.put(field, value == null ? (primaryList.size() + secondaryList.size() + 1) : value.intValue());
                else
                    fieldValues.put(field, value);
            }
            for(String field : dateFields) {
                fieldValues.put(field, getDBFDateFieldValue(file, importColumns.get(field), i, charset));
            }           

            SaleOrderDetail saleOrderDetail = new SaleOrderDetail(fieldValues, isPosted);

            String primaryKeyColumnValue = getDBFFieldValue(file, importColumns.get(primaryKeyColumn), i, charset);
            String secondaryKeyColumnValue = getDBFFieldValue(file, importColumns.get(secondaryKeyColumn), i, charset);
            if (checkKeyColumnValue(primaryKeyColumn, primaryKeyColumnValue, keyIsDigit, session, primaryKeyType, checkExistence))
                primaryList.add(saleOrderDetail);
            else if (checkKeyColumnValue(secondaryKeyColumn, secondaryKeyColumnValue, keyIsDigit))
                secondaryList.add(saleOrderDetail);
        }
        file.close();
        tempFile.delete();

        return Arrays.asList(primaryList, secondaryList);
    }

    private String readIdCustomer(DataSession session, String idCustomerStock) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        ObjectValue customerStockObject = idCustomerStock == null ? null : findProperty("stockId").readClasses(session, new DataObject(idCustomerStock));
        ObjectValue customerObject = ((customerStockObject == null || customerStockObject instanceof NullValue) ? null : findProperty("legalEntityStock").readClasses(session, (DataObject) customerStockObject));
        return (String) (customerObject == null ? null : findProperty("idLegalEntity").read(session, customerObject));
    }

    protected Boolean showField(List<SaleOrderDetail> data, String fieldName) {
        try {

            boolean found = false;
            Field fieldValues = SaleOrderDetail.class.getField("fieldValues");
            for (SaleOrderDetail entry : data) {
                Map<String, Object> values = (Map<String, Object>) fieldValues.get(entry);
                if(!found) {
                    if (values.containsKey(fieldName))
                        found = true;
                    else
                        break;
                }
                if (values.get(fieldName) != null)
                    return true;
            }

            if(!found) {
                Field field = SaleOrderDetail.class.getField(fieldName);

                for (SaleOrderDetail entry : data) {
                    if (field.get(entry) != null)
                        return true;
                }
            }
        } catch (NoSuchFieldException e) {
            return true;
        } catch (IllegalAccessException e) {
            return true;
        }
        return false;
    }
}