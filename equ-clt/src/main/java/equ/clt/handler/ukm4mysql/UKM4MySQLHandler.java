package equ.clt.handler.ukm4mysql;

import com.google.common.base.Throwables;
import equ.api.*;
import equ.api.cashregister.*;
import lsfusion.base.Pair;
import org.apache.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UKM4MySQLHandler extends CashRegisterHandler<UKM4MySQLSalesBatch> {

    protected final static Logger processTransactionLogger = Logger.getLogger("TransactionLogger");
    protected final static Logger sendSalesLogger = Logger.getLogger("SendSalesLogger");

    private FileSystemXmlApplicationContext springContext;

    public UKM4MySQLHandler(FileSystemXmlApplicationContext springContext) {
        this.springContext = springContext;
    }

    public String getGroupId(TransactionCashRegisterInfo transactionInfo) {
        return "ukm4MySql";
    }

    @Override
    public Map<Integer, SendTransactionBatch> sendTransaction(List<TransactionCashRegisterInfo> transactionList) throws IOException {

        Map<Integer, SendTransactionBatch> sendTransactionBatchMap = new HashMap<>();

        if (transactionList != null) {

            try {

                Class.forName("com.mysql.jdbc.Driver");

                UKM4MySQLSettings ukm4MySQLSettings = springContext.containsBean("ukm4MySQLSettings") ? (UKM4MySQLSettings) springContext.getBean("ukm4MySQLSettings") : null;
                String connectionString = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getImportConnectionString(); //"jdbc:mysql://172.16.0.35/import"
                String user = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getUser(); //luxsoft
                String password = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getPassword(); //123456
                Integer timeout = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getTimeout();
                timeout = timeout == null ? 300 : timeout;
                boolean skipItems = ukm4MySQLSettings == null || ukm4MySQLSettings.getSkipItems() != null && ukm4MySQLSettings.getSkipItems();
                boolean skipBarcodes = ukm4MySQLSettings == null || ukm4MySQLSettings.getSkipBarcodes() != null && ukm4MySQLSettings.getSkipBarcodes();

                if (connectionString == null) {
                    processTransactionLogger.error("No importConnectionString in ukm4MySQLSettings found");
                } else {
                    for (TransactionCashRegisterInfo transaction : transactionList) {

                        String weightCode = transaction.weightCodeGroupCashRegister;

                        Connection conn = DriverManager.getConnection(connectionString, user, password);

                        Exception exception = null;
                        try {

                            int version = getVersion(conn);

                            if (skipItems) {
                                version = version - 1;
                            } else {
                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table classif", transaction.id));
                                exportClassif(conn, transaction, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table items", transaction.id));
                                exportItems(conn, transaction, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table items_stocks", transaction.id));
                                exportItemsStocks(conn, transaction, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table pricelist", transaction.id));
                                exportPriceList(conn, transaction, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table pricetype_store_pricelist", transaction.id));
                                exportPriceTypeStorePriceList(conn, transaction, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table var", transaction.id));
                                exportVar(conn, transaction, weightCode, version);

                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table signal", transaction.id));
                                exportSignals(conn, transaction, version, true, timeout);
                            }

                            if(!skipBarcodes) {
                                processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table pricelist_var", transaction.id));
                                exportPriceListVar(conn, transaction, weightCode, version + 1);
                            }

                            processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table pricelist_items", transaction.id));
                            exportPriceListItems(conn, transaction, version + 1);

                            processTransactionLogger.info(String.format("ukm4 mysql: transaction %s, table signal", transaction.id));
                            exportSignals(conn, transaction, version + 1, false, timeout);

                        } catch (Exception e) {
                            exception = e;
                        } finally {
                            if (conn != null)
                                conn.close();
                        }
                        sendTransactionBatchMap.put(transaction.id, new SendTransactionBatch(exception));
                    }
                }
            } catch (ClassNotFoundException | SQLException e) {
                throw Throwables.propagate(e);
            }
        }
        return sendTransactionBatchMap;
    }

    private int getVersion(Connection conn) throws SQLException {
        int version;
        Statement statement = null;
        try {
            statement = conn.createStatement();
            String query = "select max(version) from `signal`";
            ResultSet rs = statement.executeQuery(query);
            version = (rs.next() ? rs.getInt(1) : 0) + 1;
        } catch (SQLException e) {
            version = 1;
        } finally {
            if (statement != null)
                statement.close();
        }
        return version;
    }

    private void exportClassif(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {

        if (transaction.itemsList != null) {

            Set<String> usedGroups = new HashSet<>();

            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO classif (id, owner, name, version, deleted) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE owner=VALUES(owner), name=VALUES(name), deleted=VALUES(deleted)");

                for (CashRegisterItemInfo item : transaction.itemsList) {
                    List<ItemGroup> itemGroupList = transaction.itemGroupMap.get(item.extIdItemGroup);
                    if (itemGroupList != null) {
                        for (ItemGroup itemGroup : itemGroupList) {
                            if (!usedGroups.contains(itemGroup.extIdItemGroup)) {
                                usedGroups.add(itemGroup.extIdItemGroup);
                                Long idItemGroup = parseGroup(itemGroup.extIdItemGroup);
                                if (idItemGroup != 0) {
                                    ps.setLong(1, idItemGroup); //id
                                    ps.setLong(2, parseGroup(itemGroup.idParentItemGroup)); //owner
                                    ps.setString(3, trim(itemGroup.nameItemGroup, 80, "")); //name
                                    ps.setInt(4, version); //version
                                    ps.setInt(5, 0); //deleted
                                    ps.addBatch();
                                }
                            }
                        }
                    }
                }

                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportItems(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {
        if (transaction.itemsList != null) {
            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO items (id, name, descr, measure, measprec, classif, prop, summary, exp_date, version, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name), descr=VALUES(descr), measure=VALUES(measure), measprec=VALUES(measprec), classif=VALUES(classif)," +
                                "prop=VALUES(prop), summary=VALUES(summary), exp_date=VALUES(exp_date), deleted=VALUES(deleted)");

                for (CashRegisterItemInfo item : transaction.itemsList) {
                    ps.setString(1, trim(item.idItem, 40)); //id
                    ps.setString(2, trim(item.name, 40, "")); //name
                    ps.setString(3, item.description == null ? "" : item.description); //descr
                    ps.setString(4, trim(item.shortNameUOM, 40, "")); //measure
                    ps.setInt(5, item.passScalesItem ? 3 : item.splitItem ? 2 : 0); //measprec
                    ps.setLong(6, parseGroup(item.extIdItemGroup)); //classif
                    ps.setInt(7, 1); //prop - признак товара ?
                    ps.setString(8, trim(item.description, 100, "")); //summary
                    ps.setDate(9, item.expiryDate); //exp_date
                    ps.setInt(10, version); //version
                    ps.setInt(11, 0); //deleted
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportItemsStocks(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {
        if(transaction.itemsList != null) {
            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO items_stocks (store, item, stock, version, deleted) VALUES (?, ?, ?, ?, ?)" +
                                "ON DUPLICATE KEY UPDATE deleted=VALUES(deleted)");

                for (CashRegisterItemInfo item : transaction.itemsList) {
                    if (item.section != null) {
                        for (String stock : item.section.split(",")) {
                            ps.setString(1, String.valueOf(transaction.departmentNumberGroupCashRegister)); //store
                            ps.setString(2, trim(item.idItem, 40, "")); //item
                            ps.setInt(3, Integer.parseInt(stock)); //stock
                            ps.setInt(4, version); //version
                            ps.setInt(5, 0); //deleted
                            ps.addBatch();
                        }
                    }
                    if (item.deleteSection != null) {
                        for (String stock : item.deleteSection.split(",")) {
                            ps.setString(1, String.valueOf(transaction.departmentNumberGroupCashRegister)); //store
                            ps.setString(2, trim(item.idItem, 40, "")); //item
                            ps.setInt(3, Integer.parseInt(stock)); //stock
                            ps.setInt(4, version); //version
                            ps.setInt(5, 1); //deleted
                            ps.addBatch();
                        }
                    }
                }

                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportPriceList(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {
        conn.setAutoCommit(false);
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(
                    "INSERT INTO pricelist (id, name, version, deleted) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name), deleted=VALUES(deleted)");

            ps.setInt(1, transaction.nppGroupMachinery); //id
            ps.setString(2, "Прайс-лист " + trim(String.valueOf(transaction.nameStockGroupCashRegister), 89, "")); //name
            ps.setInt(3, version); //version
            ps.setInt(4, 0); //deleted
            ps.addBatch();

            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    private void exportPriceListItems(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {
        if (transaction.itemsList != null) {
            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO pricelist_items (pricelist, item, price, minprice, version, deleted) VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE price=VALUES(price), minprice=VALUES(minprice), deleted=VALUES(deleted)");

                for (CashRegisterItemInfo item : transaction.itemsList) {
                    ps.setInt(1, transaction.nppGroupMachinery); //pricelist
                    ps.setString(2, trim(item.idItem, 40, "")); //item
                    ps.setBigDecimal(3, item.price); //price
                    BigDecimal minPrice = item.flags == null || ((item.flags & 16) == 0) ? item.price : item.minPrice != null ? item.minPrice : BigDecimal.ZERO;
                    ps.setBigDecimal(4, minPrice); //minprice
                    ps.setInt(5, version); //version
                    ps.setInt(6, 0); //deleted
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportPriceListVar(Connection conn, TransactionCashRegisterInfo transaction, String weightCode, int version) throws SQLException {
        if (transaction.itemsList != null) {
            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO pricelist_var (pricelist, var, price, version, deleted) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE price=VALUES(price), deleted=VALUES(deleted)");

                for (CashRegisterItemInfo item : transaction.itemsList) {
                    String barcode = makeBarcode(item.idBarcode, item.passScalesItem, weightCode);
                    if (barcode != null) {
                        ps.setInt(1, transaction.nppGroupMachinery); //pricelist
                        ps.setString(2, trim(barcode, 40)); //var
                        ps.setBigDecimal(3, item.price); //price
                        ps.setInt(4, version); //version
                        ps.setInt(5, 0); //deleted
                        ps.addBatch();
                    }
                }

                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportPriceTypeStorePriceList(Connection conn, TransactionCashRegisterInfo transaction, int version) throws SQLException {
        conn.setAutoCommit(false);
        PreparedStatement ps = null;
        try {
            if(transaction.nppGroupMachinery != null) {
                ps = conn.prepareStatement(
                        "INSERT INTO pricetype_store_pricelist (pricetype, store, pricelist, version, deleted) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE pricelist=VALUES(pricelist), deleted=VALUES(deleted)");
                ps.setInt(1, 123); //pricetype
                ps.setString(2, String.valueOf(transaction.departmentNumberGroupCashRegister != null ? transaction.departmentNumberGroupCashRegister : transaction.nppGroupMachinery)); //store
                ps.setInt(3, transaction.nppGroupMachinery); //pricelist
                ps.setInt(4, version); //version
                ps.setInt(5, 0); //deleted
                ps.addBatch();
                ps.executeBatch();
                conn.commit();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    private void exportVar(Connection conn, TransactionCashRegisterInfo transaction, String weightCode, int version) throws SQLException {
        if (transaction.itemsList != null) {
            conn.setAutoCommit(false);
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(
                        "INSERT INTO var (id, item, quantity, stock, version, deleted) VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE item=VALUES(item), quantity=VALUES(quantity), stock=VALUES(stock), deleted=VALUES(deleted)");
                for (CashRegisterItemInfo item : transaction.itemsList) {
                    String barcode = makeBarcode(item.idBarcode, item.passScalesItem, weightCode);
                    if (barcode != null && item.idItem != null) {
                        ps.setString(1, trim(barcode, 40)); //id
                        ps.setString(2, trim(item.idItem, 40)); //item
                        ps.setInt(3, item.amountBarcode != null ? item.amountBarcode.intValue() : 1); //quantity
                        ps.setInt(4, 1); //stock
                        ps.setInt(5, version); //version
                        ps.setInt(6, 0); //deleted
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            } finally {
                if (ps != null)
                    ps.close();
            }
        }
    }

    private void exportSignals(Connection conn, TransactionCashRegisterInfo transaction, int version, boolean ignoreSnapshot, int timeout) throws SQLException {
        conn.setAutoCommit(true);
        Statement statement = null;
        try {
            statement = conn.createStatement();

            String sql = String.format("INSERT INTO `signal` (`signal`, version) VALUES('%s', '%s') ON DUPLICATE KEY UPDATE `signal`=VALUES(`signal`);",
                    (transaction != null && transaction.snapshot && !ignoreSnapshot) ? "cumm" : "incr", version);
            statement.executeUpdate(sql);

            int count = 0;
            while(!waitForSignalExecution(conn, version)) {
                if(count > (timeout / 5)) {
                    String message = String.format("data was sent to db but signal record %s was not deleted", version);
                    processTransactionLogger.error(message);
                    throw new RuntimeException(message);
                } else {
                    count++;
                    processTransactionLogger.info(String.format("Waiting for deletion of signal record %s in base", version));
                    Thread.sleep(5000);
                }
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if (statement != null)
                statement.close();
        }
    }

    private boolean waitForSignalExecution(Connection conn, int version) throws SQLException {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            String sql = "SELECT COUNT(*) FROM `signal` WHERE version = " + version;
            ResultSet resultSet = statement.executeQuery(sql);
            return !resultSet.next() || resultSet.getInt(1) == 0;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if (statement != null)
                statement.close();
        }
    }

    private String makeBarcode(String idBarcode, boolean passScalesItem, String weightCode) {
        return idBarcode != null && idBarcode.length() == 5 && passScalesItem && weightCode != null ? (weightCode + idBarcode) : idBarcode;
    }

    @Override
    public void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException {

    }

    @Override
    public void sendStopListInfo(StopListInfo stopListInfo, Set<String> directorySet) throws IOException {
        UKM4MySQLSettings ukm4MySQLSettings = springContext.containsBean("ukm4MySQLSettings") ? (UKM4MySQLSettings) springContext.getBean("ukm4MySQLSettings") : null;
        String connectionString = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getImportConnectionString();
        String user = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getUser();
        String password = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getPassword();
        Integer timeout = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getTimeout();
        timeout = timeout == null ? 300 : timeout;
        boolean skipBarcodes = ukm4MySQLSettings == null || ukm4MySQLSettings.getSkipBarcodes() != null && ukm4MySQLSettings.getSkipBarcodes();

        if (connectionString != null) {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = DriverManager.getConnection(connectionString, user, password);

                int version = getVersion(conn);

                if(!skipBarcodes) {
                    processTransactionLogger.info("ukm4 mysql: executing stopLists, table pricelist_var");
                    conn.setAutoCommit(false);

                    ps = conn.prepareStatement(
                            "INSERT INTO pricelist_var (pricelist, var, price, version, deleted) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE price=VALUES(price), deleted=VALUES(deleted)");
                    for (ItemInfo item : stopListInfo.stopListItemMap.values()) {
                        if (item.idBarcode != null) {
                            for (Integer nppGroupMachinery : stopListInfo.inGroupMachineryItemMap.keySet()) {
                                ps.setInt(1, nppGroupMachinery); //pricelist
                                ps.setString(2, item.idBarcode); //var
                                ps.setBigDecimal(3, BigDecimal.ZERO); //price
                                ps.setInt(4, version); //version
                                ps.setInt(5, stopListInfo.exclude ? 0 : 1); //deleted
                                ps.addBatch();
                            }
                        }
                    }
                    ps.executeBatch();
                    conn.commit();
                }

                processTransactionLogger.info("ukm4 mysql: executing stopLists, table pricelist_items");
                ps = conn.prepareStatement(
                        "INSERT INTO pricelist_items (pricelist, item, price, minprice, version, deleted) VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE price=VALUES(price), minprice=VALUES(minprice), deleted=VALUES(deleted)");

                for (ItemInfo item : stopListInfo.stopListItemMap.values()) {
                    if(item.idItem != null) {
                        for(Integer nppGroupMachinery : stopListInfo.inGroupMachineryItemMap.keySet()) {
                            ps.setInt(1, nppGroupMachinery); //pricelist
                            ps.setString(2, trim(item.idItem, 40, "")); //item
                            ps.setBigDecimal(3, BigDecimal.ZERO); //price
                            ps.setBigDecimal(4, BigDecimal.ZERO); //minprice
                            ps.setInt(5, version); //version
                            ps.setInt(6, stopListInfo.exclude ? 0 : 1); //deleted
                            ps.addBatch();
                        }
                    }
                }
                ps.executeBatch();
                conn.commit();

                processTransactionLogger.info("ukm4 mysql: executing stopLists, table signal");
                exportSignals(conn, null, version, true, timeout);

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ps != null)
                        ps.close();
                    if (conn != null)
                        conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void sendDiscountCardList(List<DiscountCard> discountCardList, Date startDate, Set<String> directory) throws IOException {
    }

    @Override
    public void sendPromotionInfo(PromotionInfo promotionInfo, Set<String> directory) throws IOException {
    }

    @Override
    public void sendCashierInfoList(List<CashierInfo> cashierInfoList, Set<String> directory) throws IOException {
    }

    @Override
    public List<CashierTime> requestCashierTime(List<MachineryInfo> cashRegisterInfoList) throws IOException, ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public SalesBatch readSalesInfo(String directory, List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException {

        UKM4MySQLSalesBatch salesBatch = null;

        String weightCode = null;
        Map<Integer, Integer> machineryMap = new HashMap<>();
        for (CashRegisterInfo c : cashRegisterInfoList) {
            if (c.handlerModel != null && c.handlerModel.endsWith("UKM4MySQLHandler")) {
                if(c.number != null && c.numberGroup != null)
                    machineryMap.put(c.number, c.numberGroup);
                if (c.weightCodeGroupCashRegister != null) {
                    weightCode = c.weightCodeGroupCashRegister;
                }
            }
        }

        try {

            Class.forName("com.mysql.jdbc.Driver");

            UKM4MySQLSettings ukm4MySQLSettings = springContext.containsBean("ukm4MySQLSettings") ? (UKM4MySQLSettings) springContext.getBean("ukm4MySQLSettings") : null;
            String connectionString = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getExportConnectionString(); //"jdbc:mysql://172.16.0.35/export_axapta"
            String user = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getUser(); //luxsoft
            String password = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getPassword(); //123456

            if(connectionString == null) {
                processTransactionLogger.error("No exportConnectionString in ukm4MySQLSettings found");
            } else {

                Connection conn = null;

                try {
                    conn = DriverManager.getConnection(connectionString, user, password);

                    salesBatch = readSalesInfoFromSQL(conn, weightCode, machineryMap);

                } finally {
                    if (conn != null)
                        conn.close();
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return salesBatch;
    }

    private Map<Integer, String> readLoginMap(Connection conn) throws SQLException {

        Map<Integer, String> loginMap = new HashMap<>();

        Statement statement = null;
        try {
            statement = conn.createStatement();
            String query = "select id, user_id from login";
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()) {
                Integer id = rs.getInt(1);
                String idEmployee = String.valueOf(rs.getInt(2));

                loginMap.put(id, idEmployee);
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (statement != null)
                statement.close();
        }
        return loginMap;
    }

    private Map<String, Map<Integer, BigDecimal>> readPaymentMap(Connection conn) throws SQLException {

        Map<String, Map<Integer, BigDecimal>> paymentMap = new HashMap<>();

        Statement statement = null;
        try {
            statement = conn.createStatement();
            String query = "select p.cash_id, p.receipt_header, p.payment_id, p.amount, r.type " +
                    "from receipt_payment p left join receipt r on p.cash_id = r.cash_id and p.receipt_header = r.id " +
                    "where r.ext_processed = 0 AND r.result = 0 AND p.type != 3"; // type 3 это сдача
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()) {
                Integer cash_id = rs.getInt(1); //cash_id
                Integer idReceipt = rs.getInt(2); //receipt_header
                String key = String.valueOf(cash_id) + "/" + String.valueOf(idReceipt);
                Integer paymentType = rs.getInt(3); //payment_id
                if(paymentType.equals(2) || paymentType.equals(3))
                    paymentType = 1; //1, 2 и 3 - безнал
                BigDecimal amount = rs.getBigDecimal(4);
                Integer receiptType = rs.getInt(5); //r.type
                boolean isReturn = receiptType == 1 || receiptType == 4 || receiptType == 9;
                amount = isReturn ? amount.negate() : amount;

                Map<Integer, BigDecimal> paymentEntry = paymentMap.get(key);
                if(paymentEntry == null)
                    paymentEntry = new HashMap<>();
                BigDecimal sum = paymentEntry.get(paymentType);
                paymentEntry.put(paymentType, sum == null ? amount : sum.add(amount));
                paymentMap.put(key, paymentEntry);
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (statement != null)
                statement.close();
        }
        return paymentMap;
    }

    private UKM4MySQLSalesBatch readSalesInfoFromSQL(Connection conn, String weightCode, Map<Integer, Integer> machineryMap) throws SQLException {

        List<SalesInfo> salesInfoList = new ArrayList<>();

        Map<Integer, String> loginMap = readLoginMap(conn);
        Set<Pair<Integer, Integer>> receiptSet = new HashSet<>();
        Map<String, Map<Integer, BigDecimal>> paymentMap = readPaymentMap(conn);

        if(paymentMap != null) {
            Statement statement = null;
            try {
                statement = conn.createStatement();
                String query = "SELECT i.store, i.cash_number, i.cash_id, i.id, i.receipt_header, i.var, i.item, i.total_quantity, i.price, i.total," +
                        " i.position, i.real_amount, i.stock_id, r.type, r.shift_open, r.global_number, r.date, r.cash_id, r.id, r.login" +
                        " FROM receipt_item AS i LEFT JOIN receipt AS r ON i.receipt_header = r.id AND i.cash_id = r.cash_id" +
                        " WHERE r.ext_processed = 0 AND r.result = 0 AND i.type = 0" ;
                ResultSet rs = statement.executeQuery(query);

                while (rs.next()) {

                    //Integer nppGroupMachinery = Integer.parseInt(rs.getString(1)); //i.store
                    //Integer nppMachinery = rs.getInt(2); //i.cash_number

                    Integer cash_id = rs.getInt(3); //i.cash_id
                    Integer nppGroupMachinery = machineryMap.get(cash_id);

                    //Integer id = rs.getInt(4); //i.id
                    Integer idReceipt = rs.getInt(5); //i.receipt_header
                    String idBarcode = rs.getString(6); //i.var
                    if (idBarcode != null && weightCode != null && (idBarcode.length() == 13 || idBarcode.length() == 7) && idBarcode.startsWith(weightCode))
                        idBarcode = idBarcode.substring(2, 7);
                    String idItem = rs.getString(7); //i.item
                    BigDecimal totalQuantity = rs.getBigDecimal(8); //i.total_quantity
                    BigDecimal price = rs.getBigDecimal(9); //i.price
                    BigDecimal sum = rs.getBigDecimal(10); //i.total
                    Integer position = rs.getInt(11) + 1;
                    BigDecimal realAmount = rs.getBigDecimal(12); //i.real_amount
                    String idSection = rs.getString(13);

                    Map<Integer, BigDecimal> paymentEntry = paymentMap.get(cash_id + "/" + idReceipt);
                    if (paymentEntry != null && totalQuantity != null) {
                        Integer receiptType = rs.getInt(14); //r.type
                        boolean isSale = receiptType == 0 || receiptType == 8;
                        boolean isReturn = receiptType == 1 || receiptType == 4 || receiptType == 9;
                        String numberZReport = rs.getString(15); //r.shift_open
                        Integer numberReceipt = rs.getInt(16); //r.global_number
                        Date dateReceipt = rs.getDate(17); // r.date
                        Time timeReceipt = rs.getTime(17); //r.date
                        Integer login = rs.getInt(18); //r.login
                        String idEmployee = loginMap.get(login);

                        BigDecimal sumCash = paymentEntry.get(0);
                        BigDecimal sumCard = paymentEntry.get(1);
                        BigDecimal sumGiftCard = paymentEntry.get(2);

                        totalQuantity = isSale ? totalQuantity : isReturn ? totalQuantity.negate() : null;
                        BigDecimal discountSumReceiptDetail = safeSubtract(sum, realAmount);
                        if(totalQuantity != null) {
                            salesInfoList.add(new SalesInfo(false, nppGroupMachinery, cash_id, numberZReport,
                                    numberReceipt, dateReceipt, timeReceipt, idEmployee, null, null,
                                    sumCard, sumCash, sumGiftCard, idBarcode, idItem, null, null, totalQuantity, price,
                                    isSale ? realAmount : realAmount.negate(), discountSumReceiptDetail, null, null,
                                    position, null, idSection));
                            receiptSet.add(Pair.create(idReceipt, cash_id));
                        }
                    }
                }
                if (salesInfoList.size() > 0)
                    sendSalesLogger.info(String.format("UKM4: found %s records", salesInfoList.size()));
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            } finally {
                if (statement != null)
                    statement.close();
            }
        }
        return new UKM4MySQLSalesBatch(salesInfoList, receiptSet);
    }

    @Override
    public void requestSalesInfo(List<RequestExchange> requestExchangeList, Set<String> directorySet,
                                 Set<Integer> succeededRequests, Map<Integer, String> failedRequests, Map<Integer, String> ignoredRequests) throws IOException, ParseException {
        UKM4MySQLSettings ukm4MySQLSettings = springContext.containsBean("ukm4MySQLSettings") ? (UKM4MySQLSettings) springContext.getBean("ukm4MySQLSettings") : null;
        String connectionString = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getExportConnectionString(); //"jdbc:mysql://172.16.0.35/export_axapta"
        String user = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getUser(); //luxsoft
        String password = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getPassword(); //123456

        if (connectionString != null) {
            Connection conn = null;
            Statement statement = null;
            try {
                conn = DriverManager.getConnection(connectionString, user, password);

                for (RequestExchange entry : requestExchangeList) {
                    try {
                        if (entry.isSalesInfoExchange()) {
                            String dateFrom = new SimpleDateFormat("yyyy-MM-dd").format(entry.dateFrom);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(entry.dateTo);
                            cal.add(Calendar.DATE, 1);
                            String dateTo = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

                            String cashIdWhere = null;
                            if(!entry.cashRegisterSet.isEmpty()) {
                                cashIdWhere = "AND cash_id IN (";
                                for (Integer cashId : entry.cashRegisterSet) {
                                    cashIdWhere += cashId + ",";
                                }
                                cashIdWhere = cashIdWhere.substring(0, cashIdWhere.length() - 1) + ")";
                            }

                            statement = conn.createStatement();
                            String query = String.format("UPDATE receipt SET ext_processed = 0 WHERE date >= '%s' AND date <= '%s'", dateFrom, dateTo) +
                                    (cashIdWhere == null ? "" : cashIdWhere);
                            statement.execute(query);
                            succeededRequests.add(entry.requestExchange);
                        }
                    } catch (SQLException e) {
                        failedRequests.put(entry.requestExchange, e.getMessage());
                        e.printStackTrace();
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (statement != null)
                        statement.close();
                    if (conn != null)
                        conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public CashDocumentBatch readCashDocumentInfo(List<CashRegisterInfo> cashRegisterInfoList, Set<String> cashDocumentSet) throws ClassNotFoundException {
        return null;
    }

    @Override
    public void finishReadingCashDocumentInfo(CashDocumentBatch cashDocumentBatch) {
    }

    @Override
    public void finishReadingSalesInfo(UKM4MySQLSalesBatch salesBatch) {

        UKM4MySQLSettings ukm4MySQLSettings = springContext.containsBean("ukm4MySQLSettings") ? (UKM4MySQLSettings) springContext.getBean("ukm4MySQLSettings") : null;
        String connectionString = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getExportConnectionString(); //"jdbc:mysql://172.16.0.35/export_axapta"
        String user = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getUser(); //luxsoft
        String password = ukm4MySQLSettings == null ? null : ukm4MySQLSettings.getPassword(); //123456

        if (connectionString != null && salesBatch.receiptSet != null) {

            Connection conn = null;
            PreparedStatement ps = null;

            try {
                conn = DriverManager.getConnection(connectionString, user, password);

                conn.setAutoCommit(false);
                ps = conn.prepareStatement("UPDATE receipt SET ext_processed = 1 WHERE id = ? AND cash_id = ?");
                for (Pair<Integer, Integer> receiptEntry : salesBatch.receiptSet) {
                    ps.setInt(1, receiptEntry.first); //id
                    ps.setInt(2, receiptEntry.second); //cash_id
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(ps != null)
                        ps.close();
                    if (conn != null)
                        conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Map<String, Timestamp> requestSucceededSoftCheckInfo(Set<String> directorySet) {
        return null;
    }

    @Override
    public List<List<Object>> checkZReportSum(Map<String, List<Object>> zReportSumMap, List<List<Object>> cashRegisterList) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public Map<String, List<Object>> readExtraCheckZReport(List<CashRegisterInfo> cashRegisterInfoList) throws ClassNotFoundException, SQLException {
        return null;
    }

    @Override
    public ExtraCheckZReportBatch compareExtraCheckZReport(Map<String, List<Object>> handlerZReportSumMap, Map<String, BigDecimal> baseZReportSumMap) throws ClassNotFoundException, SQLException {
        return null;
    }

    protected String trim(String input, Integer length) {
        return trim(input, length, null);
    }

    protected String trim(String input, Integer length, String defaultValue) {
        return input == null ? defaultValue : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }

    protected Long parseGroup(String idItemGroup) {
        try {
            return idItemGroup == null ? 0 : Long.parseLong(idItemGroup.equals("Все") ? "0" : idItemGroup.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return (long) 0;
        }
    }

    protected BigDecimal safeSubtract(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else
            return (operand1 == null ? operand2.negate() : (operand2 == null ? operand1 : operand1.subtract((operand2))));
    }
}
