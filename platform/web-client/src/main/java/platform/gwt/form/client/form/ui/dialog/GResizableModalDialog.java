package platform.gwt.form.client.form.ui.dialog;

import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.base.client.ErrorHandlingCallback;
import platform.gwt.base.shared.actions.NumberResult;
import platform.gwt.form.client.form.FormsController;
import platform.gwt.form.shared.view.GForm;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.NativeEditEvent;

public class GResizableModalDialog extends GResizableModalForm {
    private static EditEvent initialFilterEvent;

    public GResizableModalDialog(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        super(formsController, form, hiddenHandler);
    }

    public static GResizableModalDialog showDialog(FormsController formsController, GForm form, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        initialFilterEvent = initFilterEvent;

        GResizableModalDialog dlg = new GResizableModalDialog(formsController, form, hiddenHandler);
        dlg.center();
        return dlg;
    }

    @Override
    public void initialFormChangesReceived() {
        NativeEvent event = ((NativeEditEvent) initialFilterEvent).getNativeEvent();
        if (initialFilterEvent != null && GKeyStroke.isPossibleStartFilteringEvent(event) && !GKeyStroke.isSpaceKeyEvent(event)) {
            editorForm.getInitialFilterProperty(new ErrorHandlingCallback<NumberResult>() {
                @Override
                public void success(NumberResult result) {
                    Integer initialFilterPropertyID = (Integer) result.value;

                    if (initialFilterPropertyID != null) {
                        editorForm.quickFilter(initialFilterEvent, initialFilterPropertyID);
                    }
                }
            });
        }
    }
}
