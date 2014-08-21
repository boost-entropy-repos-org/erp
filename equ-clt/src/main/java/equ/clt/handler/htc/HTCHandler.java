package equ.clt.handler.htc;

import com.google.common.base.Throwables;
import equ.api.SalesBatch;
import equ.api.SalesInfo;
import equ.api.SoftCheckInfo;
import equ.api.cashregister.*;
import equ.clt.EquipmentServer;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.xBaseJ.DBF;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.LogicalField;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTCHandler extends CashRegisterHandler<HTCSalesBatch> {

    protected final static Logger logger = Logger.getLogger(EquipmentServer.class);
    String charset = "cp866";

    public HTCHandler() {
    }

    @Override
    public void sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws IOException {

        logger.info("HTC: Send Transaction # " + transactionInfo.id);

        Set<String> directorySet = new HashSet<String>();
        for (CashRegisterInfo cashRegisterInfo : machineryInfoList) {
            if (cashRegisterInfo.directory != null)
                directorySet.add(cashRegisterInfo.directory.trim());
        }

        try {

            NumField CODE = new NumField("CODE", 6, 0);
            NumField GROUP = new NumField("GROUP", 6, 0);
            LogicalField ISGROUP = new LogicalField("ISGROUP");
            CharField ARTICUL = new CharField("ARTICUL", 20);
            NumField BAR_CODE = new NumField("BAR_CODE", 13, 0);
            CharField PRODUCT_ID = new CharField("PRODUCT_ID", 32);
            CharField TABLO_ID = new CharField("TABLO_ID", 20);
            NumField PRICE = new NumField("PRICE", 10, 0);
            NumField QUANTITY = new NumField("QUANTITY", 10, 3);
            LogicalField WEIGHT = new LogicalField("WEIGHT");
            NumField SECTION = new NumField("SECTION", 1, 0);
            NumField FLAGS = new NumField("FLAGS", 3, 0);
            NumField CMD = new NumField("CMD", 2, 0);

            for (String directory : directorySet) {

                String fileName = transactionInfo.snapshot ? "NewPrice.dbf" : "UpdPrice.dbf";
                String mdxName = transactionInfo.snapshot ? "NewPrice.mdx" : "UpdPrice.mdx";
                logger.info(String.format("HTC: creating %s file", fileName));
                File priceFile = new File(directory + "\\" + fileName);
                File flagPriceFile = new File(directory + "\\price.qry");

                boolean append = priceFile.exists();

                if (!transactionInfo.itemsList.isEmpty()) {
                    DBF dbfFile = append ? new DBF(priceFile.getAbsolutePath(), charset) : new DBF(priceFile.getAbsolutePath(), DBF.DBASEIV, true, charset);
                    if (!append)
                        dbfFile.addField(new Field[]{CODE, GROUP, ISGROUP, ARTICUL, BAR_CODE, PRODUCT_ID, TABLO_ID, PRICE, QUANTITY, WEIGHT, SECTION, FLAGS, CMD});

                    Map<String, Integer> barcodeRecordMap = new HashMap<String, Integer>();
                    for (int i = 1; i <= dbfFile.getRecordCount(); i++) {
                        String barcode = getDBFFieldValue(dbfFile, "BAR_CODE", charset);
                        barcodeRecordMap.put(barcode, i);
                    }

                    for (CashRegisterItemInfo item : transactionInfo.itemsList) {

                        String parent = null;
                        for (ItemGroup itemGroup : item.hierarchyItemGroup) {

                            Integer recordNumber = barcodeRecordMap.get(itemGroup.idItemGroup);
                            if (recordNumber != null)
                                dbfFile.gotoRecord(recordNumber);
                            String idItemGroup = itemGroup.idItemGroup == null ? null : (itemGroup.idItemGroup.equals("Все") ? "0" : itemGroup.idItemGroup.replace("_", "")); 
                            putField(dbfFile, "CODE", idItemGroup);
                            putField(dbfFile, "GROUP", parent);
                            putField(dbfFile, "ISGROUP", "T");
                            putField(dbfFile, "BAR_CODE", itemGroup.idItemGroup);
                            putField(dbfFile, "PRODUCT_ID", trim(itemGroup.nameItemGroup, 32));
                            putField(dbfFile, "TABLO_ID", trim(itemGroup.nameItemGroup, 20));
                            putField(dbfFile, "PRICE", null);
                            putField(dbfFile, "FLAGS", "0");
                            if (recordNumber != null)
                                dbfFile.update();
                            else {
                                dbfFile.write();
                                dbfFile.file.setLength(dbfFile.file.length() - 1);
                                barcodeRecordMap.put(itemGroup.idItemGroup, barcodeRecordMap.size() + 1);
                            }
                            parent = trim(itemGroup.idItemGroup, 6);
                        }
                        
                        Integer recordNumber = barcodeRecordMap.get(item.idBarcode);
                        if (recordNumber != null)
                            dbfFile.gotoRecord(recordNumber);
                        boolean isWeight = item.idBarcode != null && item.idBarcode.length()<=5;
                        putField(dbfFile, "CODE", isWeight ? item.idBarcode : "999999");
                        putField(dbfFile, "GROUP", trim(item.hierarchyItemGroup.get(0).idItemGroup, 6));
                        putField(dbfFile, "ISGROUP", "F");
                        putField(dbfFile, "BAR_CODE", item.idBarcode);
                        putField(dbfFile, "PRODUCT_ID", trim(item.name, 32));
                        putField(dbfFile, "TABLO_ID", trim(item.name, 20));
                        putField(dbfFile, "PRICE", String.valueOf(item.price.intValue()));
                        putField(dbfFile, "WEIGHT", item.splitItem ? "T" : "F");
                        putField(dbfFile, "FLAGS", item.splitItem ? "1" : "0");
                        if (recordNumber != null)
                            dbfFile.update();
                        else {
                            dbfFile.write();
                            dbfFile.file.setLength(dbfFile.file.length() - 1);
                            barcodeRecordMap.put(item.idBarcode, barcodeRecordMap.size() + 1);
                        }
                    }
                    dbfFile.close();
                    if (!append)
                        new File(directory + "\\" + mdxName).delete();

                    flagPriceFile.createNewFile();
                    logger.info("HTC: waiting for deletion of price.qry file");
                    waitForDeletion(priceFile, flagPriceFile);
                }
            }
        } catch (xBaseJException e) {
            throw Throwables.propagate(e);
        }
    }

    private void putField(DBF dbfFile, String field, String value) throws xBaseJException {
        dbfFile.getField(field).put(value == null ? "null" : value);
    }

    private void waitForDeletion(File file, File flagFile) {
        int count = 0;
        while ((flagFile.exists() || file.exists()) && count < 10) {
            try {
                Thread.sleep(1000);
                count++;
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {
    }

    @Override
    public CashDocumentBatch readCashDocumentInfo(List<CashRegisterInfo> cashRegisterInfoList, Set<String> cashDocumentSet) throws ClassNotFoundException {
        return null;
    }

    @Override
    public String checkZReportSum(Map<String, BigDecimal> zReportSumMap, String idStock) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public void finishReadingCashDocumentInfo(CashDocumentBatch cashDocumentBatch) {
    }

    @Override
    public Map<String, Timestamp> requestSucceededSoftCheckInfo(Set<String> directorySet) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public void sendStopListInfo(StopListInfo stopListInfo, Set<String> directorySet) throws IOException {

    }

    @Override
    public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException, ClassNotFoundException {

        Set<String> directorySet = new HashSet<String>();
        Map<String, Integer> directoryCashRegisterMap = new HashMap<String, Integer>();
        Map<String, Integer> directoryGroupCashRegisterMap = new HashMap<String, Integer>();
        for (CashRegisterInfo c : cashRegisterInfoList) {
            if (c.handlerModel != null && c.directory != null && c.handlerModel.endsWith("HTCHandler")) {
                directorySet.add(c.directory);
                if (c.number != null)
                    directoryCashRegisterMap.put(c.directory, c.number);
                if (c.numberGroup != null)
                    directoryGroupCashRegisterMap.put(c.directory, c.numberGroup);
            }
        }

        List<SalesInfo> salesInfoList = new ArrayList<SalesInfo>();
        List<String> filePathList = new ArrayList<String>();
        for (String directory : directorySet) {

            File salesFile = new File(directory + "\\Sales.dbf");
            File receiptFile = new File(directory + "\\Receipt.dbf");

            if (!salesFile.exists() || !receiptFile.exists())
                logger.info("HTC: No sale or receipt file found in " + directory);
            else {
                logger.info("HTC: found sale and receipt files in " + directory);

                try {

                    Map<Integer, List<Object>> receiptMap = new HashMap<Integer, List<Object>>();
                    DBF receiptDBFFile = new DBF(receiptFile.getAbsolutePath());
                    int receiptRecordCount = receiptDBFFile.getRecordCount();
                    for (int i = 0; i < receiptRecordCount; i++) {
                        receiptDBFFile.read();
                        Integer numberReceipt = getDBFIntegerFieldValue(receiptDBFFile, "NUMDOC", charset);
                        BigDecimal sumCash = getDBFBigDecimalFieldValue(receiptDBFFile, "COST1", charset);
                        BigDecimal sumCard = getDBFBigDecimalFieldValue(receiptDBFFile, "COST2", charset);
                        receiptMap.put(numberReceipt, Arrays.asList((Object) sumCash, sumCard));

                    }
                    receiptDBFFile.close();

                    DBF dbfFile = new DBF(salesFile.getAbsolutePath());
                    int recordCount = dbfFile.getRecordCount();

                    Map<Integer, Integer> numberReceiptDetailMap = new HashMap<Integer, Integer>();
                    for (int i = 0; i < recordCount; i++) {

                        dbfFile.read();
                        Integer numberReceipt = getDBFIntegerFieldValue(dbfFile, "CHECK", charset);

                        List<Object> receiptEntry = receiptMap.get(numberReceipt);
                        BigDecimal sumCash = receiptEntry == null ? null : (BigDecimal) receiptEntry.get(0);
                        BigDecimal sumCard = receiptEntry == null ? null : (BigDecimal) receiptEntry.get(1);

                        Date dateReceipt = getDBFDateFieldValue(dbfFile, "DATE", charset);
                        Time timeReceipt = new Time(DateUtils.parseDate(getDBFFieldValue(dbfFile, "TIME", charset), new String[]{"HH:mm:ss"}).getTime());
                        
                        String barcodeItem = getDBFFieldValue(dbfFile, "BAR_CODE", charset);
                        //временный чит для корректировки весовых штрихкодов
                        barcodeItem = (barcodeItem != null && barcodeItem.startsWith("22") && barcodeItem.length()==13) ? barcodeItem.substring(2, 7) : barcodeItem;
                        
                        BigDecimal quantityReceiptDetail = getDBFBigDecimalFieldValue(dbfFile, "COUNT", charset);
                        BigDecimal priceReceiptDetail = getDBFBigDecimalFieldValue(dbfFile, "PRICE", charset);
                        BigDecimal sumReceiptDetail = getDBFBigDecimalFieldValue(dbfFile, "COST", charset);
                        BigDecimal discountSumReceiptDetail = getDBFBigDecimalFieldValue(dbfFile, "SUMDISC_ON", charset);
                        BigDecimal discountSumReceipt = getDBFBigDecimalFieldValue(dbfFile, "SUMDISC_OF", charset);

                        Integer nppMachinery = directoryCashRegisterMap.get(directory);
                        Integer nppGroupMachinery = directoryGroupCashRegisterMap.get(directory);
                        Integer numberReceiptDetail = numberReceiptDetailMap.get(numberReceipt);
                        numberReceiptDetail = numberReceiptDetail == null ? 1 : (numberReceiptDetail + 1);
                        numberReceiptDetailMap.put(numberReceipt, numberReceiptDetail);
                        String numberZReport = new SimpleDateFormat("ddMMyy").format(dateReceipt) + "/" + nppGroupMachinery + "/" + nppMachinery;
                        
                        salesInfoList.add(new SalesInfo(nppGroupMachinery, nppMachinery, numberZReport, numberReceipt, dateReceipt,
                                timeReceipt, sumCard, sumCash, barcodeItem, null, quantityReceiptDetail, priceReceiptDetail,
                                sumReceiptDetail, discountSumReceiptDetail, discountSumReceipt, null, numberReceiptDetail,
                                salesFile.getName()));

                    }

                    dbfFile.close();

                } catch (Throwable e) {
                    logger.error("File: " + salesFile.getAbsolutePath(), e);
                }
                filePathList.add(salesFile.getAbsolutePath());
            }
            if (receiptFile.exists())
                filePathList.add(receiptFile.getAbsolutePath());
        }
        return (salesInfoList.isEmpty() && filePathList.isEmpty()) ? null :
                new HTCSalesBatch(salesInfoList, filePathList);
    }

    @Override
    public void finishReadingSalesInfo(HTCSalesBatch salesBatch) {
        logger.info("HTC: Finish Reading started");
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);
            if (f.delete()) {
                logger.info("HTC: file " + readFile + " has been deleted");
            } else {
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
            }
        }
    }

    @Override
    public String requestSalesInfo(List<RequestExchange> requestExchangeList) throws IOException, ParseException {
        for (RequestExchange entry : requestExchangeList) {
            if(entry.requestSalesInfo) {
                logger.info("HTC: creating request files");
                for (String directory : entry.directorySet) {

                    //если запрос с даты по дату, мы всё равно можем запросить только за 1 день
                    String date = new SimpleDateFormat("dd.MM.yyyy").format(entry.dateFrom);                    

                    if (new File(directory).exists()) {
                        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "\\" + "sales.qry"), "utf-8"));
                        writer.write(date);
                        writer.close();
                    } else
                        return "Error: " + directory + " doesn't exist. Request creation failed.";
                }
            }
        }
        return null;
    }

    protected String getDBFFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        try {
            String result = new String(importFile.getField(fieldName).getBytes(), charset).trim();
            return result.isEmpty() ? null : result;
        } catch (xBaseJException e) {
            return null;
        }
    }

    protected Date getDBFDateFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException, ParseException {
        String dateString = getDBFFieldValue(importFile, fieldName, charset);
        return dateString.isEmpty() ? null : new Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd", "dd.MM.yyyy"}).getTime());
    }

    protected BigDecimal getDBFBigDecimalFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        String result = getDBFFieldValue(importFile, fieldName, charset);
        return (result == null || result.isEmpty() ? null : new BigDecimal(result.replace(",", ".")));
    }

    protected Integer getDBFIntegerFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        String result = getDBFFieldValue(importFile, fieldName, charset);
        return result == null ? null : new Double(result).intValue();
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}
