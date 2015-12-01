package equ.clt.handler.inventoryTech;

import com.google.common.base.Throwables;
import equ.api.SendTransactionBatch;
import equ.api.SoftCheckInfo;
import equ.api.TransactionInfo;
import equ.api.terminal.*;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.xBaseJ.DBF;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

public class InventoryTechHandler extends TerminalHandler {

    protected final static Logger processTransactionLogger = Logger.getLogger("TransactionLogger");
    protected final static Logger sendTerminalDocumentLogger = Logger.getLogger("TerminalDocumentLogger");

    String charset = "cp866";
    
    public InventoryTechHandler() {
    }

    @Override
    public String getGroupId(TransactionInfo transactionInfo) throws IOException {
        return "inventoryTech";
    }

    @Override
    public Map<Integer, SendTransactionBatch> sendTransaction(List transactionList) throws IOException {

        Map<Integer, SendTransactionBatch> sendTransactionBatchMap = new HashMap<>();

        for(Object transaction : transactionList) {

            Exception exception = null;
            try {

                processTransactionLogger.info("InventoryTechTerminal: send Transaction #" + ((TransactionInfo)transaction).id);

                Set<String> directorySet = new HashSet<>();
                for (Object m : ((TransactionInfo) transaction).machineryInfoList) {
                    TerminalInfo t = (TerminalInfo) m;
                    if (t.directory != null)
                        directorySet.add(t.directory);
                }

                for (String path : directorySet) {
                    File directory = new File(path);
                    if (!directory.exists())
                        directory.mkdir();

                    if (!directory.exists())
                        processTransactionLogger.info("Directory " + directory.getAbsolutePath() + " doesn't exist");

                    try {
                        Class.forName("org.sqlite.JDBC");

                        createGoodsFile((TransactionTerminalInfo)transaction, path);
                        createBarcodeFile((TransactionTerminalInfo)transaction, path);
                        createSpravFile((TransactionTerminalInfo)transaction, path);
                        createSprDocFile((TransactionTerminalInfo)transaction, path);

                        createBasesUpdFile(path);

                    } catch (Exception e) {
                        processTransactionLogger.error("InventoryTech Error: ", e);
                        throw Throwables.propagate(e);
                    }
                }
            } catch(Exception e) {
                exception = e;
            }
            sendTransactionBatchMap.put(((TransactionInfo) transaction).id, new SendTransactionBatch(exception));
        }
        return sendTransactionBatchMap;
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {
    }

    @Override
    public void sendTerminalOrderList(List list, Integer nppGroupTerminal, String directorySet) throws IOException {
    }

    @Override
    public void saveTransactionTerminalInfo(TransactionTerminalInfo transactionInfo) throws IOException {
    }

    @Override
    public void finishReadingTerminalDocumentInfo(TerminalDocumentBatch terminalDocumentBatch) {
        
        for(Map.Entry<String, Set<Integer>> entry : ((InventoryTerminalDocumentBatch) terminalDocumentBatch).docRecordsMap.entrySet()) {

            DBF dbfFile = null;
            try {

                CharField ACCEPTED = new CharField("ACCEPTED", 1);

                dbfFile = new DBF(entry.getKey());

                for (Integer recordNumber : entry.getValue()) {
                    if (recordNumber != null) {
                        dbfFile.gotoRecord(recordNumber);
                        putField(dbfFile, ACCEPTED, "1", true);
                        dbfFile.update();
                    }
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                try {
                    if (dbfFile != null)
                        dbfFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public InventoryTerminalDocumentBatch readTerminalDocumentInfo(List machineryInfoList) throws IOException {

        try {

            Set<String> directorySet = new HashSet<>();
            Map<String, Integer> groupIds = new HashMap<>();
            for (Object m : machineryInfoList) {
                TerminalInfo t = (TerminalInfo) m;
                if (t.directory != null && t.handlerModel != null && t.handlerModel.endsWith("InventoryTechHandler")) {
                    directorySet.add(t.directory);
                    groupIds.put(t.directory, t.numberGroup);
                }
            }

            List<TerminalDocumentDetail> terminalDocumentDetailList = new ArrayList<>();
            Map<String, Set<Integer>> docRecordsMap = new HashMap<>();
            
            for (String directory : directorySet) {
                
                File docFile = new File(directory + "/DOC.DBF");
                File posFile = new File(directory + "/POS.DBF");

                if (!docFile.exists() || !posFile.exists())
                    sendTerminalDocumentLogger.info("InventoryTech: doc.dbf or pos.dbf not found in " + directory);
                else {
                    sendTerminalDocumentLogger.info("InventoryTech: found doc.dbf and pos.dbf in " + directory);
                    int count = 0;
                    Map<String, List<Object>> docDataMap = readDocFile(docFile);

                    DBF dbfFile = null;
                    try {

                        dbfFile = new DBF(posFile.getAbsolutePath());
                        int recordCount = dbfFile.getRecordCount();

                        for (int i = 0; i < recordCount; i++) {
                            dbfFile.read();
                            if (dbfFile.deleted()) continue; 
                            String idDoc = getDBFFieldValue(dbfFile, "IDDOC", charset);
                            List<Object> docEntry = docDataMap.get(idDoc);
                            if(docEntry != null) {
                                String title = (String) docEntry.get(0);
                                String idTerminalHandbookType1 = (String) docEntry.get(1);
                                String idTerminalHandbookType2 = (String) docEntry.get(2);
                                BigDecimal quantityDocument = (BigDecimal) docEntry.get(3);
                                String idDocumentType = (String) docEntry.get(4);
                                Timestamp dateTime = (Timestamp) docEntry.get(5);
                                Date date = dateTime == null ? null : new Date(dateTime.getTime());
                                Time time = dateTime == null ? null : new Time(dateTime.getTime());

                                String idBarcode = getDBFFieldValue(dbfFile, "ARTICUL", charset);
                                String name = getDBFFieldValue(dbfFile, "NAME", charset);
                                String number = getDBFFieldValue(dbfFile, "NOMPOS", charset);
                                BigDecimal price = getDBFBigDecimalFieldValue(dbfFile, "PRICE", charset);
                                BigDecimal quantity = getDBFBigDecimalFieldValue(dbfFile, "QUAN", charset);
                                BigDecimal sum = safeMultiply(price, quantity);
                                Integer numberGroup = groupIds.get(directory);
                                String idDocument = numberGroup + "/" + idDoc + "/" + dateTime;
                                String idDocumentDetail = idDocument + "/" + i;
                                count++;
                                terminalDocumentDetailList.add(new TerminalDocumentDetail(idDocument, title, date, time,
                                        null, directory, idTerminalHandbookType1, idTerminalHandbookType2, idDocumentType,
                                        quantityDocument, idDocumentDetail, number, idBarcode, name, price, quantity, sum));
                            }
                        }
                    } finally {
                        if (dbfFile != null)
                            dbfFile.close();
                    }

                    Set<Integer> docRecordsSet = new HashSet<>();
                    for (Map.Entry<String, List<Object>> entry : docDataMap.entrySet()) {
                        Integer recordNumber = (Integer) entry.getValue().get(6);
                        if (recordNumber != null)
                            docRecordsSet.add(recordNumber);
                    }
                    docRecordsMap.put(docFile.getAbsolutePath(), docRecordsSet);
                    if (count > 0) {
                        sendTerminalDocumentLogger.info(String.format("InventoryTech: processed %s records in %s", count, directory));
                    }
                }
            }

            return new InventoryTerminalDocumentBatch(terminalDocumentDetailList, docRecordsMap);
        } catch (Exception e) {
            sendTerminalDocumentLogger.error("InventoryTech Error: ", e);
            throw Throwables.propagate(e);
        }
    }

    private Map<String, List<Object>> readDocFile(File file) throws SQLException, IOException, xBaseJException, ParseException {

        Map<String, List<Object>> data = new HashMap<>();
        DBF dbfFile = null;
        try {

            dbfFile = new DBF(file.getAbsolutePath());
            int recordCount = dbfFile.getRecordCount();

            for (int i = 0; i < recordCount; i++) {
                dbfFile.read();
                if (dbfFile.deleted()) continue;
                String idDoc = getDBFFieldValue(dbfFile, "IDDOC", charset);
                String title = getDBFFieldValue(dbfFile, "TITLE", charset);
                String dateTimeValue = getDBFFieldValue(dbfFile, "CRE_DTST", charset);
                Timestamp dateTime = dateTimeValue == null ? null : new Timestamp(DateUtils.parseDate(dateTimeValue, new String[] {"yyyyMMddHHmmss"}).getTime());
                String idTerminalHandbookType1 = getDBFFieldValue(dbfFile, "CSPR1", charset);
                String idTerminalHandbookType2 = getDBFFieldValue(dbfFile, "CSPR2", charset);
                BigDecimal quantityDocument = getDBFBigDecimalFieldValue(dbfFile, "QUANDOC", charset);
                String idDocumentType = getDBFFieldValue(dbfFile, "CVIDDOC", charset);
                String accepted = getDBFFieldValue(dbfFile, "ACCEPTED", charset);
                if(accepted != null && accepted.equals("0"))
                    data.put(idDoc, Arrays.asList((Object) title, idTerminalHandbookType1, idTerminalHandbookType2, 
                            quantityDocument, idDocumentType, dateTime, i + 1));

            }
        } finally {
            if (dbfFile != null)
                dbfFile.close();
        }
        return data;
    }

    private File createGoodsFile(TransactionTerminalInfo transaction, String path) throws IOException, xBaseJException {

        if (listNotEmpty(transaction.itemsList)) {

            CharField ARTICUL = new CharField("ARTICUL", 15);
            CharField NAME = new CharField("NAME", 200);
            NumField QUAN = new NumField("QUAN", 9, 3);
            NumField PRICE = new NumField("PRICE", 11, 2);
            NumField PRICE2 = new NumField("PRICE2", 11, 2);
            CharField GR_NAME = new CharField("GR_NAME", 200);
            NumField FLAGS = new NumField("FLAGS", 8, 0);
            NumField INBOX = new NumField("INBOX", 9, 3);
            NumField IDSET = new NumField("IDSET", 8, 0);
            
            File directory = new File(path);
            if (directory.exists()) {
                File fileDBF = new File(path + "/GOODS.dbf");
                File fileMDX = new File(path + "/GOODS.mdx");
                File fileCDX = new File(path + "/GOODS.cdx");
                if (transaction.snapshot) {
                    fileDBF.delete();
                    fileMDX.delete();
                    fileCDX.delete();
                }
                boolean append = !transaction.snapshot && fileDBF.exists();
                DBF dbfWriter = null;
                try {
                    dbfWriter = append ? new DBF(fileDBF.getAbsolutePath(), charset) : new DBF(fileDBF.getAbsolutePath(), DBF.DBASEIV, true, charset);

                    if (!append)
                        dbfWriter.addField(new Field[]{ARTICUL, NAME, QUAN, PRICE, PRICE2, GR_NAME, FLAGS, INBOX, IDSET});

                    Map<String, Integer> barcodeRecordMap = new HashMap<>();
                    for (int i = 1; i <= dbfWriter.getRecordCount(); i++) {
                        dbfWriter.read();
                        String barcode = getDBFFieldValue(dbfWriter, "ARTICUL", charset);
                        barcodeRecordMap.put(barcode, i);
                    }
                    dbfWriter.startTop();

                    Set<String> usedBarcodes = new HashSet<>();
                    for (TerminalItemInfo item : transaction.itemsList) {
                        if (!Thread.currentThread().isInterrupted()) {
                            if (!usedBarcodes.contains(item.idBarcode)) {
                                Integer recordNumber = null;
                                if (append) {
                                    recordNumber = barcodeRecordMap.get(item.idBarcode);
                                    if (recordNumber != null)
                                        dbfWriter.gotoRecord(recordNumber);
                                }

                                putField(dbfWriter, ARTICUL, trim(item.idBarcode, 15), append);
                                putField(dbfWriter, NAME, trim(item.name, 200), append);
                                putField(dbfWriter, QUAN, String.valueOf(item.quantity == null ? 1 : item.quantity.intValue()), append);
                                putField(dbfWriter, PRICE, String.valueOf(item.price == null ? 0 : item.price.intValue()), append);

                                if (recordNumber != null)
                                    dbfWriter.update();
                                else {
                                    dbfWriter.write();
                                    dbfWriter.file.setLength(dbfWriter.file.length() - 1);
                                    if (append)
                                        barcodeRecordMap.put(item.idBarcode, barcodeRecordMap.size() + 1);
                                }
                                usedBarcodes.add(item.idBarcode);
                            }
                        }
                    }
                } finally {
                    if(dbfWriter != null)
                        dbfWriter.close();
                }
                return fileDBF;
            }
        }
        return null;
    }

    private File createBarcodeFile(TransactionTerminalInfo transaction, String path) throws IOException, xBaseJException {

        if (listNotEmpty(transaction.itemsList)) {

            CharField ARTICUL = new CharField("ARTICUL", 15);
            CharField BARCODE = new CharField("BARCODE", 26);
            NumField IDSET = new NumField("IDSET", 8, 0);

            File directory = new File(path);
            if (directory.exists()) {
                File fileDBF = new File(path + "/BARCODE.dbf");
                File fileMDX = new File(path + "/BARCODE.mdx");
                File fileCDX = new File(path + "/BARCODE.cdx");
                if (transaction.snapshot) {
                    fileDBF.delete();
                    fileMDX.delete();
                    fileCDX.delete();
                }
                boolean append = !transaction.snapshot && fileDBF.exists();
                DBF dbfWriter = null;
                try {
                    dbfWriter = append ? new DBF(fileDBF.getAbsolutePath(), charset) : new DBF(fileDBF.getAbsolutePath(), DBF.DBASEIV, true, charset);

                    if (!append)
                        dbfWriter.addField(new Field[]{ARTICUL, BARCODE, IDSET});

                    Map<String, Integer> barcodeRecordMap = new HashMap<>();
                    for (int i = 1; i <= dbfWriter.getRecordCount(); i++) {
                        dbfWriter.read();
                        String barcode = getDBFFieldValue(dbfWriter, "ARTICUL", charset);
                        barcodeRecordMap.put(barcode, i);
                    }
                    dbfWriter.startTop();

                    Set<String> usedBarcodes = new HashSet<>();
                    for (TerminalItemInfo item : transaction.itemsList) {
                        if (!Thread.currentThread().isInterrupted()) {
                            if (!usedBarcodes.contains(item.idBarcode)) {
                                Integer recordNumber = null;
                                if (append) {
                                    recordNumber = barcodeRecordMap.get(item.idBarcode);
                                    if (recordNumber != null)
                                        dbfWriter.gotoRecord(recordNumber);
                                }

                                putField(dbfWriter, ARTICUL, trim(item.idBarcode, 15), append);
                                putField(dbfWriter, BARCODE, trim(item.idBarcode, 26), append);
                                
                                if (recordNumber != null)
                                    dbfWriter.update();
                                else {
                                    dbfWriter.write();
                                    dbfWriter.file.setLength(dbfWriter.file.length() - 1);
                                    if (append)
                                        barcodeRecordMap.put(item.idBarcode, barcodeRecordMap.size() + 1);
                                }
                                usedBarcodes.add(item.idBarcode);
                            }
                        }
                    }
                } finally {
                    if(dbfWriter != null)
                        dbfWriter.close();
                }
                return fileDBF;
            }
        }
        return null;
    }

    private File createSpravFile(TransactionTerminalInfo transaction, String path) throws IOException, xBaseJException {

        if (listNotEmpty(transaction.terminalLegalEntityList)) {
            
            CharField CODE = new CharField("CODE", 15);
            CharField NAME = new CharField("NAME", 200);
            NumField VIDSPR = new NumField("VIDSPR", 8, 0);
            CharField COMMENT = new CharField("COMMENT", 200);
            NumField IDTERM = new NumField("IDTERM", 8, 0);
            NumField MTERM = new NumField("MTERM", 8, 0);
            NumField DISCOUNT = new NumField("DISCOUNT", 5, 2);
            NumField ROUND = new NumField("ROUND", 11, 2);
            NumField FLAGS = new NumField("FLAGS", 8, 0);
            NumField IDSET = new NumField("IDSET", 8, 0);

            File directory = new File(path);
            if (directory.exists()) {
                File fileDBF = new File(path + "/SPRAV.dbf");
                File fileMDX = new File(path + "/SPRAV.mdx");
                File fileCDX = new File(path + "/SPRAV.cdx");
                if (transaction.snapshot) {
                    fileDBF.delete();
                    fileMDX.delete();
                    fileCDX.delete();
                }
                boolean append = !transaction.snapshot && fileDBF.exists();
                DBF dbfWriter = null;
                try {
                    dbfWriter = append ? new DBF(fileDBF.getAbsolutePath(), charset) : new DBF(fileDBF.getAbsolutePath(), DBF.DBASEIV, true, charset);

                    if (!append)
                        dbfWriter.addField(new Field[]{CODE, NAME, VIDSPR, COMMENT, IDTERM, MTERM, DISCOUNT, ROUND, FLAGS, IDSET});

                    Map<String, Integer> recordMap = new HashMap<>();
                    for (int i = 1; i <= dbfWriter.getRecordCount(); i++) {
                        dbfWriter.read();
                        String code = getDBFFieldValue(dbfWriter, "CODE", charset);
                        recordMap.put(code, i);
                    }
                    dbfWriter.startTop();

                    Set<String> usedCodes = new HashSet<>();
                    putField(dbfWriter, VIDSPR, "10", append);
                    for (TerminalLegalEntity le : transaction.terminalLegalEntityList) {
                        if (!Thread.currentThread().isInterrupted()) {
                            if (!usedCodes.contains(le.idLegalEntity)) {
                                Integer recordNumber = null;
                                if (append) {
                                    recordNumber = recordMap.get(le.idLegalEntity);
                                    if (recordNumber != null)
                                        dbfWriter.gotoRecord(recordNumber);
                                }
                                
                                putField(dbfWriter, CODE, trim(le.idLegalEntity, 15), append);
                                putField(dbfWriter, NAME, trim(le.nameLegalEntity, 200), append);

                                if (recordNumber != null)
                                    dbfWriter.update();
                                else {
                                    dbfWriter.write();
                                    dbfWriter.file.setLength(dbfWriter.file.length() - 1);
                                    if (append)
                                        recordMap.put(le.idLegalEntity, recordMap.size() + 1);
                                }
                                usedCodes.add(le.idLegalEntity);
                            }
                            //count++;
                        }
                    }
                } finally {
                    if(dbfWriter != null)
                        dbfWriter.close();
                }
                return fileDBF;
            }
        }
        return null;
    }
    
    
    private File createSprDocFile(TransactionTerminalInfo transaction, String path) throws IOException, xBaseJException {

        if (listNotEmpty(transaction.terminalDocumentTypeList)) {

            CharField CODE = new CharField("CODE", 15);
            CharField NAME = new CharField("NAME", 50);
            CharField SPRT1 = new CharField("SPRT1", 15);
            NumField VIDSPR1 = new NumField("VIDSPR1", 8, 0);
            CharField SPRT2 = new CharField("SPRT2", 15);
            NumField VIDSPR2 = new NumField("VIDSPR2", 8, 0);
            NumField IDTERM = new NumField("IDTERM", 8, 0);
            NumField MTERM = new NumField("MTERM", 8, 0);
            NumField DISCOUNT = new NumField("DISCOUNT", 5, 2);
            NumField COEF = new NumField("COEF", 8, 0);
            NumField ROUND = new NumField("ROUND", 11, 2);
            NumField FLAGS = new NumField("FLAGS", 8, 0);
            NumField IDSET = new NumField("IDSET",  8, 0);

            File directory = new File(path);
            if (directory.exists()) {
                File fileDBF = new File(path + "/SPRDOC.dbf");
                File fileMDX = new File(path + "/SPRDOC.mdx");
                File fileCDX = new File(path + "/SPRDOC.cdx");
                if (transaction.snapshot) {
                    fileDBF.delete();
                    fileMDX.delete();
                    fileCDX.delete();
                }
                boolean append = !transaction.snapshot && fileDBF.exists();
                DBF dbfWriter = null;
                try {
                    dbfWriter = append ? new DBF(fileDBF.getAbsolutePath(), charset) : new DBF(fileDBF.getAbsolutePath(), DBF.DBASEIV, true, charset);

                    if (!append)
                        dbfWriter.addField(new Field[]{CODE, NAME, SPRT1, VIDSPR1, SPRT2, VIDSPR2, IDTERM, MTERM, DISCOUNT, COEF, ROUND, FLAGS, IDSET});

                    Map<String, Integer> recordMap = new HashMap<>();
                    for (int i = 1; i <= dbfWriter.getRecordCount(); i++) {
                        dbfWriter.read();
                        String code = getDBFFieldValue(dbfWriter, "CODE", charset);
                        recordMap.put(code, i);
                    }
                    dbfWriter.startTop();

                    Set<String> usedCodes = new HashSet<>();
                    for (TerminalDocumentType tdt : transaction.terminalDocumentTypeList) {
                        if (!Thread.currentThread().isInterrupted()) {
                            if (!usedCodes.contains(tdt.id)) {
                                Integer recordNumber = null;
                                if (append) {
                                    recordNumber = recordMap.get(tdt.id);
                                    if (recordNumber != null)
                                        dbfWriter.gotoRecord(recordNumber);
                                }

                                putField(dbfWriter, CODE, trim(tdt.id, 15), append);
                                putField(dbfWriter, NAME, trim(tdt.name, 50), append);
                                String sprt1 = tdt.analytics1 == null ? "" : tdt.analytics1.equals("ПС") ? "Организация" : tdt.analytics1;
                                String vidspr1 = tdt.analytics1 == null ? "0" : tdt.analytics1.equals("ПС") ? "10" : tdt.analytics1;
                                String sprt2 = tdt.analytics2 == null ? "" : tdt.analytics2.equals("ПС") ? "Организация" : tdt.analytics2;
                                String vidspr2 = tdt.analytics2 == null ? "0" : tdt.analytics2.equals("ПС") ? "10" : tdt.analytics2;
                                putField(dbfWriter, VIDSPR1, vidspr1, append);
                                putField(dbfWriter, SPRT1, trim(sprt1, 15), append);
                                putField(dbfWriter, VIDSPR2, vidspr2, append);
                                putField(dbfWriter, SPRT2, trim(sprt2, 15), append);

                                if (recordNumber != null)
                                    dbfWriter.update();
                                else {
                                    dbfWriter.write();
                                    dbfWriter.file.setLength(dbfWriter.file.length() - 1);
                                    if (append)
                                        recordMap.put(tdt.id, recordMap.size() + 1);
                                }
                                usedCodes.add(tdt.id);
                            }
                        }
                    }
                } finally {
                    if(dbfWriter != null)
                        dbfWriter.close();
                }
                return fileDBF;
            }
        }
        return null;
    }

    private boolean createBasesUpdFile(String path) throws IOException {
        File file = new File(path + "/BASES.UPD");
        if(file.createNewFile()) {
//            int count = 0;
//            while (!Thread.currentThread().isInterrupted() && file.exists()) {
//                try {
//                    count++;
//                    if(count >= 60) {
//                        throw Throwables.propagate(new RuntimeException(String.format("Inventory: file %s has been created but not processed by server", file.getAbsolutePath())));
//                    }
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    return false;
//                }
//            }
        }
        return true;
    }

    protected String getDBFFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        try {
            String result = new String(importFile.getField(fieldName).getBytes(), charset).trim();
            return result.isEmpty() ? null : result;
        } catch (xBaseJException e) {
            return null;
        }
    }

    protected BigDecimal getDBFBigDecimalFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        String result = getDBFFieldValue(importFile, fieldName, charset);
        return (result == null || result.isEmpty() ? null : new BigDecimal(result.replace(",", ".")));
    }
    
    private void putField(DBF dbfFile, Field field, String value, boolean append) throws xBaseJException {
        if(append)
            dbfFile.getField(field.getName()).put(value == null ? "null" : value);
        else
            field.put(value == null ? "null" : value);
    }

    protected BigDecimal safeMultiply(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null || operand1.doubleValue() == 0 || operand2 == null || operand2.doubleValue() == 0)
            return null;
        else return operand1.multiply(operand2);
    }

    protected boolean listNotEmpty(List list) {
        return list != null && !list.isEmpty();
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}