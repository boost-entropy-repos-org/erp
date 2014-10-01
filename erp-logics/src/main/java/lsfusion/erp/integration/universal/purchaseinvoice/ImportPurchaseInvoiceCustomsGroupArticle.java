package lsfusion.erp.integration.universal.purchaseinvoice;

import lsfusion.erp.integration.universal.ImportColumnDetail;
import lsfusion.erp.integration.universal.PurchaseInvoiceDetail;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.integration.ImportField;
import lsfusion.server.integration.ImportKey;
import lsfusion.server.integration.ImportProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.LinkedHashMap;
import java.util.List;

public class ImportPurchaseInvoiceCustomsGroupArticle extends ImportDefaultPurchaseInvoiceActionProperty {

    public ImportPurchaseInvoiceCustomsGroupArticle(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void makeImport(ExecutionContext context, List<ImportField> fields, List<ImportProperty<?>> props, LinkedHashMap<String, ImportColumnDetail> defaultColumns,
                           List<PurchaseInvoiceDetail> userInvoiceDetailsList, List<List<Object>> data, ImportKey<?> itemKey, ImportKey<?> articleKey) throws ScriptingErrorLog.SemanticErrorException {
        ScriptingLogicsModule LM = context.getBL().getModule("CustomsGroupArticle");

        if (LM != null && itemKey != null && articleKey != null) {

            if (showField(userInvoiceDetailsList, "originalCustomsGroupItem")) {
                ImportField originalCustomsGroupItemField = new ImportField(LM.findProperty("originalCustomsGroupItem"));
                props.add(new ImportProperty(originalCustomsGroupItemField, LM.findProperty("originalCustomsGroupItem").getMapping(itemKey), getReplaceOnlyNull(defaultColumns, "originalCustomsGroupItem")));
                props.add(new ImportProperty(originalCustomsGroupItemField, LM.findProperty("originalCustomsGroupArticle").getMapping(articleKey), getReplaceOnlyNull(defaultColumns, "originalCustomsGroupItem")));
                fields.add(originalCustomsGroupItemField);
                for (int i = 0; i < userInvoiceDetailsList.size(); i++)
                    data.get(i).add(userInvoiceDetailsList.get(i).originalCustomsGroupItem);
            }

        }
        
    }
}
