package lsfusion.erp.region.by.integration.excel;

import com.google.common.base.Throwables;
import jxl.write.WriteException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportExcelLegalEntitiesActionProperty extends ExportExcelActionProperty {

    public ExportExcelLegalEntitiesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportLegalEntities", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Наименование", "Полное наименование", "УНП", "Форма собственности", "Группа организаций",
                "Юридический адрес", "Телефон/Факс", "Код");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        ScriptingLogicsModule legalEntityByLM = context.getBL().getModule("LegalEntityBy");
        
        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {

            KeyExpr legalEntityExpr = new KeyExpr("LegalEntity");
            ImRevMap<Object, KeyExpr> legalEntityKeys = MapFact.singletonRev((Object) "LegalEntity", legalEntityExpr);

            String[] legalEntityNames = new String[]{"nameLegalEntity", "fullNameLegalEntity",
                    "shortNameOwnershipLegalEntity", "nameLegalEntityGroupLegalEntity", "addressLegalEntity",
                    "phoneLegalEntity"};
            LCP[] legalEntityProperties = findProperties("nameLegalEntity", "fullNameLegalEntity",
                    "shortNameOwnershipLegalEntity", "nameLegalEntityGroupLegalEntity", "addressLegalEntity",
                    "phoneLegalEntity");
            QueryBuilder<Object, Object> legalEntityQuery = new QueryBuilder<Object, Object>(legalEntityKeys);
            for (int i = 0; i < legalEntityProperties.length; i++) {
                legalEntityQuery.addProperty(legalEntityNames[i], legalEntityProperties[i].getExpr(context.getModifier(), legalEntityExpr));
            }
            if(legalEntityByLM != null)
                legalEntityQuery.addProperty("UNPLegalEntity", legalEntityByLM.findProperty("UNPLegalEntity").getExpr(context.getModifier(), legalEntityExpr));

            legalEntityQuery.and(findProperty("nameLegalEntity").getExpr(context.getModifier(), legalEntityQuery.getMapExprs().get("LegalEntity")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> legalEntityResult = legalEntityQuery.execute(session);

            for (int i = 0, size = legalEntityResult.size(); i < size; i++) {

                ImMap<Object, Object> legalEntityValue = legalEntityResult.getValue(i);

                String name = trim((String) legalEntityValue.get("nameLegalEntity"), "");
                String fullName = trim((String) legalEntityValue.get("fullNameLegalEntity"), "");
                String unp = trim((String) legalEntityValue.get("UNPLegalEntity"), "");
                String shortNameOwnership = trim((String) legalEntityValue.get("shortNameOwnershipLegalEntity"), "");
                String nameLegalEntityGroup = trim((String) legalEntityValue.get("nameLegalEntityGroupLegalEntity"), "");
                String address = trim((String) legalEntityValue.get("addressLegalEntity"), "");
                String phone = trim((String) legalEntityValue.get("phoneLegalEntity"), "");
                Integer legalEntityID = (Integer) legalEntityResult.getKey(i).get("LegalEntity");


                data.add(Arrays.asList(name, fullName, unp, shortNameOwnership, nameLegalEntityGroup, address, 
                        phone, formatValue(legalEntityID)));
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return data;
    }


}