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

public class ImportPurchaseInvoicePurchaseShipmentBox extends ImportDefaultPurchaseInvoiceActionProperty {

    public ImportPurchaseInvoicePurchaseShipmentBox(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void makeImport(ExecutionContext context, List<ImportField> fields, List<ImportKey<?>> keys, List<ImportProperty<?>> props, LinkedHashMap<String, ImportColumnDetail> defaultColumns,
                           List<PurchaseInvoiceDetail> userInvoiceDetailsList, List<List<Object>> data, ImportKey<?> userInvoiceDetailKey) throws ScriptingErrorLog.SemanticErrorException {
        ScriptingLogicsModule LM = context.getBL().getModule("PurchaseShipmentBox");
        
        if(LM != null && userInvoiceDetailKey != null) {

            if (showField(userInvoiceDetailsList, "idBox")) {
                ImportField idBoxField = new ImportField(LM.findProperty("idBox"));
                ImportKey<?> boxKey = new ImportKey((ConcreteCustomClass) LM.findClass("Box"),
                        LM.findProperty("boxId").getMapping(idBoxField));
                keys.add(boxKey);
                props.add(new ImportProperty(idBoxField, LM.findProperty("idBox").getMapping(boxKey), getReplaceOnlyNull(defaultColumns, "idBox")));
                props.add(new ImportProperty(idBoxField, LM.findProperty("boxUserInvoiceDetail").getMapping(userInvoiceDetailKey),
                        LM.object(LM.findClass("Box")).getMapping(boxKey), getReplaceOnlyNull(defaultColumns, "idBox")));
                fields.add(idBoxField);
                for (int i = 0; i < userInvoiceDetailsList.size(); i++)
                    data.get(i).add(userInvoiceDetailsList.get(i).getFieldValue("idBox"));

                if (showField(userInvoiceDetailsList, "nameBox")) {
                    addDataField(props, fields, defaultColumns, LM.findProperty("nameBox"), "nameBox", boxKey);
                    for (int i = 0; i < userInvoiceDetailsList.size(); i++)
                        data.get(i).add(userInvoiceDetailsList.get(i).getFieldValue("nameBox"));
                }
            }
            
        }
        
    }
}
