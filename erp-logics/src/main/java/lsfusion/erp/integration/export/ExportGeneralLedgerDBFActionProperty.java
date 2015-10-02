package lsfusion.erp.integration.export;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.erp.integration.DefaultExportActionProperty;
import lsfusion.erp.integration.OverJDBField;
import lsfusion.interop.Compare;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.*;

public class ExportGeneralLedgerDBFActionProperty extends DefaultExportActionProperty {
    String charset = "cp1251";

    public ExportGeneralLedgerDBFActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ObjectValue dateFrom = findProperty("dateFromExportGeneralLedgerDBF").readClasses(context);
            ObjectValue dateTo = findProperty("dateToExportGeneralLedgerDBF").readClasses(context);
            ObjectValue legalEntity = findProperty("legalEntityExportGeneralLedgerDBF").readClasses(context);
            ObjectValue glAccountType = findProperty("GLAccountTypeExportGeneralLedgerDBF").readClasses(context);

            File file = exportGeneralLedgers(context, dateFrom, dateTo, legalEntity, glAccountType);
            if (file != null) {
                context.delayUserInterfaction(new ExportFileClientAction("export.dbf", IOUtils.getFileBytes(file)));
                if(!file.delete())
                    file.deleteOnExit();
            }
            else
                context.delayUserInterfaction(new MessageClientAction("По заданным параметрам не найдено ни одной проводки", "Ошибка"));
            
        } catch (IOException | SQLException | JDBFException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private File exportGeneralLedgers(ExecutionContext context, ObjectValue dateFrom, ObjectValue dateTo, ObjectValue legalEntity, ObjectValue glAccountType) 
            throws JDBFException, ScriptingErrorLog.SemanticErrorException, IOException, SQLException, SQLHandledException {

        OverJDBField[] fields = {

                new OverJDBField("D_VV", 'D', 8, 0), new OverJDBField("DOK", 'C', 8, 0),
                new OverJDBField("VNDOK", 'C', 8, 0), new OverJDBField("TEXTPR", 'C', 60, 0),
                new OverJDBField("K_OP", 'C', 3, 0), new OverJDBField("K_SCHD", 'C', 5, 0),
                new OverJDBField("K_SCHK", 'C', 5, 0), new OverJDBField("K_ANAD1", 'C', 100, 0),
                new OverJDBField("K_ANAD2", 'C', 100, 0), new OverJDBField("K_ANAD3", 'C', 100, 0),

                new OverJDBField("K_ANAK1", 'C', 100, 0), new OverJDBField("K_ANAK2", 'C', 100, 0),
                new OverJDBField("K_ANAK3", 'C', 100, 0), new OverJDBField("N_SUM", 'F', 17, 2),
                new OverJDBField("K_MAT", 'C', 12, 0), new OverJDBField("N_MAT", 'F', 17, 3),
                new OverJDBField("N_DSUM", 'F', 15, 2), new OverJDBField("KOD_ISP", 'C', 2, 0),
                new OverJDBField("P_AVT", 'C', 3, 0)
        };

        KeyExpr generalLedgerExpr = new KeyExpr("GeneralLedger");
        KeyExpr dimensionTypeExpr = new KeyExpr("DimensionType");

        ImRevMap<Object, KeyExpr> generalLedgerKeys = MapFact.toRevMap((Object) "GeneralLedger", generalLedgerExpr, "DimensionType", dimensionTypeExpr);

        QueryBuilder<Object, Object> generalLedgerQuery = new QueryBuilder<>(generalLedgerKeys);


        String[] generalLedgerNames = new String[]{"dateGeneralLedger", "numberGLDocumentGeneralLedger",
                "descriptionGeneralLedger", "idDebitGeneralLedger", "idCreditGeneralLedger", "sumGeneralLedger",
                "idOperationGeneralLedger"};
        LCP[] generalLedgerProperties = findProperties("dateGeneralLedger", "numberGLDocumentGeneralLedger",
                "descriptionGeneralLedger", "idDebitGeneralLedger", "idCreditGeneralLedger", "sumGeneralLedger",
                "idOperationGeneralLedger");
        for (int j = 0; j < generalLedgerProperties.length; j++) {
            generalLedgerQuery.addProperty(generalLedgerNames[j], generalLedgerProperties[j].getExpr(generalLedgerExpr));
        }


        String[] dimensionTypeNames = new String[]{"idDebitGeneralLedgerDimensionType", "orderDebitGeneralLedgerDimensionType",
                "idCreditGeneralLedgerDimensionType", "orderCreditGeneralLedgerDimensionType"};
        LCP[] dimensionTypeProperties = findProperties("idDebitGeneralLedgerDimensionType", "orderDebitGeneralLedgerDimensionType",
                "idCreditGeneralLedgerDimensionType", "orderCreditGeneralLedgerDimensionType");
        for (int j = 0; j < dimensionTypeProperties.length; j++) {
            generalLedgerQuery.addProperty(dimensionTypeNames[j], dimensionTypeProperties[j].getExpr(generalLedgerExpr, dimensionTypeExpr));
        }

        generalLedgerQuery.and(findProperty("sumGeneralLedger").getExpr(generalLedgerExpr).getWhere());
        generalLedgerQuery.and(findProperty("nameDimensionType").getExpr(dimensionTypeExpr).getWhere());
        
        if(glAccountType instanceof DataObject) {
            Where where1 = findProperty("glAccountTypeDebitGeneralLedger").getExpr(generalLedgerExpr).compare((DataObject) glAccountType, Compare.EQUALS);
            Where where2 = findProperty("glAccountTypeCreditGeneralLedger").getExpr(generalLedgerExpr).compare((DataObject) glAccountType, Compare.EQUALS);
            generalLedgerQuery.and(where1.or(where2));
        }
        if(legalEntity instanceof DataObject)
            generalLedgerQuery.and(findProperty("legalEntityGeneralLedger").getExpr(generalLedgerExpr).compare((DataObject) legalEntity, Compare.EQUALS));
        if (dateFrom instanceof DataObject)
            generalLedgerQuery.and(findProperty("dateGeneralLedger").getExpr(generalLedgerExpr).compare((DataObject) dateFrom, Compare.GREATER_EQUALS));
        if (dateTo instanceof DataObject)
            generalLedgerQuery.and(findProperty("dateGeneralLedger").getExpr(generalLedgerExpr).compare((DataObject) dateTo, Compare.LESS_EQUALS));

        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> generalLedgerResult = generalLedgerQuery.executeClasses(context);

        if (generalLedgerResult.size() == 0)
            return null;

        Map<DataObject, List<Object>> generalLedgerMap = new HashMap<>();
        Map<DataObject, String> debit1Map = new HashMap<>(); //K_ANAD1
        Map<DataObject, String> debit2Map = new HashMap<>();  //K_ANAD2
        Map<DataObject, String> debit3Map = new HashMap<>();   //K_ANAD3
        Map<DataObject, String> credit1Map = new HashMap<>(); //K_ANAK1
        Map<DataObject, String> credit2Map = new HashMap<>();  //K_ANAK2
        Map<DataObject, String> credit3Map = new HashMap<>();  //K_ANAK3

        for (int i = 0, size = generalLedgerResult.size(); i < size; i++) {

            DataObject generalLedgerObject = generalLedgerResult.getKey(i).get("GeneralLedger");

            ImMap<Object, ObjectValue> resultValues = generalLedgerResult.getValue(i);

            Date dateGeneralLedger = (Date) resultValues.get("dateGeneralLedger").getValue(); //D_VV
            String numberGeneralLedger = trim((String) resultValues.get("numberGLDocumentGeneralLedger").getValue(), 8); //DOK

            String description = trim((String) resultValues.get("descriptionGeneralLedger").getValue(), 60); //TEXTPR
            String idDebit = (String) resultValues.get("idDebitGeneralLedger").getValue();   //K_SCHD
            idDebit = idDebit == null ? null : trim(idDebit.replace(".", ""), 5);
            String idCredit = (String) resultValues.get("idCreditGeneralLedger").getValue(); //K_SCHK
            idCredit = idCredit == null ? null : trim(idCredit.replace(".", ""), 5);

            BigDecimal sumGeneralLedger = (BigDecimal) resultValues.get("sumGeneralLedger").getValue(); //N_SUM                                
            String idOperationGeneralLedger = trim((String) resultValues.get("idOperationGeneralLedger").getValue(), 3); //K_OP  
            
            String nameDebit = (String) resultValues.get("idDebitGeneralLedgerDimensionType").getValue();
            Integer orderDebit = (Integer) resultValues.get("orderDebitGeneralLedgerDimensionType").getValue();
            if (orderDebit != null) {
                if (orderDebit == 1)
                    debit1Map.put(generalLedgerObject, nameDebit);
                else if (orderDebit == 2)
                    debit2Map.put(generalLedgerObject, nameDebit);
                else if (orderDebit == 3)
                    debit3Map.put(generalLedgerObject, nameDebit);
            }
            String nameCredit = (String) resultValues.get("idCreditGeneralLedgerDimensionType").getValue();
            Integer orderCredit = (Integer) resultValues.get("orderCreditGeneralLedgerDimensionType").getValue();
            if (orderCredit != null) {
                if (orderCredit == 1)
                    credit1Map.put(generalLedgerObject, nameCredit);
                else if (orderCredit == 2)
                    credit2Map.put(generalLedgerObject, nameCredit);
                else if (orderCredit == 3)
                    credit3Map.put(generalLedgerObject, nameCredit);
            }

            generalLedgerMap.put(generalLedgerObject, Arrays.asList((Object) dateGeneralLedger, numberGeneralLedger,
                    description, idDebit, idCredit, sumGeneralLedger, idOperationGeneralLedger));
        }

        File dbfFile = File.createTempFile("export", "dbf");
        DBFWriter dbfwriter = new DBFWriter(dbfFile.getAbsolutePath(), fields, charset);

        List<GeneralLedger> generalLedgerList = new ArrayList<>();
        for (Map.Entry<DataObject, List<Object>> entry : generalLedgerMap.entrySet()) {
            DataObject key = entry.getKey();
            List<Object> values = entry.getValue();
            generalLedgerList.add(new GeneralLedger((Date) values.get(0), (String) values.get(1), (String) values.get(2),
                    (String) values.get(6), (String) values.get(3), (String) values.get(4), debit1Map.get(key),
                    debit2Map.get(key), debit3Map.get(key), credit1Map.get(key), credit2Map.get(key),
                    credit3Map.get(key), (BigDecimal) values.get(5)));
        }
        
        Collections.sort(generalLedgerList, COMPARATOR);

        for(GeneralLedger gl : generalLedgerList) {
        dbfwriter.addRecord(new Object[]{gl.dateGeneralLedger, gl.numberGeneralLedger, null, gl.descriptionGeneralLedger, //4
                gl.idOperationGeneralLedger, gl.idDebitGeneralLedger, gl.idCreditGeneralLedger, gl.anad1, gl.anad2, //9 
                gl.anad3, gl.anak1, gl.anak2, gl.anak3, gl.sumGeneralLedger, null, null, 0, "00", "TMC"}); //19
        }
        dbfwriter.close();

        byte[] bytes = IOUtils.getFileBytes(dbfFile);
        if(bytes.length > 29)
            bytes[29] = getCharsetByte(charset);
        FileUtils.writeByteArrayToFile(dbfFile, bytes);

        return dbfFile;
    }

    private byte getCharsetByte(String charset) {
        switch (charset) {
            case "cp866":
                return (byte) 0x65;
            case "cp1251":
                return (byte) 0xC9;
            default:
                return (byte) 0x00;
        }
    }

    private static Comparator<GeneralLedger> COMPARATOR = new Comparator<GeneralLedger>() {
        public int compare(GeneralLedger g1, GeneralLedger g2) {
            int result = g1.dateGeneralLedger == null ? (g2.dateGeneralLedger == null ? 0 : -1) : 
                    (g2.dateGeneralLedger == null ? 1 : g1.dateGeneralLedger.compareTo(g2.dateGeneralLedger));
            if (result == 0)
                result = g1.numberGeneralLedger == null ? (g2.numberGeneralLedger == null ? 0 : -1) : 
                        (g2.numberGeneralLedger == null ? 1 : g1.numberGeneralLedger.compareTo(g2.numberGeneralLedger));
            return result;
        }
    };
}
