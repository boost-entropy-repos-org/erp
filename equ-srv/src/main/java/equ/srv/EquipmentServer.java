package equ.srv;

import com.google.common.base.Throwables;
import equ.api.*;
import equ.api.cashregister.*;
import equ.api.scales.ScalesInfo;
import equ.api.scales.ScalesItemInfo;
import equ.api.scales.TransactionScalesInfo;
import equ.api.terminal.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.*;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.remote.RmiServer;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.trim;

public class EquipmentServer extends RmiServer implements EquipmentServerInterface, InitializingBean {
    private static final Logger logger = Logger.getLogger(EquipmentServer.class);

    public static final String EXPORT_NAME = "EquipmentServer";

    private LogicsInstance logicsInstance;

    private SoftCheckInterface softCheck;

    private PromotionInterface promotionInterface;
    
    private ScriptingLogicsModule equLM;

    //Опциональные модули
    private ScriptingLogicsModule cashRegisterLM;
    private ScriptingLogicsModule cashRegisterItemLM;
    private ScriptingLogicsModule discountCardLM;
    private ScriptingLogicsModule equipmentLM;
    private ScriptingLogicsModule giftCardLM;
    private ScriptingLogicsModule itemLM;
    private ScriptingLogicsModule itemFashionLM;
    private ScriptingLogicsModule machineryLM;
    private ScriptingLogicsModule machineryPriceTransactionLM;
    private ScriptingLogicsModule machineryPriceTransactionSectionLM;
    private ScriptingLogicsModule machineryPriceTransactionBalanceLM;
    private ScriptingLogicsModule machineryPriceTransactionPromotionLM;
    private ScriptingLogicsModule machineryPriceTransactionStockTaxLM;
    private ScriptingLogicsModule priceCheckerLM;
    private ScriptingLogicsModule scalesLM;
    private ScriptingLogicsModule scalesItemLM;
    private ScriptingLogicsModule terminalLM;
    private ScriptingLogicsModule zReportLM;
    private ScriptingLogicsModule zReportDiscountCardLM;
    private ScriptingLogicsModule zReportSectionLM;
    
    private boolean started = false;

    private int skipTroubleCounter = 1;

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public void setSoftCheckHandler(SoftCheckInterface softCheck) {
        this.softCheck = softCheck;
    }

    public SoftCheckInterface getSoftCheckHandler() {
        return softCheck;
    }

