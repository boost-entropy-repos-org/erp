package lsfusion.erp.region.by.ukm;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToEmpty;

public class SynchronizeItemGroupLoyaActionProperty extends SynchronizeLoyaActionProperty {
    private final ClassPropertyInterface itemGroupInterface;

    public SynchronizeItemGroupLoyaActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        itemGroupInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            DataObject itemGroupObject = context.getDataKeyValue(itemGroupInterface);

            settings = login(context, false);
            if (settings.error == null) {

                boolean nearestForbidPromotion = findProperty("nearestForbidPromotion[ItemGroup]").read(context, itemGroupObject) != null;
                Integer maxDiscount = (Integer) (nearestForbidPromotion ? 0 :  findProperty("maxDiscountLoya[]").read(context));
                Integer maxAllowBonus = (Integer) findProperty("maxAllowBonusLoya[]").read(context);
                Integer maxAwardBonus = (Integer) findProperty("maxAwardBonusLoya[]").read(context);

                Map<String, Integer> discountLimits = getDiscountLimits(maxDiscount, maxAllowBonus, maxAwardBonus);
                Long overId = parseGroup((String) findProperty("overId[ItemGroup]").read(context, itemGroupObject));
                String name = trimToEmpty((String) findProperty("name[ItemGroup]").read(context, itemGroupObject));
                Long idParent = parseGroup((String) findProperty("idParent[ItemGroup]").read(context, itemGroupObject));
                Category category = new Category(overId, name, overId == 0 ? null : idParent);
                String result = uploadCategory(context, category, discountLimits, false);
                if(authenticationFailed(result)) {
                    settings = login(context, true);
                    if(settings.error == null) {
                        result = uploadCategory(context, category, discountLimits, true);
                    }
                }
                findProperty("synchronizeItemResult[]").change(result, context);
            } else {
                findProperty("synchronizeItemResult[]").change(settings.error, context);
                context.delayUserInteraction(new MessageClientAction(settings.error, failCaption));
            }
        } catch (Exception e) {
            ServerLoggers.importLogger.error(failCaption, e);
            try {
                findProperty("synchronizeItemResult[]").change(String.valueOf(e), context);
            } catch (ScriptingErrorLog.SemanticErrorException e1) {
                throw Throwables.propagate(e);
            } context.delayUserInteraction(new MessageClientAction(e.getMessage(), failCaption));
        }
    }
}