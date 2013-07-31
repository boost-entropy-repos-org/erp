package lsfusion.erp.region.by.integration.vetraz;

import lsfusion.erp.integration.*;
import lsfusion.server.session.ApplyFilter;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.integration.*;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportVetrazActionProperty extends ScriptingActionProperty {

    public ImportVetrazActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            Integer numberOfItems = (Integer) getLCP("importNumberItems").read(context);
            Integer numberOfUserInvoices = (Integer) getLCP("importNumberUserInvoices").read(context);
            Boolean withoutRecalc = (Boolean) getLCP("withoutRecalcVetraz").read(context);

            Object pathObject = getLCP("importVetrazDirectory").read(context);
            String path = pathObject == null ? "" : ((String) pathObject).trim();
            if (!path.isEmpty()) {

                ImportData importData = new ImportData();

                importData.setWithoutRecalc(withoutRecalc);

                importData.setLegalEntitiesList((getLCP("importLegalEntities").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//sprana.dbf") : null);

                importData.setWarehousesList((getLCP("importWarehouses").read(context) != null) ?
                        importWarehousesFromDBF(path + "//sprana.dbf") : null);

                importData.setItemGroupsList((getLCP("importItems").read(context) != null) ?
                        importItemGroupsFromDBF(false) : null);

                importData.setParentGroupsList((getLCP("importItems").read(context) != null) ?
                        importItemGroupsFromDBF(true) : null);

                importData.setUOMsList((getLCP("importUOMs").read(context) != null) ?
                        importUOMsFromDBF(path + "//sprmat.dbf") : null);

                importData.setItemsList((getLCP("importItems").read(context) != null) ?
                        importItemsFromDBF(path + "//sprmat.dbf", numberOfItems) : null);

                importData.setUserInvoicesList((getLCP("importUserInvoices").read(context) != null) ?
                        importUserInvoicesFromDBF(path + "//sprmat.dbf", path + "//cen.dbf", path + "//ostt.dbf",
                                numberOfUserInvoices) : null);

                if (getLCP("importUserInvoices").read(context) != null)
                    importUserInvoicePharmacy(context, path + "//sprmat.dbf", path + "//ostt.dbf",
                            numberOfUserInvoices, withoutRecalc);

                new ImportActionProperty(LM, importData, context).makeImport();

                if ((getLCP("importItems").read(context) != null))
                    importItemPharmacy(context, path + "//sprmat.dbf", numberOfItems);

            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private List<ItemGroup> importItemGroupsFromDBF(Boolean parents) throws IOException, xBaseJException {
        List<ItemGroup> data = new ArrayList<ItemGroup>();
        String groupTop = "ВСЕ";
        data.add(new ItemGroup(groupTop, parents ? null : groupTop, null));
        return data;
    }

    List<Double> allowedVAT = Arrays.asList(0.0, 9.09, 16.67, 10.0, 20.0, 24.0);

    private List<UOM> importUOMsFromDBF(String itemsPath) throws IOException, xBaseJException, ParseException {

        checkFileExistence(itemsPath);

        List<UOM> data = new ArrayList<UOM>();

        DBF itemsImportFile = new DBF(itemsPath);
        int recordCount = itemsImportFile.getRecordCount();

        for (int i = 0; i < recordCount; i++) {
            itemsImportFile.read();

            String UOM = getFieldValue(itemsImportFile, "K_IZM", "Cp866", null);
            data.add(new UOM(UOM, UOM, UOM));
        }
        return data;
    }

    private List<Item> importItemsFromDBF(String itemsPath, Integer numberOfItems) throws IOException, xBaseJException, ParseException {

        checkFileExistence(itemsPath);

        List<Item> data = new ArrayList<Item>();

        DBF itemsImportFile = new DBF(itemsPath);
        int totalRecordCount = itemsImportFile.getRecordCount();
        int recordCount = (numberOfItems != null && numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            itemsImportFile.read();

            String idBarcode = getFieldValue(itemsImportFile, "K_GRUP", "Cp866", null);
            String captionItem = getFieldValue(itemsImportFile, "POL_NAIM", "Cp866", null);
            String idItem = idBarcode + captionItem;

            String idUOM = getFieldValue(itemsImportFile, "K_IZM", "Cp866", null);
            BigDecimal retailVAT = getBigDecimalFieldValue(itemsImportFile, "NDSR", "Cp866", null);
            String idManufacturer = getFieldValue(itemsImportFile, "DOPPRIM", "Cp866", null);
            String nameCountry = "БЕЛАРУСЬ";
            String codeCustomsGroup = getFieldValue(itemsImportFile, "DPRM1", "Cp866", null);
            Date date = getDateFieldValue(itemsImportFile, "DATPR1", "Cp866", null);
            BigDecimal amountPack = getBigDecimalFieldValue(itemsImportFile, "N_PER2", "Cp866", null);
            BigDecimal weightItem = getBigDecimalFieldValue(itemsImportFile, "N_PER3", "Cp866", null);

            if (!idItem.trim().isEmpty())
                data.add(new Item(idItem, "ВСЕ", captionItem, idUOM, null, null, nameCountry, idBarcode, idBarcode,
                        date, null, weightItem, weightItem, null, allowedVAT.contains(retailVAT.doubleValue()) ? retailVAT : null,
                        null, null, null, null, null, null, idItem, amountPack, idManufacturer, idManufacturer, codeCustomsGroup, nameCountry));
        }
        return data;
    }

    private List<LegalEntity> importLegalEntitiesFromDBF(String path) throws
            IOException, xBaseJException {

        checkFileExistence(path);

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        String nameCountry = "БЕЛАРУСЬ";

        data.add(new LegalEntity("sle", "Стандартная Организация", null, null, null,
                null, null, null, null, null, null, null, null, nameCountry, null, true, null));

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String idLegalEntity = getFieldValue(importFile, "K_ANA", "Cp866", null);
            String nameLegalEntity = getFieldValue(importFile, "POL_NAIM", "Cp866", "");
            String addressLegalEntity = getFieldValue(importFile, "DPRA1", "Cp866", null);
            String unpLegalEntity = getFieldValue(importFile, "PRIM", "Cp866", null);
            String okpoLegalEntity = getFieldValue(importFile, "DPRIM", "Cp866", null);
            String numberAccount = getFieldValue(importFile, "DPRA4", "Cp866", null);
            String[] ownership = getAndTrimOwnershipFromName(nameLegalEntity);
            String type = getFieldValue(importFile, "K_VAN", "Cp866", null);
            Boolean isSupplier = "ПС".equals(type);
            if (isSupplier && !idLegalEntity.isEmpty())
                data.add(new LegalEntity(idLegalEntity, ownership[2], addressLegalEntity, unpLegalEntity,
                        okpoLegalEntity, null, null, ownership[1], ownership[0], numberAccount, null, null, null,
                        nameCountry, true, null, null));
        }
        return data;
    }

    private List<Warehouse> importWarehousesFromDBF(String spranaPath) throws
            IOException, xBaseJException {

        checkFileExistence(spranaPath);

        DBF importFile = new DBF(spranaPath);
        int recordCount = importFile.getRecordCount();

        List<Warehouse> data = new ArrayList<Warehouse>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_ana = getFieldValue(importFile, "K_ANA", "Cp866", null);
            String nameWarehouse = getFieldValue(importFile, "POL_NAIM", "Cp866", "");
            String addressWarehouse = getFieldValue(importFile, "DPRA1", "Cp866", null);
            String type = getFieldValue(importFile, "K_VAN", "Cp866", null);
            Boolean isWarehouse = "СК".equals(type);
            Boolean isSupplier = "ПС".equals(type);

            if (!nameWarehouse.isEmpty()) {
                if (isWarehouse)
                    data.add(new Warehouse("sle", null, k_ana, nameWarehouse, addressWarehouse));
                if (isSupplier)
                    data.add(new Warehouse(k_ana, null, k_ana + "WH", nameWarehouse, addressWarehouse));
            }
        }
        return data;
    }

    String[] declarationPatterns = new String[]{"№?((?:\\d|\\/)*)от(\\d{2}\\.\\d{2}\\.\\d{2,4})"};
    String[] compliancePatterns = new String[]{"(сертификат)", "(#)",
            "№?((?:\\p{L}|\\d|\\.)*)от(\\d{2}\\.\\d{2}\\.\\d{2})",
            "№?\\s?((?:\\p{L}|-|\\d|\\.)*)()\\s?(?:до|по)\\s?(\\d{2}\\.\\d{2}\\.\\d{2,4})",
            "№?((?:\\p{L}|\\d|\\.)*)(\\d{2}\\.\\d{2}\\.\\d{2})",
            "(\\p{L}{2}-?\\s?(?:\\d|\\.)*)", "№?((?:\\p{L}|-|\\s|\\d|\\.)*)"};
    String[] datePatterns = new String[]{"dd.MM.yy", "dd.MM.yyyy"};

    private List<UserInvoiceDetail> importUserInvoicesFromDBF(String sprmatPath, String cenPath, String osttPath, Integer numberOfUserInvoices) throws IOException, xBaseJException, ParseException {

        checkFileExistence(sprmatPath);
        checkFileExistence(cenPath);
        checkFileExistence(osttPath);

        Map<String, String> currenciesMap = new HashMap<String, String>() {
            {
                put("BGL", "BGN");
                put("BGN", "BGN");
                put("ВGN", "BGN");//В кириллическая
                put("BYR", "BLR");
                put("BYB", "BLR");
                put("ВYR", "BLR"); //В кириллическая
                put("CHF", "CHF");
                put("CZK", "CZK");
                put("СZK", "CZK");//С кириллическая
                put("EUR", "EUR");
                put("EURO", "EUR");
                put("ЕВРО", "EUR");
                put("GBP", "GBP");
                put("HUF", "HUF");
                put("INR", "INR");
                put("LVL", "LVL");
                put("MDL", "MDL");
                put("PLN", "PLN");
                put("PLZ", "PLN");
                put("RON", "RON");
                put("RUR", "RUB");
                put("RUB", "RUB");
                put("RYR", "RUB");
                put("UAH", "UAH");
                put("UAN", "UAH");
                put("USD", "USD");
                put("ДОЛ", "USD");
            }
        };

        DBF cenImportFile = new DBF(cenPath);
        int totalRecordCount = cenImportFile.getRecordCount();

        Map<String, Object[]> priceMap = new HashMap<String, Object[]>();

        for (int i = 0; i < totalRecordCount; i++) {
            cenImportFile.read();

            String k_mat = getFieldValue(cenImportFile, "K_MAT", "Cp866", "");
            BigDecimal price = getBigDecimalFieldValue(cenImportFile, "N_CENU", "Cp866", null);
            Date date = getDateFieldValue(cenImportFile, "D_CEN", "Cp866", null);

            if (!k_mat.isEmpty()) {
                Object[] value = priceMap.get(k_mat);
                if (value == null || (value != null && date != null && value[1] != null && ((Date) value[1]).before(date)))
                    priceMap.put(k_mat, new Object[]{price, date});
            }
        }

        DBF sprmatImportFile = new DBF(sprmatPath);
        totalRecordCount = sprmatImportFile.getRecordCount();

        Map<String, Object[]> sprmatMap = new HashMap<String, Object[]>();

        for (int i = 0; i < totalRecordCount; i++) {

            sprmatImportFile.read();

            String k_mat = getFieldValue(sprmatImportFile, "K_MAT", "Cp866", null);
            String k_group = getFieldValue(sprmatImportFile, "K_GRUP", "Cp866", null);
            String name = getFieldValue(sprmatImportFile, "POL_NAIM", "Cp866", null);
            String idItem = k_group + name;
            String numberUserInvoice = getFieldValue(sprmatImportFile, "POST_DOK", "Cp866", "Б\\Н");
            String idSupplier = getFieldValue(sprmatImportFile, "K_POST", "Cp866", null);
            Date date = getDateFieldValue(sprmatImportFile, "D_PRIH", "Cp866", null);
            String descriptionDeclaration = getFieldValue(sprmatImportFile, "DPRM4", "Cp866", "");
            String certificateText = getFieldValue(sprmatImportFile, "DPRM6", "Cp866", null);
            String descriptionCompliance = getFieldValue(sprmatImportFile, "DPRM7", "Cp866", "");
            Date expiryDate = getDateFieldValue(sprmatImportFile, "D_GODN", "Cp866", null);
            String bin = getFieldValue(sprmatImportFile, "DPRM9", "Cp866", null);
            String codeCustomsGroup = getFieldValue(sprmatImportFile, "DPRM1", "Cp866", null);
            BigDecimal retailVAT = getBigDecimalFieldValue(sprmatImportFile, "NDSR", "Cp866", null);

            BigDecimal n_zps = getBigDecimalFieldValue(sprmatImportFile, "N_ZPS", "Cp866", null);
            Boolean isForeign = n_zps != null && n_zps.doubleValue() > 0;

            String shortNameCurrency = isForeign ? null : "BLR";
            if (isForeign) {
                String currencyString = getFieldValue(sprmatImportFile, "DPRM3", "Cp866", "");
                for (Map.Entry<String, String> entry : currenciesMap.entrySet()) {
                    if (currencyString.toUpperCase().contains(entry.getKey())) {
                        shortNameCurrency = entry.getValue();
                        break;
                    }
                }
            }

            BigDecimal n_cenu = priceMap.containsKey(k_mat) ? (BigDecimal) priceMap.get(k_mat)[0] : null;
            BigDecimal price = isForeign ? n_zps : n_cenu;
            BigDecimal rateExchange = isForeign ? getBigDecimalFieldValue(sprmatImportFile, "DPRM12", "Cp866", null) : null;
            BigDecimal homePrice = isForeign ? ((price == null || rateExchange == null) ? null/*n_cenu*/ : price.multiply(rateExchange)) : null;
            BigDecimal dprm11 = getBigDecimalFieldValue(sprmatImportFile, "DPRM11", "Cp866", "0");
            BigDecimal priceDuty = isForeign ? dprm11.subtract(rateExchange == null ? price : price.multiply(rateExchange)) : null;
            BigDecimal manufacturingPrice = isForeign ? safeAdd(homePrice, priceDuty) : getBigDecimalFieldValue(sprmatImportFile, "NUMPR1", "Cp866", null);
            Boolean isHomeCurrency = isForeign ? true : null;

            String numberDeclaration = null;
            Date dateDeclaration = null;
            if (!descriptionDeclaration.isEmpty()) {
                for (String p : declarationPatterns) {
                    Pattern r = Pattern.compile(p);
                    Matcher m = r.matcher(descriptionDeclaration);
                    if (m.find()) {
                        numberDeclaration = m.group(1).trim();
                        try {
                            dateDeclaration = new Date(DateUtils.parseDate(m.group(2), datePatterns).getTime());
                        } catch (ParseException ignored) {
                        }
                        break;
                    }
                }
            }

            String numberCompliance = null;
            Date fromDateCompliance = null;
            Date toDateCompliance = null;
            if (!descriptionCompliance.isEmpty()) {
                for (String p : compliancePatterns) {
                    Pattern r = Pattern.compile(p);
                    Matcher m = r.matcher(descriptionCompliance);
                    if (m.find()) {
                        numberCompliance = m.group(1).trim();
                        try {
                            fromDateCompliance = (m.groupCount() >= 2 && !m.group(2).isEmpty()) ? new Date(DateUtils.parseDate(m.group(2), datePatterns).getTime()) : null;
                            toDateCompliance = (m.groupCount() >= 3 && !m.group(3).isEmpty()) ? new Date(DateUtils.parseDate(m.group(3), datePatterns).getTime()) : null;
                        } catch (ParseException ignored) {
                        }
                        break;
                    }
                }
            }

            if (!k_mat.isEmpty())
                sprmatMap.put(k_mat, new Object[]{numberUserInvoice, date, shortNameCurrency,
                        idSupplier, idItem, price, manufacturingPrice, certificateText, numberDeclaration,
                        dateDeclaration, numberCompliance, fromDateCompliance, toDateCompliance, expiryDate, bin,
                        rateExchange, isForeign, homePrice, priceDuty, isHomeCurrency, codeCustomsGroup, retailVAT});
        }


        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();

        DBF osttImportFile = new DBF(osttPath);
        totalRecordCount = osttImportFile.getRecordCount();

        int recordCount = (numberOfUserInvoices != null && numberOfUserInvoices != 0 && numberOfUserInvoices < totalRecordCount) ? numberOfUserInvoices : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {

            if (data.size() >= recordCount)
                break;

            osttImportFile.read();

            String k_mat = getFieldValue(osttImportFile, "K_MAT", "Cp866", "");
            BigDecimal quantity = getBigDecimalFieldValue(osttImportFile, "N_MAT", "Cp866", "0");
            BigDecimal shipmentSum = getBigDecimalFieldValue(osttImportFile, "N_SUM", "Cp866", null);
            String idCustomerStock = getFieldValue(osttImportFile, "K_SKL", "Cp866", "");
            idCustomerStock = idCustomerStock.isEmpty() ? null : ("СК" + idCustomerStock);

            Object[] sprmatEntry = sprmatMap.get(k_mat);

            String numberUserInvoice = sprmatEntry == null ? null : (String) sprmatEntry[0];
            Date date = sprmatEntry == null ? null : (Date) sprmatEntry[1];
            String shortNameCurrency = sprmatEntry == null ? null : (String) sprmatEntry[2];
            String idSupplier = sprmatEntry == null ? null : (String) sprmatEntry[3];
            String idItem = sprmatEntry == null ? null : (String) sprmatEntry[4];
            BigDecimal price = sprmatEntry == null ? null : (BigDecimal) sprmatEntry[5];
            BigDecimal manufacturingPrice = sprmatEntry == null ? null : (BigDecimal) sprmatEntry[6];
            String certificateText = sprmatEntry == null ? null : (String) sprmatEntry[7];
            String numberDeclaration = sprmatEntry == null ? null : (String) sprmatEntry[8];
            Date dateDeclaration = sprmatEntry == null ? null : (Date) sprmatEntry[9];
            String numberCompliance = sprmatEntry == null ? null : (String) sprmatEntry[10];
            Date fromDateCompliance = sprmatEntry == null ? null : (Date) sprmatEntry[11];
            Date toDateCompliance = sprmatEntry == null ? null : (Date) sprmatEntry[12];
            Date expiryDate = sprmatEntry == null ? null : (Date) sprmatEntry[13];
            String bin = sprmatEntry == null ? null : (String) sprmatEntry[14];
            BigDecimal rateExchange = sprmatEntry == null ? null : (BigDecimal) sprmatEntry[15];
            Boolean isForeign = sprmatEntry == null ? null : (Boolean) sprmatEntry[16];
            BigDecimal homePrice = sprmatEntry == null ? null : (isForeign ? (BigDecimal) sprmatEntry[17] : safeDivide(shipmentSum, quantity, 2));
            BigDecimal priceDuty = sprmatEntry == null ? null : (BigDecimal) sprmatEntry[18];
            Boolean isHomeCurrency = sprmatEntry == null ? null : (Boolean) sprmatEntry[19];
            String codeCustomsGroup = sprmatEntry == null ? null : (String) sprmatEntry[20];
            BigDecimal retailVAT = sprmatEntry == null ? null : (BigDecimal) sprmatEntry[21];

            if (sprmatEntry != null && quantity != null && quantity.doubleValue() != 0)
                data.add(new UserInvoiceDetail(numberUserInvoice + String.valueOf(date) + shortNameCurrency + idSupplier,
                        null, numberUserInvoice, null, true, k_mat, date, idItem, null, quantity,
                        idSupplier, idCustomerStock, idSupplier + "WH",
                        (price == null || price.doubleValue() == 0) ? null : price,
                        isForeign ? manufacturingPrice : homePrice, isForeign ? null : shipmentSum, null,
                        manufacturingPrice, null, null, null, null, certificateText, null, numberDeclaration,
                        dateDeclaration, numberCompliance, fromDateCompliance, toDateCompliance, expiryDate, bin,
                        rateExchange, homePrice, priceDuty, null, null, null, isHomeCurrency, shortNameCurrency,
                        codeCustomsGroup, allowedVAT.contains(retailVAT.doubleValue()) ? retailVAT : null));
        }

        return data;
    }

    private void importUserInvoicePharmacy(ExecutionContext context, String sprmatPath, String osttPath,
                                           Integer numberOfUserInvoices, Boolean withoutRecalc) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException, xBaseJException, ParseException {

        List<List<Object>> dataUserInvoicePharmacy = importUserInvoicePharmacyFromDBF(sprmatPath, osttPath, numberOfUserInvoices);

        if (dataUserInvoicePharmacy != null) {

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

            ImportField idUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.idUserInvoiceDetail"));
            ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.UserInvoiceDetail"),
                    LM.findLCPByCompoundName("Purchase.userInvoiceDetailId").getMapping(idUserInvoiceDetailField));
            keys.add(userInvoiceDetailKey);
            fields.add(idUserInvoiceDetailField);

            ImportField seriesPharmacyUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("Purchase.seriesPharmacyUserInvoiceDetail"));
            props.add(new ImportProperty(seriesPharmacyUserInvoiceDetailField, LM.findLCPByCompoundName("Purchase.seriesPharmacyUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
            fields.add(seriesPharmacyUserInvoiceDetailField);

            ImportTable table = new ImportTable(fields, dataUserInvoicePharmacy);

            DataSession session = context.createSession();
            if (withoutRecalc!=null)
                session.setApplyFilter(ApplyFilter.WITHOUT_RECALC);
            session.sql.pushVolatileStats(null);
            IntegrationService service = new IntegrationService(session, table, keys, props);
            service.synchronize(true, false);
            session.apply(context.getBL());
            session.sql.popVolatileStats(null);
            session.close();

        }
    }

    private List<List<Object>> importUserInvoicePharmacyFromDBF(String sprmatPath, String osttPath, Integer numberOfUserInvoices) throws IOException, xBaseJException, ParseException {

        checkFileExistence(sprmatPath);
        checkFileExistence(osttPath);

        DBF sprmatImportFile = new DBF(sprmatPath);
        int totalRecordCount = sprmatImportFile.getRecordCount();

        Map<String, Object[]> sprmatMap = new HashMap<String, Object[]>();

        for (int i = 0; i < totalRecordCount; i++) {

            sprmatImportFile.read();
            String k_mat = getFieldValue(sprmatImportFile, "K_MAT", "Cp866", null);
            String seriesPharmacyUserInvoice = getFieldValue(sprmatImportFile, "PRIM", "Cp866", null);

            if (!k_mat.isEmpty())
                sprmatMap.put(k_mat, new Object[]{seriesPharmacyUserInvoice});
        }

        List<List<Object>> dataUserInvoicePharmacy = new ArrayList<List<Object>>();

        DBF osttImportFile = new DBF(osttPath);
        totalRecordCount = osttImportFile.getRecordCount();

        int recordCount = (numberOfUserInvoices != null && numberOfUserInvoices != 0 && numberOfUserInvoices < totalRecordCount) ? numberOfUserInvoices : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {

            if (dataUserInvoicePharmacy.size() >= recordCount)
                break;

            osttImportFile.read();

            String k_mat = getFieldValue(osttImportFile, "K_MAT", "Cp866", "");
            BigDecimal quantity = getBigDecimalFieldValue(osttImportFile, "N_MAT", "Cp866", null);

            Object[] sprmatEntry = sprmatMap.get(k_mat);

            String seriesPharmacyUserInvoice = sprmatEntry == null ? null : (String) sprmatEntry[0];

            if (sprmatEntry != null && quantity != null && quantity.doubleValue() != 0)
                dataUserInvoicePharmacy.add(Arrays.asList((Object) k_mat, seriesPharmacyUserInvoice));
        }

        return dataUserInvoicePharmacy;
    }

    private void importItemPharmacy(ExecutionContext context, String sprmatPath, Integer numberOfItems) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException, xBaseJException {

        List<List<Object>> data = importItemPharmacyFromFile(sprmatPath, numberOfItems);

        if (data != null) {
            ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));
            ImportField idSubstanceField = new ImportField(LM.findLCPByCompoundName("idSubstance"));
            ImportField idPharmacyPriceGroupField = new ImportField(LM.findLCPByCompoundName("idPharmacyPriceGroup"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                    LM.findLCPByCompoundName("itemId").getMapping(idItemField));

            ImportKey<?> substanceKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Substance"),
                    LM.findLCPByCompoundName("substanceId").getMapping(idSubstanceField));

            ImportKey<?> pharmacyPriceGroupKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("PharmacyPriceGroup"),
                    LM.findLCPByCompoundName("pharmacyPriceGroupId").getMapping(idPharmacyPriceGroupField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(idSubstanceField, LM.findLCPByCompoundName("idSubstance").getMapping(substanceKey)));
            props.add(new ImportProperty(idSubstanceField, LM.findLCPByCompoundName("nameSubstance").getMapping(substanceKey)));
            props.add(new ImportProperty(idSubstanceField, LM.findLCPByCompoundName("substanceItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("Substance")).getMapping(substanceKey)));

            props.add(new ImportProperty(idPharmacyPriceGroupField, LM.findLCPByCompoundName("idPharmacyPriceGroup").getMapping(pharmacyPriceGroupKey)));
            props.add(new ImportProperty(idPharmacyPriceGroupField, LM.findLCPByCompoundName("namePharmacyPriceGroup").getMapping(pharmacyPriceGroupKey)));
            props.add(new ImportProperty(idPharmacyPriceGroupField, LM.findLCPByCompoundName("pharmacyPriceGroupItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("PharmacyPriceGroup")).getMapping(pharmacyPriceGroupKey)));

            ImportTable table = new ImportTable(Arrays.asList(idItemField, idSubstanceField, idPharmacyPriceGroupField), data);

            DataSession session = context.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, substanceKey, pharmacyPriceGroupKey), props);
            service.synchronize(true, false);
            session.apply(context.getBL());
            session.close();
        }
    }

    private List<List<Object>> importItemPharmacyFromFile(String sprmatPath, Integer numberOfItems) throws IOException, xBaseJException {

        checkFileExistence(sprmatPath);

        List<List<Object>> data = new ArrayList<List<Object>>();

        DBF importFile = new DBF(sprmatPath);
        int totalRecordCount = importFile.getRecordCount();
        int recordCount = (numberOfItems != null && numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;


        for (int i = 0; i < recordCount; i++) {
            importFile.read();

            String k_group = getFieldValue(importFile, "K_GRUP", "Cp866", null);
            String name = getFieldValue(importFile, "POL_NAIM", "Cp866", null);
            String idItem = k_group + name;
            String idSubstance = getFieldValue(importFile, "DPRM8", "Cp866", null);
            String idPriceGroup = getFieldValue(importFile, "DPRM14", "Cp866", null);

            if (!idItem.isEmpty())
                data.add(Arrays.asList((Object) idItem, idSubstance, idPriceGroup));
        }
        return data;
    }


    String[][] ownershipsList = new String[][]{
            {"ОАОТ", "Открытое акционерное общество торговое"},
            {"ОАО", "Открытое акционерное общество"},
            {"СООО", "Совместное общество с ограниченной ответственностью"},
            {"ООО", "Общество с ограниченной ответственностью"},
            {"ОДО", "Общество с дополнительной ответственностью"},
            {"ЗАО", "Закрытое акционерное общество"},
            {"ЧТУП", "Частное торговое унитарное предприятие"},
            {"ЧУТП", "Частное унитарное торговое предприятие"},
            {"ТЧУП", "Торговое частное унитарное предприятие"},
            {"ЧУП", "Частное унитарное предприятие"},
            {"РУП", "Республиканское унитарное предприятие"},
            {"РДУП", "Республиканское дочернее унитарное предприятие"},
            {"УП", "Унитарное предприятие"},
            {"ИП", "Индивидуальный предприниматель"},
            {"СПК", "Сельскохозяйственный производственный кооператив"},
            {"СП", "Совместное предприятие"}};

    private String[] getAndTrimOwnershipFromName(String name) {
        name = name == null ? "" : name;
        String ownershipName = "";
        String ownershipShortName = "";
        for (String[] ownership : ownershipsList) {
            if (name.contains(ownership[0] + " ") || name.contains(" " + ownership[0])) {
                ownershipName = ownership[1];
                ownershipShortName = ownership[0];
                name = name.replace(ownership[0], "");
            }
        }
        return new String[]{ownershipShortName, ownershipName, name};
    }

    private void checkFileExistence(String filePath) {
        if (!(new File(filePath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + filePath + " не найден");
    }

    private BigDecimal getBigDecimalFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        String value = getFieldValue(importFile, fieldName, charset, defaultValue);
        return value == null ? null : new BigDecimal(value);
    }

    private Date getDateFieldValue(DBF importFile, String fieldName, String charset, Date defaultValue) throws UnsupportedEncodingException, ParseException {
        String dateString = getFieldValue(importFile, fieldName, charset, "");
        return dateString.isEmpty() ? defaultValue : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());

    }

    private String getFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        try {
            String result = new String(importFile.getField(fieldName).getBytes(), charset).trim();
            return result.isEmpty() ? defaultValue : result;
        } catch (xBaseJException e) {
            return defaultValue;
        }
    }

    private BigDecimal safeAdd(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else return (operand1 == null ? operand2 : (operand2 == null ? operand1 : operand1.add(operand2)));
    }

    private BigDecimal safeDivide(BigDecimal dividend, BigDecimal quotient, int scale) {
        if (dividend == null || dividend.doubleValue() == 0 || quotient == null || quotient.doubleValue() == 0)
            return null;
        else return dividend.divide(quotient, scale, RoundingMode.HALF_UP);
    }

}