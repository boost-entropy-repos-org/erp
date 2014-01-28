package equ.srv;

import com.google.common.base.Throwables;
import equ.api.*;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import lsfusion.base.DateConverter;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.*;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class EquipmentServer extends LifecycleAdapter implements EquipmentServerInterface, InitializingBean {
    private static final Logger logger = Logger.getLogger(EquipmentServer.class);

    public static final String EXPORT_NAME = "EquipmentServer";

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule equLM;

    //Опциональные модули
    private ScriptingLogicsModule scalesItemLM;

    private boolean started = false;

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public RMIManager getRmiManager() {
        return logicsInstance.getRmiManager();
    }

    public BusinessLogics getBusinessLogics() {
        return logicsInstance.getBusinessLogics();
    }

    public DBManager getDbManager() {
        return logicsInstance.getDbManager();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        equLM = (ScriptingLogicsModule) getBusinessLogics().getModule("Equipment");
        Assert.notNull(equLM, "can't find Equipment module");
        scalesItemLM = (ScriptingLogicsModule) getBusinessLogics().getModule("ScalesItem");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Binding Equipment Server.");
        try {
            getRmiManager().bindAndExport(EXPORT_NAME, this);
            started = true;
        } catch (Exception e) {
            throw new RuntimeException("Error exporting Equipment Server: ", e);
        }
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (started) {
            logger.info("Stopping Equipment Server.");
            try {
                getRmiManager().unbindAndUnexport(EXPORT_NAME, this);
            } catch (Exception e) {
                throw new RuntimeException("Error stopping Equipment Server: ", e);
            }
        }
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException {
        try {

            DataSession session = getDbManager().createSession();
            List<TransactionInfo> transactionList = new ArrayList<TransactionInfo>();

            LCP isMachineryPriceTransaction = equLM.is(equLM.findClassByCompoundName("MachineryPriceTransaction"));
            ImRevMap<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);

            query.addProperty("dateTimeMPT", equLM.findLCPByCompoundOldName("dateTimeMachineryPriceTransaction").getExpr(key));
            query.addProperty("groupMachineryMPT", equLM.findLCPByCompoundOldName("groupMachineryMachineryPriceTransaction").getExpr(key));
            query.addProperty("snapshotMPT", equLM.findLCPByCompoundOldName("groupMachineryMachineryPriceTransaction").getExpr(key));

            query.and(equLM.findLCPByCompoundOldName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
            query.and(equLM.findLCPByCompoundOldName("processMachineryPriceTransaction").getExpr(key).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> transactionObjects = new ArrayList<Object[]>();
            for (int i = 0, size = result.size(); i < size; i++) {
                ImMap<Object, ObjectValue> value = result.getValue(i);
                DataObject dateTimeMPT = (DataObject) value.get("dateTimeMPT");
                DataObject groupMachineryMPT = (DataObject) value.get("groupMachineryMPT");
                DataObject transactionObject = result.getKey(i).singleValue();
                Boolean snapshotMPT = value.get("snapshotMPT") instanceof DataObject;
                transactionObjects.add(new Object[]{groupMachineryMPT, transactionObject, dateTimeCode((Timestamp) dateTimeMPT.getValue()), dateTimeMPT, snapshotMPT});
            }

            List<ItemInfo> skuTransactionList;
            for (Object[] transaction : transactionObjects) {

                DataObject groupObject = (DataObject) transaction[0];
                DataObject transactionObject = (DataObject) transaction[1];
                String dateTimeCode = (String) transaction[2];
                Date date = new Date(((Timestamp) ((DataObject) transaction[3]).getValue()).getTime());
                Boolean snapshotTransaction = (Boolean) transaction[4];

                skuTransactionList = new ArrayList<ItemInfo>();
                KeyExpr barcodeExpr = new KeyExpr("barcode");
                ImRevMap<Object, KeyExpr> skuKeys = MapFact.singletonRev((Object) "barcode", barcodeExpr);

                QueryBuilder<Object, Object> skuQuery = new QueryBuilder<Object, Object>(skuKeys);
                skuQuery.addProperty("idBarcode", equLM.findLCPByCompoundOldName("idBarcode").getExpr(barcodeExpr));
                skuQuery.addProperty("name", equLM.findLCPByCompoundOldName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("price", equLM.findLCPByCompoundOldName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("expiryDate", equLM.findLCPByCompoundOldName("expiryDateMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("isWeight", equLM.findLCPByCompoundOldName("isWeightMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("skuGroup", equLM.findLCPByCompoundOldName("skuGroupMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                if (scalesItemLM != null) {
                    skuQuery.addProperty("daysExpiry", scalesItemLM.findLCPByCompoundOldName("daysExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                    skuQuery.addProperty("hoursExpiry", scalesItemLM.findLCPByCompoundOldName("hoursExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                    skuQuery.addProperty("labelFormat", scalesItemLM.findLCPByCompoundOldName("labelFormatMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                    skuQuery.addProperty("composition", scalesItemLM.findLCPByCompoundOldName("compositionMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                }

                skuQuery.and(equLM.findLCPByCompoundOldName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> skuResult = skuQuery.execute(session.sql);

                for (ImMap<Object, Object> row : skuResult.valueIt()) {
                    String barcode = (String) row.get("idBarcode");
                    String name = (String) row.get("name");
                    BigDecimal price = (BigDecimal) row.get("price");
                    BigDecimal daysExpiry = (BigDecimal) row.get("daysExpiry");
                    Integer hoursExpiry = (Integer) row.get("hoursExpiry");
                    Date expiryDate = (Date) row.get("expiryDate");
                    Integer labelFormat = (Integer) row.get("labelFormat");
                    String composition = (String) row.get("composition");
                    Boolean isWeight = row.get("isWeight") != null;
                    Integer numberSkuGroup = (Integer) row.get("skuGroup");
                    String canonicalNameSkuGroup = numberSkuGroup == null ? "" : (String) equLM.findLCPByCompoundOldName("canonicalNameSkuGroup").read(session, new DataObject(numberSkuGroup, (ConcreteClass) equLM.findClassByCompoundName("ItemGroup")));

                    Integer cellScalesObject = composition == null ? null : (Integer) equLM.findLCPByCompoundOldName("cellScalesGroupScalesComposition").read(session, groupObject, new DataObject(composition, StringClass.text));
                    Integer compositionNumberCellScales = cellScalesObject == null ? null : (Integer) equLM.findLCPByCompoundOldName("numberCellScales").read(session, new DataObject(cellScalesObject, (ConcreteClass) equLM.findClassByCompoundName("CellScales")));

                    skuTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price, daysExpiry, hoursExpiry, expiryDate, labelFormat, composition, compositionNumberCellScales, isWeight, numberSkuGroup == null ? 0 : numberSkuGroup, canonicalNameSkuGroup.trim()));
                }

                if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("CashRegisterPriceTransaction"))) {
                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                    LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("CashRegister"));

                    ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                    KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                    cashRegisterQuery.addProperty("directoryCashRegister", equLM.findLCPByCompoundOldName("directoryCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("numberCashRegister", equLM.findLCPByCompoundOldName("numberCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundOldName("nameModelMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundOldName("handlerModelMachinery").getExpr(cashRegisterKey));

                    cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                    cashRegisterQuery.and(equLM.findLCPByCompoundOldName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    cashRegisterQuery.and(equLM.findLCPByCompoundOldName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                    for (ImMap<Object, Object> row : cashRegisterResult.valueIt()) {
                        String directoryCashRegister = (String) row.get("directoryCashRegister");
                        String portMachinery = (String) row.get("portMachinery");
                        Integer nppMachinery = (Integer) row.get("nppMachinery");
                        String numberCashRegister = (String) row.get("numberCashRegister");
                        String nameModel = (String) row.get("nameModelMachinery");
                        String handlerModel = (String) row.get("handlerModelMachinery");
                        cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister));
                    }

                    transactionList.add(new TransactionCashRegisterInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, date, skuTransactionList, cashRegisterInfoList));

                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("ScalesPriceTransaction"))) {
                    List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
                    String directory = (String) equLM.findLCPByCompoundOldName("directoryGroupScales").read(session, groupObject);
                    String pieceCodeGroupScales = (String) equLM.findLCPByCompoundOldName("pieceCodeGroupScales").read(session, groupObject);
                    String weightCodeGroupScales = (String) equLM.findLCPByCompoundOldName("weightCodeGroupScales").read(session, groupObject);

                    LCP<PropertyInterface> isScales = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Scales"));

                    ImRevMap<PropertyInterface, KeyExpr> scalesKeys = isScales.getMapKeys();
                    KeyExpr scalesKey = scalesKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> scalesQuery = new QueryBuilder<PropertyInterface, Object>(scalesKeys);

                    scalesQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundOldName("nameModelMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundOldName("handlerModelMachinery").getExpr(scalesKey));
                    scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                    scalesQuery.and(equLM.findLCPByCompoundOldName("groupScalesScales").getExpr(scalesKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    scalesQuery.and(equLM.findLCPByCompoundOldName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> scalesResult = scalesQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : scalesResult.valueIt()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        scalesInfoList.add(new ScalesInfo(nppMachinery, nameModel, handlerModel, portMachinery, directory,
                                pieceCodeGroupScales, weightCodeGroupScales));
                    }

                    transactionList.add(new TransactionScalesInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, scalesInfoList, snapshotTransaction));

                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("PriceCheckerPriceTransaction"))) {
                    List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<PriceCheckerInfo>();
                    LCP<PropertyInterface> isCheck = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Check"));

                    ImRevMap<PropertyInterface, KeyExpr> checkKeys = isCheck.getMapKeys();
                    KeyExpr checkKey = checkKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> checkQuery = new QueryBuilder<PropertyInterface, Object>(checkKeys);

                    checkQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nameCheckModelCheck", equLM.findLCPByCompoundOldName("nameCheckModelCheck").getExpr(checkKey));
                    //checkQuery.addProperty("handlerCheckModelCheck", equLM.findLCPByCompoundOldName("handlerCheckModelCheck").getExpr(checkKey));
                    checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                    checkQuery.and(equLM.findLCPByCompoundOldName("groupPriceCheckerPriceChecker").getExpr(checkKey).compare(groupObject, Compare.EQUALS));

                    //if (snapshotTransaction)
                    //    checkQuery.and(equLM.findLCPByCompoundOldName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> checkResult = checkQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : checkResult.valueIt()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameCheckModelCheck");
                        //String handlerModel = (String) values.get("handlerCheckModelCheck");
                        String handlerModel = null;
                        priceCheckerInfoList.add(new PriceCheckerInfo(nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionPriceCheckerInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, priceCheckerInfoList));


                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("TerminalPriceTransaction"))) {
                    List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();
                    LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Terminal"));

                    ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                    KeyExpr terminalKey = terminalKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                    terminalQuery.addProperty("directoryTerminal", equLM.findLCPByCompoundOldName("directoryTerminal").getExpr(terminalKey));
                    terminalQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundOldName("nameModelMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundOldName("handlerModelMachinery").getExpr(terminalKey));
                    terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                    terminalQuery.and(equLM.findLCPByCompoundOldName("groupTerminalTerminal").getExpr(terminalKey).compare(groupObject, Compare.EQUALS));

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : terminalResult.valueIt()) {
                        String directory = (String) values.get("directoryTerminal");
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        terminalInfoList.add(new TerminalInfo(directory, nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionTerminalInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, terminalInfoList, snapshotTransaction));
                }
            }
            return transactionList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("GroupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.and(equLM.findLCPByCompoundOldName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<DataObject> groupMachineryObjects = new ArrayList<DataObject>();
            for (int i = 0, size = result.size(); i < size; i++) {
                DataObject groupMachineryObject = result.getKey(i).getValue(0);
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (DataObject groupMachineryObject : groupMachineryObjects) {

                LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("CashRegister"));

                ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                cashRegisterQuery.addProperty("directoryCashRegister", equLM.findLCPByCompoundOldName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("numberCashRegister", equLM.findLCPByCompoundOldName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundOldName("nameModelMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundOldName("handlerModelMachinery").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(equLM.findLCPByCompoundOldName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (ImMap<Object, Object> row : cashRegisterResult.values()) {
                    String directoryCashRegister = (String) row.get("directoryCashRegister");
                    String portMachinery = (String) row.get("portMachinery");
                    Integer nppMachinery = (Integer) row.get("nppMachinery");
                    String numberCashRegister = (String) row.get("numberCashRegister");
                    String nameModel = (String) row.get("nameModelMachinery");
                    String handlerModel = (String) row.get("handlerModelMachinery");
                    cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister));
                }
            }
            return cashRegisterInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<TerminalInfo> readTerminalInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("GroupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.and(equLM.findLCPByCompoundOldName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object> groupMachineryObjects = new ArrayList<Object>();
            for (ImMap<PropertyInterface, DataObject> entry : result.keyIt()) {
                DataObject groupMachineryObject = entry.getValue(0);
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (Object groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery;

                LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Terminal"));

                ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                KeyExpr terminalKey = terminalKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                terminalQuery.addProperty("directoryTerminal", equLM.findLCPByCompoundOldName("directoryTerminal").getExpr(terminalKey));
                terminalQuery.addProperty("portMachinery", equLM.findLCPByCompoundOldName("portMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nppMachinery", equLM.findLCPByCompoundOldName("nppMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundOldName("nameModelMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundOldName("handlerModelMachinery").getExpr(terminalKey));

                terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                terminalQuery.and(equLM.findLCPByCompoundOldName("groupTerminalTerminal").getExpr(terminalKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                for (ImMap<Object, Object> row : terminalResult.valueIt()) {
                    String directoryTerminal = (String) row.get("directoryTerminal");
                    String portMachinery = (String) row.get("portMachinery");
                    Integer nppMachinery = (Integer) row.get("nppMachinery");
                    String nameModel = (String) row.get("nameModelMachinery");
                    String handlerModel = (String) row.get("handlerModelMachinery");
                    terminalInfoList.add(new TerminalInfo(directoryTerminal, nppMachinery, nameModel, handlerModel, portMachinery));
                }
            }
            return terminalInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<TerminalDocumentTypeInfo> readTerminalDocumentTypeInfo() throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();

            List<LegalEntityInfo> legalEntityInfoList = new ArrayList<LegalEntityInfo>();

            LCP<PropertyInterface> isLegalEntity = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("LegalEntity"));

            ImRevMap<PropertyInterface, KeyExpr> legalEntityKeys = isLegalEntity.getMapKeys();
            KeyExpr legalEntityKey = legalEntityKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> legalEntityQuery = new QueryBuilder<PropertyInterface, Object>(legalEntityKeys);

            legalEntityQuery.addProperty("name", equLM.findLCPByCompoundOldName("nameLegalEntity").getExpr(legalEntityKey));
            legalEntityQuery.and(isLegalEntity.property.getExpr(legalEntityKeys).getWhere());
            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> legalEntityResult = legalEntityQuery.execute(session.sql);
            for (int i = 0, size = legalEntityResult.size(); i < size; i++) {
                String id = String.valueOf(legalEntityResult.getKey(i).getValue(0));
                String name = (String) legalEntityResult.getValue(i).get("name");
                DataObject terminalHandbookTypeObject = ((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalHandbookType")).getDataObject("terminalHandbookTypeLegalEntity");
                String type = (String) equLM.findLCPByCompoundOldName("idTerminalHandbookType").read(session, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList = new ArrayList<TerminalDocumentTypeInfo>();
            LCP<PropertyInterface> isTerminalDocumentType = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("TerminalDocumentType"));

            ImRevMap<PropertyInterface, KeyExpr> terminalDocumentTypeKeys = isTerminalDocumentType.getMapKeys();
            KeyExpr terminalDocumentTypeKey = terminalDocumentTypeKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> terminalDocumentTypeQuery = new QueryBuilder<PropertyInterface, Object>(terminalDocumentTypeKeys);
            terminalDocumentTypeQuery.addProperty("idTerminalDocumentType", equLM.findLCPByCompoundOldName("idTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameTerminalDocumentType", equLM.findLCPByCompoundOldName("nameTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook1TerminalDocumentType", equLM.findLCPByCompoundOldName("nameInHandbook1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType1TerminalDocumentType", equLM.findLCPByCompoundOldName("idTerminalHandbookType1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook2TerminalDocumentType", equLM.findLCPByCompoundOldName("nameInHandbook2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType2TerminalDocumentType", equLM.findLCPByCompoundOldName("idTerminalHandbookType2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.and(isTerminalDocumentType.property.getExpr(terminalDocumentTypeKeys).getWhere());

            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalDocumentTypeResult = terminalDocumentTypeQuery.execute(session.sql);

            for (ImMap<Object, Object> values : terminalDocumentTypeResult.valueIt()) {
                String id = (String) values.get("idTerminalDocumentType");
                String name = (String) values.get("nameTerminalDocumentType");
                String nameInHandbook1 = (String) values.get("nameInHandbook1TerminalDocumentType");
                String idTerminalHandbookType1 = (String) values.get("idTerminalHandbookType1TerminalDocumentType");
                String nameInHandbook2 = (String) values.get("nameInHandbook2TerminalDocumentType");
                String idTerminalHandbookType2 = (String) values.get("idTerminalHandbookType1TerminalDocumentType");
                terminalDocumentTypeInfoList.add(new TerminalDocumentTypeInfo(id, name, nameInHandbook1, idTerminalHandbookType1,
                        nameInHandbook2, idTerminalHandbookType2, legalEntityInfoList));
            }
            return terminalDocumentTypeInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String sendSalesInfo(List<SalesInfo> salesInfoList, String equipmentServer) throws IOException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            ImportField numberCashRegisterField = new ImportField(equLM.findLCPByCompoundOldName("numberCashRegister"));

            ImportField idZReportField = new ImportField(equLM.findLCPByCompoundOldName("idZReport"));
            ImportField numberZReportField = new ImportField(equLM.findLCPByCompoundOldName("numberZReport"));

            ImportField idReceiptField = new ImportField(equLM.findLCPByCompoundOldName("idReceipt"));
            ImportField numberReceiptField = new ImportField(equLM.findLCPByCompoundOldName("numberReceipt"));
            ImportField dateReceiptField = new ImportField(equLM.findLCPByCompoundOldName("dateReceipt"));
            ImportField timeReceiptField = new ImportField(equLM.findLCPByCompoundOldName("timeReceipt"));
            ImportField isPostedZReportField = new ImportField(equLM.findLCPByCompoundOldName("isPostedZReport"));

            ImportField idReceiptDetailField = new ImportField(equLM.findLCPByCompoundOldName("idReceiptDetail"));
            ImportField numberReceiptDetailField = new ImportField(equLM.findLCPByCompoundOldName("numberReceiptDetail"));
            ImportField idBarcodeReceiptDetailField = new ImportField(equLM.findLCPByCompoundOldName("idBarcodeReceiptDetail"));

            ImportField quantityReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundOldName("quantityReceiptSaleDetail"));
            ImportField priceReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundOldName("priceReceiptSaleDetail"));
            ImportField sumReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundOldName("sumReceiptSaleDetail"));
            ImportField discountSumReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundOldName("discountSumReceiptSaleDetail"));
            ImportField discountSumSaleReceiptField = new ImportField(equLM.findLCPByCompoundOldName("discountSumSaleReceipt"));

            ImportField quantityReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundOldName("quantityReceiptReturnDetail"));
            ImportField priceReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundOldName("priceReceiptReturnDetail"));
            ImportField retailSumReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundOldName("sumReceiptReturnDetail"));
            ImportField discountSumReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundOldName("discountSumReceiptReturnDetail"));
            ImportField discountSumReturnReceiptField = new ImportField(equLM.findLCPByCompoundOldName("discountSumReturnReceipt"));

            ImportField idPaymentField = new ImportField(equLM.findLCPByCompoundOldName("idPayment"));
            ImportField sidTypePaymentField = new ImportField(equLM.findLCPByCompoundOldName("sidPaymentType"));
            ImportField sumPaymentField = new ImportField(equLM.findLCPByCompoundOldName("sumPayment"));
            ImportField numberPaymentField = new ImportField(equLM.findLCPByCompoundOldName("numberPayment"));

            ImportField seriesNumberDiscountCardField = new ImportField(equLM.findLCPByCompoundOldName("seriesNumberDiscountCard"));

            List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ZReport"), equLM.findLCPByCompoundOldName("zReportId").getMapping(idZReportField));
            ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("CashRegister"), equLM.findLCPByCompoundOldName("cashRegisterNumber").getMapping(numberCashRegisterField));
            ImportKey<?> receiptKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("Receipt"), equLM.findLCPByCompoundOldName("receiptId").getMapping(idReceiptField));
            ImportKey<?> skuKey = new ImportKey((CustomClass) equLM.findClassByCompoundName("Sku"), equLM.findLCPByCompoundOldName("skuBarcodeIdDate").getMapping(idBarcodeReceiptDetailField, dateReceiptField));
            ImportKey<?> discountCardKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("DiscountCard"), equLM.findLCPByCompoundOldName("discountCardSeriesNumber").getMapping(seriesNumberDiscountCardField, dateReceiptField));

            saleProperties.add(new ImportProperty(idZReportField, equLM.findLCPByCompoundOldName("idZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(numberZReportField, equLM.findLCPByCompoundOldName("numberZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(numberCashRegisterField, equLM.findLCPByCompoundOldName("cashRegisterZReport").getMapping(zReportKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
            saleProperties.add(new ImportProperty(dateReceiptField, equLM.findLCPByCompoundOldName("dateZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(timeReceiptField, equLM.findLCPByCompoundOldName("timeZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(isPostedZReportField, equLM.findLCPByCompoundOldName("isPostedZReport").getMapping(zReportKey)));

            saleProperties.add(new ImportProperty(idReceiptField, equLM.findLCPByCompoundOldName("idReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundOldName("numberReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(dateReceiptField, equLM.findLCPByCompoundOldName("dateReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(timeReceiptField, equLM.findLCPByCompoundOldName("timeReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(discountSumSaleReceiptField, equLM.findLCPByCompoundOldName("discountSumSaleReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(idZReportField, equLM.findLCPByCompoundOldName("zReportReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundOldName("seriesNumberDiscountCard").getMapping(discountCardKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundOldName("discountCardReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptSaleDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ReceiptSaleDetail"), equLM.findLCPByCompoundOldName("receiptDetailId").getMapping(idReceiptDetailField));
            saleProperties.add(new ImportProperty(idReceiptDetailField, equLM.findLCPByCompoundOldName("idReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(numberReceiptDetailField, equLM.findLCPByCompoundOldName("numberReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundOldName("idBarcodeReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(quantityReceiptSaleDetailField, equLM.findLCPByCompoundOldName("quantityReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(priceReceiptSaleDetailField, equLM.findLCPByCompoundOldName("priceReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(sumReceiptSaleDetailField, equLM.findLCPByCompoundOldName("sumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(discountSumReceiptSaleDetailField, equLM.findLCPByCompoundOldName("discountSumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(idReceiptField, equLM.findLCPByCompoundOldName("receiptReceiptDetail").getMapping(receiptSaleDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundOldName("skuReceiptSaleDetail").getMapping(receiptSaleDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Sku")).getMapping(skuKey)));

            returnProperties.add(new ImportProperty(idZReportField, equLM.findLCPByCompoundOldName("idZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(numberZReportField, equLM.findLCPByCompoundOldName("numberZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(numberCashRegisterField, equLM.findLCPByCompoundOldName("cashRegisterZReport").getMapping(zReportKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
            returnProperties.add(new ImportProperty(dateReceiptField, equLM.findLCPByCompoundOldName("dateZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(timeReceiptField, equLM.findLCPByCompoundOldName("timeZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(isPostedZReportField, equLM.findLCPByCompoundOldName("isPostedZReport").getMapping(zReportKey)));

            returnProperties.add(new ImportProperty(idReceiptField, equLM.findLCPByCompoundOldName("idReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundOldName("numberReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(dateReceiptField, equLM.findLCPByCompoundOldName("dateReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(timeReceiptField, equLM.findLCPByCompoundOldName("timeReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(discountSumReturnReceiptField, equLM.findLCPByCompoundOldName("discountSumReturnReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(idZReportField, equLM.findLCPByCompoundOldName("zReportReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundOldName("seriesNumberDiscountCard").getMapping(discountCardKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundOldName("discountCardReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptReturnDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ReceiptReturnDetail"), equLM.findLCPByCompoundOldName("receiptDetailId").getMapping(idReceiptDetailField));
            returnProperties.add(new ImportProperty(idReceiptDetailField, equLM.findLCPByCompoundOldName("idReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(numberReceiptDetailField, equLM.findLCPByCompoundOldName("numberReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundOldName("idBarcodeReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(quantityReceiptReturnDetailField, equLM.findLCPByCompoundOldName("quantityReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(priceReceiptReturnDetailField, equLM.findLCPByCompoundOldName("priceReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailSumReceiptReturnDetailField, equLM.findLCPByCompoundOldName("sumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(discountSumReceiptReturnDetailField, equLM.findLCPByCompoundOldName("discountSumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(idReceiptField, equLM.findLCPByCompoundOldName("receiptReceiptDetail").getMapping(receiptReturnDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundOldName("skuReceiptReturnDetail").getMapping(receiptReturnDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Sku")).getMapping(skuKey)));

            List<List<Object>> dataSale = new ArrayList<List<Object>>();
            List<List<Object>> dataReturn = new ArrayList<List<Object>>();

            List<List<Object>> dataPayment = new ArrayList<List<Object>>();
            
            if (salesInfoList != null)
                for (SalesInfo sale : salesInfoList) {
                    String idReceipt = sale.numberZReport + "_" + sale.numberReceipt + "_" + sale.numberCashRegister;
                    String idZReport = sale.numberZReport + "_" + sale.numberCashRegister;
                    String idReceiptDetail = sale.numberZReport + "_" + sale.numberReceipt + "_" + sale.numberReceiptDetail + "_" + sale.numberCashRegister;
                    if (sale.quantityReceiptDetail.doubleValue() < 0)
                        dataReturn.add(Arrays.<Object>asList(sale.numberCashRegister, idZReport, sale.numberZReport,
                                sale.dateReceipt, sale.timeReceipt, true, idReceipt, sale.numberReceipt,
                                idReceiptDetail, sale.numberReceiptDetail, sale.barcodeItem, sale.quantityReceiptDetail.negate(),
                                sale.priceReceiptDetail, sale.sumReceiptDetail.negate(), sale.discountSumReceiptDetail,
                                sale.discountSumReceipt, sale.seriesNumberDiscountCard));
                    else
                        dataSale.add(Arrays.<Object>asList(sale.numberCashRegister, idZReport, sale.numberZReport,
                                sale.dateReceipt, sale.timeReceipt, true, idReceipt, sale.numberReceipt,
                                idReceiptDetail, sale.numberReceiptDetail, sale.barcodeItem, sale.quantityReceiptDetail,
                                sale.priceReceiptDetail, sale.sumReceiptDetail, sale.discountSumReceiptDetail,
                                sale.discountSumReceipt, sale.seriesNumberDiscountCard));
                    if (sale.sumCash != null && sale.sumCash.doubleValue() != 0) {
                            dataPayment.add(Arrays.<Object>asList(idReceipt + "1", idReceipt, sale.numberCashRegister, "cash", sale.sumCash, 1));
                    }
                    if (sale.sumCard != null && sale.sumCard.doubleValue() != 0) {
                            dataPayment.add(Arrays.<Object>asList(idReceipt + "2", idReceipt, sale.numberCashRegister, "card", sale.sumCard, 2));
                    }
                }

            List<ImportField> saleImportFields = Arrays.asList(numberCashRegisterField, idZReportField,
                    numberZReportField, dateReceiptField, timeReceiptField, isPostedZReportField, idReceiptField,
                    numberReceiptField, idReceiptDetailField, numberReceiptDetailField, idBarcodeReceiptDetailField,
                    quantityReceiptSaleDetailField, priceReceiptSaleDetailField, sumReceiptSaleDetailField,
                    discountSumReceiptSaleDetailField, discountSumSaleReceiptField, seriesNumberDiscountCardField);

            List<ImportField> returnImportFields = Arrays.asList(numberCashRegisterField, idZReportField,
                    numberZReportField, dateReceiptField, timeReceiptField, isPostedZReportField, idReceiptField,
                    numberReceiptField, idReceiptDetailField, numberReceiptDetailField, idBarcodeReceiptDetailField,
                    quantityReceiptReturnDetailField, priceReceiptReturnDetailField, retailSumReceiptReturnDetailField,
                    discountSumReceiptReturnDetailField, discountSumReturnReceiptField, seriesNumberDiscountCardField);


            new IntegrationService(session, new ImportTable(saleImportFields, dataSale), Arrays.asList(zReportKey,
                    cashRegisterKey, receiptKey, receiptSaleDetailKey, skuKey, discountCardKey),
                    saleProperties).synchronize(true);

            new IntegrationService(session, new ImportTable(returnImportFields, dataReturn), Arrays.asList(zReportKey,
                    cashRegisterKey, receiptKey, receiptReturnDetailKey, skuKey, discountCardKey),
                    returnProperties).synchronize(true);

            ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("Payment"), equLM.findLCPByCompoundOldName("paymentId").getMapping(idPaymentField));
            ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("PaymentType"), equLM.findLCPByCompoundOldName("typePaymentSID").getMapping(sidTypePaymentField));
            paymentProperties.add(new ImportProperty(idPaymentField, equLM.findLCPByCompoundOldName("idPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sumPaymentField, equLM.findLCPByCompoundOldName("sumPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(numberPaymentField, equLM.findLCPByCompoundOldName("numberPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sidTypePaymentField, equLM.findLCPByCompoundOldName("paymentTypePayment").getMapping(paymentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("PaymentType")).getMapping(paymentTypeKey)));
            paymentProperties.add(new ImportProperty(idReceiptField, equLM.findLCPByCompoundOldName("receiptPayment").getMapping(paymentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

            List<ImportField> paymentImportFields = Arrays.asList(idPaymentField, idReceiptField, numberCashRegisterField, sidTypePaymentField,
                    sumPaymentField, numberPaymentField);

            if (salesInfoList != null && salesInfoList.size() != 0) {
                String message = "Загружено записей: " + (dataSale.size() + dataReturn.size());
                List<String> cashRegisterNumbers = new ArrayList<String>();
                List<String> fileNames = new ArrayList<String>();
                for (SalesInfo salesInfo : salesInfoList) {
                    if (!cashRegisterNumbers.contains(salesInfo.numberCashRegister.trim()))
                        cashRegisterNumbers.add(salesInfo.numberCashRegister.trim());
                    if ((salesInfo.filename != null) && (!fileNames.contains(salesInfo.filename.trim())))
                        fileNames.add(salesInfo.filename.trim());
                }
                message += "\nИз касс: ";
                for (String cashRegisterNumber : cashRegisterNumbers)
                    message += cashRegisterNumber + ", ";
                message = message.substring(0, message.length() - 2);

                message += "\nИз файлов: ";
                for (String filename : fileNames)
                    message += filename + ", ";
                message = message.substring(0, message.length() - 2);

                DataObject logObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerLog"));
                Object equipmentServerObject = equLM.findLCPByCompoundOldName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
                equLM.findLCPByCompoundOldName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                equLM.findLCPByCompoundOldName("dataEquipmentServerLog").change(message, session, logObject);
                equLM.findLCPByCompoundOldName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, receiptKey, cashRegisterKey),
                    paymentProperties).synchronize(true);

            return session.applyMessage(getBusinessLogics());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            ImportField idTerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("idTerminalDocument"));
            ImportField typeTerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("idTerminalDocumentTypeTerminalDocument"));
            ImportField idTerminalHandbookType1TerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("idTerminalHandbookType1TerminalDocument"));
            ImportField idTerminalHandbookType2TerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("idTerminalHandbookType2TerminalDocument"));
            ImportField titleTerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("titleTerminalDocument"));
            ImportField quantityTerminalDocumentField = new ImportField(equLM.findLCPByCompoundOldName("quantityTerminalDocument"));

            ImportField numberTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("numberTerminalDocumentDetail"));
            ImportField barcodeTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("barcodeTerminalDocumentDetail"));
            ImportField nameTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("nameTerminalDocumentDetail"));
            ImportField quantityTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("quantityTerminalDocumentDetail"));
            ImportField priceTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("priceTerminalDocumentDetail"));
            ImportField sumTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("sumTerminalDocumentDetail"));

            ImportField isNewTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundOldName("isNewTerminalDocumentDetail"));

            List<ImportProperty<?>> terminalDocumentProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> terminalDocumentDetailProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocument"), equLM.findLCPByCompoundOldName("terminalDocumentID").getMapping(idTerminalDocumentField));

            terminalDocumentProperties.add(new ImportProperty(idTerminalDocumentField, equLM.findLCPByCompoundOldName("idTerminalDocument").getMapping(terminalDocumentKey)));
            //terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundOldName("typeTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(titleTerminalDocumentField, equLM.findLCPByCompoundOldName("titleTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType1TerminalDocumentField, equLM.findLCPByCompoundOldName("idTerminalHandbookType1TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType2TerminalDocumentField, equLM.findLCPByCompoundOldName("idTerminalHandbookType2TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(quantityTerminalDocumentField, equLM.findLCPByCompoundOldName("quantityTerminalDocument").getMapping(terminalDocumentKey)));

            ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocumentDetail"), equLM.findLCPByCompoundOldName("terminalDocumentDetailIDDocumentIDDetail").getMapping(idTerminalDocumentField, numberTerminalDocumentDetailField));

            terminalDocumentDetailProperties.add(new ImportProperty(numberTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("numberTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(barcodeTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("barcodeTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(nameTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("nameTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(quantityTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("quantityTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(priceTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("priceTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(sumTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("sumTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(isNewTerminalDocumentDetailField, equLM.findLCPByCompoundOldName("isNewTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(idTerminalDocumentField, equLM.findLCPByCompoundOldName("terminalDocumentTerminalDocumentDetail").getMapping(terminalDocumentDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("TerminalDocument")).getMapping(terminalDocumentKey)));


            ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocumentType"), equLM.findLCPByCompoundOldName("terminalDocumentTypeID").getMapping(typeTerminalDocumentField));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundOldName("idTerminalDocumentType").getMapping(terminalDocumentTypeKey)));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundOldName("terminalDocumentTypeTerminalDocument").getMapping(terminalDocumentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("TerminalDocumentType")).getMapping(terminalDocumentTypeKey)));

            List<List<Object>> dataTerminalDocument = new ArrayList<List<Object>>();
            List<List<Object>> dataTerminalDocumentDetail = new ArrayList<List<Object>>();

            for (TerminalDocumentInfo docInfo : terminalDocumentInfoList) {
                dataTerminalDocument.add(Arrays.<Object>asList(docInfo.idDocument, docInfo.typeDocument, docInfo.handbook1,
                        docInfo.handbook2, docInfo.title, docInfo.quantity));
                Integer number = 1;
                for (TerminalDocumentDetailInfo docDetailInfo : docInfo.terminalDocumentDetailInfoList) {
                    dataTerminalDocumentDetail.add(Arrays.<Object>asList(number, docDetailInfo.barcode, docDetailInfo.name,
                            docDetailInfo.isNew, docDetailInfo.quantity, docDetailInfo.price, docDetailInfo.sum, docInfo.idDocument));
                    number++;
                }
            }

            List<ImportField> terminalDocumentImportFields = Arrays.asList(idTerminalDocumentField, typeTerminalDocumentField,
                    idTerminalHandbookType1TerminalDocumentField, idTerminalHandbookType2TerminalDocumentField,
                    titleTerminalDocumentField, quantityTerminalDocumentField);

            new IntegrationService(session, new ImportTable(terminalDocumentImportFields, dataTerminalDocument),
                    Arrays.asList(terminalDocumentKey, terminalDocumentTypeKey),
                    terminalDocumentProperties).synchronize(true);

            List<ImportField> terminalDocumentDetailImportFields = Arrays.asList(numberTerminalDocumentDetailField,
                    barcodeTerminalDocumentDetailField, nameTerminalDocumentDetailField, isNewTerminalDocumentDetailField,
                    quantityTerminalDocumentDetailField, priceTerminalDocumentDetailField, sumTerminalDocumentDetailField,
                    idTerminalDocumentField);


            new IntegrationService(session, new ImportTable(terminalDocumentDetailImportFields, dataTerminalDocumentDetail),
                    Arrays.asList(terminalDocumentDetailKey, terminalDocumentKey), terminalDocumentDetailProperties).synchronize(true);


            if (terminalDocumentInfoList.size() != 0) {
                String message = "Загружено записей: " + dataTerminalDocument.size();

                DataObject logObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerLog"));
                Object equipmentServerObject = equLM.findLCPByCompoundOldName("sidToEquipmentServer").read(session, new DataObject(equServerID, StringClass.get(20)));
                equLM.findLCPByCompoundOldName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                equLM.findLCPByCompoundOldName("dataEquipmentServerLog").change(message, session, logObject);
                equLM.findLCPByCompoundOldName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            return session.applyMessage(getBusinessLogics());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            equLM.findLCPByCompoundOldName("succeededMachineryPriceTransaction").change(true, session,
                    session.getDataObject(equLM.findClassByCompoundName("MachineryPriceTransaction"), transactionID));
            session.apply(getBusinessLogics());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<byte[][]> readLabelFormats(List<String> scalesModelsList) throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();

            List<byte[][]> fileLabelFormats = new ArrayList<byte[][]>();

            for (String scalesModel : scalesModelsList) {

                DataObject scalesModelObject = new DataObject(equLM.findLCPByCompoundOldName("scalesModelName").read(session, new DataObject(scalesModel)), (ConcreteClass) equLM.findClassByCompoundName("scalesModel"));

                LCP<PropertyInterface> isLabelFormat = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("LabelFormat"));

                ImRevMap<PropertyInterface, KeyExpr> labelFormatKeys = isLabelFormat.getMapKeys();
                KeyExpr labelFormatKey = labelFormatKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> labelFormatQuery = new QueryBuilder<PropertyInterface, Object>(labelFormatKeys);

                labelFormatQuery.addProperty("fileLabelFormat", equLM.findLCPByCompoundOldName("fileLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.addProperty("fileMessageLabelFormat", equLM.findLCPByCompoundOldName("fileMessageLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.and(isLabelFormat.property.getExpr(labelFormatKeys).getWhere());
                labelFormatQuery.and(equLM.findLCPByCompoundOldName("scalesModelLabelFormat").getExpr(labelFormatKey).compare((scalesModelObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> labelFormatResult = labelFormatQuery.execute(session.sql);

                for (ImMap<Object, Object> row : labelFormatResult.valueIt()) {
                    byte[] fileLabelFormat = (byte[]) row.get("fileLabelFormat");
                    byte[] fileMessageLabelFormat = (byte[]) row.get("fileMessageLabelFormat");
                    fileLabelFormats.add(new byte[][]{fileLabelFormat, fileMessageLabelFormat});
                }
            }
            return fileLabelFormats;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void errorTransactionReport(Integer transactionID, Exception e) throws RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("MachineryPriceTransactionError"));
            equLM.findLCPByCompoundOldName("machineryPriceTransactionMachineryPriceTransactionError").change(transactionID, session, errorObject);
            equLM.findLCPByCompoundOldName("dataMachineryPriceTransactionError").change(e.toString(), session, errorObject);
            equLM.findLCPByCompoundOldName("dateMachineryPriceTransactionError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            equLM.findLCPByCompoundOldName("errorTraceMachineryPriceTransactionError").change(os.toString(), session, errorObject);

            session.apply(getBusinessLogics());
        } catch (Exception e2) {
            throw Throwables.propagate(e2);
        }
    }

    @Override
    public void errorEquipmentServerReport(String equipmentServer, Throwable exception) throws
            RemoteException, SQLException {
        try {
            DataSession session = getDbManager().createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerError"));
            Object equipmentServerObject = equLM.findLCPByCompoundOldName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            equLM.findLCPByCompoundOldName("equipmentServerEquipmentServerError").change(equipmentServerObject, session, errorObject);
            equLM.findLCPByCompoundOldName("dataEquipmentServerError").change(exception.toString(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            equLM.findLCPByCompoundOldName("erTraceEquipmentServerError").change(os.toString(), session, errorObject);

            equLM.findLCPByCompoundOldName("dateEquipmentServerError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

            session.apply(getBusinessLogics());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            ThreadLocalContext.set(logicsInstance.getContext());
            DataSession session = getDbManager().createSession();
            Integer equipmentServerID = (Integer) equLM.findLCPByCompoundOldName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            Integer delay = (Integer) equLM.findLCPByCompoundOldName("delayEquipmentServer").read(session, new DataObject(equipmentServerID, (ConcreteClass) equLM.findClassByCompoundName("EquipmentServer")));
            return new EquipmentServerSettings(delay);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String dateTimeCode(Timestamp timeStamp) {
        String result = "";
        long time = timeStamp.getTime() / 1000;
        while (time > 26) {
            result = (char) (time % 26 + 97) + result;
            time = time / 26;
        }
        result = (char) (time + 97) + result;
        return result;
    }

    @Aspect
    private static class RemoteLogicsContextHoldingAspect {
        @Before("execution(* equ.api.EquipmentServerInterface.*(..)) && target(remoteLogics)")
        public void beforeCall(EquipmentServer remoteLogics) {
            ThreadLocalContext.set(remoteLogics.logicsInstance.getContext());
        }
        @AfterReturning("execution(* equ.api.EquipmentServerInterface.*(..)) && target(remoteLogics)")
        public void afterReturning(EquipmentServer remoteLogics) {
            ThreadLocalContext.set(null);
        }
    }
}
