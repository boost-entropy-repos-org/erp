package lsfusion.erp.integration.universal.productionorder;

import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.erp.integration.universal.ImportColumnDetail;
import lsfusion.erp.integration.universal.ImportDocumentActionProperty;
import lsfusion.erp.integration.universal.ImportDocumentSettings;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

public class ImportProductionOrdersActionProperty extends ImportDocumentActionProperty {

    public ImportProductionOrdersActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataSession session = context.getSession();
            
            LCP<PropertyInterface> isImportType = (LCP<PropertyInterface>) is(findClass("ImportType"));
            ImRevMap<PropertyInterface, KeyExpr> importTypeKeys = isImportType.getMapKeys();
            KeyExpr importTypeKey = importTypeKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> importTypeQuery = new QueryBuilder<>(importTypeKeys);
            importTypeQuery.addProperty("autoImportDirectoryImportType", findProperty("autoImportDirectoryImportType").getExpr(session.getModifier(), importTypeKey));
            importTypeQuery.addProperty("startRowImportType", findProperty("startRowImportType").getExpr(session.getModifier(), importTypeKey));
            importTypeQuery.addProperty("isPostedImportType", findProperty("isPostedImportType").getExpr(session.getModifier(), importTypeKey));
            importTypeQuery.addProperty("separatorImportType", findProperty("separatorImportType").getExpr(session.getModifier(), importTypeKey));
            importTypeQuery.addProperty("captionPrimaryKeyTypeImportType", findProperty("captionPrimaryKeyTypeImportType").getExpr(session.getModifier(), importTypeKey));
            importTypeQuery.addProperty("captionSecondaryKeyTypeImportType", findProperty("captionSecondaryKeyTypeImportType").getExpr(session.getModifier(), importTypeKey));

            importTypeQuery.and(isImportType.getExpr(importTypeKey).getWhere());
            importTypeQuery.and(findProperty("autoImportImportType").getExpr(importTypeKey).getWhere());
            importTypeQuery.and(findProperty("autoImportDirectoryImportType").getExpr(importTypeKey).getWhere());
            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> importTypeResult = importTypeQuery.executeClasses(session);

            for (int i = 0, size = importTypeResult.size(); i < size; i++) {
                ImMap<Object, ObjectValue> entryValue = importTypeResult.getValue(i);

                DataObject importTypeObject = importTypeResult.getKey(i).valueIt().iterator().next();

                String directory = trim((String) entryValue.get("autoImportDirectoryImportType").getValue());
                
                ObjectValue operationObject = findProperty("autoImportOperationImportType").readClasses(session, (DataObject) importTypeObject);

                Map<String, ImportColumnDetail> importColumns = readImportColumns(session, importTypeObject).get(0);
                ImportDocumentSettings settings = readImportDocumentSettings(session, importTypeObject);
                String fileExtension = settings.getFileExtension();
                
                if (directory != null && fileExtension != null) {
                    File dir = new File(directory);

                    if (dir.exists()) {

                        for (File f : dir.listFiles()) {
                            if (f.getName().toLowerCase().endsWith(fileExtension.toLowerCase())) {
                                try (DataSession currentSession = context.createSession()) {
                                    try {
                                        boolean importResult = new ImportProductionOrderActionProperty(LM).makeImport(context.getBL(), currentSession, null,
                                                importColumns, IOUtils.getFileBytes(f), settings, fileExtension, operationObject);

                                        if (importResult)
                                            renameImportedFile(context, f.getAbsolutePath(), "." + fileExtension);

                                    } catch (Exception e) {
                                        ServerLoggers.systemLogger.error("ImportProductionOrders Error: ", e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}