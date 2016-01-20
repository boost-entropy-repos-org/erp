package equ.srv.terminal;

import com.google.common.base.Throwables;
import equ.api.terminal.TerminalAssortment;
import equ.api.terminal.TerminalDocumentType;
import equ.api.terminal.TerminalHandbookType;
import equ.api.terminal.TerminalLegalEntity;
import equ.srv.EquipmentServer;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.*;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DefaultTerminalHandler implements TerminalHandlerInterface {

    private LogicsInstance logicsInstance;

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public DefaultTerminalHandler() {
        super();
    }

    @Override
    public List<String> readItem(DataSession session, DataObject user, String barcode) throws RemoteException, SQLException {
        try {
            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if(terminalHandlerLM != null) {
                String nameSkuBarcode = (String) terminalHandlerLM.findProperty("nameSku[Barcode]").read(session, terminalHandlerLM.findProperty("barcode[STRING[15]]").readClasses(session, new DataObject(barcode)));
                if(nameSkuBarcode == null)
                    return null;
                ObjectValue skuObject = terminalHandlerLM.findProperty("skuBarcode[STRING[15]]").readClasses(session, new DataObject(barcode));
                ObjectValue stockObject = user == null ? NullValue.instance : terminalHandlerLM.findProperty("stock[Employee]").readClasses(session, user);
                BigDecimal price = null;
                BigDecimal quantity = null;
                if(skuObject instanceof DataObject && stockObject instanceof DataObject) {
                    price = (BigDecimal) terminalHandlerLM.findProperty("currentRetailPricingPrice[Sku,Stock]").read(session, skuObject, stockObject);
                    quantity = (BigDecimal) terminalHandlerLM.findProperty("currentBalance[Sku,Stock]").read(session, skuObject, stockObject);
                }
                return Arrays.asList(barcode, nameSkuBarcode, price == null ? "0" : String.valueOf(price.longValue()),
                        quantity == null ? "0" : String.valueOf(quantity.longValue()));
            } else return null;

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String readItemHtml(DataSession session, String barcode, String idStock) throws RemoteException, SQLException {
        try {
            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if(terminalHandlerLM != null) {
                String nameSkuBarcode = (String) terminalHandlerLM.findProperty("nameSku[Barcode]").read(session, terminalHandlerLM.findProperty("barcode[STRING[15]]").readClasses(session, new DataObject(barcode)));
                if(nameSkuBarcode == null)
                    return null;

                ObjectValue skuObject = terminalHandlerLM.findProperty("skuBarcode[STRING[15]]").readClasses(session, new DataObject(barcode));
                ObjectValue stockObject = terminalHandlerLM.findProperty("stock[VARSTRING[100]]").readClasses(session, new DataObject(idStock));
                BigDecimal price = null;
                BigDecimal oldPrice = null;
                if(skuObject instanceof DataObject && stockObject instanceof DataObject) {
                    price = (BigDecimal) terminalHandlerLM.findProperty("transactionPrice[Sku,Stock]").read(session, skuObject, stockObject);
                    oldPrice = (BigDecimal) terminalHandlerLM.findProperty("transactionPrice[Sku,Stock]").read(session, skuObject, stockObject);
                }
                boolean action = price != null && oldPrice != null && price.compareTo(oldPrice) == 0;

                /*boolean action = barcode.equals("1");
                String nameSkuBarcode = barcode.equals("1") ? "Товар 1 со скидкой" : "Товар 2 без скидки";
                BigDecimal price = barcode.equals("1") ? BigDecimal.valueOf(5000) : BigDecimal.valueOf(10000);
                BigDecimal oldPrice = BigDecimal.valueOf(12000);*/

                return action ?
                        String.format("<html><body bgcolor=\"#FFFF00\">Наименование: <b>%s</b><br/><b><font color=\"#FF0000\">Акция</font></b> Цена: <b>%s</b>, Скидка: <b>%s</b></body></html>",
                        nameSkuBarcode, String.valueOf(price.longValue()), String.valueOf(oldPrice.longValue() - price.longValue()))
                        : String.format("<html><body>Наименование: <b>%s</b><br/>Цена: <b>%s</b></body></html>",
                        nameSkuBarcode, price == null ? "0" : String.valueOf(price.longValue()));
            } else return null;

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public byte[] readBase(DataSession session, DataObject userObject) throws RemoteException, SQLException {
        Connection connection = null;
        File file = null;
        File zipFile = null;
        try {
            BusinessLogics BL = getLogicsInstance().getBusinessLogics();
            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if (terminalHandlerLM != null) {

                ObjectValue stockObject = terminalHandlerLM.findProperty("stock[Employee]").readClasses(session, userObject);
                DataObject priceListTypeObject = ((ConcreteCustomClass) terminalHandlerLM.findClass("SystemLedgerPriceListType")).getDataObject("manufacturingPriceStockPriceListType");

                List<List<Object>> itemList = readItemList(session, stockObject);
                List<TerminalAssortment> assortmentList = EquipmentServer.readTerminalAssortmentList(session, BL, priceListTypeObject, stockObject);
                List<TerminalHandbookType> handbookTypeList = EquipmentServer.readTerminalHandbookTypeList(session, BL);
                List<TerminalLegalEntity> terminalLegalEntityList = EquipmentServer.readTerminalLegalEntityList(session, BL);
                List<TerminalDocumentType> terminalDocumentTypeList = EquipmentServer.readTerminalDocumentTypeList(session, BL);
                file = File.createTempFile("terminalHandler", ".db");

                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

                createGoodsTable(connection);
                updateGoodsTable(connection, itemList);

                createOrderTable(connection);

                createAssortTable(connection);
                updateAssortTable(connection, assortmentList);

                createVANTable(connection);
                updateVANTable(connection, handbookTypeList);

                createANATable(connection);
                updateANATable(connection, terminalLegalEntityList);

                createVOPTable(connection);
                updateVOPTable(connection, terminalDocumentTypeList);

                //copy base to exchange directory
                FileInputStream fis = new FileInputStream(file);
                zipFile = File.createTempFile("base", ".zip");
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                zos.putNextEntry(new ZipEntry("tsd.db"));
                byte[] buf = new byte[1024];
                int len;
                while ((len = fis.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                fis.close();
                zos.close();

                return IOUtils.getFileBytes(zipFile);
            } else return null;

        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if(file != null && !file.delete())
                file.deleteOnExit();
            if(zipFile != null && !zipFile.delete())
                zipFile.deleteOnExit();
            if (connection != null)
                connection.close();
        }
    }

    private List<List<Object>> readItemList(DataSession session, ObjectValue stockObject) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        List<List<Object>> result = new ArrayList<>();
        ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
        if(terminalHandlerLM != null) {
            KeyExpr itemExpr = new KeyExpr("item");
            ImRevMap<Object, KeyExpr> itemKeys = MapFact.singletonRev((Object) "item", itemExpr);

            QueryBuilder<Object, Object> itemQuery = new QueryBuilder<>(itemKeys);
            itemQuery.addProperty("idBarcodeSku", terminalHandlerLM.findProperty("idBarcode[Sku]").getExpr(itemExpr));
            itemQuery.addProperty("nameSku", terminalHandlerLM.findProperty("name[Sku]").getExpr(itemExpr));
            itemQuery.addProperty("transactionPriceSkuStock", terminalHandlerLM.findProperty("transactionPrice[Sku,Stock]").getExpr(itemExpr, stockObject.getExpr()));
            //itemQuery.addProperty("quantitySkuStock", terminalHandlerLM.findProperty("quantitySkuStock").getExpr(itemExpr, stockObject.getExpr()));
            itemQuery.and(terminalHandlerLM.findProperty("idBarcode[Sku]").getExpr(itemExpr).getWhere());
            itemQuery.and(terminalHandlerLM.findProperty("transactionPrice[Sku,Stock]").getExpr(itemExpr, stockObject.getExpr()).getWhere());
            //itemQuery.and(terminalHandlerLM.findProperty("quantitySkuStock").getExpr(itemExpr, stockObject.getExpr()).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> itemResult = itemQuery.execute(session);
            for (ImMap<Object, Object> entry : itemResult.values()) {

                String idBarcodeSku = trim((String) entry.get("idBarcodeSku"));
                String nameSku = trim((String) entry.get("nameSku"));
                BigDecimal transactionPriceSkuStock = (BigDecimal) entry.get("transactionPriceSkuStock");
                BigDecimal quantitySkuStock = BigDecimal.ONE;//(BigDecimal) entry.get("quantitySkuStock");

                result.add(Arrays.<Object>asList(idBarcodeSku, nameSku, transactionPriceSkuStock, quantitySkuStock));

            }
        }
        return result;
    }

    private String trim(String input) {
        return input == null ? null : input.trim();
    }

    private void createOrderTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE zayavki " +
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
        statement.execute("CREATE INDEX zayavki_post ON zayavki (post);");
        statement.close();
    }

    private void createGoodsTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE goods " +
                "(barcode TEXT PRIMARY KEY," +
                " naim    TEXT," +
                " price   REAL," +
                " quant   REAL," +
                " fld1    TEXT," +
                " fld2    TEXT," +
                " fld3    TEXT," +
                " fld4    TEXT," +
                " fld5    TEXT," +
                " image   TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateGoodsTable(Connection connection, List<List<Object>> itemList) throws SQLException {
        if (!itemList.isEmpty()) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO goods VALUES(?, ?, ?, ?, '', '', '', '', '', '');";
                statement = connection.prepareStatement(sql);
                for (List<Object> item : itemList) {
                    if (item.get(0) != null) {
                        statement.setObject(1, formatValue(item.get(0))); //idBarcode
                        statement.setObject(2, formatValue(item.get(1))); //name
                        statement.setObject(3, formatValue(item.get(2))); //price
                        statement.setObject(4, formatValue(item.get(3))); //quantity
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
                connection.createStatement().execute("CREATE INDEX goods_naim ON goods (naim ASC);");
                connection.commit();

            } finally {
                if(statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createAssortTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE assort " +
                "(post    TEXT," +
                " barcode TEXT," +
                "PRIMARY KEY ( post, barcode))";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateAssortTable(Connection connection, List<TerminalAssortment> terminalAssortmentList) throws SQLException {
        if (!terminalAssortmentList.isEmpty()) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO assort VALUES(?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalAssortment assortment : terminalAssortmentList) {
                    if (assortment.idSupplier != null && assortment.idBarcode != null) {
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

    private void createVANTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE van " +
                "(van    TEXT PRIMARY KEY," +
                " naim   TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateVANTable(Connection connection, List<TerminalHandbookType> terminalHandbookTypeList) throws SQLException {
        if (!terminalHandbookTypeList.isEmpty()) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO van VALUES(?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalHandbookType terminalHandbookType : terminalHandbookTypeList) {
                    if (terminalHandbookType.id != null && terminalHandbookType.name != null) {
                        statement.setObject(1, formatValue(terminalHandbookType.id)); //id
                        statement.setObject(2, formatValue(terminalHandbookType.name)); //name
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

    private void createANATable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE ana " +
                "(ana  TEXT PRIMARY KEY," +
                " naim TEXT," +
                " fld1 TEXT," +
                " fld2 TEXT," +
                " fld3 TEXT)";
        statement.executeUpdate(sql);
        statement.close();
    }

    private void updateANATable(Connection connection, List<TerminalLegalEntity> terminalLegalEntityList) throws SQLException {
        if (!terminalLegalEntityList.isEmpty()) {
            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO ana VALUES(?, ?, '', '', '');";
                statement = connection.prepareStatement(sql);
                for (TerminalLegalEntity legalEntity : terminalLegalEntityList) {
                    if (legalEntity.idLegalEntity != null) {
                        statement.setObject(1, "ПС" + formatValue(legalEntity.idLegalEntity));
                        statement.setObject(2, formatValue(legalEntity.nameLegalEntity));
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
                connection.createStatement().execute("CREATE INDEX ana_naim ON ana (naim ASC);");
                connection.commit();
            } finally {
                if (statement != null)
                    statement.close();
                connection.setAutoCommit(true);
            }
        }
    }

    private void createVOPTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE vop " +
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

    private void updateVOPTable(Connection connection, List<TerminalDocumentType> terminalDocumentTypeList) throws SQLException {
        if (!terminalDocumentTypeList.isEmpty()) {

            PreparedStatement statement = null;
            try {
                connection.setAutoCommit(false);
                String sql = "INSERT OR REPLACE INTO vop VALUES(?, ?, ?, ?, ?, ?, ?);";
                statement = connection.prepareStatement(sql);
                for (TerminalDocumentType tdt : terminalDocumentTypeList) {
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

    private Object formatValue(Object value) {
        return value == null ? "" : value;
    }

    @Override
    public String importTerminalDocument(DataSession session, DataObject userObject, String idTerminalDocument, List<List<Object>> terminalDocumentDetailList, boolean emptyDocument) throws RemoteException, SQLException {
        try {

            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if(terminalHandlerLM != null) {

                List<ImportProperty<?>> props = new ArrayList<>();
                List<ImportField> fields = new ArrayList<>();
                List<ImportKey<?>> keys = new ArrayList<>();

                ImportField idTerminalDocumentField = new ImportField(terminalHandlerLM.findProperty("id[TerminalDocument]"));
                ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) terminalHandlerLM.findClass("TerminalDocument"),
                        terminalHandlerLM.findProperty("terminalDocument[VARSTRING[100]]").getMapping(idTerminalDocumentField));
                keys.add(terminalDocumentKey);
                props.add(new ImportProperty(idTerminalDocumentField, terminalHandlerLM.findProperty("id[TerminalDocument]").getMapping(terminalDocumentKey)));
                fields.add(idTerminalDocumentField);

                ImportField numberTerminalDocumentField = new ImportField(terminalHandlerLM.findProperty("title[TerminalDocument]"));
                props.add(new ImportProperty(numberTerminalDocumentField, terminalHandlerLM.findProperty("title[TerminalDocument]").getMapping(terminalDocumentKey)));
                fields.add(numberTerminalDocumentField);

                ImportField idTerminalDocumentTypeField = new ImportField(terminalHandlerLM.findProperty("id[TerminalDocumentType]"));
                ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) terminalHandlerLM.findClass("TerminalDocumentType"),
                        terminalHandlerLM.findProperty("terminalDocumentType[VARSTRING[100]]").getMapping(idTerminalDocumentTypeField));
                terminalDocumentTypeKey.skipKey = true;
                keys.add(terminalDocumentTypeKey);
                props.add(new ImportProperty(idTerminalDocumentTypeField, terminalHandlerLM.findProperty("terminalDocumentType[TerminalDocument]").getMapping(terminalDocumentKey),
                        terminalHandlerLM.object(terminalHandlerLM.findClass("TerminalDocumentType")).getMapping(terminalDocumentTypeKey)));
                fields.add(idTerminalDocumentTypeField);

                if (!emptyDocument) {

                    ImportField idTerminalDocumentDetailField = new ImportField(terminalHandlerLM.findProperty("id[TerminalDocumentDetail]"));
                    ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) terminalHandlerLM.findClass("TerminalDocumentDetail"),
                            terminalHandlerLM.findProperty("terminalIdTerminalId[VARSTRING[100],VARSTRING[100]]").getMapping(idTerminalDocumentField, idTerminalDocumentDetailField));
                    keys.add(terminalDocumentDetailKey);
                    props.add(new ImportProperty(idTerminalDocumentDetailField, terminalHandlerLM.findProperty("id[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey)));
                    props.add(new ImportProperty(idTerminalDocumentField, terminalHandlerLM.findProperty("terminalDocument[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey),
                            terminalHandlerLM.object(terminalHandlerLM.findClass("TerminalDocument")).getMapping(terminalDocumentKey)));
                    fields.add(idTerminalDocumentDetailField);

                    ImportField numberTerminalDocumentDetailField = new ImportField(terminalHandlerLM.findProperty("number[TerminalDocumentDetail]"));
                    props.add(new ImportProperty(numberTerminalDocumentDetailField, terminalHandlerLM.findProperty("number[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey)));
                    fields.add(numberTerminalDocumentDetailField);

                    ImportField barcodeTerminalDocumentDetailField = new ImportField(terminalHandlerLM.findProperty("barcode[TerminalDocumentDetail]"));
                    props.add(new ImportProperty(barcodeTerminalDocumentDetailField, terminalHandlerLM.findProperty("barcode[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey)));
                    fields.add(barcodeTerminalDocumentDetailField);

                    ImportField quantityTerminalDocumentDetailField = new ImportField(terminalHandlerLM.findProperty("quantity[TerminalDocumentDetail]"));
                    props.add(new ImportProperty(quantityTerminalDocumentDetailField, terminalHandlerLM.findProperty("quantity[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey)));
                    fields.add(quantityTerminalDocumentDetailField);

                    ImportField priceTerminalDocumentDetailField = new ImportField(terminalHandlerLM.findProperty("price[TerminalDocumentDetail]"));
                    props.add(new ImportProperty(priceTerminalDocumentDetailField, terminalHandlerLM.findProperty("price[TerminalDocumentDetail]").getMapping(terminalDocumentDetailKey)));
                    fields.add(priceTerminalDocumentDetailField);
                }

                ImportTable table = new ImportTable(fields, terminalDocumentDetailList);

                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);

                ObjectValue terminalDocumentObject = terminalHandlerLM.findProperty("terminalDocument[VARSTRING[100]]").readClasses(session, session.getModifier(), session.getQueryEnv(), new DataObject(idTerminalDocument));
                terminalHandlerLM.findProperty("createdUser[TerminalDocument]").change(userObject.object, session, (DataObject) terminalDocumentObject);
                terminalHandlerLM.findAction("process[TerminalDocument]").execute(session, terminalDocumentObject);

                String result = session.applyMessage(getLogicsInstance().getBusinessLogics());
                session.close();
                return result;

            } else return "-1";

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean isActiveTerminal(DataSession session, String idTerminal) throws RemoteException, SQLException {
        try {
            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if (terminalHandlerLM != null) {
                boolean checkIdTerminal = terminalHandlerLM.findProperty("checkIdTerminal[]").read(session) != null;
                if(checkIdTerminal) {
                    ObjectValue terminalObject = terminalHandlerLM.findProperty("terminal[VARSTRING[100]]").readClasses(session, new DataObject(idTerminal));
                    return terminalObject instanceof DataObject && terminalHandlerLM.findProperty("blocked[Terminal]").read(session, terminalObject) == null;
                }
            }
            return true;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public DataObject login(DataSession session, String login, String password, String idTerminal) throws RemoteException, SQLException {
        try {

            ScriptingLogicsModule terminalHandlerLM = getLogicsInstance().getBusinessLogics().getModule("TerminalHandler");
            if(terminalHandlerLM != null) {
                terminalHandlerLM.findAction("calculateBase64Hash[STRING[10],STRING[30]]").execute(session, new DataObject("SHA-256"), new DataObject(password));
                String calculatedHash = (String) terminalHandlerLM.findProperty("calculatedHash[]").read(session);
                ObjectValue customUser = terminalHandlerLM.findProperty("customUserUpcase[?]").readClasses(session, new DataObject(login.toUpperCase()));
                String sha256PasswordCustomUser = (String) terminalHandlerLM.findProperty("sha256Password[CustomUser]").read(session, customUser);
                boolean check = customUser instanceof DataObject && sha256PasswordCustomUser != null && calculatedHash != null && sha256PasswordCustomUser.equals(calculatedHash);
                DataObject result = check ? (DataObject) customUser : null;
                if(result != null) {
                    ObjectValue terminalObject = terminalHandlerLM.findProperty("terminal[VARSTRING[100]]").readClasses(session, new DataObject(idTerminal));
                    if(terminalObject instanceof DataObject) {
                        terminalHandlerLM.findProperty("lastConnectionTime[Terminal]").change(new Timestamp(Calendar.getInstance().getTime().getTime()), session, (DataObject) terminalObject);
                        terminalHandlerLM.findProperty("lastUser[Terminal]").change(result.getValue(), session, (DataObject) terminalObject);
                        String applyMessage = session.applyMessage(getLogicsInstance().getBusinessLogics());
                        if(applyMessage != null)
                            ServerLoggers.systemLogger.error(String.format("Terminal Login error: %s, login %s, terminal %s", applyMessage, login, idTerminal));
                    }
                }
                return result;
            } else return null;

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


}