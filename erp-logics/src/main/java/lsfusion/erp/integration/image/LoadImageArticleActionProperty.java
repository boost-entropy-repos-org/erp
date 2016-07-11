package lsfusion.erp.integration.image;

import lsfusion.base.IOUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class LoadImageArticleActionProperty extends DefaultImageArticleActionProperty {
    private final ClassPropertyInterface articleInterface;
    private final ClassPropertyInterface urlInterface;

    public LoadImageArticleActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        articleInterface = i.next();
        urlInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        DataObject articleObject = context.getDataKeyValue(articleInterface);
        DataObject urlObject = context.getDataKeyValue(urlInterface);

        try {

            File file = readImage((String) urlObject.object);
            if (file != null) {
                findProperty("image[Article]").change(IOUtils.getFileBytes(file), context, articleObject);
                file.delete();
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();
        }
    }
}