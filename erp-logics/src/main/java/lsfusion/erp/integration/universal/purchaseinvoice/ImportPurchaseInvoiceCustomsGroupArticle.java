package lsfusion.erp.integration.universal.purchaseinvoice;

import lsfusion.erp.integration.universal.ImportColumnDetail;
import lsfusion.server.integration.ImportField;
import lsfusion.server.integration.ImportKey;
import lsfusion.server.integration.ImportProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;

import java.util.LinkedHashMap;
import java.util.List;

public class ImportPurchaseInvoiceCustomsGroupArticle extends ImportDefaultPurchaseInvoiceActionProperty {

    public ImportPurchaseInvoiceCustomsGroupArticle(ScriptingLogicsModule LM) throws ScriptingModuleErrorLog.SemanticError {
        super(LM);
    }

    public void makeImport(ExecutionContext context, List<ImportField> fields, List<ImportProperty<?>> props, LinkedHashMap<String, ImportColumnDetail> defaultColumns,
                           List<PurchaseInvoiceDetail> userInvoiceDetailsList, List<List<Object>> data, ImportKey<?> itemKey, ImportKey<?> articleKey) throws ScriptingModuleErrorLog.SemanticError {
        ScriptingLogicsModule LM = context.getBL().getModule("CustomsGroupArticle");

        if (LM != null && itemKey != null && articleKey != null) {

            if (showField(userInvoiceDetailsList, "originalCustomsGroupItem")) {
                ImportField originalCustomsGroupItemField = new ImportField(LM.findProperty("originalCustomsGroup[Item]"));
                props.add(new ImportProperty(originalCustomsGroupItemField, LM.findProperty("originalCustomsGroup[Item]").getMapping(itemKey), getReplaceOnlyNull(defaultColumns, "originalCustomsGroupItem")));
                props.add(new ImportProperty(originalCustomsGroupItemField, LM.findProperty("originalCustomsGroup[Article]").getMapping(articleKey), getReplaceOnlyNull(defaultColumns, "originalCustomsGroupItem")));
                fields.add(originalCustomsGroupItemField);
                for (int i = 0; i < userInvoiceDetailsList.size(); i++)
                    data.get(i).add(userInvoiceDetailsList.get(i).getFieldValue("originalCustomsGroupItem"));
            }

        }
        
    }
}
