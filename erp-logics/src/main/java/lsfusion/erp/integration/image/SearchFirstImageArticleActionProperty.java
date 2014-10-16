package lsfusion.erp.integration.image;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class SearchFirstImageArticleActionProperty extends DefaultImageArticleActionProperty {
    private final ClassPropertyInterface articleInterface;

    public SearchFirstImageArticleActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClass("Article"));
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        articleInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject articleObject = context.getDataKeyValue(articleInterface);
        loadFirstImage(context, articleObject);
    }
}