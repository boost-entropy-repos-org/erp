package equ.clt.handler.ukm4;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import equ.api.*;
import equ.api.cashregister.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
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

public class UKM4Handler extends CashRegisterHandler<UKM4SalesBatch> {

    String defaultCharset = "Cp1251";
    
    public UKM4Handler() {
    }

    @Override
    public List<MachineryInfo> sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws IOException {

        DBFWriter barDBFWriter = null;
        DBFWriter classifDBFWriter = null;
        DBFWriter pluCashDBFWriter = null;
        DBFWriter pluLimDBFWriter = null;

        try {

            List<String> directoriesList = new ArrayList<String>();
            for (CashRegisterInfo cashRegisterInfo : machineryInfoList) {
                if ((cashRegisterInfo.port != null) && (!directoriesList.contains(cashRegisterInfo.port.trim())))
                    directoriesList.add(cashRegisterInfo.port.trim());
                if ((cashRegisterInfo.directory != null) && (!directoriesList.contains(cashRegisterInfo.directory.trim())))
                    directoriesList.add(cashRegisterInfo.directory.trim());
            }

            for (String directory : directoriesList) {
                if (!new File(directory.trim()).exists())
                    throw new RuntimeException("The folder " + directory + " doesn't exist");

                //BAR.DBF
                OverJDBField[] barFields = {
                        new OverJDBField("BARCODE", 'C', 15, 0),
                        new OverJDBField("CARDARTICU", 'C', 30, 0),
                        new OverJDBField("CARDSIZE", 'C', 10, 0),
                        new OverJDBField("QUANTITY", 'N', 16, 6)
                };
                barDBFWriter = new DBFWriter(directory + "/BAR.DBF", barFields, "CP866");                                
                for (CashRegisterItemInfo item : transactionInfo.itemsList) {
                    barDBFWriter.addRecord(new Object[]{trim(item.idBarcode, 15), trim(item.idBarcode, 30)/*или что туда надо писать?*/,
                            "NOSIZE", 1/*без разницы, что писать в количество?*/});
                }
                barDBFWriter.close();


                //CLASSIF.DBF
                OverJDBField[] classifFields = {
                        new OverJDBField("GROOP1", 'N', 6, 0),
                        new OverJDBField("GROOP2", 'N', 6, 0),
                        new OverJDBField("GROOP3", 'N', 6, 0),
                        new OverJDBField("GROOP4", 'N', 6, 0),
                        new OverJDBField("GROOP5", 'N', 6, 0),
                        new OverJDBField("NAME", 'C', 80, 0)
                };
                classifDBFWriter = new DBFWriter(directory + "/CLASSIF.DBF", classifFields, "CP866");
                Set<Long> idItemGroups = new HashSet<Long>();
                for (CashRegisterItemInfo item : transactionInfo.itemsList) {
                    List<ItemGroup> hierarchyItemGroup = transactionInfo.itemGroupMap.get(item.idItemGroup);
                    int size = hierarchyItemGroup.size();
                    Long group1 = parseGroup(size >= 1 ? trim(hierarchyItemGroup.get(0).idItemGroup, 6) : "0");
                    Long group2 = parseGroup(size >= 2 ? trim(hierarchyItemGroup.get(1).idItemGroup, 6) : "0");
                    Long group3 = parseGroup(size >= 3 ? trim(hierarchyItemGroup.get(2).idItemGroup, 6) : "0");
                    Long group4 = parseGroup(size >= 4 ? trim(hierarchyItemGroup.get(3).idItemGroup, 6) : "0");
                    Long group5 = parseGroup(size >= 5 ? trim(hierarchyItemGroup.get(4).idItemGroup, 6) : "0");
                    String name = trim(item.nameItemGroup, 80);
                    if(!idItemGroups.contains(group1)) {
                        idItemGroups.add(group1);
                        if (size == 5) {
                            classifDBFWriter.addRecord(new Object[]{group5, group4, group3, group2, group1, name});
                            classifDBFWriter.addRecord(new Object[]{group5, group4, group3, group2, 0, hierarchyItemGroup.get(1).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group5, group4, group3, 0, 0, hierarchyItemGroup.get(2).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group5, group4, 0, 0, 0, hierarchyItemGroup.get(3).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group5, 0, 0, 0, 0, hierarchyItemGroup.get(4).nameItemGroup});
                        } else if (size == 4) {
                            classifDBFWriter.addRecord(new Object[]{group4, group3, group2, group1, 0, name});
                            classifDBFWriter.addRecord(new Object[]{group4, group3, group2, 0, 0, hierarchyItemGroup.get(1).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group4, group3, 0, 0, 0, hierarchyItemGroup.get(2).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group4, 0, 0, 0, 0, hierarchyItemGroup.get(3).nameItemGroup});
                        } else if (size == 3) {
                            classifDBFWriter.addRecord(new Object[]{group3, group2, group1, 0, 0, name});
                            classifDBFWriter.addRecord(new Object[]{group3, group2, 0, 0, 0, hierarchyItemGroup.get(1).nameItemGroup});
                            classifDBFWriter.addRecord(new Object[]{group3, 0, 0, 0, 0, hierarchyItemGroup.get(2).nameItemGroup});
                        } else if (size == 2) {
                            classifDBFWriter.addRecord(new Object[]{group2, group1, 0, 0, 0, name});
                            classifDBFWriter.addRecord(new Object[]{group2, 0, 0, 0, 0, hierarchyItemGroup.get(1).nameItemGroup});
                        } else if (size == 1)
                            classifDBFWriter.addRecord(new Object[]{group1, 0, 0, 0, 0, name});
                    }
                }
                classifDBFWriter.close();


                //PLUCASH.DBF
                OverJDBField[] pluCashFields = {
                        new OverJDBField("ARTICUL", 'C', 30, 0),
                        new OverJDBField("NAME", 'C', 80, 0),
                        new OverJDBField("MESURIMENT", 'C', 10, 0),
                        new OverJDBField("MESPRESISI", 'N', 16, 6),
                        new OverJDBField("ADD1", 'C', 20, 0),
                        new OverJDBField("ADD2", 'C', 20, 0),
                        new OverJDBField("ADD3", 'C', 20, 0),
                        new OverJDBField("ADDNUM1", 'N', 16, 6),
                        new OverJDBField("ADDNUM2", 'N', 16, 6),
                        new OverJDBField("ADDNUM3", 'N', 16, 6),
                        new OverJDBField("SCALE", 'C', 10, 0),
                        new OverJDBField("GROOP1", 'N', 6, 0),
                        new OverJDBField("GROOP2", 'N', 6, 0),
                        new OverJDBField("GROOP3", 'N', 6, 0),
                        new OverJDBField("GROOP4", 'N', 6, 0),
                        new OverJDBField("GROOP5", 'N', 6, 0),
                        new OverJDBField("PRICERUB", 'N', 16, 2),
                        new OverJDBField("PRICECUR", 'N', 16, 2),
                        new OverJDBField("CLIENTINDE", 'N', 6, 0),
                        new OverJDBField("COMMENTARY", 'C', 80, 0),
                        new OverJDBField("DELETED", 'N', 6, 0),
                        new OverJDBField("MODDATE", 'D', 8, 0),
                        new OverJDBField("MODTIME", 'N', 6, 0),
                        new OverJDBField("MODPERSONI", 'N', 6, 0)
                };
                pluCashDBFWriter = new DBFWriter(directory + "/PLUCASH.DBF", pluCashFields, "CP866");
                              
                for (CashRegisterItemInfo item : transactionInfo.itemsList) {
                    
                    String mesuriment = item.passScalesItem && item.splitItem ? "кг" : "1";
                    double mespresisi = item.splitItem ? 0.001 : 1.000;
                    List<ItemGroup> hierarchyItemGroup = transactionInfo.itemGroupMap.get(item.idItemGroup);
                    int size = hierarchyItemGroup.size();
                    Long group1 = parseGroup(size >= 1 ? trim(hierarchyItemGroup.get(size - 1).idItemGroup, 6) : "0");
                    Long group2 = parseGroup(size >= 2 ? trim(hierarchyItemGroup.get(size - 2).idItemGroup, 6) : "0");
                    Long group3 = parseGroup(size >= 3 ? trim(hierarchyItemGroup.get(size - 3).idItemGroup, 6) : "0");
                    Long group4 = parseGroup(size >= 4 ? trim(hierarchyItemGroup.get(size - 4).idItemGroup, 6) : "0");
                    Long group5 = parseGroup(size >= 5 ? trim(hierarchyItemGroup.get(size - 5).idItemGroup, 6) : "0");

                    pluCashDBFWriter.addRecord(new Object[]{trim(item.idBarcode, 30), trim(item.name, 80), mesuriment, mespresisi, null, null, 
                            null, null, null, null, "NOSIZE", group1, group2, group3, group4, group5, 
                            item.price.doubleValue(), null, 0, null, 1, transactionInfo.date, null, null});
                }
                pluCashDBFWriter.close();


                //PLULIM.DBF
                OverJDBField[] pluLimFields = {
                        new OverJDBField("CARDARTICU", 'C', 30, 0),
                        new OverJDBField("PERCENT", 'N', 16, 2)
                };
                pluLimDBFWriter = new DBFWriter(directory + "/PLULIM.DBF", pluLimFields, "CP866");
                for (CashRegisterItemInfo item : transactionInfo.itemsList) {
                    pluLimDBFWriter.addRecord(new Object[]{trim(item.idBarcode, 30), 0/*откуда брать макс. процент скидки?*/});
                }
                pluLimDBFWriter.close();                
                
                File flagFile = new File(directory + "/cash01." + (transactionInfo.snapshot ? "cng" : "upd"));
                flagFile.createNewFile();
                waitForDeletion(flagFile);

            }
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                if (barDBFWriter != null)
                    barDBFWriter.close();
                if (pluCashDBFWriter != null)
                    pluCashDBFWriter.close();
                if (pluLimDBFWriter != null)
                    pluLimDBFWriter.close();
                if (classifDBFWriter != null)
                    classifDBFWriter.close();
            } catch (JDBFException ignored) {
            }
        }
        return null;
    }

    private void waitForDeletion(File file) {
        while (file.exists()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {
        
    }

    @Override
    public void sendStopListInfo(StopListInfo stopListInfo, Set<String> directorySet) throws IOException {
        
    }

    @Override
    public void sendDiscountCardList(List<DiscountCard> discountCardList, Set<String> directory) throws IOException {
    }

    @Override
    public void sendPromotionInfo(PromotionInfo promotionInfo, Set<String> directory) throws IOException {       
    }

    @Override
    public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException {
        Set<String> directorySet = new HashSet<String>();
        Map<String, Integer> directoryGroupCashRegisterMap = new HashMap<String, Integer>();
        for (CashRegisterInfo cashRegister : cashRegisterInfoList) {
            if (cashRegister.directory != null && cashRegister.handlerModel.endsWith("UKM4Handler"))
                directorySet.add(cashRegister.directory.trim());
            if (cashRegister.number != null && cashRegister.numberGroup != null)
                directoryGroupCashRegisterMap.put(cashRegister.directory + "_" + cashRegister.number, cashRegister.numberGroup);
        }
        List<SalesInfo> salesInfoList = new ArrayList<SalesInfo>();
        List<String> readFiles = new ArrayList<String>();
        for (String directory : directorySet) {
            DBF importSailFile = null;
            DBF importDiscFile = null;
            DBF importCardFile = null;
            Map<String, BigDecimal> discountMap = new HashMap<String, BigDecimal>();
            Map<String, String> discountCardMap = new HashMap<String, String>();
            try {
                String fileDiscPath = directory + "/sail/CASHDISC.DBF";
                if (new File(fileDiscPath).exists()) {
                    importDiscFile = new DBF(fileDiscPath);
                    readFiles.add(fileDiscPath);
                    int recordDiscCount = importDiscFile.getRecordCount();
                    for (int i = 0; i < recordDiscCount; i++) {
                        importDiscFile.read();

                        String cashRegisterNumber = getDBFFieldValue(importDiscFile, "CASHNUMBER", defaultCharset);
                        String zNumber = getDBFFieldValue(importDiscFile, "ZNUMBER", defaultCharset);
                        Integer receiptNumber = getDBFIntegerFieldValue(importDiscFile, "CHECKNUMBE", defaultCharset);
                        Integer numberReceiptDetail = getDBFIntegerFieldValue(importDiscFile, "ID", defaultCharset);
                        BigDecimal discountSum = getDBFBigDecimalFieldValue(importDiscFile, "DISCOUNTCU", defaultCharset);

                        String sid = cashRegisterNumber + "_" + zNumber + "_" + receiptNumber + "_" + numberReceiptDetail;
                        BigDecimal tempSum = discountMap.get(sid);
                        discountMap.put(sid, safeAdd(discountSum, tempSum));
                    }
                    importDiscFile.close();
                }

                String fileCardPath = directory + "/sail/CASHDCRD.DBF";
                if (new File(fileCardPath).exists()) {
                    importCardFile = new DBF(fileCardPath);
                    readFiles.add(fileCardPath);
                    int recordCardCount = importCardFile.getRecordCount();
                    for (int i = 0; i < recordCardCount; i++) {
                        importCardFile.read();

                        String cashRegisterNumber = getDBFFieldValue(importCardFile, "CASHNUMBER", defaultCharset);
                        String zNumber = getDBFFieldValue(importCardFile, "ZNUMBER", defaultCharset);
                        Integer receiptNumber = getDBFIntegerFieldValue(importCardFile, "CHECKNUMBE", "Cp1251");
                        String cardNumber = getDBFFieldValue(importCardFile, "CARDNUMBER", "Cp1251");

                        String sid = cashRegisterNumber + "_" + zNumber + "_" + receiptNumber;
                        discountCardMap.put(sid, cardNumber);
                    }
                    importCardFile.close();
                }

                String fileSailPath = directory + "/sail/CASHSAIL.DBF";
                if (new File(fileSailPath).exists()) {
                    importSailFile = new DBF(fileSailPath);
                    readFiles.add(fileSailPath);
                    int recordSailCount = importSailFile.getRecordCount();
                    Map<Integer, BigDecimal[]> receiptNumberSumReceipt = new HashMap<Integer, BigDecimal[]>();

                    for (int i = 0; i < recordSailCount; i++) {
                        importSailFile.read();

                        Integer operation = getDBFIntegerFieldValue(importSailFile, "OPERATION", defaultCharset);
                        //0 - возврат cash, 1 - продажа cash, 2,4 - возврат card, 3,5 - продажа card

                        String numberCashRegister = getDBFFieldValue(importSailFile, "CASHNUMBER", defaultCharset);
                        String zNumber = getDBFFieldValue(importSailFile, "ZNUMBER", defaultCharset);
                        Integer receiptNumber = getDBFIntegerFieldValue(importSailFile, "CHECKNUMBE", defaultCharset);
                        Integer numberReceiptDetail = getDBFIntegerFieldValue(importSailFile, "ID", defaultCharset);
                        Date date = getDBFDateFieldValue(importSailFile, "DATE", defaultCharset);
                        String timeString = getDBFFieldValue(importSailFile, "TIME", defaultCharset);
                        timeString = timeString.length() == 3 ? ("0" + timeString) : timeString;
                        Time time = new Time(DateUtils.parseDate(timeString, new String[]{"HHmm"}).getTime());
                        String barcodeReceiptDetail = getDBFFieldValue(importSailFile, "CARDARTICU", defaultCharset);
                        BigDecimal quantityReceiptDetail = getDBFBigDecimalFieldValue(importSailFile, "QUANTITY", defaultCharset);
                        BigDecimal priceReceiptDetail = getDBFBigDecimalFieldValue(importSailFile, "PRICERUB", defaultCharset);
                        BigDecimal sumReceiptDetail = getDBFBigDecimalFieldValue(importSailFile, "TOTALRUB", defaultCharset);
                        BigDecimal discountSumReceiptDetail = discountMap.get(numberCashRegister + "_" + zNumber + "_" + receiptNumber + "_" + numberReceiptDetail);
                        String discountCardNumber = discountCardMap.get(numberCashRegister + "_" + zNumber + "_" + receiptNumber);

                        BigDecimal[] tempSumReceipt = receiptNumberSumReceipt.get(receiptNumber);
                        BigDecimal tempSum1 = tempSumReceipt != null ? tempSumReceipt[0] : null;
                        BigDecimal tempSum2 = tempSumReceipt != null ? tempSumReceipt[1] : null;
                        receiptNumberSumReceipt.put(receiptNumber, new BigDecimal[]{safeAdd(tempSum1, (operation <= 1 ? sumReceiptDetail : null)),
                                safeAdd(tempSum2, (operation > 1 ? sumReceiptDetail : null))});

                        salesInfoList.add(new SalesInfo(directoryGroupCashRegisterMap.get(directory + "_" + numberCashRegister), Integer.parseInt(numberCashRegister), zNumber,
                                receiptNumber, date, time, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, barcodeReceiptDetail,
                                null, operation % 2 == 1 ? quantityReceiptDetail : quantityReceiptDetail.negate(),
                                priceReceiptDetail,
                                operation % 2 == 1 ? sumReceiptDetail : sumReceiptDetail.negate(),
                                discountSumReceiptDetail, null, discountCardNumber, numberReceiptDetail, null));
                    }
                    for (SalesInfo salesInfo : salesInfoList) {
                        salesInfo.sumCash = receiptNumberSumReceipt.get(salesInfo.numberReceipt)[0];
                        salesInfo.sumCard = receiptNumberSumReceipt.get(salesInfo.numberReceipt)[1];
                    }
                }

            } catch (xBaseJException e) {
                throw new RuntimeException(e.toString(), e.getCause());
            } finally {
                if (importSailFile != null)
                    importSailFile.close();
                if (importCardFile != null)
                    importCardFile.close();
                if (importDiscFile != null)
                    importDiscFile.close();
            }
        }
        return new UKM4SalesBatch(salesInfoList, readFiles);
    }

    @Override
    public String requestSalesInfo(List<RequestExchange> requestExchangeList) throws IOException, ParseException {
        return null;
    }

    @Override
    public CashDocumentBatch readCashDocumentInfo(List<CashRegisterInfo> cashRegisterInfoList, Set<String> cashDocumentSet) throws ClassNotFoundException {
        return null;
    }

    @Override
    public void finishReadingCashDocumentInfo(CashDocumentBatch cashDocumentBatch) {       
    }

    @Override
    public void finishReadingSalesInfo(UKM4SalesBatch salesBatch) {
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);
            if (!f.delete())
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
        }
    }

    @Override
    public Map<String, Timestamp> requestSucceededSoftCheckInfo(Set<String> directorySet) {
        return null;
    }

    @Override
    public List<List<Object>> checkZReportSum(Map<String, BigDecimal> zReportSumMap, List<Integer> nppCashRegisterList) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public ExtraCheckZReportBatch extraCheckZReportSum(List<CashRegisterInfo> cashRegisterInfoList, Map<String, BigDecimal> zReportSumMap) throws ClassNotFoundException, SQLException {
        return null;
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
    
    protected Long parseGroup(String idItemGroup) {
        return idItemGroup == null ? 0 : Long.parseLong(idItemGroup.equals("Все") ? "0" : idItemGroup.replace("_", ""));
    }

    protected BigDecimal safeAdd(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else return (operand1 == null ? operand2 : (operand2 == null ? operand1 : operand1.add(operand2)));
    }

    protected String getDBFFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        return getDBFFieldValue(importFile, fieldName, charset, null);
    }
    
    protected String getDBFFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        return getDBFFieldValue(importFile, fieldName, charset, false, defaultValue);
    }

    protected String getDBFFieldValue(DBF importFile, String fieldName, String charset, Boolean zeroIsNull, String defaultValue) throws UnsupportedEncodingException {
        try {
            String result = new String(importFile.getField(fieldName).getBytes(), charset).trim();
            return result.isEmpty() || (zeroIsNull && result.equals("0")) ? defaultValue : result;
        } catch (xBaseJException e) {
            return defaultValue;
        }
    }

    protected BigDecimal getDBFBigDecimalFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        return getDBFBigDecimalFieldValue(importFile, fieldName, charset, null);
    }

    protected BigDecimal getDBFBigDecimalFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        return getDBFBigDecimalFieldValue(importFile, fieldName, charset, false, defaultValue);
    }

    protected BigDecimal getDBFBigDecimalFieldValue(DBF importFile, String fieldName, String charset, Boolean zeroIsNull, String defaultValue) throws UnsupportedEncodingException {
        String result = getDBFFieldValue(importFile, fieldName, charset, zeroIsNull, defaultValue);
        return (result == null || result.isEmpty() || (zeroIsNull && Double.valueOf(result).equals(new Double(0)))) ? null : new BigDecimal(result.replace(",", "."));
    }

    protected Integer getDBFIntegerFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException {
        return getDBFIntegerFieldValue(importFile, fieldName, charset, false, null);
    }
    
    protected Integer getDBFIntegerFieldValue(DBF importFile, String fieldName, String charset, Boolean zeroIsNull, String defaultValue) throws UnsupportedEncodingException {
        String result = getDBFFieldValue(importFile, fieldName, charset, zeroIsNull, defaultValue);
        return (result == null || (zeroIsNull && Double.valueOf(result).equals(new Double(0)))) ? null : new Double(result).intValue();
    }

    protected Date getDBFDateFieldValue(DBF importFile, String fieldName, String charset) throws UnsupportedEncodingException, ParseException {
        return getDBFDateFieldValue(importFile, fieldName, charset, null);
    }
    
    protected Date getDBFDateFieldValue(DBF importFile, String fieldName, String charset, Date defaultValue) throws UnsupportedEncodingException, ParseException {
        String dateString = getDBFFieldValue(importFile, fieldName, charset, false, "");
        return dateString.isEmpty() ? defaultValue : new Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd", "dd.MM.yyyy"}).getTime());
    }
}