    public void setPromotionHandler(PromotionInterface promotionInterface) {
        this.promotionInterface = promotionInterface;
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
        equLM = getBusinessLogics().getModule("Equipment");
        Assert.notNull(equLM, "can't find Equipment module");
        cashRegisterLM = getBusinessLogics().getModule("EquipmentCashRegister");
        cashRegisterItemLM = getBusinessLogics().getModule("CashRegisterItem");
        discountCardLM = getBusinessLogics().getModule("DiscountCard");
        equipmentLM = getBusinessLogics().getModule("Equipment");
        giftCardLM = getBusinessLogics().getModule("GiftCard");
        itemLM = getBusinessLogics().getModule("Item");
        itemFashionLM = getBusinessLogics().getModule("ItemFashion");
        machineryLM = getBusinessLogics().getModule("Machinery");
        machineryPriceTransactionLM = getBusinessLogics().getModule("MachineryPriceTransaction");
        machineryPriceTransactionSectionLM = getBusinessLogics().getModule("MachineryPriceTransactionSection");
        machineryPriceTransactionBalanceLM = getBusinessLogics().getModule("MachineryPriceTransactionBalance");
        machineryPriceTransactionPromotionLM = getBusinessLogics().getModule("MachineryPriceTransactionPromotion");
        machineryPriceTransactionStockTaxLM = getBusinessLogics().getModule("MachineryPriceTransactionStockTax");
        priceCheckerLM = getBusinessLogics().getModule("EquipmentPriceChecker");
        scalesLM = getBusinessLogics().getModule("EquipmentScales");
        scalesItemLM = getBusinessLogics().getModule("ScalesItem");
        terminalLM = getBusinessLogics().getModule("EquipmentTerminal");
        zReportLM = getBusinessLogics().getModule("ZReport");
        zReportDiscountCardLM = getBusinessLogics().getModule("ZReportDiscountCard");
        zReportSectionLM = getBusinessLogics().getModule("ZReportSection");
        DeleteBarcodeEquipmentServer.init(getBusinessLogics());
        MachineryExchangeEquipmentServer.init(getBusinessLogics());
        SendSalesEquipmentServer.init(getBusinessLogics());
        StopListEquipmentServer.init(getBusinessLogics());
        TerminalDocumentEquipmentServer.init(getBusinessLogics());
        TerminalEquipmentServer.init(getBusinessLogics());
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        if(getDbManager().isServer()) {
            logger.info("Binding Equipment Server.");
            try {
                getRmiManager().bindAndExport(EXPORT_NAME, this);
                started = true;
            } catch (Exception e) {
                throw new RuntimeException("Error exporting Equipment Server: ", e);
            }
        } else {
            logger.info("Equipment Server disabled, change serverComputer() to enable");
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

    public EquipmentServer() {
        super(HIGH_DAEMON_ORDER);
    }

    @Override
    public boolean enabledSoftCheckInfo() throws RemoteException, SQLException {
        return softCheck != null;
    }

    @Override
    public List<SoftCheckInfo> readSoftCheckInfo() throws RemoteException, SQLException {
        return softCheck == null ? null : softCheck.readSoftCheckInfo();
    }

    @Override
    public void finishSoftCheckInfo(Map<String, SoftCheckInvoice> invoiceMap) throws RemoteException, SQLException {
        if(softCheck != null)
            softCheck.finishSoftCheckInfo(invoiceMap);
    }

    @Override
    public String sendSucceededSoftCheckInfo(String sidEquipmentServer, Map<String, Timestamp> invoiceSet) throws RemoteException, SQLException {
        return softCheck == null ? null : softCheck.sendSucceededSoftCheckInfo(sidEquipmentServer, invoiceSet);
    }

    @Override
    public String sendCashierTimeList(List<CashierTime> cashierTimeList) throws RemoteException, SQLException {
        return softCheck == null ? null : softCheck.sendCashierTimeList(cashierTimeList);
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String sidEquipmentServer) throws RemoteException, SQLException {
        try (DataSession session = getDbManager().createSession()) {
            List<TransactionInfo> transactionList = new ArrayList<>();

            ObjectValue equipmentServerObject = equipmentLM.findProperty("sidTo[VARSTRING[20]]").readClasses(session, new DataObject(sidEquipmentServer));
            Integer minutesTroubleMachineryGroups = (Integer) equipmentLM.findProperty("minutesTroubleMachineryGroup[EquipmentServer]").read(session, equipmentServerObject);
            Integer skipTroubles = (Integer) equipmentLM.findProperty("skipTroublesDelay[EquipmentServer]").read(session, equipmentServerObject);
            Integer selectTop = (Integer) equipmentLM.findProperty("selectTop[EquipmentServer]").read(session, equipmentServerObject);
            if(selectTop == null)
                selectTop = 0;

            KeyExpr machineryPriceTransactionExpr = new KeyExpr("machineryPriceTransaction");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "machineryPriceTransaction", machineryPriceTransactionExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);

            String[] mptNames = new String[]{"dateTimeMachineryPriceTransaction", "groupMachineryMachineryPriceTransaction",
                    "nppGroupMachineryMachineryPriceTransaction", "nameGroupMachineryMachineryPriceTransaction", "snapshotMachineryPriceTransaction",
                    "descriptionMachineryPriceTransaction", "lastDateMachineryPriceTransactionErrorMachineryPriceTransaction"};
            LCP[] mptProperties = equLM.findProperties("dateTime[MachineryPriceTransaction]", "groupMachinery[MachineryPriceTransaction]",
                    "nppGroupMachinery[MachineryPriceTransaction]", "nameGroupMachinery[MachineryPriceTransaction]", "snapshot[MachineryPriceTransaction]",
                    "description[MachineryPriceTransaction]", "lastDateMachineryPriceTransactionError[MachineryPriceTransaction]");
            for (int i = 0; i < mptProperties.length; i++) {
                query.addProperty(mptNames[i], mptProperties[i].getExpr(machineryPriceTransactionExpr));
            }
            query.and(equLM.findProperty("sidEquipmentServer[MachineryPriceTransaction]").getExpr(machineryPriceTransactionExpr).compare(new DataObject(sidEquipmentServer), Compare.EQUALS));
            query.and(equLM.findProperty("process[MachineryPriceTransaction]").getExpr(machineryPriceTransactionExpr).getWhere());

            logger.info(String.format("Starting to read top %s transactions", selectTop));
            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session, selectTop);
            List<Object[]> transactionObjects = new ArrayList<>();
            for (int i = 0, size = result.size(); i < size; i++) {
                ImMap<Object, ObjectValue> value = result.getValue(i);
                ObjectValue dateTimeMPT = value.get("dateTimeMachineryPriceTransaction");
                if(dateTimeMPT instanceof DataObject) {
                    DataObject groupMachineryMPT = (DataObject) value.get("groupMachineryMachineryPriceTransaction");
                    Integer nppGroupMachineryMPT = (Integer) value.get("nppGroupMachineryMachineryPriceTransaction").getValue();
                    String nameGroupMachineryMPT = (String) value.get("nameGroupMachineryMachineryPriceTransaction").getValue();
                    DataObject transactionObject = result.getKey(i).singleValue();
                    boolean snapshotMPT = value.get("snapshotMachineryPriceTransaction") instanceof DataObject;
                    String descriptionMPT = (String) value.get("descriptionMachineryPriceTransaction").getValue();
                    Timestamp lastErrorDate = (Timestamp) value.get("lastDateMachineryPriceTransactionErrorMachineryPriceTransaction").getValue();
                    transactionObjects.add(new Object[]{groupMachineryMPT, nppGroupMachineryMPT, nameGroupMachineryMPT, transactionObject,
                            dateTimeCode((Timestamp) dateTimeMPT.getValue()), dateTimeMPT, snapshotMPT, descriptionMPT, lastErrorDate});
                }
            }

            Map<String, List<ItemGroup>> itemGroupMap = transactionObjects.isEmpty() ? null : readItemGroupMap(session);
            List<Integer> troubleMachineryGroups = readTroubleMachineryGroups(session, minutesTroubleMachineryGroups);
            boolean skipTroubleMachineryGroups = skipTroubles != null && skipTroubleCounter < skipTroubles;
            if (skipTroubleMachineryGroups)
                skipTroubleCounter++;
            else
                skipTroubleCounter = 1;
            
            logger.info("" + transactionObjects.size() + " transactions read");
            int count = 0;
            for (Object[] transaction : transactionObjects) {

                count++;
                logger.info("Reading transaction number " + count + " of " + transactionObjects.size());
                DataObject groupMachineryObject = (DataObject) transaction[0];
                Integer nppGroupMachinery = (Integer) transaction[1];
                if(troubleMachineryGroups.contains(nppGroupMachinery) && skipTroubleMachineryGroups)
                    continue;

                String nameGroupMachinery = (String) transaction[2];
                DataObject transactionObject = (DataObject) transaction[3];
                String dateTimeCode = (String) transaction[4];
                Date date = new Date(((Timestamp) ((DataObject) transaction[5]).getValue()).getTime());
                boolean snapshotTransaction = (boolean) transaction[6];
                String descriptionTransaction = (String) transaction[7];
                Timestamp lastErrorDateTransaction = (Timestamp) transaction[8];

                boolean isCashRegisterPriceTransaction = cashRegisterLM != null && transactionObject.objectClass.equals(cashRegisterLM.findClass("CashRegisterPriceTransaction"));
                boolean isScalesPriceTransaction = scalesLM != null && transactionObject.objectClass.equals(scalesLM.findClass("ScalesPriceTransaction"));
                boolean isPriceCheckerPriceTransaction = priceCheckerLM != null && transactionObject.objectClass.equals(priceCheckerLM.findClass("PriceCheckerPriceTransaction"));
                boolean isTerminalPriceTransaction = terminalLM != null && transactionObject.objectClass.equals(terminalLM.findClass("TerminalPriceTransaction"));
                
                String handlerModelGroupMachinery = (String) equLM.findProperty("handlerModel[GroupMachinery]").read(session, groupMachineryObject);
                String nameModelGroupMachinery = (String) equLM.findProperty("nameModel[GroupMachinery]").read(session, groupMachineryObject);

                ValueExpr transactionExpr = transactionObject.getExpr();
                KeyExpr barcodeExpr = new KeyExpr("barcode");
                ImRevMap<Object, KeyExpr> skuKeys = MapFact.singletonRev((Object) "barcode", barcodeExpr);

                QueryBuilder<Object, Object> skuQuery = new QueryBuilder<>(skuKeys);

                String[] skuNames = new String[]{"nameMachineryPriceTransactionBarcode", "priceMachineryPriceTransactionBarcode",
                        "expiryDateMachineryPriceTransactionBarcode", "splitMachineryPriceTransactionBarcode", "passScalesMachineryPriceTransactionBarcode",
                        "idUOMMachineryPriceTransactionBarcode", "shortNameUOMMachineryPriceTransactionBarcode", "pluNumberMachineryPriceTransactionBarcode",
                        "flagsMachineryPriceTransactionBarcode", "expiryDaysMachineryPriceTransactionBarcode", "minPriceMachineryPriceTransactionBarcode",
                        "canonicalNameSkuGroupMachineryPriceTransactionBarcode"};
                LCP[] skuProperties = equLM.findProperties("name[MachineryPriceTransaction,Barcode]", "price[MachineryPriceTransaction,Barcode]",
                        "expiryDate[MachineryPriceTransaction,Barcode]", "split[MachineryPriceTransaction,Barcode]", "passScales[MachineryPriceTransaction,Barcode]",
                        "idUOM[MachineryPriceTransaction,Barcode]", "shortNameUOM[MachineryPriceTransaction,Barcode]", "pluNumber[MachineryPriceTransaction,Barcode]",
                        "flags[MachineryPriceTransaction,Barcode]", "expiryDays[MachineryPriceTransaction,Barcode]", "minPrice[MachineryPriceTransaction,Barcode]",
                        "canonicalNameSkuGroup[MachineryPriceTransaction,Barcode]");
                for (int i = 0; i < skuProperties.length; i++) {
                    skuQuery.addProperty(skuNames[i], skuProperties[i].getExpr(transactionExpr, barcodeExpr));
                }

                String[] barcodeNames = new String[]{"idBarcode", "skuBarcode", "idSkuBarcode", "skuGroupBarcode"};
                LCP[] barcodeProperties = equLM.findProperties("id[Barcode]", "sku[Barcode]", "idSku[Barcode]", "skuGroup[Barcode]");
                for (int i = 0; i < barcodeProperties.length; i++) {
                    skuQuery.addProperty(barcodeNames[i], barcodeProperties[i].getExpr(barcodeExpr));
                }

                if (isCashRegisterPriceTransaction) {
                    skuQuery.addProperty("amountBarcode", equLM.findProperty("amount[Barcode]").getExpr(barcodeExpr));
                }
                
                if(itemLM != null) {
                    skuQuery.addProperty("idBrandBarcode", itemLM.findProperty("idBrand[Barcode]").getExpr(barcodeExpr));
                    skuQuery.addProperty("nameBrandBarcode", itemLM.findProperty("nameBrand[Barcode]").getExpr(barcodeExpr));
                }
                
                if(itemFashionLM != null) {
                    skuQuery.addProperty("idSeasonBarcode", itemFashionLM.findProperty("idSeason[Barcode]").getExpr(barcodeExpr));
                    skuQuery.addProperty("nameSeasonBarcode", itemFashionLM.findProperty("nameSeason[Barcode]").getExpr(barcodeExpr));
                }

                if(cashRegisterItemLM != null) {
                    skuQuery.addProperty("CashRegisterItem.idSkuGroupMachineryPriceTransactionBarcode", 
                            cashRegisterItemLM.findProperty("idSkuGroup[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                    skuQuery.addProperty("CashRegisterItem.overIdSkuGroupMachineryPriceTransactionBarcode",
                            cashRegisterItemLM.findProperty("overIdSkuGroup[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                }
                
                if(scalesItemLM != null) {
                    skuQuery.addProperty("ScalesItem.idSkuGroupMachineryPriceTransactionBarcode", 
                            scalesItemLM.findProperty("idSkuGroup[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                }
                
                if (scalesItemLM != null) {
                    String[] scalesSkuNames = new String[]{"hoursExpiryMachineryPriceTransactionBarcode",
                            "labelFormatMachineryPriceTransactionBarcode", "descriptionMachineryPriceTransactionBarcode", "descriptionNumberMachineryPriceTransactionBarcode"};
                    LCP[] scalesSkuProperties = scalesItemLM.findProperties("hoursExpiry[MachineryPriceTransaction,Barcode]",
                            "labelFormat[MachineryPriceTransaction,Barcode]", "description[MachineryPriceTransaction,Barcode]", "descriptionNumber[MachineryPriceTransaction,Barcode]");
                    for (int i = 0; i < scalesSkuProperties.length; i++) {
                        skuQuery.addProperty(scalesSkuNames[i], scalesSkuProperties[i].getExpr(transactionExpr, barcodeExpr));
                    }
                    skuQuery.addProperty("extraPercent", scalesItemLM.findProperty("extraPercent[MachineryPriceTransaction, Barcode]").getExpr(transactionExpr, barcodeExpr));
                }
                
                if (machineryPriceTransactionStockTaxLM != null) {
                    String[] taxNames = new String[]{"VATMachineryPriceTransactionBarcode"};
                    LCP[] taxProperties = machineryPriceTransactionStockTaxLM.findProperties("VAT[MachineryPriceTransaction,Barcode]");
                    for (int i = 0; i < taxProperties.length; i++) {
                        skuQuery.addProperty(taxNames[i], taxProperties[i].getExpr(transactionExpr, barcodeExpr));
                    }
                }

                if(machineryPriceTransactionSectionLM != null) {
                    skuQuery.addProperty("sectionMachineryPriceTransactionBarcode",
                            machineryPriceTransactionSectionLM.findProperty("section[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                    skuQuery.addProperty("deleteSectionBarcode",
                            machineryPriceTransactionSectionLM.findProperty("deleteSectionBarcode[?]").getExpr(barcodeExpr));
                }

                if(machineryPriceTransactionBalanceLM != null) {
                    skuQuery.addProperty("balanceMachineryPriceTransactionBarcode",
                            machineryPriceTransactionBalanceLM.findProperty("balance[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                }

                if(machineryPriceTransactionPromotionLM != null) {
                    skuQuery.addProperty("restrictionToDateTimeMachineryPriceTransactionBarcode",
                            machineryPriceTransactionPromotionLM.findProperty("restrictionToDateTime[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr));
                }

                skuQuery.and(equLM.findProperty("in[MachineryPriceTransaction,Barcode]").getExpr(transactionExpr, barcodeExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> skuResult = skuQuery.execute(session);

                String[] machineryNames = new String[]{"nppMachinery", "portMachinery", "overDirectoryMachinery"};
                LCP[] machineryProperties = machineryLM.findProperties("npp[Machinery]", "port[Machinery]", "overDirectory[Machinery]");
                
                if (isCashRegisterPriceTransaction) {
                    
                    java.sql.Date startDateGroupCashRegister = (java.sql.Date) cashRegisterLM.findProperty("startDate[GroupCashRegister]").read(session, groupMachineryObject);
                    boolean notDetailedGroupCashRegister = cashRegisterLM.findProperty("notDetailed[GroupCashRegister]").read(session, groupMachineryObject) != null;
                    Integer overDepartmentNumberGroupCashRegister = (Integer) cashRegisterLM.findProperty("overDepartmentNumberCashRegister[GroupMachinery]").read(session, groupMachineryObject);
                    String idDepartmentStoreGroupCashRegister = (String) cashRegisterLM.findProperty("idDepartmentStore[GroupCashRegister]").read(session, groupMachineryObject);
                    String pieceCodeGroupCashRegister = (String) cashRegisterLM.findProperty("pieceCode[GroupCashRegister]").read(session, groupMachineryObject);
                    String weightCodeGroupCashRegister = (String) cashRegisterLM.findProperty("weightCode[GroupCashRegister]").read(session, groupMachineryObject);
                    String nameStockGroupCashRegister = (String) cashRegisterLM.findProperty("nameStock[GroupMachinery]").read(session, groupMachineryObject);
                    String sectionGroupCashRegister = (String) cashRegisterLM.findProperty("section[GroupMachinery]").read(session, groupMachineryObject);

                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<>();
                    KeyExpr cashRegisterExpr = new KeyExpr("cashRegister");
                    ImRevMap<Object, KeyExpr> cashRegisterKeys = MapFact.singletonRev((Object) "cashRegister", cashRegisterExpr);
                    QueryBuilder<Object, Object> cashRegisterQuery = new QueryBuilder<>(cashRegisterKeys);
                    
                    for (int i = 0; i < machineryProperties.length; i++) {
                        cashRegisterQuery.addProperty(machineryNames[i], machineryProperties[i].getExpr(cashRegisterExpr));
                    }
                    cashRegisterQuery.addProperty("disableSalesCashRegister", cashRegisterLM.findProperty("disableSales[CashRegister]").getExpr(cashRegisterExpr));
                    cashRegisterQuery.addProperty("succeededMachineryMachineryPriceTransaction",
                            cashRegisterLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").getExpr(cashRegisterExpr, transactionExpr));
                    cashRegisterQuery.addProperty("clearedMachineryMachineryPriceTransaction",
                            cashRegisterLM.findProperty("cleared[Machinery,MachineryPriceTransaction]").getExpr(cashRegisterExpr, transactionExpr));
                    cashRegisterQuery.addProperty("inMachineryPriceTransactionMachinery",
                            equLM.findProperty("in[MachineryPriceTransaction,Machinery]").getExpr(transactionExpr, cashRegisterExpr));
                    cashRegisterQuery.and(cashRegisterLM.findProperty("groupCashRegister[CashRegister]").getExpr(cashRegisterExpr).compare(groupMachineryObject, Compare.EQUALS));
                    cashRegisterQuery.and(cashRegisterLM.findProperty("active[CashRegister]").getExpr(cashRegisterExpr).getWhere());

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session);

                    for (ImMap<Object, Object> row : cashRegisterResult.valueIt()) {
                        Integer nppMachinery = (Integer) row.get("nppMachinery");
                        String portMachinery = (String) row.get("portMachinery");
                        String directoryCashRegister = (String) row.get("overDirectoryMachinery");
                        boolean succeeded = row.get("succeededMachineryMachineryPriceTransaction") != null;
                        boolean cleared = row.get("clearedMachineryPriceTransaction") != null;
                        Boolean disableSalesCashRegister = row.get("disableSalesCashRegister") != null;
                        boolean enabled = row.get("inMachineryPriceTransactionMachinery") != null;
                        cashRegisterInfoList.add(new CashRegisterInfo(enabled, cleared, succeeded, nppGroupMachinery, nppMachinery,
                                nameModelGroupMachinery, handlerModelGroupMachinery, portMachinery, directoryCashRegister,
                                startDateGroupCashRegister, overDepartmentNumberGroupCashRegister, idDepartmentStoreGroupCashRegister, notDetailedGroupCashRegister,
                                disableSalesCashRegister, pieceCodeGroupCashRegister, weightCodeGroupCashRegister, sectionGroupCashRegister, null));
                    }

                    List<CashRegisterItemInfo> cashRegisterItemInfoList = new ArrayList<>();

                    for (ImMap<Object, Object> row : skuResult.valueIt()) {
                        String barcode = getRowValue(row, "idBarcode");
                        BigDecimal amountBarcode = (BigDecimal) row.get("amountBarcode");
                        String name = getRowValue(row, "nameMachineryPriceTransactionBarcode");
                        BigDecimal price = (BigDecimal) row.get("priceMachineryPriceTransactionBarcode");
                        boolean split = row.get("splitMachineryPriceTransactionBarcode") != null;
                        Integer daysExpiry = (Integer) row.get("expiryDaysMachineryPriceTransactionBarcode");
                        Date expiryDate = (Date) row.get("expiryDateMachineryPriceTransactionBarcode");
                        Integer flags = (Integer) row.get("flagsMachineryPriceTransactionBarcode");
                        boolean passScales = row.get("passScalesMachineryPriceTransactionBarcode") != null;
                        String idUOM = (String) row.get("idUOMMachineryPriceTransactionBarcode");
                        String shortNameUOM = (String) row.get("shortNameUOMMachineryPriceTransactionBarcode");
                        String idBrand = itemLM == null ? null : (String) row.get("idBrandBarcode");
                        String nameBrand = itemLM == null ? null : (String) row.get("nameBrandBarcode");
                        String idSeason = itemFashionLM == null ? null : (String) row.get("idSeasonBarcode");
                        String nameSeason = itemFashionLM == null ? null : (String) row.get("nameSeasonBarcode");
                        BigDecimal valueVAT = machineryPriceTransactionStockTaxLM == null ? null : (BigDecimal) row.get("VATMachineryPriceTransactionBarcode");
                        String idItem = (String) row.get("idSkuBarcode");
                        Integer itemGroupObject = (Integer) row.get("skuGroupBarcode");
                        Integer pluNumber = (Integer) row.get("pluNumberMachineryPriceTransactionBarcode");
                        String description = scalesItemLM == null ? null : (String) row.get("descriptionMachineryPriceTransactionBarcode");

                        String idItemGroup = cashRegisterItemLM == null ? null : (String) row.get("CashRegisterItem.idSkuGroupMachineryPriceTransactionBarcode");
                        String overIdItemGroup = cashRegisterItemLM == null ? null : (String) row.get("CashRegisterItem.overIdSkuGroupMachineryPriceTransactionBarcode");
                        String canonicalNameSkuGroup = (String) row.get("canonicalNameSkuGroupMachineryPriceTransactionBarcode");
                        String section = machineryPriceTransactionSectionLM == null ? null : (String) row.get("sectionMachineryPriceTransactionBarcode");
                        String deleteSection = machineryPriceTransactionSectionLM == null ? null : (String) row.get("deleteSectionBarcode");
                        BigDecimal balance = machineryPriceTransactionBalanceLM == null ? null : (BigDecimal) row.get("balanceMachineryPriceTransactionBarcode");
                        BigDecimal minPrice = (BigDecimal) row.get("minPriceMachineryPriceTransactionBarcode");
                        Timestamp restrictionToDateTime = (Timestamp) row.get("restrictionToDateTimeMachineryPriceTransactionBarcode");

                        CashRegisterItemInfo c = new CashRegisterItemInfo(idItem, barcode, name, price, split, daysExpiry, expiryDate, passScales, valueVAT,
                                pluNumber, flags, idItemGroup, canonicalNameSkuGroup, idUOM, shortNameUOM, itemGroupObject, description, idBrand, nameBrand, idSeason,
                                nameSeason, idDepartmentStoreGroupCashRegister, section, deleteSection, minPrice, overIdItemGroup, amountBarcode, balance, restrictionToDateTime);
                        cashRegisterItemInfoList.add(c);
                    }
                    
                    transactionList.add(new TransactionCashRegisterInfo((Integer) transactionObject.getValue(), dateTimeCode, 
                            date, handlerModelGroupMachinery, (Integer) groupMachineryObject.object, nppGroupMachinery,
                            nameGroupMachinery, descriptionTransaction, itemGroupMap, cashRegisterItemInfoList,
                            cashRegisterInfoList, snapshotTransaction, lastErrorDateTransaction, overDepartmentNumberGroupCashRegister, weightCodeGroupCashRegister, nameStockGroupCashRegister));

                } else if (isScalesPriceTransaction) {
                    List<ScalesInfo> scalesInfoList = new ArrayList<>();
                    String directory = (String) scalesLM.findProperty("directory[GroupScales]").read(session, groupMachineryObject);
                    String pieceCodeGroupScales = (String) scalesLM.findProperty("pieceCode[GroupScales]").read(session, groupMachineryObject);
                    String weightCodeGroupScales = (String) scalesLM.findProperty("weightCode[GroupScales]").read(session, groupMachineryObject);

                    KeyExpr scalesExpr = new KeyExpr("scales");
                    ImRevMap<Object, KeyExpr> scalesKeys = MapFact.singletonRev((Object) "scales", scalesExpr);
                    QueryBuilder<Object, Object> scalesQuery = new QueryBuilder<>(scalesKeys);
                    
                    for (int i = 0; i < machineryProperties.length; i++) {
                        scalesQuery.addProperty(machineryNames[i], machineryProperties[i].getExpr(scalesExpr));
                    }
                    scalesQuery.addProperty("inMachineryPriceTransactionMachinery", 
                            scalesLM.findProperty("in[MachineryPriceTransaction,Machinery]").getExpr(transactionExpr, scalesExpr));
                    scalesQuery.addProperty("succeededMachineryMachineryPriceTransaction",
                                scalesLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").getExpr(scalesExpr, transactionExpr));
                    scalesQuery.addProperty("clearedMachineryMachineryPriceTransaction",
                            scalesLM.findProperty("cleared[Machinery,MachineryPriceTransaction]").getExpr(scalesExpr, transactionExpr));
                    scalesQuery.addProperty("activeScales", scalesLM.findProperty("active[Scales]").getExpr(scalesExpr));
                    scalesQuery.and(scalesLM.findProperty("groupScales[Scales]").getExpr(scalesExpr).compare(groupMachineryObject, Compare.EQUALS));
                    //scalesQuery.and(scalesLM.findProperty("active[Scales]").getExpr(scalesExpr).getWhere());

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> scalesResult = scalesQuery.execute(session);

                    int enabledCount = 0;
                    int enabledInactiveCount = 0;
                    for (ImMap<Object, Object> values : scalesResult.valueIt()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        boolean enabled = values.get("inMachineryPriceTransactionMachinery") != null;
                        boolean succeeded = values.get("succeededMachineryMachineryPriceTransaction") != null;
                        boolean cleared = values.get("clearedMachineryMachineryPriceTransaction") != null;
                        boolean active = values.get("activeScales") != null;
                        if(enabled) {
                            enabledCount++;
                            if(!active)
                                enabledInactiveCount++;
                        }
                        if(active)
                            scalesInfoList.add(new ScalesInfo(enabled, cleared, succeeded, nppGroupMachinery, nppMachinery,
                                nameModelGroupMachinery, handlerModelGroupMachinery, portMachinery, directory,
                                pieceCodeGroupScales, weightCodeGroupScales));
                    }
                    //если все отмеченные - неактивные, то не посылаем весы вообще
                    if(enabledCount > 0 && enabledCount==enabledInactiveCount)
                        scalesInfoList = new ArrayList<>();

                    List<ScalesItemInfo> scalesItemInfoList = new ArrayList<>();
                    
                    for (ImMap<Object, Object> row : skuResult.valueIt()) {
                        String idItem = getRowValue(row, "idSkuBarcode");
                        String barcode = getRowValue(row, "idBarcode");
                        String name = getRowValue(row, "nameMachineryPriceTransactionBarcode");
                        BigDecimal price = (BigDecimal) row.get("priceMachineryPriceTransactionBarcode");
                        Integer pluNumber = (Integer) row.get("pluNumberMachineryPriceTransactionBarcode");
                        Integer flags = (Integer) row.get("flagsMachineryPriceTransactionBarcode");
                        Date expiryDate = (Date) row.get("expiryDateMachineryPriceTransactionBarcode");
                        boolean split = row.get("splitMachineryPriceTransactionBarcode") != null;
                        Integer daysExpiry = (Integer) row.get("expiryDaysMachineryPriceTransactionBarcode");
                        Integer hoursExpiry = (Integer) row.get("hoursExpiryMachineryPriceTransactionBarcode");
                        Integer labelFormat = (Integer) row.get("labelFormatMachineryPriceTransactionBarcode");
                        String description = (String) row.get("descriptionMachineryPriceTransactionBarcode");
                        Integer descriptionNumberCellScales = (Integer) row.get("descriptionNumberMachineryPriceTransactionBarcode");
                        boolean passScales = row.get("passScalesMachineryPriceTransactionBarcode") != null;
                        BigDecimal valueVAT = machineryPriceTransactionStockTaxLM == null ? null : (BigDecimal) row.get("VATMachineryPriceTransactionBarcode");
                        String idUOM = (String) row.get("idUOMMachineryPriceTransactionBarcode");
                        String shortNameUOM = (String) row.get("shortNameUOMMachineryPriceTransactionBarcode");
                        
                        String idItemGroup = scalesItemLM == null ? null : (String) row.get("ScalesItem.idSkuGroupMachineryPriceTransactionBarcode");
                        String canonicalNameSkuGroup = (String) row.get("canonicalNameSkuGroupMachineryPriceTransactionBarcode");
                        BigDecimal extraPercent = scalesItemLM == null ? null : (BigDecimal) row.get("extraPercent");

                        scalesItemInfoList.add(new ScalesItemInfo(idItem, barcode, name, price, split, daysExpiry, expiryDate, 
                                passScales, valueVAT, pluNumber, flags, idItemGroup, canonicalNameSkuGroup, hoursExpiry,
                                labelFormat, description, descriptionNumberCellScales, idUOM, shortNameUOM, extraPercent));
                    }

                    transactionList.add(new TransactionScalesInfo((Integer) transactionObject.getValue(), dateTimeCode, 
                            date, handlerModelGroupMachinery, (Integer) groupMachineryObject.object, nppGroupMachinery,
                            nameGroupMachinery, descriptionTransaction, scalesItemInfoList, scalesInfoList, snapshotTransaction,
                            lastErrorDateTransaction));

                } else if (isPriceCheckerPriceTransaction) {
                    List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<>();
                    KeyExpr priceCheckerExpr = new KeyExpr("priceChecker");
                    ImRevMap<Object, KeyExpr> priceCheckerKeys = MapFact.singletonRev((Object) "priceChecker", priceCheckerExpr);
                    QueryBuilder<Object, Object> priceCheckerQuery = new QueryBuilder<>(priceCheckerKeys);
                    
                    for (int i = 0; i < machineryProperties.length; i++) {
                        priceCheckerQuery.addProperty(machineryNames[i], machineryProperties[i].getExpr(priceCheckerExpr));
                    }
                    priceCheckerQuery.addProperty("inMachineryPriceTransactionMachinery",
                            priceCheckerLM.findProperty("in[MachineryPriceTransaction,Machinery]").getExpr(transactionExpr, priceCheckerExpr));
                    priceCheckerQuery.addProperty("succeededMachineryMachineryPriceTransaction",
                            priceCheckerLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").getExpr(priceCheckerExpr, transactionExpr));
                    priceCheckerQuery.addProperty("clearedMachineryMachineryPriceTransaction",
                            priceCheckerLM.findProperty("cleared[Machinery,MachineryPriceTransaction]").getExpr(priceCheckerExpr, transactionExpr));
                    priceCheckerQuery.and(priceCheckerLM.findProperty("groupPriceChecker[PriceChecker]").getExpr(priceCheckerExpr).compare(groupMachineryObject, Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> priceCheckerResult = priceCheckerQuery.execute(session);

                    for (ImMap<Object, Object> row : priceCheckerResult.valueIt()) {
                        boolean enabled = row.get("inMachineryPriceTransactionMachinery") != null;
                        Boolean succeeded = row.get("succeededMachineryMachineryPriceTransaction") != null;
                        Boolean cleared = row.get("clearedMachineryMachineryPriceTransaction") != null;
                        priceCheckerInfoList.add(new PriceCheckerInfo(enabled, cleared, succeeded, nppGroupMachinery, (Integer) row.get("nppMachinery"),
                                nameModelGroupMachinery, handlerModelGroupMachinery, (String) row.get("portMachinery")));
                    }

                    List<PriceCheckerItemInfo> priceCheckerItemInfoList = new ArrayList<>();
                    for (ImMap<Object, Object> row : skuResult.valueIt()) {
                        String idItem = getRowValue(row, "idSkuBarcode");
                        String barcode = getRowValue(row, "idBarcode");
                        String name = getRowValue(row, "nameMachineryPriceTransactionBarcode");
                        BigDecimal price = (BigDecimal) row.get("priceMachineryPriceTransactionBarcode");
                        boolean split = row.get("splitMachineryPriceTransactionBarcode") != null;
                        Integer daysExpiry = (Integer) row.get("expiryDaysMachineryPriceTransactionBarcode");
                        Date expiryDate = (Date) row.get("expiryDateMachineryPriceTransactionBarcode");
                        boolean passScales = row.get("passScalesMachineryPriceTransactionBarcode") != null;
                        BigDecimal valueVAT = machineryPriceTransactionStockTaxLM == null ? null : (BigDecimal) row.get("VATMachineryPriceTransactionBarcode");
                        Integer pluNumber = (Integer) row.get("pluNumberMachineryPriceTransactionBarcode");
                        Integer flags = (Integer) row.get("flagsMachineryPriceTransactionBarcode");
                        
                        priceCheckerItemInfoList.add(new PriceCheckerItemInfo(idItem, barcode, name, price, split, 
                                daysExpiry, expiryDate, passScales, valueVAT, pluNumber, flags, null, null));
                    }
                    
                    transactionList.add(new TransactionPriceCheckerInfo((Integer) transactionObject.getValue(), dateTimeCode,
                            date, handlerModelGroupMachinery, (Integer) groupMachineryObject.object, nppGroupMachinery,
                            nameGroupMachinery, descriptionTransaction, priceCheckerItemInfoList, priceCheckerInfoList,
                            snapshotTransaction, lastErrorDateTransaction));


                } else if (isTerminalPriceTransaction) {
                    List<TerminalInfo> terminalInfoList = new ArrayList<>();
                    
                    Integer nppGroupTerminal = (Integer) terminalLM.findProperty("npp[GroupMachinery]").read(session, groupMachineryObject);
                    String directoryGroupTerminal = (String) terminalLM.findProperty("directory[GroupTerminal]").read(session, groupMachineryObject);
                    ObjectValue priceListTypeGroupMachinery = terminalLM.findProperty("priceListType[GroupMachinery]").readClasses(session, groupMachineryObject);
                    ObjectValue stockGroupTerminal = terminalLM.findProperty("stock[GroupTerminal]").readClasses(session, groupMachineryObject);
                    String idPriceListType = (String) terminalLM.findProperty("id[PriceListType]").read(session, priceListTypeGroupMachinery);

                    KeyExpr terminalExpr = new KeyExpr("terminal");
                    ImRevMap<Object, KeyExpr> terminalKeys = MapFact.singletonRev((Object) "terminal", terminalExpr);
                    QueryBuilder<Object, Object> terminalQuery = new QueryBuilder<>(terminalKeys);
                    
                    for (int i = 0; i < machineryProperties.length; i++) {
                        terminalQuery.addProperty(machineryNames[i], machineryProperties[i].getExpr(terminalExpr));
                    }
                    terminalQuery.addProperty("inMachineryPriceTransactionMachinery",
                            terminalLM.findProperty("in[MachineryPriceTransaction,Machinery]").getExpr(transactionExpr, terminalExpr));
                    terminalQuery.addProperty("succeededMachineryMachineryPriceTransaction",
                            terminalLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").getExpr(terminalExpr, transactionExpr));
                    terminalQuery.addProperty("clearedMachineryMachineryPriceTransaction",
                            terminalLM.findProperty("cleared[Machinery,MachineryPriceTransaction]").getExpr(terminalExpr, transactionExpr));
                    terminalQuery.and(terminalLM.findProperty("groupTerminal[Terminal]").getExpr(terminalExpr).compare(groupMachineryObject, Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> terminalResult = terminalQuery.execute(session);

                    for (ImMap<Object, Object> row : terminalResult.valueIt()) {
                        boolean enabled = row.get("inMachineryPriceTransactionMachinery") != null;
                        Boolean succeeded = row.get("succeededMachineryMachineryPriceTransaction") != null;
                        Boolean cleared = row.get("clearedMachineryMachineryPriceTransaction") != null;
                        terminalInfoList.add(new TerminalInfo(enabled, cleared, succeeded, nppGroupMachinery, (Integer) row.get("nppMachinery"),
                                nameModelGroupMachinery, handlerModelGroupMachinery, getRowValue(row, "portMachinery"),
                                directoryGroupTerminal, idPriceListType));
                    }

                    List<TerminalItemInfo> terminalItemInfoList = new ArrayList<>();
                    for (ImMap<Object, Object> row : skuResult.valueIt()) {
                        String idItem = getRowValue(row, "idSkuBarcode");
                        String barcode = getRowValue(row, "idBarcode");
                        String name = getRowValue(row, "nameMachineryPriceTransactionBarcode");
                        BigDecimal price = (BigDecimal) row.get("priceMachineryPriceTransactionBarcode");
                        boolean split = row.get("splitMachineryPriceTransactionBarcode") != null;
                        Integer daysExpiry = (Integer) row.get("expiryDaysMachineryPriceTransactionBarcode");
                        Date expiryDate = (Date) row.get("expiryDateMachineryPriceTransactionBarcode");
                        Integer pluNumber = (Integer) row.get("pluNumberMachineryPriceTransactionBarcode");
                        Integer flags = (Integer) row.get("flagsMachineryPriceTransactionBarcode");
                        boolean passScales = row.get("passScalesMachineryPriceTransactionBarcode") != null;
                        BigDecimal valueVAT = machineryPriceTransactionStockTaxLM == null ? null : (BigDecimal) row.get("VATMachineryPriceTransactionBarcode");
                        String canonicalNameSkuGroup = (String) row.get("canonicalNameSkuGroupMachineryPriceTransactionBarcode");

                        terminalItemInfoList.add(new TerminalItemInfo(idItem, barcode, name, price, split, daysExpiry, 
                                expiryDate, passScales, valueVAT, pluNumber, flags, null, canonicalNameSkuGroup, null, null));
                    }

                    List<TerminalAssortment> terminalAssortmentList = TerminalEquipmentServer.readTerminalAssortmentList(session, getBusinessLogics(), priceListTypeGroupMachinery, stockGroupTerminal);
                    List<TerminalHandbookType> terminalHandbookTypeList = TerminalEquipmentServer.readTerminalHandbookTypeList(session, getBusinessLogics());
                    List<TerminalDocumentType> terminalDocumentTypeList = TerminalEquipmentServer.readTerminalDocumentTypeList(session, getBusinessLogics());
                    List<TerminalLegalEntity> terminalLegalEntityList = TerminalEquipmentServer.readTerminalLegalEntityList(session, getBusinessLogics());

                    transactionList.add(new TransactionTerminalInfo((Integer) transactionObject.getValue(), dateTimeCode, 
                            date, handlerModelGroupMachinery, (Integer) groupMachineryObject.object, nppGroupMachinery, nameGroupMachinery,
                            descriptionTransaction, terminalItemInfoList, terminalInfoList, snapshotTransaction, lastErrorDateTransaction,
                            terminalHandbookTypeList, terminalDocumentTypeList, terminalLegalEntityList, terminalAssortmentList,
                            nppGroupTerminal, directoryGroupTerminal));
                }
            }
            return transactionList;
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private List<Integer> readTroubleMachineryGroups(DataSession session, Integer minutes) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        List<Integer> result = new ArrayList();
        if(minutes != null) {
            KeyExpr groupMachineryExpr = new KeyExpr("GroupMachinery");
            ImRevMap<Object, KeyExpr> groupMachineryKeys = MapFact.singletonRev((Object) "groupMachinery", groupMachineryExpr);
            QueryBuilder<Object, Object> groupMachineryQuery = new QueryBuilder<>(groupMachineryKeys);

            String[] groupMachineryNames = new String[]{"npp", "lastErrorTime"};
            LCP[] groupMachineryProperties = machineryPriceTransactionLM.findProperties("npp[GroupMachinery]", "lastErrorTime[GroupMachinery]");
            for (int i = 0; i < groupMachineryProperties.length; i++) {
                groupMachineryQuery.addProperty(groupMachineryNames[i], groupMachineryProperties[i].getExpr(groupMachineryExpr));
            }
            groupMachineryQuery.and(machineryPriceTransactionLM.findProperty("lastErrorTime[GroupMachinery]").getExpr(groupMachineryExpr).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> groupMachineryResult = groupMachineryQuery.execute(session);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -minutes);
            Timestamp minTime = new Timestamp(cal.getTimeInMillis());
            for (ImMap<Object, Object> row : groupMachineryResult.valueIt()) {
                Integer npp = (Integer) row.get("npp");
                Timestamp lastErrorTime = (Timestamp) row.get("lastErrorTime");
                if (lastErrorTime != null && lastErrorTime.compareTo(minTime) <= 0)
                    result.add(npp);
            }
        }
        return result;
    }

    private Map<String, List<ItemGroup>> readItemGroupMap(DataSession session) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        Map<String, List<ItemGroup>> result = new HashMap<>();
        Map<String, ItemGroup> itemGroupMap = new HashMap<>();
        
        KeyExpr itemGroupExpr = new KeyExpr("ItemGroup");        
        ImRevMap<Object, KeyExpr> itemGroupKeys = MapFact.singletonRev((Object) "itemGroup", itemGroupExpr);
        QueryBuilder<Object, Object> itemGroupQuery = new QueryBuilder<>(itemGroupKeys);

        String[] itemGroupNames = new String[] {"idItemGroup", "overIdItemGroup", "nameItemGroup", "idParentItemGroup"};
        LCP[] itemGroupProperties = itemLM.findProperties("id[ItemGroup]", "overId[ItemGroup]", "name[ItemGroup]", "idParent[ItemGroup]");
        for (int i = 0; i < itemGroupProperties.length; i++) {
            itemGroupQuery.addProperty(itemGroupNames[i], itemGroupProperties[i].getExpr(itemGroupExpr));
        }
        
        itemGroupQuery.and(itemLM.findProperty("id[ItemGroup]").getExpr(itemGroupExpr).getWhere());
        
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> itemGroupResult = itemGroupQuery.execute(session);

        for (ImMap<Object, Object> row : itemGroupResult.valueIt()) {
            String idItemGroup = getRowValue(row, "idItemGroup");
            String overIdItemGroup = getRowValue(row, "overIdItemGroup");
            String nameItemGroup = getRowValue(row, "nameItemGroup");
            String idParentItemGroup = getRowValue(row, "idParentItemGroup");
            itemGroupMap.put(overIdItemGroup, new ItemGroup(idItemGroup, overIdItemGroup, nameItemGroup, idParentItemGroup));
        }
        
        for(Map.Entry<String, ItemGroup> entry : itemGroupMap.entrySet()) {
            List<ItemGroup> hierarchy = new ArrayList<>(Collections.singletonList(entry.getValue()));
            String idParent = entry.getValue().idParentItemGroup;
            while(itemGroupMap.containsKey(idParent)) {
                ItemGroup parentItemGroup = itemGroupMap.get(idParent);
                hierarchy.add(parentItemGroup);
                idParent = parentItemGroup.idParentItemGroup;
            }
            result.put(entry.getKey(), hierarchy);
        }
        return result;
    }

    @Override
    public List<DiscountCard> readDiscountCardList(String numberDiscountCardFrom, String numberDiscountCardTo) throws RemoteException, SQLException {
        return MachineryExchangeEquipmentServer.readDiscountCardList(getDbManager(), numberDiscountCardFrom, numberDiscountCardTo);
    }

    @Override
    public List<CashierInfo> readCashierInfoList() throws RemoteException, SQLException {
        return MachineryExchangeEquipmentServer.readCashierInfoList(getDbManager());
    }

    @Override
    public boolean enabledStopListInfo() throws RemoteException, SQLException {
        return StopListEquipmentServer.enabledStopListInfo();
    }

    @Override
    public List<StopListInfo> readStopListInfo() throws RemoteException, SQLException {
        return StopListEquipmentServer.readStopListInfo(getDbManager());
    }

    @Override
    public boolean enabledDeleteBarcodeInfo() throws RemoteException, SQLException {
        return DeleteBarcodeEquipmentServer.enabledDeleteBarcodeInfo();
    }

    @Override
    public List<DeleteBarcodeInfo> readDeleteBarcodeInfoList() throws RemoteException, SQLException {
        return DeleteBarcodeEquipmentServer.readDeleteBarcodeInfo(getDbManager());
    }

    @Override
    public void errorDeleteBarcodeReport(Integer nppGroupMachinery, Exception exception) throws RemoteException, SQLException {
        DeleteBarcodeEquipmentServer.errorDeleteBarcodeReport(getBusinessLogics(), getDbManager(), getStack(), nppGroupMachinery, exception);
    }

    @Override
    public void finishDeleteBarcode(Integer nppGroupMachinery, boolean markSucceeded) throws RemoteException, SQLException {
        DeleteBarcodeEquipmentServer.finishDeleteBarcode(getBusinessLogics(), getDbManager(), getStack(), nppGroupMachinery, markSucceeded);
    }

    @Override
    public void succeedDeleteBarcode(Integer nppGroupMachinery, Set<String> deleteBarcodeSet) throws RemoteException, SQLException {
        DeleteBarcodeEquipmentServer.succeedDeleteBarcode(getBusinessLogics(), getDbManager(), getStack(), nppGroupMachinery, deleteBarcodeSet);
    }

    @Override
    public void errorStopListReport(String numberStopList, Exception e) throws RemoteException, SQLException {
        StopListEquipmentServer.errorStopListReport(getBusinessLogics(), getStack(), getDbManager(), numberStopList, e);
    }

    @Override
    public void succeedStopList(String numberStopList, Set<String> idStockSet) throws RemoteException, SQLException {
        StopListEquipmentServer.succeedStopList(getBusinessLogics(), getStack(), getDbManager(), numberStopList, idStockSet);
    }

    @Override
    public List<TerminalOrder> readTerminalOrderList(RequestExchange requestExchange) throws RemoteException, SQLException {
        return MachineryExchangeEquipmentServer.readTerminalOrderList(getDbManager(), requestExchange);
    }

    @Override
    public List<RequestExchange> readRequestExchange() throws RemoteException, SQLException {
        return MachineryExchangeEquipmentServer.readRequestExchange(getDbManager(), getBusinessLogics(), getStack());
    }

    @Override
    public void finishRequestExchange(Set<Integer> succeededRequestsSet) throws RemoteException, SQLException {
        MachineryExchangeEquipmentServer.finishRequestExchange(getDbManager(), getBusinessLogics(), getStack(), succeededRequestsSet);
    }

    @Override
    public void errorRequestExchange(Map<Integer, String> succeededRequestsMap) throws RemoteException, SQLException {
        MachineryExchangeEquipmentServer.errorRequestExchange(getDbManager(), getBusinessLogics(), getStack(), succeededRequestsMap);
    }

    @Override
    public Map<String, BigDecimal> readZReportSumMap() throws RemoteException, SQLException {
        return SendSalesEquipmentServer.readZReportSumMap(getDbManager());
    }
    
    @Override
    public void succeedExtraCheckZReport(List<String> idZReportList) throws RemoteException, SQLException {
        SendSalesEquipmentServer.succeedExtraCheckZReport(getBusinessLogics(), getDbManager(), getStack(), idZReportList);
    }

    @Override
    public List<CashRegisterInfo> readCashRegisterInfo(String sidEquipmentServer) throws RemoteException, SQLException {
        return SendSalesEquipmentServer.readCashRegisterInfo(getDbManager(), sidEquipmentServer);
    }

    @Override
    public boolean enabledTerminalInfo() throws RemoteException, SQLException {
        return terminalLM != null;
    }

    @Override
    public List<TerminalInfo> readTerminalInfo(String sidEquipmentServer) throws RemoteException, SQLException {
        return TerminalDocumentEquipmentServer.readTerminalInfo(getDbManager(), sidEquipmentServer);
    }

    @Override
    public String sendTerminalInfo(List<TerminalDocumentDetail> terminalDocumentDetailList) throws RemoteException, SQLException {
        return TerminalDocumentEquipmentServer.sendTerminalInfo(getBusinessLogics(), getDbManager(), getStack(), terminalDocumentDetailList);
    }

    @Override
    public List<MachineryInfo> readMachineryInfo(String sidEquipmentServer) throws RemoteException, SQLException {
        return MachineryExchangeEquipmentServer.readMachineryInfo(getDbManager(), sidEquipmentServer);
    }

    @Override
    public Map<String, List<Object>> readRequestZReportSumMap(String idStock, Date dateFrom, Date dateTo) {
        return SendSalesEquipmentServer.readRequestZReportSumMap(getBusinessLogics(), getDbManager(), getStack(), idStock, dateFrom, dateTo);
    }

    @Override
    public void logRequestZReportSumCheck(Integer idRequestExchange, Integer nppGroupMachinery, List<List<Object>> checkSumResult) throws RemoteException, SQLException {
        SendSalesEquipmentServer.logRequestZReportSumCheck(getDbManager(), getBusinessLogics(), getStack(), idRequestExchange, nppGroupMachinery, checkSumResult);
    }

    @Override
    public Map<Integer, List<List<Object>>> readCashRegistersStock(String idStock) throws RemoteException, SQLException {
        return SendSalesEquipmentServer.readCashRegistersStock(getDbManager(), idStock);
    }

    @Override
    public PromotionInfo readPromotionInfo() throws RemoteException, SQLException {
        return promotionInterface == null ? null : promotionInterface.readPromotionInfo();
    }

    @Override
    public String sendSalesInfo(List<SalesInfo> salesInfoList, String sidEquipmentServer, String directory) throws IOException, SQLException {
        return sendSalesInfoNonRemote(getStack(), salesInfoList, sidEquipmentServer, directory);
    }


    public String sendSalesInfoNonRemote(ExecutionStack stack, List<SalesInfo> salesInfoList, String sidEquipmentServer, String directory) throws IOException, SQLException {
        try {

            if (zReportLM != null && notNullNorEmpty(salesInfoList)) {

                Collections.sort(salesInfoList, COMPARATOR);

                for (int start = 0; true;) {

                    try (DataSession session = getDbManager().createSession()) {
                        ObjectValue equipmentServerObject = equLM.findProperty("sidTo[VARSTRING[20]]").readClasses(session, new DataObject(sidEquipmentServer));
                        Integer numberAtATime = (Integer) equLM.findProperty("numberAtATime[EquipmentServer]").read(session, equipmentServerObject);
                        if (numberAtATime == null)
                            numberAtATime = salesInfoList.size();

                        boolean ignoreReceiptsAfterDocumentsClosedDate = equLM.findProperty("ignoreReceiptsAfterDocumentsClosedDate[EquipmentServer]").read(session, equipmentServerObject) != null;
                        List<Integer> allowReceiptsAfterDocumentsClosedDateCashRegisterList = SendSalesEquipmentServer.readAllowReceiptsAfterDocumentsClosedDateCashRegisterList(getDbManager());

                        Timestamp timeStart = getCurrentTimestamp();

                        int finish = (start + numberAtATime) < salesInfoList.size() ? (start + numberAtATime) : salesInfoList.size();

                        Integer lastNumberReceipt = start < finish ? salesInfoList.get(finish - 1).numberReceipt : null;
                        if (lastNumberReceipt != null) {
                            while (start < finish && salesInfoList.size() > finish && salesInfoList.get(finish).numberReceipt.equals(lastNumberReceipt))
                                finish++;
                        }

                        List<SalesInfo> data = start < finish ? salesInfoList.subList(start, finish) : new ArrayList<SalesInfo>();
                        start = finish;
                        int left = salesInfoList.size() - finish;
                        if (!notNullNorEmpty(data))
                            return null;

                        logger.info(String.format("Sending SalesInfo from %s to %s", start, finish));

                        Date startDate = (Date) equLM.findProperty("startDate[EquipmentServer]").read(session, equipmentServerObject);
                        Boolean timeId = (Boolean) equLM.findProperty("timeId[EquipmentServer]").read(session, equipmentServerObject);

                        ImportField nppGroupMachineryField = new ImportField(zReportLM.findProperty("npp[GroupMachinery]"));
                        ImportField nppMachineryField = new ImportField(zReportLM.findProperty("npp[Machinery]"));

                        ImportField idZReportField = new ImportField(zReportLM.findProperty("id[ZReport]"));
                        ImportField numberZReportField = new ImportField(zReportLM.findProperty("number[ZReport]"));
                        ImportField dateZReportField = new ImportField(zReportLM.findProperty("date[ZReport]"));
                        ImportField timeZReportField = new ImportField(zReportLM.findProperty("time[ZReport]"));

                        ImportField idReceiptField = new ImportField(zReportLM.findProperty("id[Receipt]"));
                        ImportField numberReceiptField = new ImportField(zReportLM.findProperty("number[Receipt]"));
                        ImportField dateReceiptField = new ImportField(zReportLM.findProperty("date[Receipt]"));
                        ImportField timeReceiptField = new ImportField(zReportLM.findProperty("time[Receipt]"));
                        ImportField isPostedZReportField = new ImportField(zReportLM.findProperty("isPosted[ZReport]"));

                        ImportField idEmployeeField = new ImportField(zReportLM.findProperty("id[Employee]"));
                        ImportField firstNameContactField = new ImportField(zReportLM.findProperty("firstName[Contact]"));
                        ImportField lastNameContactField = new ImportField(zReportLM.findProperty("lastName[Contact]"));

                        ImportField idReceiptDetailField = new ImportField(zReportLM.findProperty("id[ReceiptDetail]"));
                        ImportField numberReceiptDetailField = new ImportField(zReportLM.findProperty("number[ReceiptDetail]"));
                        ImportField idBarcodeReceiptDetailField = new ImportField(zReportLM.findProperty("idBarcode[ReceiptDetail]"));

                        //sale 1
                        ImportField quantityReceiptSaleDetailField = new ImportField(zReportLM.findProperty("quantity[ReceiptSaleDetail]"));
                        ImportField priceReceiptSaleDetailField = new ImportField(zReportLM.findProperty("price[ReceiptSaleDetail]"));
                        ImportField sumReceiptSaleDetailField = new ImportField(zReportLM.findProperty("sum[ReceiptSaleDetail]"));
                        ImportField discountSumReceiptSaleDetailField = new ImportField(zReportLM.findProperty("discountSum[ReceiptSaleDetail]"));
                        ImportField discountSumSaleReceiptField = new ImportField(zReportLM.findProperty("discountSumSale[Receipt]"));

                        //return 1
                        ImportField idSaleReceiptReceiptReturnDetailField = new ImportField(zReportLM.findProperty("id[Receipt]"));
                        ImportField quantityReceiptReturnDetailField = new ImportField(zReportLM.findProperty("quantity[ReceiptReturnDetail]"));
                        ImportField priceReceiptReturnDetailField = new ImportField(zReportLM.findProperty("price[ReceiptReturnDetail]"));
                        ImportField retailSumReceiptReturnDetailField = new ImportField(zReportLM.findProperty("sum[ReceiptReturnDetail]"));
                        ImportField discountSumReceiptReturnDetailField = new ImportField(zReportLM.findProperty("discountSum[ReceiptReturnDetail]"));
                        ImportField discountSumReturnReceiptField = new ImportField(zReportLM.findProperty("discountSumReturn[Receipt]"));

                        //giftCard 1
                        ImportField priceReceiptGiftCardSaleDetailField = null;
                        ImportField sumReceiptGiftCardSaleDetailField = null;
                        ImportField idGiftCardField = null;
                        if (giftCardLM != null) {
                            priceReceiptGiftCardSaleDetailField = new ImportField(giftCardLM.findProperty("price[ReceiptGiftCardSaleDetail]"));
                            sumReceiptGiftCardSaleDetailField = new ImportField(giftCardLM.findProperty("sum[ReceiptGiftCardSaleDetail]"));
                            idGiftCardField = new ImportField(giftCardLM.findProperty("id[GiftCard]"));
                        }

                        ImportField seriesNumberDiscountCardField = discountCardLM == null ? null : new ImportField(discountCardLM.findProperty("seriesNumber[DiscountCard]"));
                        ImportField idSectionField = zReportSectionLM == null ? null : new ImportField(zReportSectionLM.findProperty("id[Section]"));

                        List<ImportProperty<?>> saleProperties = new ArrayList<>();
                        List<ImportProperty<?>> returnProperties = new ArrayList<>();
                        List<ImportProperty<?>> giftCardProperties = new ArrayList<>();

                        ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("ZReport"), zReportLM.findProperty("zReport[VARSTRING[100]]").getMapping(idZReportField));
                        ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("CashRegister"), zReportLM.findProperty("cashRegisterNppGroupCashRegister[INTEGER,INTEGER]").getMapping(nppGroupMachineryField, nppMachineryField));
                        cashRegisterKey.skipKey = true;
                        ImportKey<?> receiptKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("Receipt"), zReportLM.findProperty("receipt[VARSTRING[100]]").getMapping(idReceiptField));
                        ImportKey<?> skuKey = new ImportKey((CustomClass) zReportLM.findClass("Sku"), zReportLM.findProperty("skuBarcode[VARSTRING[15],DATE]").getMapping(idBarcodeReceiptDetailField, dateReceiptField));
                        ImportKey<?> employeeKey = new ImportKey((CustomClass) zReportLM.findClass("Employee"), zReportLM.findProperty("employee[VARSTRING[100]]").getMapping(idEmployeeField));

                        ImportKey<?> discountCardKey = discountCardLM == null ? null : new ImportKey((ConcreteCustomClass) discountCardLM.findClass("DiscountCard"), discountCardLM.findProperty("discountSeriesNumber[STRING[18]]").getMapping(seriesNumberDiscountCardField, dateReceiptField));
                        ImportKey<?> giftCardKey = giftCardLM == null ? null : new ImportKey((ConcreteCustomClass) giftCardLM.findClass("GiftCard"), giftCardLM.findProperty("giftCard[VARSTRING[100]]").getMapping(idGiftCardField));
                        ImportKey<?> sectionKey = zReportSectionLM == null ? null : new ImportKey((ConcreteCustomClass) zReportSectionLM.findClass("Section"), zReportSectionLM.findProperty("section[VARSTRING[100]]").getMapping(idSectionField));

                        //sale 2
                        saleProperties.add(new ImportProperty(idZReportField, zReportLM.findProperty("id[ZReport]").getMapping(zReportKey)));
                        saleProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("number[ZReport]").getMapping(zReportKey)));
                        saleProperties.add(new ImportProperty(nppMachineryField, zReportLM.findProperty("cashRegister[ZReport]").getMapping(zReportKey),
                                zReportLM.object(zReportLM.findClass("CashRegister")).getMapping(cashRegisterKey)));
                        saleProperties.add(new ImportProperty(dateZReportField, zReportLM.findProperty("date[ZReport]").getMapping(zReportKey)));
                        saleProperties.add(new ImportProperty(timeZReportField, zReportLM.findProperty("time[ZReport]").getMapping(zReportKey)));
                        saleProperties.add(new ImportProperty(isPostedZReportField, zReportLM.findProperty("isPosted[ZReport]").getMapping(zReportKey)));

                        saleProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("id[Receipt]").getMapping(receiptKey)));
                        saleProperties.add(new ImportProperty(numberReceiptField, zReportLM.findProperty("number[Receipt]").getMapping(receiptKey)));
                        saleProperties.add(new ImportProperty(dateReceiptField, zReportLM.findProperty("date[Receipt]").getMapping(receiptKey)));
                        saleProperties.add(new ImportProperty(timeReceiptField, zReportLM.findProperty("time[Receipt]").getMapping(receiptKey)));
                        saleProperties.add(new ImportProperty(discountSumSaleReceiptField, zReportLM.findProperty("discountSumSale[Receipt]").getMapping(receiptKey)));
                        saleProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("zReport[Receipt]").getMapping(receiptKey),
                                zReportLM.object(zReportLM.findClass("ZReport")).getMapping(zReportKey)));
                        if (discountCardLM != null && zReportDiscountCardLM != null) {
                            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, discountCardLM.findProperty("number[DiscountCard]").getMapping(discountCardKey), true));
                            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, zReportDiscountCardLM.findProperty("discountCard[Receipt]").getMapping(receiptKey),
                                    discountCardLM.object(discountCardLM.findClass("DiscountCard")).getMapping(discountCardKey)));
                        }
                        saleProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("id[Employee]").getMapping(employeeKey)));
                        saleProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("employee[Receipt]").getMapping(receiptKey),
                                zReportLM.object(zReportLM.findClass("Employee")).getMapping(employeeKey)));
                        saleProperties.add(new ImportProperty(firstNameContactField, zReportLM.findProperty("firstName[Contact]").getMapping(employeeKey), true));
                        saleProperties.add(new ImportProperty(lastNameContactField, zReportLM.findProperty("lastName[Contact]").getMapping(employeeKey), true));

                        ImportKey<?> receiptSaleDetailKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("ReceiptSaleDetail"), zReportLM.findProperty("receiptDetail[VARSTRING[100]]").getMapping(idReceiptDetailField));
                        saleProperties.add(new ImportProperty(idReceiptDetailField, zReportLM.findProperty("id[ReceiptDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(numberReceiptDetailField, zReportLM.findProperty("number[ReceiptDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, zReportLM.findProperty("idBarcode[ReceiptDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(quantityReceiptSaleDetailField, zReportLM.findProperty("quantity[ReceiptSaleDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(priceReceiptSaleDetailField, zReportLM.findProperty("price[ReceiptSaleDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(sumReceiptSaleDetailField, zReportLM.findProperty("sum[ReceiptSaleDetail]").getMapping(receiptSaleDetailKey)));
                        saleProperties.add(new ImportProperty(discountSumReceiptSaleDetailField, zReportLM.findProperty("discountSum[ReceiptSaleDetail]").getMapping(receiptSaleDetailKey)));

                        saleProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("receipt[ReceiptDetail]").getMapping(receiptSaleDetailKey),
                                zReportLM.object(zReportLM.findClass("Receipt")).getMapping(receiptKey)));

                        saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, zReportLM.findProperty("sku[ReceiptSaleDetail]").getMapping(receiptSaleDetailKey),
                                zReportLM.object(zReportLM.findClass("Sku")).getMapping(skuKey)));

                        if(zReportSectionLM != null) {
                            saleProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("id[Section]").getMapping(sectionKey), true));
                            saleProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("section[ReceiptDetail]").getMapping(receiptSaleDetailKey),
                                    zReportSectionLM.object(zReportSectionLM.findClass("Section")).getMapping(sectionKey)));
                        }

                        //return 2
                        returnProperties.add(new ImportProperty(idZReportField, zReportLM.findProperty("id[ZReport]").getMapping(zReportKey)));
                        returnProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("number[ZReport]").getMapping(zReportKey)));
                        returnProperties.add(new ImportProperty(nppMachineryField, zReportLM.findProperty("cashRegister[ZReport]").getMapping(zReportKey),
                                zReportLM.object(zReportLM.findClass("CashRegister")).getMapping(cashRegisterKey)));
                        returnProperties.add(new ImportProperty(dateZReportField, zReportLM.findProperty("date[ZReport]").getMapping(zReportKey)));
                        returnProperties.add(new ImportProperty(timeZReportField, zReportLM.findProperty("time[ZReport]").getMapping(zReportKey)));
                        returnProperties.add(new ImportProperty(isPostedZReportField, zReportLM.findProperty("isPosted[ZReport]").getMapping(zReportKey)));

                        returnProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("id[Receipt]").getMapping(receiptKey)));
                        returnProperties.add(new ImportProperty(numberReceiptField, zReportLM.findProperty("number[Receipt]").getMapping(receiptKey)));
                        returnProperties.add(new ImportProperty(dateReceiptField, zReportLM.findProperty("date[Receipt]").getMapping(receiptKey)));
                        returnProperties.add(new ImportProperty(timeReceiptField, zReportLM.findProperty("time[Receipt]").getMapping(receiptKey)));
                        if (discountCardLM != null) {
                            returnProperties.add(new ImportProperty(discountSumReturnReceiptField, zReportLM.findProperty("discountSumReturn[Receipt]").getMapping(receiptKey)));
                        }
                        returnProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("zReport[Receipt]").getMapping(receiptKey),
                                zReportLM.object(zReportLM.findClass("ZReport")).getMapping(zReportKey)));
                        if (discountCardLM != null && zReportDiscountCardLM != null) {
                            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, discountCardLM.findProperty("number[DiscountCard]").getMapping(discountCardKey), true));
                            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, zReportDiscountCardLM.findProperty("discountCard[Receipt]").getMapping(receiptKey),
                                    discountCardLM.object(discountCardLM.findClass("DiscountCard")).getMapping(discountCardKey)));
                        }
                        returnProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("login[CustomUser]").getMapping(employeeKey), true));
                        returnProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("employee[Receipt]").getMapping(receiptKey),
                                zReportLM.object(zReportLM.findClass("CustomUser")).getMapping(employeeKey)));
                        returnProperties.add(new ImportProperty(firstNameContactField, zReportLM.findProperty("firstName[Contact]").getMapping(employeeKey), true));
                        returnProperties.add(new ImportProperty(lastNameContactField, zReportLM.findProperty("lastName[Contact]").getMapping(employeeKey), true));

                        ImportKey<?> receiptReturnDetailKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("ReceiptReturnDetail"), zReportLM.findProperty("receiptDetail[VARSTRING[100]]").getMapping(idReceiptDetailField));
                        returnProperties.add(new ImportProperty(idReceiptDetailField, zReportLM.findProperty("id[ReceiptDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(numberReceiptDetailField, zReportLM.findProperty("number[ReceiptDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, zReportLM.findProperty("idBarcode[ReceiptDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(quantityReceiptReturnDetailField, zReportLM.findProperty("quantity[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(priceReceiptReturnDetailField, zReportLM.findProperty("price[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(retailSumReceiptReturnDetailField, zReportLM.findProperty("sum[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(discountSumReceiptReturnDetailField, zReportLM.findProperty("discountSum[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey)));
                        returnProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("receipt[ReceiptDetail]").getMapping(receiptReturnDetailKey),
                                zReportLM.object(zReportLM.findClass("Receipt")).getMapping(receiptKey)));

                        returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, zReportLM.findProperty("sku[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey),
                                zReportLM.object(zReportLM.findClass("Sku")).getMapping(skuKey)));

                        ImportKey<?> receiptSaleDetailReceiptReturnDetailKey = new ImportKey((ConcreteCustomClass) zReportLM.findClass("Receipt"), zReportLM.findProperty("receipt[VARSTRING[100]]").getMapping(idSaleReceiptReceiptReturnDetailField));
                        receiptSaleDetailReceiptReturnDetailKey.skipKey = true;
                        returnProperties.add(new ImportProperty(idSaleReceiptReceiptReturnDetailField, zReportLM.findProperty("saleReceipt[ReceiptReturnDetail]").getMapping(receiptReturnDetailKey),
                                zReportLM.object(zReportLM.findClass("Receipt")).getMapping(receiptSaleDetailReceiptReturnDetailKey)));

                        if(zReportSectionLM != null) {
                            returnProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("id[Section]").getMapping(sectionKey), true));
                            returnProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("section[ReceiptDetail]").getMapping(receiptReturnDetailKey),
                                    zReportSectionLM.object(zReportSectionLM.findClass("Section")).getMapping(sectionKey)));
                        }

                        //giftCard 2
                        ImportKey<?> receiptGiftCardSaleDetailKey = null;
                        if (giftCardLM != null) {
                            giftCardProperties.add(new ImportProperty(idZReportField, zReportLM.findProperty("id[ZReport]").getMapping(zReportKey)));
                            giftCardProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("number[ZReport]").getMapping(zReportKey)));
                            giftCardProperties.add(new ImportProperty(nppMachineryField, zReportLM.findProperty("cashRegister[ZReport]").getMapping(zReportKey),
                                    zReportLM.object(zReportLM.findClass("CashRegister")).getMapping(cashRegisterKey)));
                            giftCardProperties.add(new ImportProperty(dateZReportField, zReportLM.findProperty("date[ZReport]").getMapping(zReportKey)));
                            giftCardProperties.add(new ImportProperty(timeZReportField, zReportLM.findProperty("time[ZReport]").getMapping(zReportKey)));
                            giftCardProperties.add(new ImportProperty(isPostedZReportField, zReportLM.findProperty("isPosted[ZReport]").getMapping(zReportKey)));

                            giftCardProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("id[Receipt]").getMapping(receiptKey)));
                            giftCardProperties.add(new ImportProperty(numberReceiptField, zReportLM.findProperty("number[Receipt]").getMapping(receiptKey)));
                            giftCardProperties.add(new ImportProperty(dateReceiptField, zReportLM.findProperty("date[Receipt]").getMapping(receiptKey)));
                            giftCardProperties.add(new ImportProperty(timeReceiptField, zReportLM.findProperty("time[Receipt]").getMapping(receiptKey)));

                            giftCardProperties.add(new ImportProperty(numberZReportField, zReportLM.findProperty("zReport[Receipt]").getMapping(receiptKey),
                                    zReportLM.object(zReportLM.findClass("ZReport")).getMapping(zReportKey)));
                            giftCardProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("login[CustomUser]").getMapping(employeeKey), true));
                            giftCardProperties.add(new ImportProperty(idEmployeeField, zReportLM.findProperty("employee[Receipt]").getMapping(receiptKey),
                                    zReportLM.object(zReportLM.findClass("CustomUser")).getMapping(employeeKey)));
                            giftCardProperties.add(new ImportProperty(firstNameContactField, zReportLM.findProperty("firstName[Contact]").getMapping(employeeKey), true));
                            giftCardProperties.add(new ImportProperty(lastNameContactField, zReportLM.findProperty("lastName[Contact]").getMapping(employeeKey), true));

                            receiptGiftCardSaleDetailKey = new ImportKey((ConcreteCustomClass) giftCardLM.findClass("ReceiptGiftCardSaleDetail"), zReportLM.findProperty("receiptDetail[VARSTRING[100]]").getMapping(idReceiptDetailField));
                            giftCardProperties.add(new ImportProperty(idReceiptDetailField, zReportLM.findProperty("id[ReceiptDetail]").getMapping(receiptGiftCardSaleDetailKey)));
                            giftCardProperties.add(new ImportProperty(numberReceiptDetailField, zReportLM.findProperty("number[ReceiptDetail]").getMapping(receiptGiftCardSaleDetailKey)));
                            giftCardProperties.add(new ImportProperty(priceReceiptGiftCardSaleDetailField, giftCardLM.findProperty("price[ReceiptGiftCardSaleDetail]").getMapping(receiptGiftCardSaleDetailKey)));
                            giftCardProperties.add(new ImportProperty(sumReceiptGiftCardSaleDetailField, giftCardLM.findProperty("sum[ReceiptGiftCardSaleDetail]").getMapping(receiptGiftCardSaleDetailKey)));
                            giftCardProperties.add(new ImportProperty(idReceiptField, zReportLM.findProperty("receipt[ReceiptDetail]").getMapping(receiptGiftCardSaleDetailKey),
                                    zReportLM.object(zReportLM.findClass("Receipt")).getMapping(receiptKey)));

                            giftCardProperties.add(new ImportProperty(idGiftCardField, giftCardLM.findProperty("id[GiftCard]").getMapping(giftCardKey)));
                            giftCardProperties.add(new ImportProperty(idGiftCardField, giftCardLM.findProperty("number[GiftCard]").getMapping(giftCardKey)));
                            giftCardProperties.add(new ImportProperty(idGiftCardField, giftCardLM.findProperty("giftCard[ReceiptGiftCardSaleDetail]").getMapping(receiptGiftCardSaleDetailKey),
                                    zReportLM.object(giftCardLM.findClass("GiftCard")).getMapping(giftCardKey)));
                        }

                        if(zReportSectionLM != null) {
                            giftCardProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("id[Section]").getMapping(sectionKey), true));
                            giftCardProperties.add(new ImportProperty(idSectionField, zReportSectionLM.findProperty("section[ReceiptDetail]").getMapping(receiptGiftCardSaleDetailKey),
                                    zReportSectionLM.object(zReportSectionLM.findClass("Section")).getMapping(sectionKey)));
                        }

                        List<List<Object>> dataSale = new ArrayList<>();
                        List<List<Object>> dataReturn = new ArrayList<>();
                        List<List<Object>> dataGiftCard = new ArrayList<>();

                        Map<Object, String> barcodeMap = new HashMap<>();
                        for (SalesInfo sale : data) {
                            if (!overDocumentsClosedDate(sale, ignoreReceiptsAfterDocumentsClosedDate, allowReceiptsAfterDocumentsClosedDateCashRegisterList)) {
                                String barcode = (notNullNorEmpty(sale.barcodeItem)) ? sale.barcodeItem :
                                        (sale.itemObject != null ? barcodeMap.get(sale.itemObject) : sale.idItem != null ? barcodeMap.get(sale.idItem) : null);
                                if (barcode == null && sale.itemObject != null) {
                                    barcode = trim((String) itemLM.findProperty("idBarcode[Sku]").read(session, new DataObject(sale.itemObject, (ConcreteClass) itemLM.findClass("Item"))));
                                    barcodeMap.put(sale.itemObject, barcode);
                                }
                                if (barcode == null && sale.idItem != null) {
                                    barcode = trim((String) itemLM.findProperty("idBarcodeSku[VARSTRING[100]]").read(session, new DataObject(sale.idItem, StringClass.get((100)))));
                                    barcodeMap.put(sale.idItem, barcode);
                                }

                                String idReceipt = sale.getIdReceipt(startDate, timeId);
                                if (sale.isGiftCard) {
                                    //giftCard 3
                                    List<Object> row = Arrays.<Object>asList(sale.nppGroupMachinery, sale.nppMachinery, sale.getIdZReport(startDate), sale.numberZReport,
                                            sale.dateZReport, sale.timeZReport, sale.dateReceipt, sale.timeReceipt, true, sale.idEmployee, sale.firstNameContact, sale.lastNameContact,
                                            idReceipt, sale.numberReceipt, sale.getIdReceiptDetail(startDate, timeId), sale.numberReceiptDetail, barcode,
                                            sale.priceReceiptDetail, sale.sumReceiptDetail);
                                    if (zReportSectionLM != null) {
                                        row = new ArrayList<>(row);
                                        row.add(sale.idSection);
                                    }
                                    dataGiftCard.add(row);
                                } else if (sale.quantityReceiptDetail.doubleValue() < 0) {
                                    //return 3
                                    List<Object> row = Arrays.<Object>asList(sale.nppGroupMachinery, sale.nppMachinery, sale.getIdZReport(startDate), sale.numberZReport,
                                            sale.dateZReport, sale.timeZReport, sale.dateReceipt, sale.timeReceipt, true, sale.idEmployee, sale.firstNameContact, sale.lastNameContact,
                                            idReceipt, sale.numberReceipt, sale.getIdReceiptDetail(startDate, timeId), sale.numberReceiptDetail, barcode, sale.quantityReceiptDetail.negate(),
                                            sale.priceReceiptDetail, sale.sumReceiptDetail.negate(), sale.discountSumReceiptDetail, sale.discountSumReceipt, sale.idSaleReceiptReceiptReturnDetail);
                                    if (discountCardLM != null) {
                                        row = new ArrayList<>(row);
                                        row.add(sale.seriesNumberDiscountCard);
                                    }
                                    if (zReportSectionLM != null) {
                                        row = new ArrayList<>(row);
                                        row.add(sale.idSection);
                                    }
                                    dataReturn.add(row);
                                } else {
                                    //sale 3
                                    List<Object> row = Arrays.<Object>asList(sale.nppGroupMachinery, sale.nppMachinery, sale.getIdZReport(startDate), sale.numberZReport,
                                            sale.dateZReport, sale.timeZReport, sale.dateReceipt, sale.timeReceipt, true, sale.idEmployee, sale.firstNameContact, sale.lastNameContact,
                                            idReceipt, sale.numberReceipt, sale.getIdReceiptDetail(startDate, timeId), sale.numberReceiptDetail, barcode, sale.quantityReceiptDetail,
                                            sale.priceReceiptDetail, sale.sumReceiptDetail, sale.discountSumReceiptDetail, sale.discountSumReceipt);
                                    if (discountCardLM != null) {
                                        row = new ArrayList<>(row);
                                        row.add(sale.seriesNumberDiscountCard);
                                    }
                                    if (zReportSectionLM != null) {
                                        row = new ArrayList<>(row);
                                        row.add(sale.idSection);
                                    }
                                    dataSale.add(row);
                                }
                            }
                        }

                        //sale 4
                        List<ImportField> saleImportFields = Arrays.asList(nppGroupMachineryField, nppMachineryField,
                                idZReportField, numberZReportField, dateZReportField, timeZReportField, dateReceiptField, timeReceiptField, isPostedZReportField,
                                idEmployeeField, firstNameContactField, lastNameContactField, idReceiptField, numberReceiptField,
                                idReceiptDetailField, numberReceiptDetailField, idBarcodeReceiptDetailField,
                                quantityReceiptSaleDetailField, priceReceiptSaleDetailField, sumReceiptSaleDetailField,
                                discountSumReceiptSaleDetailField, discountSumSaleReceiptField);
                        if (discountCardLM != null) {
                            saleImportFields = new ArrayList<>(saleImportFields);
                            saleImportFields.add(seriesNumberDiscountCardField);
                        }
                        if (zReportSectionLM != null) {
                            saleImportFields = new ArrayList<>(saleImportFields);
                            saleImportFields.add(idSectionField);
                        }

                        //return 4
                        List<ImportField> returnImportFields = Arrays.asList(nppGroupMachineryField, nppMachineryField,
                                idZReportField, numberZReportField, dateZReportField, timeZReportField, dateReceiptField, timeReceiptField, isPostedZReportField,
                                idEmployeeField, firstNameContactField, lastNameContactField, idReceiptField, numberReceiptField,
                                idReceiptDetailField, numberReceiptDetailField, idBarcodeReceiptDetailField,
                                quantityReceiptReturnDetailField, priceReceiptReturnDetailField, retailSumReceiptReturnDetailField,
                                discountSumReceiptReturnDetailField, discountSumReturnReceiptField, idSaleReceiptReceiptReturnDetailField);
                        if (discountCardLM != null) {
                            returnImportFields = new ArrayList<>(returnImportFields);
                            returnImportFields.add(seriesNumberDiscountCardField);
                        }
                        if (zReportSectionLM != null) {
                            returnImportFields = new ArrayList<>(returnImportFields);
                            returnImportFields.add(idSectionField);
                        }

                        //giftCard 4
                        List<ImportField> giftCardImportFields = Arrays.asList(nppGroupMachineryField, nppMachineryField,
                                idZReportField, numberZReportField, dateZReportField, timeZReportField, dateReceiptField, timeReceiptField, isPostedZReportField,
                                idEmployeeField, firstNameContactField, lastNameContactField, idReceiptField, numberReceiptField,
                                idReceiptDetailField, numberReceiptDetailField, idGiftCardField,
                                priceReceiptGiftCardSaleDetailField, sumReceiptGiftCardSaleDetailField);
                        if (zReportSectionLM != null) {
                            giftCardImportFields = new ArrayList<>(giftCardImportFields);
                            giftCardImportFields.add(idSectionField);
                        }
                        
                        //sale 5
                        List<ImportKey<?>> saleKeys = Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptSaleDetailKey, skuKey, employeeKey);
                        if (discountCardLM != null) {
                            saleKeys = new ArrayList<>(saleKeys);
                            saleKeys.add(discountCardKey);
                        }
                        if (zReportSectionLM != null) {
                            saleKeys = new ArrayList<>(saleKeys);
                            saleKeys.add(sectionKey);
                        }
                        new IntegrationService(session, new ImportTable(saleImportFields, dataSale), saleKeys, saleProperties).synchronize(true);

                        //return 5
                        List<ImportKey<?>> returnKeys = Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptReturnDetailKey, skuKey, employeeKey, receiptSaleDetailReceiptReturnDetailKey);
                        if (discountCardLM != null) {
                            returnKeys = new ArrayList<>(returnKeys);
                            returnKeys.add(discountCardKey);
                        }
                        if (zReportSectionLM != null) {
                            returnKeys = new ArrayList<>(returnKeys);
                            returnKeys.add(sectionKey);
                        }
                        new IntegrationService(session, new ImportTable(returnImportFields, dataReturn), returnKeys, returnProperties).synchronize(true);

                        //giftCard 5
                        if (giftCardLM != null) {
                            List<ImportKey<?>> giftCardKeys = Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptGiftCardSaleDetailKey, giftCardKey, employeeKey);
                            new IntegrationService(session, new ImportTable(giftCardImportFields, dataGiftCard), giftCardKeys, giftCardProperties).synchronize(true);
                        }

                        EquipmentServerImport.importPayment(getBusinessLogics(), session, data, startDate, timeId);

                        EquipmentServerImport.importPaymentGiftCard(getBusinessLogics(), session, data, startDate, timeId);


                        session.setKeepLastAttemptCountMap(true);
                        String result = session.applyMessage(getBusinessLogics(), stack);
                        if (result == null) {
                            logCompleteMessage(stack, session, data, dataSale.size() + dataReturn.size() + dataGiftCard.size(), left, timeStart, sidEquipmentServer, directory);
                        } else
                            return result;
                    }
                }
            } else return null;

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean overDocumentsClosedDate(SalesInfo salesInfo, boolean ignoreReceiptsAfterDocumentsClosedDate, List<Integer> allowReceiptsAfterDocumentsClosedDateCashRegisterList) {
        return ignoreReceiptsAfterDocumentsClosedDate && !allowReceiptsAfterDocumentsClosedDateCashRegisterList.contains(salesInfo.nppGroupMachinery) &&
                salesInfo.dateReceipt != null && salesInfo.cashRegisterInfo != null && salesInfo.cashRegisterInfo.documentsClosedDate != null &&
                salesInfo.dateReceipt.compareTo(salesInfo.cashRegisterInfo.documentsClosedDate) < 0;
    }
    
    private String logCompleteMessage(ExecutionStack stack, DataSession mainSession, List<SalesInfo> data, int dataSize, int left, Timestamp timeStart, String sidEquipmentServer, String directory) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {
        
        String message = formatCompleteMessage(mainSession, data, dataSize, left, timeStart, directory);
        
        try (DataSession session = getDbManager().createSession()) {
            ObjectValue equipmentServerObject = equLM.findProperty("sidTo[VARSTRING[20]]").readClasses(session, new DataObject(sidEquipmentServer));
            DataObject logObject = session.addObject((ConcreteCustomClass) equLM.findClass("EquipmentServerLog"));
            equLM.findProperty("equipmentServer[EquipmentServerLog]").change(equipmentServerObject.getValue(), session, logObject);
            equLM.findProperty("data[EquipmentServerLog]").change(message, session, logObject);
            equLM.findProperty("date[EquipmentServerLog]").change(getCurrentTimestamp(), session, logObject);
            return session.applyMessage(getBusinessLogics(), stack);
        }
    }
    
    private String formatCompleteMessage(DataSession session, List<SalesInfo> data, int dataSize, int left, Timestamp timeStart, String directory) {

        String conflicts = session.getLastAttemptCountMap();
        Timestamp timeFinish = getCurrentTimestamp();
        String message = String.format("Затрачено времени: %s с (%s - %s)\nЗагружено записей: %s, Осталось записей: %s",
                (timeFinish.getTime() - timeStart.getTime())/1000, formatDateTime(timeStart), formatDateTime(timeFinish), dataSize, left);
        if(conflicts != null)
            message += "\nКонфликты: " + conflicts;

        Map<Integer, Set<Integer>> nppCashRegisterMap = new HashMap<>();
        List<String> fileNames = new ArrayList<>();
        Set<String> dates = new HashSet<>();
        for (SalesInfo salesInfo : data) {
            if(nppCashRegisterMap.containsKey(salesInfo.nppGroupMachinery))
                nppCashRegisterMap.get(salesInfo.nppGroupMachinery).add(salesInfo.nppMachinery);
            else
                nppCashRegisterMap.put(salesInfo.nppGroupMachinery, new HashSet<>(Collections.singletonList(salesInfo.nppMachinery)));
            if ((salesInfo.filename != null) && (!fileNames.contains(salesInfo.filename.trim())))
                fileNames.add(salesInfo.filename.trim());
            if(salesInfo.dateZReport != null)
                dates.add(formatDate(salesInfo.dateZReport));
        }
        
        message += "\nИз касс: ";
        for (Map.Entry<Integer, Set<Integer>> cashRegisterEntry : nppCashRegisterMap.entrySet()) {
            for(Integer cashRegister : cashRegisterEntry.getValue())
                message += String.format("%s(%s), ", cashRegister, cashRegisterEntry.getKey());
        }
        message = message.substring(0, message.length() - 2);

        if(directory != null) {
            message+= "\nДиректория: " + directory;
        }
        if(!fileNames.isEmpty()) {
            message += "\nИз файлов: ";
            for (String filename : fileNames)
                message += filename + ", ";
            message = message.substring(0, message.length() - 2);
        }

        if(notNullNorEmpty(dates)) {
            message += "\nЗа даты: ";
            for (String date : dates)
                message += date + ", ";
            message = message.substring(0, message.length() - 2);
        }
        return message;
    }

    @Override
    public Set<String> readCashDocumentSet() throws IOException, SQLException {
        return SendSalesEquipmentServer.readCashDocumentSet(getDbManager());
    }

    @Override
    public String sendCashDocumentInfo(List<CashDocument> cashDocumentList) throws IOException, SQLException {
        return SendSalesEquipmentServer.sendCashDocumentInfo(getBusinessLogics(), getDbManager(), getStack(), cashDocumentList);
    }

    @Override
    public void succeedTransaction(Integer transactionId, Timestamp dateTime) throws RemoteException, SQLException {
        synchronized (this) {
            try (DataSession session = getDbManager().createSession()) {
                DataObject transactionObject = session.getDataObject(equLM.findClass("MachineryPriceTransaction"), transactionId);
                equLM.findProperty("succeeded[MachineryPriceTransaction]").change(true, session, transactionObject);
                equLM.findProperty("dateTimeSucceeded[MachineryPriceTransaction]").change(dateTime, session, transactionObject);
                session.apply(getBusinessLogics(), getStack());
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void processingTransaction(Integer transactionId, Timestamp dateTime) throws RemoteException, SQLException {
        if (machineryPriceTransactionLM != null) {
            try (DataSession session = getDbManager().createSession()) {
                DataObject transactionObject = session.getDataObject(equLM.findClass("MachineryPriceTransaction"), transactionId);
                machineryPriceTransactionLM.findProperty("dateTimeProcessing[MachineryPriceTransaction]").change(dateTime, session, transactionObject);
                session.apply(getBusinessLogics(), getStack());
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void clearedMachineryTransaction(Integer transactionId, List<MachineryInfo> machineryInfoList) throws RemoteException, SQLException {
        if(machineryPriceTransactionLM != null) {
            try (DataSession session = getDbManager().createSession()) {
                DataObject transactionObject = session.getDataObject(equLM.findClass("MachineryPriceTransaction"), transactionId);
                for (MachineryInfo machineryInfo : machineryInfoList) {
                    ObjectValue machineryObject = null;
                    if (machineryInfo instanceof CashRegisterInfo && cashRegisterLM != null)
                        machineryObject = cashRegisterLM.findProperty("cashRegisterNppGroupCashRegister[INTEGER,INTEGER]").readClasses(session, new DataObject(machineryInfo.numberGroup), new DataObject(machineryInfo.number));
                    else if (machineryInfo instanceof ScalesInfo && scalesLM != null)
                        machineryObject = scalesLM.findProperty("scalesNppGroupScales[INTEGER,INTEGER]").readClasses(session, new DataObject(machineryInfo.numberGroup), new DataObject(machineryInfo.number));
                    if (machineryObject != null && !machineryInfo.cleared)
                        machineryPriceTransactionLM.findProperty("cleared[Machinery,MachineryPriceTransaction]").change(true, session, (DataObject) machineryObject, transactionObject);
                }
                session.apply(getBusinessLogics(), getStack());
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void succeedMachineryTransaction(Integer transactionId, List<MachineryInfo> machineryInfoList, Timestamp dateTime) throws RemoteException, SQLException {
        synchronized (this) {
            if (machineryPriceTransactionLM != null) {
                try (DataSession session = getDbManager().createSession()) {
                    DataObject transactionObject = session.getDataObject(equLM.findClass("MachineryPriceTransaction"), transactionId);
                    for (MachineryInfo machineryInfo : machineryInfoList) {
                        ObjectValue machineryObject = null;
                        if (machineryInfo instanceof CashRegisterInfo && cashRegisterLM != null)
                            machineryObject = cashRegisterLM.findProperty("cashRegisterNppGroupCashRegister[INTEGER,INTEGER]").readClasses(session, new DataObject(machineryInfo.numberGroup), new DataObject(machineryInfo.number));
                        else if (machineryInfo instanceof ScalesInfo && scalesLM != null)
                            machineryObject = scalesLM.findProperty("scalesNppGroupScales[INTEGER,INTEGER]").readClasses(session, new DataObject(machineryInfo.numberGroup), new DataObject(machineryInfo.number));
                        if (machineryObject != null && (!(machineryInfo instanceof CashRegisterInfo) || !((CashRegisterInfo) machineryInfo).succeeded)) {
                            boolean alreadySucceeded = machineryPriceTransactionLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").read(session, machineryObject, transactionObject) != null;
                            if (!alreadySucceeded) {
                                machineryPriceTransactionLM.findProperty("succeeded[Machinery,MachineryPriceTransaction]").change(true, session,
                                        (DataObject) machineryObject, transactionObject);
                                machineryPriceTransactionLM.findProperty("dateTimeSucceeded[Machinery,MachineryPriceTransaction]").change(dateTime, session,
                                        (DataObject) machineryObject, transactionObject);
                            }
                        }
                    }
                    session.apply(getBusinessLogics(), getStack());
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    @Override
    public void errorTransactionReport(Integer transactionID, Throwable e) throws RemoteException, SQLException {
        try (DataSession session = getDbManager().createSession()) {
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClass("MachineryPriceTransactionError"));
            equLM.findProperty("machineryPriceTransaction[MachineryPriceTransactionError]").change(transactionID, session, errorObject);
            equLM.findProperty("data[MachineryPriceTransactionError]").change(e.toString(), session, errorObject);
            equLM.findProperty("date[MachineryPriceTransactionError]").change(getCurrentTimestamp(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            equLM.findProperty("errorTrace[MachineryPriceTransactionError]").change(os.toString(), session, errorObject);

            session.apply(getBusinessLogics(), getStack());
        } catch (Exception e2) {
            throw Throwables.propagate(e2);
        }
    }

    @Override
    public void errorEquipmentServerReport(String equipmentServer, Throwable exception, String extraData) throws
            RemoteException, SQLException {
        try (DataSession session = getDbManager().createSession()) {
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClass("EquipmentServerError"));
            Object equipmentServerObject = equLM.findProperty("sidTo[VARSTRING[20]]").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            equLM.findProperty("equipmentServer[EquipmentServerError]").change(equipmentServerObject, session, errorObject);
            equLM.findProperty("data[EquipmentServerError]").change(exception.toString(), session, errorObject);
            if(extraData != null)
                equLM.findProperty("extraData[EquipmentServerError]").change(extraData, session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            equLM.findProperty("erTrace[EquipmentServerError]").change(os.toString(), session, errorObject);

            equLM.findProperty("date[EquipmentServerError]").change(getCurrentTimestamp(), session, errorObject);

            session.apply(getBusinessLogics(), getStack());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            ThreadLocalContext.assureRmi(this);
            try (DataSession session = getDbManager().createSession()) {
                ObjectValue equipmentServerObject = equLM.findProperty("sidTo[VARSTRING[20]]").readClasses(session, new DataObject(equipmentServer));
                if (equipmentServerObject instanceof DataObject) {
                    Time timeFrom = (Time) equLM.findProperty("timeFrom[EquipmentServer]").read(session, equipmentServerObject);
                    Time timeTo = (Time) equLM.findProperty("timeTo[EquipmentServer]").read(session, equipmentServerObject);
                    Integer delay = (Integer) equLM.findProperty("delay[EquipmentServer]").read(session, equipmentServerObject);
                    Integer sendSalesDelay = (Integer) equLM.findProperty("sendSalesDelay[EquipmentServer]").read(session, equipmentServerObject);
                    return new EquipmentServerSettings(timeFrom, timeTo, delay, sendSalesDelay);
                } else return null;
            }
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

    private String getRowValue(ImMap<Object, Object> row, String key) {
        return trim((String) row.get(key));
    }

    protected boolean notNullNorEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    protected static boolean notNullNorEmpty(List value) {
        return value != null && !value.isEmpty();
    }

    protected boolean notNullNorEmpty(Set value) {
        return value != null && !value.isEmpty();
    }
    
    protected String formatDate(Date date) {
        return new SimpleDateFormat("dd.MM.yyyy").format(date);
    }
    
    protected String formatDateTime(Timestamp date) {
        return new SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(date);
    }
    
    public static Timestamp getCurrentTimestamp() {
        return DateConverter.dateToStamp(Calendar.getInstance().getTime());
    }

    private static Comparator<SalesInfo> COMPARATOR = new Comparator<SalesInfo>() {
        public int compare(SalesInfo o1, SalesInfo o2) {
            int compareGroupCashRegister = BaseUtils.nullCompareTo(o1.nppGroupMachinery, o2.nppGroupMachinery);
            if (compareGroupCashRegister == 0) {
                int compareCashRegister = BaseUtils.nullCompareTo(o1.nppMachinery, o2.nppMachinery);
                if (compareCashRegister == 0)
                    return BaseUtils.nullCompareTo(o1.numberZReport, o2.numberZReport);
                else
                    return compareCashRegister;
            } else
                return compareGroupCashRegister;
        }
    };

    @Override
    public String getEventName() {
        return "equipment-server";
    }
}
