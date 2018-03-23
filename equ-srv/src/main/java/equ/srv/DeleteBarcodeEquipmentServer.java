package equ.srv;

import com.google.common.base.Throwables;
import equ.api.DeleteBarcodeInfo;
import equ.api.cashregister.CashRegisterItemInfo;
import lsfusion.base.DateConverter;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.trim;

class DeleteBarcodeEquipmentServer {

    static ScriptingLogicsModule deleteBarcodeLM;

    public static void init(BusinessLogics BL) {
        deleteBarcodeLM = BL.getModule("DeleteBarcode");
    }


    static boolean enabledDeleteBarcodeInfo() {
        return deleteBarcodeLM != null;
    }

    static List<DeleteBarcodeInfo> readDeleteBarcodeInfo(DBManager dbManager) throws SQLException {

        Map<String, DeleteBarcodeInfo> barcodeMap = new HashMap<>();
        if(deleteBarcodeLM != null) {
            try (DataSession session = dbManager.createSession()) {

                KeyExpr deleteBarcodeExpr = new KeyExpr("deleteBarcode");
                ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "deleteBarcode", deleteBarcodeExpr);
                QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
                String[] names = new String[]{"barcode", "idSku", "nameSku", "idUOMSku", "shortNameUOMSku",
                        "nppGroupMachinery", "overDepartmentNumberGroupMachinery",
                        "handlerModelGroupMachinery", "valueVATSku", "idItemGroup", "nameItemGroup", "directoryGroupMachinery",};
                LCP[] properties = deleteBarcodeLM.findProperties("barcode[DeleteBarcode]", "idSku[DeleteBarcode]", "nameSku[DeleteBarcode]",
                        "idUOMSku[DeleteBarcode]", "shortNameUOMSku[DeleteBarcode]",
                        "nppGroupMachinery[DeleteBarcode]", "overDepartmentNumberGroupMachinery[DeleteBarcode]", "handlerModelGroupMachinery[DeleteBarcode]",
                        "valueVATSku[DeleteBarcode]", "idItemGroup[DeleteBarcode]", "nameItemGroup[DeleteBarcode]", "directoryGroupMachinery[DeleteBarcode]");
                for (int i = 0; i < properties.length; i++) {
                    query.addProperty(names[i], properties[i].getExpr(deleteBarcodeExpr));
                }

                query.and(deleteBarcodeLM.findProperty("activeGroupMachinery[DeleteBarcode]").getExpr(deleteBarcodeExpr).getWhere());
                query.and(deleteBarcodeLM.findProperty("barcode[DeleteBarcode]").getExpr(deleteBarcodeExpr).getWhere());
                query.and(deleteBarcodeLM.findProperty("succeeded[DeleteBarcode]").getExpr(deleteBarcodeExpr).getWhere().not());
                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session);
                for (ImMap<Object, Object> value : result.values()) {
                    String barcode = (String) value.get("barcode");
                    String idSku = (String) value.get("idSku");
                    String name = (String) value.get("nameSku");
                    String idUOM = (String) value.get("idUOMSku");
                    String shortNameUOM = (String) value.get("shortNameUOMSku");
                    Integer nppGroupMachinery = (Integer) value.get("nppGroupMachinery");
                    Integer overDepartmentNumberGroupMachinery = (Integer) value.get("overDepartmentNumberGroupMachinery");
                    String handlerModelGroupMachinery = (String) value.get("handlerModelGroupMachinery");
                    BigDecimal valueVAT = (BigDecimal) value.get("valueVATSku");
                    String idItemGroup = (String) value.get("idItemGroup");
                    String nameItemGroup = (String) value.get("nameItemGroup");
                    String key = handlerModelGroupMachinery + "/" + nppGroupMachinery;
                    String directory = trim((String) value.get("directoryGroupMachinery"));
                    DeleteBarcodeInfo deleteBarcodeInfo = barcodeMap.get(key);
                    if(deleteBarcodeInfo == null)
                        deleteBarcodeInfo = new DeleteBarcodeInfo(new ArrayList<CashRegisterItemInfo>(), nppGroupMachinery,
                                overDepartmentNumberGroupMachinery, handlerModelGroupMachinery, directory);
                    deleteBarcodeInfo.barcodeList.add(new CashRegisterItemInfo(idSku, barcode, name, null, false, null, null,
                            false, valueVAT, null, null, idItemGroup, nameItemGroup, idUOM, shortNameUOM, null, null, null, null, null, null,
                            null, null, null, null, null, null, null, null, null));
                    barcodeMap.put(key, deleteBarcodeInfo);

                }
            } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
        return new ArrayList<>(barcodeMap.values());
    }

    static void errorDeleteBarcodeReport(BusinessLogics BL, DBManager dbManager, ExecutionStack stack, Integer nppGroupMachinery, Exception exception) {
        try (DataSession session = dbManager.createSession()) {
            DataObject errorObject = session.addObject((ConcreteCustomClass) deleteBarcodeLM.findClass("DeleteBarcodeError"));
            ObjectValue groupMachineryObject = deleteBarcodeLM.findProperty("groupMachineryNpp[INTEGER]").readClasses(session, new DataObject(nppGroupMachinery));
            deleteBarcodeLM.findProperty("groupMachinery[DeleteBarcode]").change(groupMachineryObject, session, errorObject);
            deleteBarcodeLM.findProperty("data[DeleteBarcodeError]").change(exception.toString(), session, errorObject);
            deleteBarcodeLM.findProperty("date[DeleteBarcodeError]").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            deleteBarcodeLM.findProperty("errorTrace[DeleteBarcodeError]").change(os.toString(), session, errorObject);

            session.apply(BL, stack);
        } catch (Exception e2) {
            throw Throwables.propagate(e2);
        }
    }

    static void finishDeleteBarcode(BusinessLogics BL, DBManager dbManager, ExecutionStack stack, Integer nppGroupMachinery, boolean markSucceeded) {
        try (DataSession session = dbManager.createSession()) {
            deleteBarcodeLM.findAction("finishDeleteBarcode[INTEGER, BOOLEAN]").execute(session, stack, new DataObject(nppGroupMachinery), markSucceeded ? new DataObject(true) : NullValue.instance);
            session.apply(BL, stack);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    static void succeedDeleteBarcode(BusinessLogics BL, DBManager dbManager, ExecutionStack stack, Integer nppGroupMachinery, Set<String> deleteBarcodeSet) {
        try (DataSession session = dbManager.createSession()) {
            for (String barcode : deleteBarcodeSet) {
                deleteBarcodeLM.findAction("succeedDeleteBarcode[INTEGER, VARSTRING[28]]").execute(session, stack, new DataObject(nppGroupMachinery), new DataObject(barcode));
            }
            session.apply(BL, stack);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
