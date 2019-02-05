package lsfusion.erp.region.by.integration.edi;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.Iterator;

public class PrintToImportLogActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface stringInterface;

    public PrintToImportLogActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        ServerLoggers.importLogger.info(context.getDataKeyValue(stringInterface).object);
    }
}