package lsfusion.erp.region.by.integration.excel;

import com.google.common.base.Throwables;
import jxl.write.WriteException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.DateClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.util.*;

public class ExportExcelBanksActionProperty extends ExportExcelActionProperty {

    public ExportExcelBanksActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportBank", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Код банка", "Название", "Адрес", "Отдел банка", "Код МФО", "ЦБУ");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {

            KeyExpr bankExpr = new KeyExpr("Bank");
            ImRevMap<Object, KeyExpr> bankKeys = MapFact.singletonRev((Object) "Bank", bankExpr);

            String[] bankNames = new String[]{"idBank", "nameBank", "departmentBank", "MFOBank", "CBUBank"};
            LCP[] bankProperties = findProperties("idBank", "nameBank", "departmentBank", "MFOBank", "CBUBank");
            QueryBuilder<Object, Object> bankQuery = new QueryBuilder<Object, Object>(bankKeys);
            for (int i = 0; i < bankProperties.length; i++) {
                bankQuery.addProperty(bankNames[i], bankProperties[i].getExpr(context.getModifier(), bankExpr));
            }
            java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
            bankQuery.addProperty("dataAddressBankDate", findProperty("dataAddressBankDate").getExpr(context.getModifier(), bankExpr, new DataObject(date, DateClass.instance).getExpr()));

            bankQuery.and(findProperty("nameBank").getExpr(context.getModifier(), bankExpr).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> bankResult = bankQuery.execute(session);

            for (ImMap<Object, Object> bankValue : bankResult.values()) {

                String idBank = (String) bankValue.get("idBank");
                String nameBank = (String) bankValue.get("nameBank");
                String departmentBank = (String) bankValue.get("departmentBank");
                String MFOBank = (String) bankValue.get("MFOBank");
                String CBUBank = (String) bankValue.get("CBUBank");
                String dataAddressBankDate = (String) bankValue.get("dataAddressBankDate");

                data.add(Arrays.asList(trim(idBank), trim(nameBank), trim(departmentBank), trim(MFOBank), 
                        trim(CBUBank), trim(dataAddressBankDate)));
                
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return data;
    }
}