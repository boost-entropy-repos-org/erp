package equ.clt.handler.kristal10;

import com.google.common.base.Throwables;
import equ.api.*;
import equ.api.cashregister.*;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Kristal10Handler extends CashRegisterHandler<Kristal10SalesBatch> {

    protected final static Logger processTransactionLogger = Logger.getLogger("TransactionLogger");
    protected final static Logger processStopListLogger = Logger.getLogger("StopListLogger");
    protected final static Logger sendSalesLogger = Logger.getLogger("SendSalesLogger");

    String encoding = "utf-8";

    private FileSystemXmlApplicationContext springContext;

    public Kristal10Handler(FileSystemXmlApplicationContext springContext) {
        this.springContext = springContext;
    }

    public String getGroupId(TransactionCashRegisterInfo transactionInfo) {
        return "kristal10";
    }

    @Override
    public Map<Integer, SendTransactionBatch> sendTransaction(List<TransactionCashRegisterInfo> transactionList) throws IOException {

        Map<File, Integer> fileMap = new HashMap<>();
        Map<Integer, Exception> failedTransactionMap = new HashMap<>();
        Set<Integer> emptyTransactionSet = new HashSet<>();

        for(TransactionCashRegisterInfo transaction : transactionList) {

            try {

                processTransactionLogger.info("Kristal10: Send Transaction # " + transaction.id);

                Kristal10Settings kristalSettings = springContext.containsBean("kristal10Settings") ? (Kristal10Settings) springContext.getBean("kristal10Settings") : null;
                boolean brandIsManufacturer = kristalSettings != null && kristalSettings.getBrandIsManufacturer() != null && kristalSettings.getBrandIsManufacturer();
                boolean seasonIsCountry = kristalSettings != null && kristalSettings.getSeasonIsCountry() != null && kristalSettings.getSeasonIsCountry();
                boolean idItemInMarkingOfTheGood = kristalSettings != null && kristalSettings.isIdItemInMarkingOfTheGood() != null && kristalSettings.isIdItemInMarkingOfTheGood();
                boolean useShopIndices = kristalSettings != null && kristalSettings.getUseShopIndices() != null && kristalSettings.getUseShopIndices();
                List<String> tobaccoGroups = getTobaccoGroups(kristalSettings != null ? kristalSettings.getTobaccoGroup() : null);

                List<String> directoriesList = new ArrayList<>();
                for (CashRegisterInfo cashRegisterInfo : transaction.machineryInfoList) {
                    if ((cashRegisterInfo.port != null) && (!directoriesList.contains(cashRegisterInfo.port.trim())))
                        directoriesList.add(cashRegisterInfo.port.trim());
                    if ((cashRegisterInfo.directory != null) && (!directoriesList.contains(cashRegisterInfo.directory.trim())))
                        directoriesList.add(cashRegisterInfo.directory.trim());
                }

                if(directoriesList.isEmpty())
                    emptyTransactionSet.add(transaction.id);

                for (String directory : directoriesList) {

                    String exchangeDirectory = directory.trim() + "/products/source/";

                    if (!new File(exchangeDirectory).exists()) {
                        processTransactionLogger.info("Kristal10: exchange directory not found, trying to create: " + exchangeDirectory);
                        if(!new File(exchangeDirectory).mkdir() && !new File(exchangeDirectory).mkdirs())
                            processTransactionLogger.info("Kristal10: exchange directory not found, failed to create: " + exchangeDirectory);
                    }

                    //catalog-goods.xml
                    processTransactionLogger.info("Kristal10: creating catalog-goods file (Transaction " + transaction.id + ") - " + transaction.itemsList.size() + " items");

                    Element rootElement = new Element("goods-catalog");
                    Document doc = new Document(rootElement);
                    doc.setRootElement(rootElement);

                    String weightCode = transaction.weightCodeGroupCashRegister == null ? "21" : transaction.weightCodeGroupCashRegister;

                    for (CashRegisterItemInfo item : transaction.itemsList) {
                        if (!Thread.currentThread().isInterrupted()) {

                            //parent: rootElement
                            Element good = new Element("good");

                            String barcodeItem = transformBarcode(item.idBarcode, weightCode, item.passScalesItem);
                            String idItem = idItemInMarkingOfTheGood ? item.idItem : barcodeItem;

                            setAttribute(good, "marking-of-the-good", idItem);
                            rootElement.addContent(good);

                            //parent: rootElement
                            Element maxDiscountRestriction = new Element("max-discount-restriction");
                            setAttribute(maxDiscountRestriction, "id", barcodeItem);
                            setAttribute(maxDiscountRestriction, "subject-type", "GOOD");
                            setAttribute(maxDiscountRestriction, "subject-code", idItem);
                            setAttribute(maxDiscountRestriction, "type", "MAX_DISCOUNT_PERCENT");
                            setAttribute(maxDiscountRestriction, "value", "0");
                            addStringElement(maxDiscountRestriction, "since-date", currentDate());
                            addStringElement(maxDiscountRestriction, "till-date", "2021-01-01T23:59:59");
                            addStringElement(maxDiscountRestriction, "since-time", "00:00:00");
                            addStringElement(maxDiscountRestriction, "till-time", "23:59:59");
                            addStringElement(maxDiscountRestriction, "deleted", item.flags != null && ((item.flags & 16) == 0) ? "false" : "true");
                            if (useShopIndices)
                                addStringElement(maxDiscountRestriction, "shop-indices", item.idDepartmentStore);
                            rootElement.addContent(maxDiscountRestriction);

                            if (useShopIndices)
                                addStringElement(good, "shop-indices", item.idDepartmentStore);

                            addStringElement(good, "name", item.name);

                            //parent: good
                            Element barcode = new Element("bar-code");
                            setAttribute(barcode, "code", barcodeItem);
                            addStringElement(barcode, "default-code", "true");
                            good.addContent(barcode);

                            addProductType(good, item, tobaccoGroups);

                            if(item.splitItem && !item.passScalesItem) {
                                Element pluginProperty = new Element("plugin-property");
                                setAttribute(pluginProperty, "key", "precision");
                                setAttribute(pluginProperty, "value", "0.001");
                                good.addContent(pluginProperty);
                            }

                            //parent: good
                            Element priceEntry = new Element("price-entry");
                            Object price = item.price == null ? null : (item.price.intValue() == 0 ? "0.00" : item.price.intValue());
                            setAttribute(priceEntry, "price", price);
                            setAttribute(priceEntry, "deleted", "false");
                            addStringElement(priceEntry, "begin-date", currentDate());
                            addStringElement(priceEntry, "number", "1");
                            good.addContent(priceEntry);

                            addStringElement(good, "vat", item.vat == null || item.vat.intValue() == 0 ? "20" : String.valueOf(item.vat.intValue()));

                            //parent: priceEntry
                            Element department = new Element("department");
                            setAttribute(department, "number", transaction.nppGroupMachinery);
                            addStringElement(department, "name", transaction.nameGroupMachinery == null ? "Отдел" : transaction.nameGroupMachinery);
                            priceEntry.addContent(department);

                            //parent: good
                            Element group = new Element("group");
                            setAttribute(group, "id", item.idItemGroup);
                            addStringElement(group, "name", item.nameItemGroup);
                            good.addContent(group);

                            List<ItemGroup> hierarchyItemGroup = transaction.itemGroupMap.get(item.idItemGroup);
                            if (hierarchyItemGroup != null)
                                addHierarchyItemGroup(group, hierarchyItemGroup.subList(1, hierarchyItemGroup.size()));

                            //parent: good
                            if (item.idUOM == null || item.shortNameUOM == null) {
                                String error = "Kristal10: Error! UOM not specified for item with barcode " + barcodeItem;
                                processTransactionLogger.error(error);
                                throw Throwables.propagate(new RuntimeException(error));
                            }
                            Element measureType = new Element("measure-type");
                            setAttribute(measureType, "id", item.idUOM);
                            addStringElement(measureType, "name", item.shortNameUOM);
                            good.addContent(measureType);

                            if (brandIsManufacturer) {
                                //parent: good
                                Element manufacturer = new Element("manufacturer");
                                setAttribute(manufacturer, "id", item.idBrand);
                                addStringElement(manufacturer, "name", item.nameBrand);
                                good.addContent(manufacturer);
                            }

                            if (seasonIsCountry) {
                                //parent: good
                                Element country = new Element("country");
                                setAttribute(country, "id", item.idSeason);
                                addStringElement(country, "name", item.nameSeason);
                                good.addContent(country);
                            }

                            addStringElement(good, "delete-from-cash", "false");

                        }
                    }
                    processTransactionLogger.info(String.format("Kristal10: created catalog-goods file (Transaction %s)", transaction.id));
                    File file = makeExportFile(exchangeDirectory, "catalog-goods");
                    XMLOutputter xmlOutput = new XMLOutputter();
                    xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(encoding));
                    PrintWriter fw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
                    xmlOutput.output(doc, fw);
                    fw.close();
                    processTransactionLogger.info(String.format("Kristal10: output catalog-goods file (Transaction %s)", transaction.id));

                    fileMap.put(file, transaction.id);
                }
            } catch (Exception e) {
                processTransactionLogger.error("Kristal10: ", e);
                failedTransactionMap.put(transaction.id, e);
            }
        }
        processTransactionLogger.info(String.format("Kristal10: starting to wait for deletion %s files", fileMap.size()));
        return waitForDeletion(fileMap, failedTransactionMap, emptyTransactionSet);
    }

    private void addProductType(Element good, ItemInfo item, List<String> tobaccoGroups) {
        String productType;
        if(item.idItemGroup != null && tobaccoGroups != null && tobaccoGroups.contains(item.idItemGroup))
            productType = "ProductCiggyEntity";
        else if (item.passScalesItem)
            productType = item.splitItem ? "ProductWeightEntity" : "ProductPieceWeightEntity";
        else
            productType = (item.flags == null || ((item.flags & 256) == 0)) ? "ProductPieceEntity" : "ProductSpiritsEntity";
        addStringElement(good, "product-type", productType);
    }

    private List<String> getTobaccoGroups (String tobaccoGroup) {
        List<String> tobaccoGroups = new ArrayList<>();
        if (tobaccoGroup != null)
            Collections.addAll(tobaccoGroups, tobaccoGroup.split(","));
        return tobaccoGroups;
    }

    private Map<Integer, SendTransactionBatch> waitForDeletion(Map<File, Integer> filesMap, Map<Integer, Exception> failedTransactionMap, Set<Integer> emptyTransactionSet) {
        int count = 0;
        Map<Integer, SendTransactionBatch> result = new HashMap<>();
        while (!Thread.currentThread().isInterrupted() && !filesMap.isEmpty()) {
            try {
                Map<File, Integer> nextFilesMap = new HashMap<>();
                count++;
                if (count >= 180) {
                    processTransactionLogger.info("Kristal10 (wait for deletion): timeout");
                    break;
                }
                for (Map.Entry<File, Integer> entry : filesMap.entrySet()) {
                    File file = entry.getKey();
                    Integer idTransaction = entry.getValue();
                    if (file.exists())
                        nextFilesMap.put(file, idTransaction);
                    else {
                        processTransactionLogger.info(String.format("Kristal10 (wait for deletion): file %s has been deleted", file.getAbsolutePath()));
                        result.put(idTransaction, new SendTransactionBatch(failedTransactionMap.get(idTransaction)));
                        failedTransactionMap.remove(idTransaction);
                    }
                }
                filesMap = nextFilesMap;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        for(Map.Entry<File, Integer> file : filesMap.entrySet()) {
            processTransactionLogger.info(String.format("Kristal10 (wait for deletion): file %s has NOT been deleted", file.getKey().getAbsolutePath()));
            result.put(file.getValue(), new SendTransactionBatch(new RuntimeException(String.format("Kristal10: file %s has been created but not processed by server", file.getKey().getAbsolutePath()))));
        }
        for(Map.Entry<Integer, Exception> entry : failedTransactionMap.entrySet()) {
            result.put(entry.getKey(), new SendTransactionBatch(entry.getValue()));
        }
        for(Integer emptyTransaction : emptyTransactionSet) {
            result.put(emptyTransaction, new SendTransactionBatch(null));
        }
        return result;
    }

    private synchronized File makeExportFile(String exchangeDirectory, String prefix) {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy'T'HH-mm-ss");
        File file = new File(exchangeDirectory + "//" + prefix + "_" + dateFormat.format(Calendar.getInstance().getTime()) + ".xml");
        //чит для избежания ситуации, совпадения имён у двух файлов (в основе имени - текущее время с точностью до секунд)
        while(file.exists()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            file = new File(exchangeDirectory + "//" + "catalog-goods_" + dateFormat.format(Calendar.getInstance().getTime()) + ".xml");
        }
        return file;
    }

    private void addStringElement(Element parent, String id, String value) {
        if (value != null)
            parent.addContent(new Element(id).setText(value));
    }

    private void setAttribute(Element element, String id, Object value) {
        if (value != null)
            element.setAttribute(new Attribute(id, String.valueOf(value)));
    }

    private void addHierarchyItemGroup(Element parent, List<ItemGroup> hierarchyItemGroup) {
        if (!hierarchyItemGroup.isEmpty()) {
            Element element = new Element("parent-group");
            setAttribute(element, "id", hierarchyItemGroup.get(0).idItemGroup);
            addStringElement(element, "name", hierarchyItemGroup.get(0).nameItemGroup);
            parent.addContent(element);
            addHierarchyItemGroup(element, hierarchyItemGroup.subList(1, hierarchyItemGroup.size()));
        }
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {
    }

    @Override
    public void requestSalesInfo(List<RequestExchange> requestExchangeList, Set<String> directorySet,
                                 Set<Integer> succeededRequests, Map<Integer, String> failedRequests, Map<Integer, String> ignoredRequests) throws IOException, ParseException {
        for (RequestExchange entry : requestExchangeList) {
            int count = 0;
            String requestResult = null;
            if(entry.isSalesInfoExchange()) {

                //временный лог. Потом не забыть убрать
                for(String directory : directorySet)
                    sendSalesLogger.info("Kristal10 directory: " + directory);

                for (String directory : entry.directoryStockMap.keySet()) {

                    if (!directorySet.contains(directory)) continue;

                    sendSalesLogger.info("Kristal10: creating request files for directory : " + directory);
                    String dateFrom = new SimpleDateFormat("dd.MM.yyyy").format(entry.dateFrom);
                    String dateTo = new SimpleDateFormat("dd.MM.yyyy").format(entry.dateTo);

                    String exchangeDirectory = directory + "/reports/source/";

                    if (new File(exchangeDirectory).exists() || new File(exchangeDirectory).mkdirs()) {
                        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exchangeDirectory + "reports.request"), encoding));
                        writer.write(String.format("dateRange: %s-%s\nreport: purchases", dateFrom, dateTo));
                        writer.close();
                    } else
                        requestResult = "Error: " + exchangeDirectory + " doesn't exist. Request creation failed.";
                    count++;
                }
                if(count > 0) {
                    if(requestResult == null)
                        succeededRequests.add(entry.requestExchange);
                    else
                        failedRequests.put(entry.requestExchange, requestResult);
                } else
                    succeededRequests.add(entry.requestExchange);
            }
        }
    }

    @Override
    public void finishReadingSalesInfo(Kristal10SalesBatch salesBatch) {
        sendSalesLogger.info("Kristal10: Finish Reading started");
        Kristal10Settings kristalSettings = springContext.containsBean("kristal10Settings") ? (Kristal10Settings) springContext.getBean("kristal10Settings") : null;
        Integer cleanOldFilesDays = kristalSettings == null ? null : kristalSettings.getCleanOldFilesDays();
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);

            try {
                Calendar calendar = Calendar.getInstance();
                String directory = f.getParent() + "/success-" + new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()) + "/";
                if(cleanOldFilesDays != null) {
                    calendar.add(Calendar.DATE, -cleanOldFilesDays);
                    String oldDirectory = f.getParent() + "/success-" + new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()) + "/";
                    File oldDir = new File(oldDirectory);
                    File[] files = oldDir.listFiles();
                    if(files != null) {
                        for (File file : files) {
                            if (!file.delete())
                                file.deleteOnExit();
                        }
                    }
                    if(!oldDir.delete())
                        oldDir.deleteOnExit();
                }
                if (new File(directory).exists() || new File(directory).mkdirs())
                    FileCopyUtils.copy(f, new File(directory + f.getName()));
            } catch (IOException e) {
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be copied to success files", e);
            }

            if (f.delete()) {
                sendSalesLogger.info("Kristal10: file " + readFile + " has been deleted");
            } else {
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
            }
        }
    }

    @Override
    public Map<String, Timestamp> requestSucceededSoftCheckInfo(Set<String> directorySet) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public List<List<Object>> checkZReportSum(Map<String, List<Object>> zReportSumMap, List<List<Object>> cashRegisterList) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public CashDocumentBatch readCashDocumentInfo(List<CashRegisterInfo> cashRegisterInfoList, Set<String> cashDocumentSet) throws ClassNotFoundException {

        Map<String, Integer> directoryGroupCashRegisterMap = new HashMap<>();
        Set<String> directorySet = new HashSet<>();
        for (CashRegisterInfo c : cashRegisterInfoList) {
            if (c.directory != null && c.handlerModel.endsWith("Kristal10Handler")) {
                directorySet.add(c.directory);
                if (c.handlerModel != null && c.number != null && c.numberGroup != null) {
                    directoryGroupCashRegisterMap.put(c.directory + "_" + c.number, c.numberGroup);
                }
            }
        }

        List<CashDocument> cashDocumentList = new ArrayList<>();
        List<String> readFiles = new ArrayList<>();
        for (String directory : directorySet) {

            String exchangeDirectory = directory + "/reports/";

            File[] filesList = new File(exchangeDirectory).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return (pathname.getName().startsWith("cash_in") || pathname.getName().startsWith("cash_out")) && pathname.getPath().endsWith(".xml");
                }
            });

            if (filesList == null || filesList.length == 0)
                sendSalesLogger.info("Kristal10: No cash documents found in " + exchangeDirectory);
            else {
                sendSalesLogger.info("Kristal10: found " + filesList.length + " file(s) in " + exchangeDirectory);

                for (File file : filesList) {
                    try {
                        String fileName = file.getName();
                        sendSalesLogger.info("Kristal10: reading " + fileName);
                        if (isFileLocked(file)) {
                            sendSalesLogger.info("Kristal10: " + fileName + " is locked");
                        } else {
                            SAXBuilder builder = new SAXBuilder();

                            Document document = builder.build(file);
                            Element rootNode = document.getRootElement();

                            boolean cashIn = file.getName().startsWith("cash_in");

                            List cashDocumentsList = rootNode.getChildren(cashIn ? "introduction" : "withdrawal");

                            for (Object cashDocumentNode : cashDocumentsList) {

                                String numberCashDocument = readStringXMLAttribute(cashDocumentNode, "number");
                                Integer numberCashRegister = readIntegerXMLAttribute(cashDocumentNode, "cash");
                                BigDecimal sumCashDocument = readBigDecimalXMLAttribute(cashDocumentNode, "amount");
                                if(!cashIn)
                                    sumCashDocument = sumCashDocument == null ? null : sumCashDocument.negate();

                                long dateTimeCashDocument = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(readStringXMLAttribute(cashDocumentNode, "regtime")).getTime();
                                Date dateCashDocument = new Date(dateTimeCashDocument);
                                Time timeCashDocument = new Time(dateTimeCashDocument);

                                cashDocumentList.add(new CashDocument(numberCashDocument, numberCashDocument, dateCashDocument, timeCashDocument,
                                        directoryGroupCashRegisterMap.get(directory + "_" + numberCashRegister), numberCashRegister, sumCashDocument));
                            }
                            readFiles.add(file.getAbsolutePath());
                        }
                    } catch (Throwable e) {
                        sendSalesLogger.error("File: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return new CashDocumentBatch(cashDocumentList, readFiles);
    }

    @Override
    public void finishReadingCashDocumentInfo(CashDocumentBatch cashDocumentBatch) {
        sendSalesLogger.info("Kristal10: Finish ReadingCashDocumentInfo started");
        for (String readFile : cashDocumentBatch.readFiles) {
            File f = new File(readFile);
            if (f.delete()) {
                sendSalesLogger.info("Kristal10: file " + readFile + " has been deleted");
            } else {
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
            }
        }
    }

    @Override
    public void sendStopListInfo(StopListInfo stopListInfo, Set<String> directorySet) throws IOException {

        //из-за временного решения с весовыми товарами для этих весовых товаров стоп-листы работать не будут
        processStopListLogger.info("Kristal10: Send StopList # " + stopListInfo.number);

        Kristal10Settings kristalSettings = springContext.containsBean("kristal10Settings") ? (Kristal10Settings) springContext.getBean("kristal10Settings") : null;
        boolean useShopIndices = kristalSettings == null || kristalSettings.getUseShopIndices() != null && kristalSettings.getUseShopIndices();
        boolean idItemInMarkingOfTheGood = kristalSettings == null || kristalSettings.isIdItemInMarkingOfTheGood() != null && kristalSettings.isIdItemInMarkingOfTheGood();
        List<String> tobaccoGroups = getTobaccoGroups(kristalSettings != null ? kristalSettings.getTobaccoGroup() : null);

        for (String directory : directorySet) {

            if (stopListInfo.dateFrom == null || stopListInfo.timeFrom == null) {
                String error = "Kristal10: Error! Start DateTime not specified for stopList " + stopListInfo.number;
                processStopListLogger.error(error);
                throw Throwables.propagate(new RuntimeException(error));
            }

            if (stopListInfo.dateTo == null || stopListInfo.timeTo == null) {
                stopListInfo.dateTo = new Date(2040 - 1900, 0, 1);
                stopListInfo.timeTo = new Time(23, 59, 59);
            }

            String exchangeDirectory = directory.trim() + "/products/source/";

            if (!new File(exchangeDirectory).exists())
                new File(exchangeDirectory).mkdirs();

            Element rootElement = new Element("goods-catalog");
            Document doc = new Document(rootElement);
            doc.setRootElement(rootElement);

            if (!stopListInfo.exclude) {
                for (Map.Entry<String, ItemInfo> entry : stopListInfo.stopListItemMap.entrySet()) {
                    String idBarcode = entry.getKey();
                    ItemInfo item = entry.getValue();

                    //parent: rootElement
                    Element good = new Element("good");
                    idBarcode = transformBarcode(idBarcode, null, false);
                    setAttribute(good, "marking-of-the-good", idItemInMarkingOfTheGood ? item.idItem : idBarcode);
                    addStringElement(good, "name", item.name);

                    addProductType(good, item, tobaccoGroups);

                    rootElement.addContent(good);

                    if (useShopIndices) {
                        String shopIndices = "";
                        Set<MachineryInfo> machineryInfoSet = stopListInfo.handlerMachineryMap.get(getClass().getName());
                        if (machineryInfoSet != null) {
                            Set<String> stockSet = new HashSet<>();
                            for (MachineryInfo machineryInfo : machineryInfoSet) {
                                if (machineryInfo instanceof CashRegisterInfo)
                                    stockSet.add(((CashRegisterInfo) machineryInfo).idStock);
                            }
                            for (String idStock : stockSet) {
                                shopIndices += idStock + " ";
                            }
                        }
                        shopIndices = shopIndices.isEmpty() ? shopIndices : shopIndices.substring(0, shopIndices.length() - 1);
                        addStringElement(good, "shop-indices", shopIndices);
                    }

                    //parent: good
                    Element barcode = new Element("bar-code");
                    setAttribute(barcode, "code", item.idBarcode);
                    addStringElement(barcode, "default-code", "true");
                    good.addContent(barcode);

                    //parent: good
                    Element priceEntry = new Element("price-entry");
                    setAttribute(priceEntry, "price", 1);
                    setAttribute(priceEntry, "deleted", "true");
                    addStringElement(priceEntry, "begin-date", formatDate(stopListInfo.dateFrom));
                    addStringElement(priceEntry, "number", "1");
                    good.addContent(priceEntry);

                    Element measureType = new Element("measure-type");
                    setAttribute(measureType, "id", item.idUOM);
                    addStringElement(measureType, "name", item.shortNameUOM);
                    good.addContent(measureType);

                    addStringElement(good, "vat", item.vat == null || item.vat.intValue() == 0 ? "20" : String.valueOf(item.vat.intValue()));

                    //parent: priceEntry
                    Set<MachineryInfo> machineryInfoSet = stopListInfo.handlerMachineryMap.get(getClass().getName());
                    if (machineryInfoSet != null) {
                        Set<Integer> nppGroupMachinerySet = new HashSet<>();
                        for (MachineryInfo machineryInfo : machineryInfoSet) {
                            if (machineryInfo instanceof CashRegisterInfo)
                                nppGroupMachinerySet.add(((CashRegisterInfo) machineryInfo).numberGroup);
                        }
                        for (Integer number : nppGroupMachinerySet) {
                            Element department = new Element("department");
                            setAttribute(department, "number", number);
                            priceEntry.addContent(department);
                        }
                    }

                    //parent: good
                    Element group = new Element("group");
                    setAttribute(group, "id", item.idItemGroup);
                    addStringElement(group, "name", item.nameItemGroup);
                    good.addContent(group);

                }

                if (!stopListInfo.stopListItemMap.isEmpty()) {
                    XMLOutputter xmlOutput = new XMLOutputter();
                    xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(encoding));

                    PrintWriter fw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(makeExportFile(exchangeDirectory, "catalog-goods")), encoding));
                    xmlOutput.output(doc, fw);
                    fw.close();
                }
            }
        }
    }

    @Override
    public void sendDiscountCardList(List<DiscountCard> discountCardList, Date startDate, Set<String> directorySet) throws IOException {

        Kristal10Settings kristalSettings = springContext.containsBean("kristal10Settings") ? (Kristal10Settings) springContext.getBean("kristal10Settings") : null;
        Map<Double, String> discountCardPercentTypeMap = kristalSettings != null ? kristalSettings.getDiscountCardPercentTypeMap() : new HashMap<Double, String>();

        if (!discountCardList.isEmpty()) {
            for (String directory : directorySet) {
                String exchangeDirectory = directory.trim() + "/products/source/";
                if (new File(exchangeDirectory).exists() || new File(exchangeDirectory).mkdirs()) {
                    processStopListLogger.info(String.format("Kristal10: Send DiscountCards to %s", exchangeDirectory));

                    Element rootElement = new Element("cards-catalog");
                    Document doc = new Document(rootElement);
                    doc.setRootElement(rootElement);

                    Date currentDate = new Date(Calendar.getInstance().getTime().getTime());

                    for (Map.Entry<Double, String> discountCardType : discountCardPercentTypeMap.entrySet()) {
                        //parent: rootElement
                        Element internalCard = new Element("internal-card-type");
                        setAttribute(internalCard, "guid", discountCardType.getValue());
                        setAttribute(internalCard, "name", discountCardType.getKey() + "%");
                        setAttribute(internalCard, "personalized", "false");
                        setAttribute(internalCard, "percentage-discount", discountCardType.getKey());
                        setAttribute(internalCard, "deleted", "false");

                        rootElement.addContent(internalCard);
                    }

                    for (DiscountCard discountCard : discountCardList) {
                        //parent: rootElement
                        Element internalCard = new Element("internal-card");
                        Double percent = discountCard.percentDiscountCard == null ? 0 : discountCard.percentDiscountCard.doubleValue();
                        String guid = discountCardPercentTypeMap.get(percent);
                        if(discountCard.numberDiscountCard != null) {
                            setAttribute(internalCard, "number", discountCard.numberDiscountCard);
                            setAttribute(internalCard, "amount", discountCard.initialSumDiscountCard == null ? "0.00" : discountCard.initialSumDiscountCard);
                            setAttribute(internalCard, "expiration-date", discountCard.dateToDiscountCard == null ? "2050-12-03" : discountCard.dateToDiscountCard);
                            setAttribute(internalCard, "status",
                                    discountCard.dateFromDiscountCard == null || currentDate.compareTo(discountCard.dateFromDiscountCard) > 0 ? "ACTIVE" : "BLOCKED");
                            setAttribute(internalCard, "deleted", "false");
                            setAttribute(internalCard, "card-type-guid", guid == null ? "0" : guid);

                            rootElement.addContent(internalCard);
                        }
                    }
                    exportXML(doc, exchangeDirectory, "catalog-cards");
                }
            }
        }
    }

    @Override
    public void sendPromotionInfo(PromotionInfo promotionInfo, Set<String> directory) throws IOException {
    }

    @Override
    public void sendCashierInfoList(List<CashierInfo> cashierInfoList, Map<String, Set<String>> directoryStockMap) throws IOException {
    }

    @Override
    public List<CashierTime> requestCashierTime(List<MachineryInfo> cashRegisterInfoList) throws IOException, ClassNotFoundException, SQLException {
        return null;
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

//    private String formatTime(Time time) {
//        return new SimpleDateFormat("HH:mm:ss.SSS").format(time);
//    }

    private String currentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()) + "T00:00:00";
    }

    @Override
    public SalesBatch readSalesInfo(String directory, List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException, ClassNotFoundException {

        Map<String, Integer> directoryDepartNumberGroupCashRegisterMap = new HashMap<>();
        Map<String, Integer> directoryGroupCashRegisterMap = new HashMap<>();
        Map<String, Date> directoryStartDateMap = new HashMap<>();
        Map<String, String> directoryWeightCodeMap = new HashMap<>();
        for (CashRegisterInfo c : cashRegisterInfoList) {
            if (c.directory != null) {
                directoryDepartNumberGroupCashRegisterMap.put(c.directory + "_" + c.number + "_" + c.overDepartNumber, c.numberGroup);
                if (c.number != null && c.numberGroup != null)
                    directoryGroupCashRegisterMap.put(c.directory + "_" + c.number, c.numberGroup);
                if (c.number != null && c.startDate != null)
                    directoryStartDateMap.put(c.directory + "_" + c.number, c.startDate);
                if (c.weightCodeGroupCashRegister != null)
                    directoryWeightCodeMap.put(c.directory + "_" + c.number + "_" + c.overDepartNumber, c.weightCodeGroupCashRegister);
            }
        }

        Kristal10Settings kristalSettings = springContext.containsBean("kristal10Settings") ? (Kristal10Settings) springContext.getBean("kristal10Settings") : null;
        String transformUPCBarcode = kristalSettings == null ? null : kristalSettings.getTransformUPCBarcode();
        Integer maxFilesCount = kristalSettings == null ? null : kristalSettings.getMaxFilesCount();
        boolean ignoreSalesWeightPrefix = kristalSettings == null || kristalSettings.getIgnoreSalesWeightPrefix() != null && kristalSettings.getIgnoreSalesWeightPrefix();

        List<SalesInfo> salesInfoList = new ArrayList<>();
        List<String> filePathList = new ArrayList<>();

        String exchangeDirectory = directory + "/reports/";

        File[] filesList = new File(exchangeDirectory).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("purchases") && pathname.getPath().endsWith(".xml");
            }
        });

        if (filesList == null || filesList.length == 0)
            sendSalesLogger.info("Kristal10: No checks found in " + exchangeDirectory);
        else {
            if(maxFilesCount == null)
                sendSalesLogger.info(String.format("Kristal10: found %s file(s) in %s", filesList.length, exchangeDirectory));
            else
                sendSalesLogger.info(String.format("Kristal10: found %s file(s) in %s, will read %s file(s)", filesList.length, exchangeDirectory, Math.min(filesList.length, maxFilesCount)));

            int filesCount = 0;
            for (File file : filesList) {
                filesCount++;
                if(maxFilesCount != null && maxFilesCount < filesCount)
                    break;
                try {
                    String fileName = file.getName();
                    sendSalesLogger.info("Kristal10: reading " + fileName);
                    if (isFileLocked(file)) {
                        sendSalesLogger.info("Kristal10: " + fileName + " is locked");
                    } else {
                        SAXBuilder builder = new SAXBuilder();

                        Document document = builder.build(file);
                        Element rootNode = document.getRootElement();
                        List purchasesList = rootNode.getChildren("purchase");

                        for (Object purchaseNode : purchasesList) {

                            String operationType = readStringXMLAttribute(purchaseNode, "operationType");
                            Boolean isSale = operationType == null || operationType.equals("true");
                            Integer numberCashRegister = readIntegerXMLAttribute(purchaseNode, "cash");
                            String numberZReport = readStringXMLAttribute(purchaseNode, "shift");
                            Integer numberReceipt = readIntegerXMLAttribute(purchaseNode, "number");
                            String idEmployee = readStringXMLAttribute(purchaseNode, "tabNumber");
                            String nameEmployee = readStringXMLAttribute(purchaseNode, "userName");
                            String firstNameEmployee = null;
                            String lastNameEmployee = null;
                            if (nameEmployee != null) {
                                String[] splittedNameEmployee = nameEmployee.split(" ");
                                lastNameEmployee = splittedNameEmployee[0];
                                for (int i = 1; i < splittedNameEmployee.length; i++) {
                                    firstNameEmployee = firstNameEmployee == null ? splittedNameEmployee[i] : (firstNameEmployee + " " + splittedNameEmployee[i]);
                                }
                            }
                            BigDecimal discountSumReceipt = null; //пока считаем, что скидки по чеку нету //readBigDecimalXMLAttribute(purchaseNode, "discountAmount");
                            //discountSumReceipt = (discountSumReceipt != null && !isSale) ? discountSumReceipt.negate() : discountSumReceipt;

                            long dateTimeReceipt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"/*.SSSX"*/).parse(readStringXMLAttribute(purchaseNode, "saletime")).getTime();
                            Date dateReceipt = new Date(dateTimeReceipt);
                            Time timeReceipt = new Time(dateTimeReceipt);

                            BigDecimal sumCard = BigDecimal.ZERO;
                            BigDecimal sumCash = BigDecimal.ZERO;
                            BigDecimal sumGiftCard = BigDecimal.ZERO;
                            List paymentsList = ((Element) purchaseNode).getChildren("payments");
                            for (Object paymentNode : paymentsList) {

                                List paymentEntryList = ((Element) paymentNode).getChildren("payment");
                                for (Object paymentEntryNode : paymentEntryList) {
                                    String paymentType = readStringXMLAttribute(paymentEntryNode, "typeClass");
                                    if (paymentType != null) {
                                        BigDecimal sum = readBigDecimalXMLAttribute(paymentEntryNode, "amount");
                                        sum = (sum != null && !isSale) ? sum.negate() : sum;
                                        switch (paymentType) {
                                            case "CashPaymentEntity":
                                                sumCash = safeAdd(sumCash, sum);
                                                break;
                                            case "CashChangePaymentEntity":
                                                sumCash = safeSubtract(sumCash, sum);
                                                break;
                                            case "ExternalBankTerminalPaymentEntity":
                                            case "BankCardPaymentEntity":
                                                sumCard = safeAdd(sumCard, sum);
                                                break;
                                            case "GiftCardPaymentEntity":
                                                sumGiftCard = safeAdd(sumGiftCard, sum);
                                                break;
                                        }
                                    }
                                }
                            }

                            String discountCard = null;
                            List discountCardsList = ((Element) purchaseNode).getChildren("discountCards");
                            for (Object discountCardNode : discountCardsList) {
                                List discountCardList = ((Element) discountCardNode).getChildren("discountCard");
                                for (Object discountCardEntry : discountCardList) {
                                    discountCard = ((Element) discountCardEntry).getValue();
                                    if (discountCard != null) {
                                        discountCard = discountCard.trim();
                                        break;
                                    }
                                }
                            }

                            List positionsList = ((Element) purchaseNode).getChildren("positions");
                            List<SalesInfo> currentSalesInfoList = new ArrayList<>();
                            BigDecimal currentPaymentSum = BigDecimal.ZERO;


                            for (Object positionNode : positionsList) {

                                List positionEntryList = ((Element) positionNode).getChildren("position");

                                int count = 1;
                                String departNumber = null;
                                for (Object positionEntryNode : positionEntryList) {

                                    if (departNumber == null)
                                        departNumber = readStringXMLAttribute(positionEntryNode, "departNumber");

                                    String weightCode = directoryWeightCodeMap.containsKey(directory + "_" + numberCashRegister + "_" + departNumber) ?
                                            directoryWeightCodeMap.get(directory + "_" + numberCashRegister + "_" + departNumber) : "21";

                                    String idItem = readStringXMLAttribute(positionEntryNode, "goodsCode");
                                    String barcode = transformUPCBarcode(readStringXMLAttribute(positionEntryNode, "barCode"), transformUPCBarcode);

                                    //обнаруживаем продажу сертификатов
                                    boolean isGiftCard = false;
                                    if (barcode != null && barcode.length() == 3 && !barcode.equals("666")) {
                                        isGiftCard = true;
                                        barcode = dateTimeReceipt + "/" + count;
                                    }

                                    BigDecimal quantity = readBigDecimalXMLAttribute(positionEntryNode, "count");

                                    //временное решение для весовых товаров
                                    if(barcode != null) {
                                        if (barcode.length() == 7 && barcode.startsWith("2") && ignoreSalesWeightPrefix) {
                                            barcode = barcode.substring(2);
                                        } else if (barcode.startsWith(weightCode) && barcode.length() == 7)
                                            barcode = barcode.substring(2);


                                        // временно для касс самообслуживания в виталюре
                                        if (ignoreSalesWeightPrefix && barcode.length() == 13 && barcode.startsWith("22") && !barcode.substring(8, 13).equals("00000") &&
                                                quantity != null && (quantity.intValue() != quantity.doubleValue() || parseWeight(barcode.substring(7, 12)) == quantity.doubleValue()))
                                            barcode = barcode.substring(2, 7);
                                    }

                                    quantity = (quantity != null && !isSale) ? quantity.negate() : quantity;
                                    BigDecimal price = readBigDecimalXMLAttribute(positionEntryNode, "cost");
                                    BigDecimal sumReceiptDetail = readBigDecimalXMLAttribute(positionEntryNode, "amount");
                                    sumReceiptDetail = (sumReceiptDetail != null && !isSale) ? sumReceiptDetail.negate() : sumReceiptDetail;
                                    currentPaymentSum = safeAdd(currentPaymentSum, sumReceiptDetail);
                                    BigDecimal discountSumReceiptDetail = readBigDecimalXMLAttribute(positionEntryNode, "discountValue");
                                    //discountSumReceiptDetail = (discountSumReceiptDetail != null && !isSale) ? discountSumReceiptDetail.negate() : discountSumReceiptDetail; 
                                    Integer numberReceiptDetail = readIntegerXMLAttribute(positionEntryNode, "order");

                                    Date startDate = directoryStartDateMap.get(directory + "_" + numberCashRegister);
                                    if (startDate == null || dateReceipt.compareTo(startDate) >= 0) {
                                        Integer nppGroupMachinery = directoryDepartNumberGroupCashRegisterMap.get(directory + "_" + numberCashRegister + "_" + departNumber);
                                        nppGroupMachinery = nppGroupMachinery != null ? nppGroupMachinery : directoryGroupCashRegisterMap.get(directory + "_" + numberCashRegister);

                                        String idSaleReceiptReceiptReturnDetail = null;
                                        Element originalPurchase = ((Element) purchaseNode).getChild("original-purchase");
                                        if(originalPurchase != null) {
                                            Integer numberCashRegisterOriginal = readIntegerXMLAttribute(originalPurchase, "cash");
                                            String numberZReportOriginal = readStringXMLAttribute(originalPurchase, "shift");
                                            Integer numberReceiptOriginal = readIntegerXMLAttribute(originalPurchase, "number");
                                            Date dateReceiptOriginal = new Date(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(readStringXMLAttribute(originalPurchase, "saletime")).getTime());
                                            idSaleReceiptReceiptReturnDetail = nppGroupMachinery + "_" + numberCashRegisterOriginal + "_" + numberZReportOriginal + "_"
                                                    + new SimpleDateFormat("ddMMyyyy").format(dateReceiptOriginal) + "_" + numberReceiptOriginal;
                                        }

                                        currentSalesInfoList.add(new SalesInfo(isGiftCard, nppGroupMachinery, numberCashRegister,
                                                numberZReport, dateReceipt, timeReceipt, numberReceipt, dateReceipt, timeReceipt, idEmployee, firstNameEmployee, lastNameEmployee, sumCard, sumCash,
                                                sumGiftCard, barcode, idItem, null, idSaleReceiptReceiptReturnDetail, quantity, price, sumReceiptDetail, discountSumReceiptDetail,
                                                discountSumReceipt, discountCard, numberReceiptDetail, fileName, null));
                                    }
                                    count++;
                                }

                            }

                            //чит для случая, когда не указана сумма платежа. Недостающую сумму пишем в наличные.
                            BigDecimal sum = safeAdd(safeAdd(sumCard, sumCash), sumGiftCard);
                            if (sum == null || sum.compareTo(currentPaymentSum) < 0)
                                for (SalesInfo salesInfo : currentSalesInfoList) {
                                    salesInfo.sumCash = safeSubtract(safeSubtract(currentPaymentSum, sumCard), sumGiftCard);
                                }

                            salesInfoList.addAll(currentSalesInfoList);
                        }
                        filePathList.add(file.getAbsolutePath());
                    }
                } catch (Throwable e) {
                    sendSalesLogger.error("File: " + file.getAbsolutePath(), e);
                }
            }
        }
        return (salesInfoList.isEmpty() && filePathList.isEmpty()) ? null :
                new Kristal10SalesBatch(salesInfoList, filePathList);
    }

    @Override
    public Map<String, List<Object>> readExtraCheckZReport(List<CashRegisterInfo> cashRegisterInfoList) {

        Map<String, List<Object>> zReportSumMap = new HashMap<>();

        Set<String> directorySet = new HashSet<>();
        Map<String, Integer> directoryGroupCashRegisterMap = new HashMap<>();
        for (CashRegisterInfo c : cashRegisterInfoList) {
            if (c.directory != null && c.handlerModel.endsWith("Kristal10Handler"))
                directorySet.add(c.directory);
            if (c.directory != null && c.number != null && c.numberGroup != null)
                directoryGroupCashRegisterMap.put(c.directory + "_" + c.number, c.numberGroup);
        }

        for (String directory : directorySet) {

            String exchangeDirectory = directory + "/reports/";

            File[] filesList = new File(exchangeDirectory).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith("zreports") && pathname.getPath().endsWith(".xml");
                }
            });

            if (filesList != null && filesList.length > 0) {
                sendSalesLogger.info("Kristal10: found " + filesList.length + " z-report(s) in " + exchangeDirectory);

                for (File file : filesList) {
                    try {
                        String fileName = file.getName();
                        sendSalesLogger.info("Kristal10: reading " + fileName);
                        if (isFileLocked(file)) {
                            sendSalesLogger.info("Kristal10: " + fileName + " is locked");
                        } else {
                            SAXBuilder builder = new SAXBuilder();

                            Document document = builder.build(file);
                            Element rootNode = document.getRootElement();
                            List zReportsList = rootNode.getChildren("zreport");

                            for (Object zReportNode : zReportsList) {

                                Integer numberCashRegister = readIntegerXMLValue(zReportNode, "cashNumber");
                                Integer numberGroupCashRegister = directoryGroupCashRegisterMap.get(directory + "_" + numberCashRegister);
                                String numberZReport = readStringXMLValue(zReportNode, "shiftNumber");
                                String idZReport = numberGroupCashRegister + "_" + numberCashRegister + "_" + numberZReport;

                                BigDecimal sumSale = readBigDecimalXMLValue(zReportNode, "amountByPurchaseFiscal");
                                BigDecimal sumReturn = readBigDecimalXMLValue(zReportNode, "amountByReturnFiscal");
                                BigDecimal kristalSum = safeSubtract(sumSale, sumReturn);
                                zReportSumMap.put(idZReport, Arrays.asList((Object) kristalSum, numberCashRegister, numberZReport, idZReport));

                            }
                            File successDir = new File(file.getParent() + "/success/");
                            if (successDir.exists() || successDir.mkdirs())
                                FileCopyUtils.copy(file, new File(file.getParent() + "/success/" + file.getName()));
                            if(!file.delete())
                                file.deleteOnExit();
                        }
                    } catch (Throwable e) {
                        sendSalesLogger.error("File: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return zReportSumMap.isEmpty() ? null : zReportSumMap;
    }


    @Override
    public ExtraCheckZReportBatch compareExtraCheckZReport(Map<String, List<Object>> handlerZReportSumMap, Map<String, BigDecimal> baseZReportSumMap) {

        String message = "";
        List<String> idZReportList = new ArrayList<>();

        for (Map.Entry<String, List<Object>> kristalEntry : handlerZReportSumMap.entrySet()) {

            String idZReportHandler = kristalEntry.getKey();
            List<Object> valuesHandler = kristalEntry.getValue();
            BigDecimal sumHandler = (BigDecimal) valuesHandler.get(0);
            Integer numberCashRegister = (Integer) valuesHandler.get(1);
            String numberZReport = (String) valuesHandler.get(2);
            String idZReport = (String) valuesHandler.get(3);

            BigDecimal sumBase = baseZReportSumMap.get(idZReportHandler);

            if (sumHandler == null || sumBase == null || sumHandler.doubleValue() != sumBase.doubleValue())
                message += String.format("CashRegister %s. \nZReport %s checksum failed: %s(fusion) != %s(kristal);\n",
                        numberCashRegister, numberZReport, sumBase, sumHandler);
            else
                idZReportList.add(idZReport);
        }
        return idZReportList.isEmpty() && message.isEmpty() ? null : new ExtraCheckZReportBatch(idZReportList, message);
    }

    private File exportXML(Document doc, String exchangeDirectory, String prefix) throws IOException {
        File file = makeExportFile(exchangeDirectory, prefix);
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(encoding));
        PrintWriter fw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        xmlOutput.output(doc, fw);
        fw.close();
        return file;
    }

    protected BigDecimal safeAdd(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else return (operand1 == null ? operand2 : (operand2 == null ? operand1 : operand1.add(operand2)));
    }

    protected BigDecimal safeSubtract(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else
            return (operand1 == null ? operand2.negate() : (operand2 == null ? operand1 : operand1.subtract((operand2))));
    }

    private String transformBarcode(String idBarcode, String weightCode, boolean passScalesItem) {
        //временное решение для весовых товаров
        return passScalesItem && idBarcode.length() <= 6 && weightCode != null ? (weightCode + idBarcode) : idBarcode;
    }

    private String transformUPCBarcode(String idBarcode, String transformUPCBarcode) {
        if(idBarcode != null && transformUPCBarcode != null) {
            if(transformUPCBarcode.equals("13to12") && idBarcode.length() == 13 && idBarcode.startsWith("0"))
                idBarcode = idBarcode.substring(1);
            else if(transformUPCBarcode.equals("12to13") && idBarcode.length() == 12)
                idBarcode += "0";

        }
        return idBarcode;
    }

    private String readStringXMLValue(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getChildText(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        return value;
    }

    private String readStringXMLAttribute(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getAttributeValue(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        return value;
    }

    private BigDecimal readBigDecimalXMLValue(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getChildText(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            sendSalesLogger.error("Kristal10 Error: ", e);
            return null;
        }
    }

    private BigDecimal readBigDecimalXMLAttribute(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getAttributeValue(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            sendSalesLogger.error("Kristal10 Error: ", e);
            return null;
        }
    }

    private Integer readIntegerXMLValue(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getChildText(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            sendSalesLogger.error("Kristal10 Error: ", e);
            return null;
        }
    }

    private Integer readIntegerXMLAttribute(Object element, String field) {
        if (element == null || !(element instanceof Element))
            return null;
        String value = ((Element) element).getAttributeValue(field);
        if (value == null || value.isEmpty()) {
            sendSalesLogger.error("Attribute " + field + " is empty");
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            sendSalesLogger.error("Kristal10 Error: ", e);
            return null;
        }
    }

    private double parseWeight(String value) {
        try {
            return (double) Integer.parseInt(value) / 1000;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static boolean isFileLocked(File file) {
        boolean isLocked = false;
        FileChannel channel = null;
        FileLock lock = null;
        try {
            channel = new RandomAccessFile(file, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null)
                isLocked = true;
        } catch (Exception e) {
            sendSalesLogger.info(e);
            isLocked = true;
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception e) {
                    sendSalesLogger.info(e);
                    isLocked = true;
                }
            }
            if (channel != null)
                try {
                    channel.close();
                } catch (IOException e) {
                    sendSalesLogger.info(e);
                    isLocked = true;
                }
        }
        return isLocked;
    }
}
