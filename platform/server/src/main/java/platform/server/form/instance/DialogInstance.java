package platform.server.form.instance;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.auth.SecurityPolicy;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.logics.*;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PullChangeProperty;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class DialogInstance<T extends BusinessLogics<T>> extends FormInstance<T> {
    private static Logger logger = Logger.getLogger(DialogInstance.class);

    private ObjectInstance dialogObject;
    public PropertyDrawEntity initFilterPropertyDraw;
    public Boolean undecorated;

    private final ImSet<PullChangeProperty> pullProps;

    public DialogInstance(FormEntity<T> entity,
                          LogicsInstance logicsInstance,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          ObjectValue dialogValue,
                          PropertyObjectInterfaceInstance computer,
                          DataObject connection) throws SQLException {
        this(entity, logicsInstance, session, securityPolicy, tFocusView, classListener, dialogEntity, dialogValue, computer, connection, null, null);
    }

    public DialogInstance(FormEntity<T> entity,
                          LogicsInstance logicsInstance,
                          DataSession session,
                          SecurityPolicy securityPolicy,
                          FocusListener<T> tFocusView,
                          CustomClassListener classListener,
                          ObjectEntity dialogEntity,
                          ObjectValue dialogValue,
                          PropertyObjectInterfaceInstance computer,
                          DataObject connection,
                          ImSet<FilterEntity> additionalFilters,
                          ImSet<PullChangeProperty> pullProps) throws SQLException {
        super(entity,
              logicsInstance,
              session,
              securityPolicy,
              tFocusView,
              classListener,
              computer,
              connection,
              MapFact.singleton(dialogEntity, dialogValue),
              true,
              false,
              false,
              true,
              true,
              additionalFilters
        );
        // все равно нашли объекты или нет

        this.pullProps = pullProps;
        dialogObject = instanceFactory.getInstance(dialogEntity);
    }

    @Override
    protected FunctionSet<CalcProperty> getNoHints() {
        FunctionSet<CalcProperty> result = super.getNoHints();
        if(pullProps==null)
            return result;

        return BaseUtils.merge(result, new FunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                for(PullChangeProperty pullProp : pullProps)
                    if(pullProp.isChangeBetween(element))
                        return true;
                return false;
            }

            public boolean isEmpty() {
                return false;
            }

            public boolean isFull() {
                return false;
            }
        });
    }

    public ObjectValue getDialogObjectValue() {
        return dialogObject.getObjectValue();
    }

    public Object getDialogValue() {
        return getDialogObjectValue().getValue();
    }

    public Object getCellDisplayValue() {
        try {
            if (initFilterPropertyDraw != null) {
                CalcPropertyObjectInstance filterInstance = instanceFactory.getInstance((CalcPropertyObjectEntity)initFilterPropertyDraw.propertyObject);
                if (filterInstance != null) {
                    return read(filterInstance);
                }
            }
            return getDialogValue();
        } catch (SQLException e) {
            logger.error(ServerResourceBundle.getString("form.instance.error.getting.property.value.for.display"), e);
            return null;
        }
    }
}
