package platform.gwt.form.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import platform.gwt.base.client.ErrorHandlingCallback;
import platform.gwt.base.client.ui.DialogBoxHelper;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.GUserInputResult;
import platform.gwt.form.shared.view.actions.GUpdateEditValueAction;
import platform.gwt.form.shared.view.actions.GRequestUserInputAction;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;

public class GEditPropertyDispatcher extends GFormActionDispatcher {

    private final GEditPropertyHandler editHandler;

    private GType readType;
    private Object oldValue;
    private GPropertyDraw simpleChangeProperty;
    private GGroupObjectValue editColumnKey;

    private boolean valueRequested;


    public GEditPropertyDispatcher(GFormController form, GEditPropertyHandler editHandler) {
        super(form);
        this.editHandler = editHandler;
    }

    public void executePropertyEditAction(final GPropertyDraw editProperty, final GGroupObjectValue columnKey, String actionSID, final Object currentValue) {
        valueRequested = false;
        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;
        oldValue = null;

        final boolean asyncModifyObject = form.isAsyncModifyObject(editProperty);
        if (GEditBindingMap.CHANGE.equals(actionSID) && (asyncModifyObject || editProperty.changeType != null)) {
            if (editProperty.askConfirm) {
                form.blockingConfirm("lsFusion", editProperty.askConfirmMessage, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        if (chosenOption == DialogBoxHelper.OptionType.YES) {
                            executeSimpleChangeProperty(asyncModifyObject, editProperty, columnKey, currentValue);
                        } else {
                            editHandler.onEditFinished();
                        }
                    }
                });
            } else {
                executeSimpleChangeProperty(asyncModifyObject, editProperty, columnKey, currentValue);
            }
        } else {
            form.executeEditAction(editProperty, columnKey, actionSID, new ErrorHandlingCallback<ServerResponseResult>() {
                @Override
                public void success(ServerResponseResult response) {
                    Log.debug("Execute edit action response recieved...");
                    dispatchResponse(response);
                }
            });
        }
    }

    private void executeSimpleChangeProperty(boolean asyncModifyObject, GPropertyDraw editProperty, GGroupObjectValue columnKey, Object currentValue) {
        if (asyncModifyObject) {
            form.modifyObject(editProperty, columnKey);
            editHandler.onEditFinished();
        } else {
//          т.е. property.changeType != null
            editColumnKey = columnKey;
            simpleChangeProperty = editProperty;
            oldValue = currentValue;
            requestValue(simpleChangeProperty.changeType);
        }
    }

    @Override
    public void dispatchResponse(ServerResponseResult response) {
        super.dispatchResponse(response);

        if (readType != null) {
            GType editType = readType;
            readType = null;
            requestValue(editType);
        }
    }

    @Override
    protected void postDispatchResponse(ServerResponseResult response) {
        super.postDispatchResponse(response);
        editHandler.onEditFinished();
    }

    private void requestValue(GType type) {
        Log.debug("Edit started.");
        valueRequested = true;
        editHandler.requestValue(type, oldValue);
    }

    public void cancelEdit() {
        Log.debug("Edit canceled.");
        internalCommitValue(GUserInputResult.canceled);
    }

    public void commitValue(Object newValue) {
        Log.debug("Edit commit:" + newValue);
        internalCommitValue(new GUserInputResult(newValue));
    }

    private void internalCommitValue(GUserInputResult inputResult) {
        if (!valueRequested) {
            throw new IllegalStateException("value wasn't requested");
        }

        valueRequested = false;

        if (simpleChangeProperty != null) {
            if (!inputResult.isCanceled()) {
                form.changeProperty(editHandler, simpleChangeProperty, editColumnKey, inputResult.getValue(), oldValue);
                editHandler.onEditFinished();
            }
            return;
        }

        continueDispatching(inputResult);
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        readType = action.readType;
        oldValue = action.oldValue;

        pauseDispatching();

        return null;
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
        editHandler.updateEditValue(action.value);
    }
}
