package lsfusion.erp;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class ParseCompositionItemActionProperty extends ParseCompositionActionProperty {
    private final ClassPropertyInterface itemInterface;

    public ParseCompositionItemActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClass("Item"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        itemInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject itemObject = context.getDataKeyValue(itemInterface);
            String compositionItem = trim((String) findProperty("compositionItem").read(context, itemObject));
            parseComposition(context, true, itemObject, compositionItem);
            
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}