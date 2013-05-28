package platform.server.logics.property.actions.flow;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.type.Type;
import platform.server.form.instance.FormCloseType;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class RequestUserInputActionProperty extends AroundAspectActionProperty {
    private final Type requestValueType;

    private final String chosenKey;

    private final LCP requestCanceledProperty;
    private final AnyValuePropertyHolder requestedValueProperty;

    private final AnyValuePropertyHolder chosenValueProperty;

    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;

    public <I extends PropertyInterface> RequestUserInputActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action,
                                                                        Type requestValueType, String chosenKey,
                                                                        LCP requestCanceledProperty, AnyValuePropertyHolder requestedValueProperty,
                                                                        AnyValuePropertyHolder chosenValueProperty, ConcreteCustomClass formResultClass, LCP formResultProperty) {
        super(sID, caption, innerInterfaces, action);

        this.requestValueType = requestValueType;
        this.chosenKey = chosenKey;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedValueProperty = requestedValueProperty;

        this.chosenValueProperty = chosenValueProperty;

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
        ObjectValue pushedUserInput = context.getPushedUserInput();
        if (pushedUserInput == null) {
            proceed(context);

            if (chosenKey != null) {
                int closeFormResultID = formResultClass.getObjectID(FormCloseType.CLOSE.asString());
                int dropFormResultID = formResultClass.getObjectID(FormCloseType.DROP.asString());

                Object value = formResultProperty.read(context);

                if (value != null && !value.equals(closeFormResultID)) {
                    ObjectValue chosenValue = value.equals(dropFormResultID) ? NullValue.instance : chosenValueProperty.read(requestValueType, context, new DataObject(chosenKey));
                    updateRequestedValue(context, chosenValue);
                } else {
                    requestCanceledProperty.change(true, context);
                }
            }
        } else {
            updateRequestedValue(context, pushedUserInput);
        }

        return FlowResult.FINISH;
    }

    private void updateRequestedValue(ExecutionContext context, ObjectValue requestedValue) throws SQLException {
        requestedValueProperty.write(requestValueType, requestedValue, context);
        requestCanceledProperty.change((Object)null, context);
    }
}
