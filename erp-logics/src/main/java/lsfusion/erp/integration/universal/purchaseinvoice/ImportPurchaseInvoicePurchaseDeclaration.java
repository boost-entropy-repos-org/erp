package lsfusion.erp.integration.universal.purchaseinvoice;

import lsfusion.erp.integration.universal.ImportColumnDetail;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.integration.ImportField;
import lsfusion.server.integration.ImportKey;
import lsfusion.server.integration.ImportProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.LinkedHashMap;
import java.util.List;

public class ImportPurchaseInvoicePurchaseDeclaration extends ImportDefaultPurchaseInvoiceActionProperty {

    public ImportPurchaseInvoicePurchaseDeclaration(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void makeImport(ExecutionContext context, List<ImportField> fields, List<ImportKey<?>> keys, List<ImportProperty<?>> props, LinkedHashMap<String, ImportColumnDetail> defaultColumns,
                           List<PurchaseInvoiceDetail> userInvoiceDetailsList, List<List<Object>> data, ImportKey<?> userInvoiceDetailKey) throws ScriptingErrorLog.SemanticErrorException {
        ScriptingLogicsModule LM = context.getBL().getModule("PurchaseDeclaration");
        
        if(LM != null && userInvoiceDetailKey != null) {
            
            if (showField(userInvoiceDetailsList, "declaration")) {
                ImportField numberDeclarationField = new ImportField(LM.findProperty("number[Declaration]"));
                ImportKey<?> declarationKey = new ImportKey((ConcreteCustomClass) LM.findClass("Declaration"),
                        LM.findProperty("declaration[VARSTRING[100]]").getMapping(numberDeclarationField));
                keys.add(declarationKey);
                props.add(new ImportProperty(numberDeclarationField, LM.findProperty("number[Declaration]").getMapping(declarationKey), getReplaceOnlyNull(defaultColumns, "numberDeclaration")));
                props.add(new ImportProperty(numberDeclarationField, LM.findProperty("id[Declaration]").getMapping(declarationKey), getReplaceOnlyNull(defaultColumns, "numberDeclaration")));
                props.add(new ImportProperty(numberDeclarationField, LM.findProperty("declaration[UserInvoiceDetail]").getMapping(userInvoiceDetailKey),
                        object(LM.findClass("Declaration")).getMapping(declarationKey), getReplaceOnlyNull(defaultColumns, "numberDeclaration")));
                fields.add(numberDeclarationField);
                for (int i = 0; i < userInvoiceDetailsList.size(); i++)
                    data.get(i).add(userInvoiceDetailsList.get(i).getFieldValue("declaration"));
            }
            
        }
        
    }
}
