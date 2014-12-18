package lsfusion.erp.integration.image;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class SearchImageArticleActionProperty extends DefaultImageArticleActionProperty {
    private final ClassPropertyInterface articleInterface;

    public SearchImageArticleActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        articleInterface = i.next();

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        DataObject articleObject = context.getDataKeyValue(articleInterface);

        resetImages(context);

        loadImages(context, articleObject, 0, 8);
    }

    public void resetImages(ExecutionContext context) throws SQLHandledException {

        try {

            for (int i = 0; i < 64; i++) {

                DataObject currentObject = new DataObject(i);
                findProperty("thumbnailImage").change((Object) null, context, currentObject);
                findProperty("urlImage").change((Object)null, context, currentObject);
                findProperty("sizeImage").change((Object)null, context, currentObject);
            }

            findProperty("startImage").change((Object)null, context);
            findProperty("articleImage").change((Object)null, context);

        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}