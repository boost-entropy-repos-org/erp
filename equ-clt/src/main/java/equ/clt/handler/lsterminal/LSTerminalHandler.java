package equ.clt.handler.lsterminal;

import com.google.common.base.Throwables;
import equ.api.MachineryInfo;
import equ.api.SendTransactionBatch;
import equ.api.SoftCheckInfo;
import equ.api.TransactionInfo;
import equ.api.terminal.*;
import equ.clt.handler.HandlerUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LSTerminalHandler extends TerminalHandler {

    protected final static Logger processTransactionLogger = Logger.getLogger("TransactionLogger");
    protected final static Logger sendTerminalDocumentLogger = Logger.getLogger("TerminalDocumentLogger");
    protected final static Logger machineryExchangeLogger = Logger.getLogger("MachineryExchangeLogger");

    String dbPath = "/db";

    public LSTerminalHandler() {
    }

    @Override
    public String getGroupId(TransactionInfo transactionInfo) throws IOException {
        return "lsterminal";
    }

    @Override
    public Map<Integer, SendTransactionBatch> sendTransaction(List transactionInfoList) throws IOException {

        Map<Integer, SendTransactionBatch> sendTransactionBatchMap = new HashMap<>();

        for(Object transactionInfo : transactionInfoList) {
            Exception exception = null;
            try {
                Integer nppGroupTerminal = ((TransactionTerminalInfo) transactionInfo).nppGroupTerminal;
                String directory = ((TransactionTerminalInfo) transactionInfo).directoryGroupTerminal;
                if (directory != null) {
                    String exchangeDirectory = directory + "/exchange";
                    if ((new File(exchangeDirectory).exists() || new File(exchangeDirectory).mkdir())) {
                        //copy base to exchange directory
                        FileInputStream fis = new FileInputStream(new File(makeDBPath(directory + dbPath, nppGroupTerminal)));
                        FileOutputStream fos = new FileOutputStream(new File(exchangeDirectory + "/base.zip"));
                        ZipOutputStream zos = new ZipOutputStream(fos);
                        zos.putNextEntry(new ZipEntry("tsd.db"));
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = fis.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                        fis.close();
                        zos.close();
                    }
                }
            } catch (Exception e) {
                processTransactionLogger.error("LSTerminal Error: ", e);
                exception = e;
            }
            sendTransactionBatchMap.put(((TransactionTerminalInfo) transactionInfo).id, new SendTransactionBatch(exception));
        }
        return sendTransactionBatchMap;
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {
    }

    @Override
    public void sendTerminalOrderList(List terminalOrderList, MachineryInfo machinery) throws IOException {
        try {

            File directory = new File(machinery.directory + dbPath);
            if (directory.exists() || directory.mkdir()) {
                Class.forName("org.sqlite.JDBC");
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection("jdbc:sqlite:" + makeDBPath(machinery.directory + dbPath, machinery.numberGroup));

                    createGoodsTableIfNotExists(connection);
                    updateTerminalGoodsTable(connection, terminalOrderList, machinery.denominationStage);

                    createOrderTable(connection);
                    updateOrderTable(connection, terminalOrderList, machinery.denominationStage);
                } finally {
                    if(connection != null)
                        connection.close();
                }

            } else {
                machineryExchangeLogger.error("Directory " + directory.getAbsolutePath() + " doesn't exist");
                throw Throwables.propagate(new RuntimeException("Directory " + directory.getAbsolutePath() + " doesn't exist"));
            }

            if (machinery.directory != null) {
                String exchangeDirectory = machinery.directory + "/exchange";
                if ((new File(exchangeDirectory).exists() || new File(exchangeDirectory).mkdir())) {
                    //copy base to exchange directory                   
                    FileInputStream fis = new FileInputStream(new File(makeDBPath(machinery.directory + dbPath, machinery.numberGroup)));
                    FileOutputStream fos = new FileOutputStream(new File(exchangeDirectory + "/base.zip"));
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    zos.putNextEntry(new ZipEntry("tsd.db"));
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                    fis.close();
                    zos.close();
                }
            }
        } catch (Exception e) {
            machineryExchangeLogger.error("LSTerminal Error: ", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void saveTransactionTerminalInfo(TransactionTerminalInfo transactionInfo) throws IOException {

        processTransactionLogger.info("LSTerminal: save Transaction #" + transactionInfo.id);

        Integer nppGroupTerminal = transactionInfo.nppGroupTerminal;
        File directory = new File(transactionInfo.directoryGroupTerminal + dbPath);
        if (directory.exists() || directory.mkdir()) {
            try {
                Class.forName("org.sqlite.JDBC");
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection("jdbc:sqlite:" +
                            makeDBPath(transactionInfo.directoryGroupTerminal + dbPath, nppGroupTerminal));

                    processTransactionLogger.info(String.format("LSTerminal: Transaction #%s, creating or updating table Goods", transactionInfo.id));
                    createGoodsTableIfNotExists(connection);
                    updateGoodsTable(connection, transactionInfo);

                    processTransactionLogger.info(String.format("LSTerminal: Transaction #%s, creating or updating table Assort", transactionInfo.id));
                    createAssortTableIfNotExists(connection);
                    updateAssortTable(connection, transactionInfo);

                    processTransactionLogger.info(String.format("LSTerminal: Transaction #%s, creating or updating table VAN", transactionInfo.id));
                    createVANTableIfNotExists(connection);
                    updateVANTable(connection, transactionInfo);

                    processTransactionLogger.info(String.format("LSTerminal: Transaction #%s, creating or updating table ANA", transactionInfo.id));
                    createANATableIfNotExists(connection);
                    updateANATable(connection, transactionInfo);

                    processTransactionLogger.info(String.format("LSTerminal: Transaction #%s, creating or updating table VOP", transactionInfo.id));
                    createVOPTableIfNotExists(connection);
                    updateVOPTable(connection, transactionInfo);
                } finally {
                    if(connection != null)
                        connection.close();
                }

            } catch (Exception e) {
                processTransactionLogger.error("LSTerminal Error: ", e);
                throw Throwables.propagate(e);
            }
        } else {
            processTransactionLogger.error("Directory " + directory.getAbsolutePath() + " doesn't exist");
            throw Throwables.propagate(new RuntimeException("Directory " + directory.getAbsolutePath() + " doesn't exist"));
        }
    }

    public void finishReadingTerminalDocumentInfo(TerminalDocumentBatch terminalDocumentBatch) {
        sendTerminalDocumentLogger.info("LSTerminal: Finish Reading started");
        for (String readFile : terminalDocumentBatch.readFiles) {
            File f = new File(readFile);
            if (f.delete()) {
                sendTerminalDocumentLogger.info("LSTerminal: file " + readFile + " has been deleted");
            } else {
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
            }
        }
    }

    @Override
    public TerminalDocumentBatch readTerminalDocumentInfo(List machineryInfoList) throws IOException {

        try {

            Class.forName("org.sqlite.JDBC");

            Map<String, TerminalInfo> directoryMachineryMap = new HashMap<>();
            for (Object m : machineryInfoList) {
                TerminalInfo t = (TerminalInfo) m;
                if (t.directory != null && t.handlerModel != null && t.handlerModel.endsWith("LSTerminalHandler")) {
                    directoryMachineryMap.put(t.directory, t);
                }
            }

            List<String> filePathList = new ArrayList<>();

            List<TerminalDocumentDetail> terminalDocumentDetailList = new ArrayList<>();

            for (Map.Entry<String, TerminalInfo> directoryEntry : directoryMachineryMap.entrySet()) {

                String exchangeDirectory = directoryEntry.getKey() + "/exchange";

                File[] filesList = new File(exchangeDirectory).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().toUpperCase().startsWith("DOK_") && pathname.getPath().toUpperCase().endsWith(".DB");
                    }
                });

                if (filesList == null || filesList.length == 0)
                    sendTerminalDocumentLogger.info("LSTerminal: No terminal documents found in " + exchangeDirectory);
                else {
                    sendTerminalDocumentLogger.info("LSTerminal: found " + filesList.length + " file(s) in " + exchangeDirectory);

                    for (File file : filesList) {
                        try {
                            String fileName = file.getName();
                            sendTerminalDocumentLogger.info("LSTerminal: reading " + fileName);
                            if (isFileLocked(file)) {
                                sendTerminalDocumentLogger.info("LSTerminal: " + fileName + " is locked");
                            } else {
                                Connection connection = null;
                                try {
                                    connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                                    String denominationStage = directoryEntry.getValue() == null ? null : directoryEntry.getValue().denominationStage;
                                    List<List<Object>> dokData = readDokFile(connection, denominationStage);

                                    for (List<Object> entry : dokData) {

                                        String dateTimeValue = (String) entry.get(0); //DV
                                        Timestamp dateTime = dateTimeValue == null ? null : new Timestamp(DateUtils.parseDate(dateTimeValue, new String[]{"yyyy-MM-dd HH:mm:ss"}).getTime());
                                        Date date = dateTime == null ? null : new Date(dateTime.getTime());
                                        Time time = dateTime == null ? null : new Time(dateTime.getTime());
                                        String numberDocument = (String) entry.get(1); //NUM
                                        String idDocument = dateTimeValue + "/" + numberDocument;
                                        String idDocumentType = (String) entry.get(2); //VOP
                                        String idTerminalHandbookType1 = (String) entry.get(3); //ANA1
                                        String idTerminalHandbookType2 = (String) entry.get(4); //ANA2
                                        String barcode = (String) entry.get(5); //BARCODE
                                        BigDecimal quantity = (BigDecimal) entry.get(6); //QUANT
                                        BigDecimal price = denominateDivideType2((BigDecimal) entry.get(7), denominationStage); //PRICE
                                        String numberDocumentDetail = (String) entry.get(8); //npp
                                        String commentDocument = (String) entry.get(9); //PRIM
                                        BigDecimal sum = HandlerUtils.safeMultiply(quantity, price);
                                        String idDocumentDetail = idDocument + numberDocumentDetail;

                                        if (quantity != null && !quantity.equals(BigDecimal.ZERO))
                                            terminalDocumentDetailList.add(new TerminalDocumentDetail(idDocument, numberDocument,
                                                    date, time, commentDocument, directoryEntry.getKey(), idTerminalHandbookType1, idTerminalHandbookType2,
                                                    idDocumentType, null, idDocumentDetail, numberDocumentDetail, barcode, null,
                                                    price, quantity, sum));
                                    }
                                } finally {
                                    if(connection != null)
                                        connection.close();
                                }
                                filePathList.add(file.getAbsolutePath());
                            }
                        } catch (Throwable e) {
                            sendTerminalDocumentLogger.error("File: " + file.getAbsolutePath(), e);
                        }
                    }
                }
            }

            return new TerminalDocumentBatch(terminalDocumentDetailList, filePathList);
        } catch (Exception e) {
            sendTerminalDocumentLogger.error("LSTerminal Error: ", e);
            throw Throwables.propagate(e);
        }
    }

    private List<List<Object>> readDokFile(Connection connection, String denominationStage) throws SQLException {

        List<List<Object>> itemsList = new ArrayList<>();

        String dv = null;
        String num = null;
        String vop = null;
        String ana1 = null;
        String ana2 = null;
        String comment = null;

        Statement statement = connection.createStatement();
        String sql = "SELECT dv, num, vop, ana1, ana2, prim FROM dok LIMIT 1;";
        ResultSet resultSet = statement.executeQuery(sql);
        if (resultSet.next()) {
            dv = resultSet.getString("dv");
            num = resultSet.getString("num");
            vop = resultSet.getString("vop");
            ana1 = resultSet.getString("ana1");
            ana2 = resultSet.getString("ana2");
            comment = resultSet.getString("prim");
        }
        resultSet.close();
        statement.close();

        statement = connection.createStatement();
        sql = "SELECT barcode, quant, price, npp FROM pos;";
        resultSet = statement.executeQuery(sql);
        int count = 1;
        while (resultSet.next()) {
            String barcode = resultSet.getString("barcode");
            BigDecimal quantity = new BigDecimal(resultSet.getDouble("quant"));
            BigDecimal price = denominateDivideType2(new BigDecimal(resultSet.getDouble("price")), denominationStage);
            Integer npp = resultSet.getInt("npp");
            npp = npp == 0 ? count : npp;
            count++;
            itemsList.add(Arrays.asList((Object) dv, num, vop, ana1, ana2, barcode, quantity, price, String.valueOf(npp), comment));
        }
        resultSet.close();
        statement.close();

        return itemsList;
    }

    private void createANATableIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS ana " +
                "(ana  TEXT PRIMARY KEY," +
                " naim TEXT," +
                " fld1 TEXT," +
                " fld2 TEXT," +
                " fld3 TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateANATable(Connection connection, TransactionTerminalInfo transactionInfo) throws SQLException {
        if (listNotEmpty(transactionInfo.terminalLegalEntityList)) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO ana VALUES(?, ?, '', '', '');";
                statement = connection.prepareStatement(sql);
                for (TerminalLegalEntity legalEntity : transactionInfo.terminalLegalEntityList) {
                    if (legalEntity.idLegalEntity != null) {
                        statement.setObject(1, "ПС" + formatValue(legalEntity.idLegalEntity));
                        statement.setObject(2, formatValue(legalEntity.nameLegalEntity));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            } finally {
                if (statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createVOPTableIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS vop " +
                "(vop  TEXT PRIMARY KEY," +
                " rvop TEXT," +
                " naim TEXT," +
                " van1 TEXT," +
                " van2 TEXT," +
                " van3 TEXT," +
                " FLAGS INTEGER )";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateVOPTable(Connection connection, TransactionTerminalInfo transactionInfo) throws SQLException {
        if (listNotEmpty(transactionInfo.terminalDocumentTypeList)) {

            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO vop VALUES(?, ?, ?, ?, ?, ?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalDocumentType tdt : transactionInfo.terminalDocumentTypeList) {
                    if (tdt.id != null) {
                        statement.setObject(1, "ПС" + formatValue(tdt.id));
                        statement.setObject(2, "");
                        statement.setObject(3, formatValue(tdt.name));
                        statement.setObject(4, formatValue(tdt.analytics1));
                        statement.setObject(5, formatValue(tdt.analytics2));
                        statement.setObject(6, "");
                        statement.setObject(7, formatValue(tdt.flag == null ? "1" : tdt.flag));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            } finally {
                if (statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }


    private void createAssortTableIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS assort " +
                "(post    TEXT," +
                " barcode TEXT," +
                "PRIMARY KEY ( post, barcode))";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateAssortTable(Connection connection, TransactionTerminalInfo transactionInfo) throws SQLException {
        if(transactionInfo.snapshot) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM assort");
            statement.close();
        }
        if (listNotEmpty(transactionInfo.terminalAssortmentList)) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO assort VALUES(?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalAssortment assortment : transactionInfo.terminalAssortmentList) {
                    if (assortment.idBarcode != null && assortment.idSupplier != null) {
                        statement.setObject(1, formatValue(("ПС" + assortment.idSupplier)));
                        statement.setObject(2, formatValue(assortment.idBarcode));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
            } finally {
                if (statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createGoodsTableIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS goods " +
                "(barcode TEXT PRIMARY KEY," +
                " naim    TEXT," +
                " price   REAL," +
                " quant   REAL," +
                " fld1    TEXT," +
                " fld2    TEXT," +
                " fld3    TEXT," +
                " fld4    TEXT," +
                " fld5    TEXT," +
                " image   TEXT," +
                " weight  TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateTerminalGoodsTable(Connection connection, List<TerminalOrder> terminalOrderList, String denominationStage) throws SQLException {
        if (listNotEmpty(terminalOrderList)) {
            Statement statement = connection.createStatement();
            String sql = "BEGIN TRANSACTION;";
            for (TerminalOrder order : terminalOrderList) {
                if (order.barcode != null)
                    sql += String.format("INSERT OR IGNORE INTO goods VALUES('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                            formatValue(order.barcode), formatValue(order.name), formatValue(denominateMultiplyType2(order.price, denominationStage)), "", "", "", "", "", "", "", "");
            }
            sql += "COMMIT;";
            statement.executeUpdate(sql);
            statement.close();
        }
    }
    
    private void updateGoodsTable(Connection connection, TransactionTerminalInfo transaction) throws SQLException {
        if(transaction.snapshot) {
            createOrderTable(connection);
            Statement statement = connection.createStatement();      
            statement.executeUpdate("BEGIN TRANSACTION; DELETE FROM goods; COMMIT;");
            statement.close();
        }
        if (listNotEmpty(transaction.itemsList)) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO goods VALUES(?, ?, ?, ?, '', '', '', '', '', ?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalItemInfo item : transaction.itemsList) {
                    if (item.idBarcode != null) {
                        statement.setObject(1, formatValue(item.idBarcode));
                        statement.setObject(2, formatValue(item.name));
                        statement.setObject(3, formatValue(denominateMultiplyType2(item.price, transaction.denominationStage)));
                        statement.setObject(4, formatValue(item.quantity));
                        statement.setObject(5, formatValue(item.image));
                        statement.setObject(6, item.passScalesItem ? "1" : "0");
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();

            } finally {
                if(statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createVANTableIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS van " +
                "(van    TEXT PRIMARY KEY," +
                " naim   TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateVANTable(Connection connection, TransactionTerminalInfo transactionInfo) throws SQLException {
        if (listNotEmpty(transactionInfo.terminalHandbookTypeList)) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO van VALUES(?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalHandbookType terminalHandbookType : transactionInfo.terminalHandbookTypeList) {
                    if (terminalHandbookType.id != null && terminalHandbookType.name != null) {
                        statement.setObject(1, formatValue(terminalHandbookType.id));
                        statement.setObject(2, formatValue(terminalHandbookType.name));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();

            } finally {
                if(statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createOrderTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "DROP TABLE IF EXISTS zayavki;" +
                "CREATE TABLE zayavki " +
                "(dv     TEXT," +
                " num   TEXT," +
                " post  TEXT," +
                " barcode   TEXT," +
                " quant   REAL," +
                " price    REAL," +
                " minquant    REAL," +
                " maxquant    REAL," +
                " minprice    REAL," +
                " maxprice    REAL," +
                "PRIMARY KEY (num, barcode))";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateOrderTable(Connection connection, List<TerminalOrder> terminalOrderList, String denominationStage) throws SQLException {
        if (listNotEmpty(terminalOrderList)) {

            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO zayavki VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalOrder order : terminalOrderList) {
                    if (order.number != null) {
                        String supplier = order.supplier == null ? "" : ("ПС" + formatValue(order.supplier));
                        statement.setObject(1, formatValue(order.date));
                        statement.setObject(2, formatValue(order.number));
                        statement.setObject(3,supplier);
                        statement.setObject(4,formatValue(order.barcode));
                        statement.setObject(5,formatValue(order.quantity));
                        statement.setObject(6,formatValue(denominateMultiplyType2(order.price, denominationStage)));
                        statement.setObject(7,formatValue(order.minQuantity));
                        statement.setObject(8,formatValue(order.maxQuantity));
                        statement.setObject(9,formatValue(denominateMultiplyType2(order.minPrice, denominationStage)));
                        statement.setObject(10,formatValue(denominateMultiplyType2(order.maxPrice, denominationStage)));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();

            } finally {
                if(statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private Object formatValue(Object value) {
        return value == null ? "" : value;
    }

    private String makeDBPath(String directory, Integer nppGroupTerminal) {
        return directory + "/" + (nppGroupTerminal == null ? "tsd" : nppGroupTerminal) + ".db";
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
            sendTerminalDocumentLogger.info(e);
            isLocked = true;
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception e) {
                    sendTerminalDocumentLogger.info(e);
                    isLocked = true;
                }
            }
            if (channel != null)
                try {
                    channel.close();
                } catch (IOException e) {
                    sendTerminalDocumentLogger.info(e);
                    isLocked = true;
                }
        }
        return isLocked;
    }

    protected boolean listNotEmpty(List list) {
        return list != null && !list.isEmpty();
    }
}