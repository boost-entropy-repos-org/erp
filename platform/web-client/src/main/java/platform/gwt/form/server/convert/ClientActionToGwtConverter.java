package platform.gwt.form.server.convert;

import platform.client.logics.ClientFormChanges;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.gwt.form.server.FileUtils;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.view.actions.*;
import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import platform.gwt.form.shared.view.classes.GObjectClass;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.window.GModalityType;
import platform.interop.ModalityType;
import platform.interop.action.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static platform.base.BaseUtils.deserializeObject;

@SuppressWarnings("UnusedDeclaration")
public class ClientActionToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientActionToGwtConverter instance = new ClientActionToGwtConverter();
    }

    public static ClientActionToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientFormChangesToGwtConverter valuesConverter = ClientFormChangesToGwtConverter.getInstance();

    private ClientActionToGwtConverter() {
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    @Converter(from = ChooseClassClientAction.class)
    public GChooseClassAction convertAction(ChooseClassClientAction action) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(action.classes));

        GObjectClass baseClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        GObjectClass defaultClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        return new GChooseClassAction(action.concrete, baseClass, defaultClass);
    }

    @Converter(from = ConfirmClientAction.class)
    public GConfirmAction convertAction(ConfirmClientAction action) {
        return new GConfirmAction(action.message, action.caption);
    }

    @Converter(from = DialogClientAction.class)
    public GDialogAction convertAction(DialogClientAction action, FormDispatchServlet servlet) throws IOException {
        return new GDialogAction(servlet.getFormSessionManager().createForm(action.dialog, servlet));
    }

    @Converter(from = FormClientAction.class)
    public GFormAction convertAction(FormClientAction action, FormDispatchServlet servlet) throws IOException {
        GModalityType modalityType = convertOrCast(action.modalityType);
        return new GFormAction(modalityType, servlet.getFormSessionManager().createForm(action.remoteForm, servlet));
    }

    @Converter(from = ModalityType.class)
    public GModalityType convertModalityType(ModalityType modalityType) {
        switch (modalityType) {
            case DOCKED: return GModalityType.DOCKED;
            case MODAL: return GModalityType.MODAL;
            case FULLSCREEN_MODAL: return GModalityType.FULLSCREEN_MODAL;
            case DOCKED_MODAL: return GModalityType.DOCKED_MODAL;
        }
        return null;
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action, LogicsAwareDispatchServlet servlet) throws IOException {
        return new GHideFormAction();
    }

    @Converter(from = LogMessageClientAction.class)
    public GLogMessageAction convertAction(LogMessageClientAction action, LogicsAwareDispatchServlet servlet) throws IOException {
        ArrayList<ArrayList<String>> arrayData = new ArrayList<ArrayList<String>>();
        for(List<String> row : action.data)
            arrayData.add(new ArrayList<String>(row));
        return new GLogMessageAction(action.failed, action.message, arrayData, new ArrayList<String>(action.titles));
    }

    @Converter(from = MessageClientAction.class)
    public GMessageAction convertAction(MessageClientAction action) {
        return new GMessageAction(action.message, action.caption);
    }

    @Converter(from = ProcessFormChangesClientAction.class)
    public GProcessFormChangesAction convertAction(ProcessFormChangesClientAction action, FormSessionObject form, FormDispatchServlet servlet) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm);

        GFormChangesDTO changesDTO = ClientFormChangesToGwtConverter.getInstance().convertOrCast(changes, servlet.getBLProvider());

        return new GProcessFormChangesAction(changesDTO);
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, FormSessionObject form) throws IOException {
        return new GReportAction(FileUtils.exportReport(false, action.generationData));
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action, FormDispatchServlet servlet) throws IOException {
        GType type = typeConverter.convertOrCast(
                ClientTypeSerializer.deserializeClientType(action.readType)
        ) ;

        Object value = valuesConverter.convertOrCast(deserializeServerValue(action.oldValue), servlet.getBLProvider());

        return new GRequestUserInputAction(type, value);
    }

    private Object deserializeServerValue(byte[] valueBytes) throws IOException {
        return valuesConverter.convertOrCast(deserializeObject(valueBytes));
    }

    @Converter(from = RunPrintReportClientAction.class)
    public GReportAction convertAction(RunPrintReportClientAction action, FormSessionObject form) throws IOException {
        return new GReportAction(FileUtils.exportReport(false, form.remoteForm.getReportData(-1, null, false, null)));
    }

    @Converter(from = RunOpenInExcelClientAction.class)
    public GReportAction convertAction(RunOpenInExcelClientAction action, FormSessionObject form) throws IOException {
        return new GReportAction(FileUtils.exportReport(true, form.remoteForm.getReportData(-1, null, true, null)));
    }

    @Converter(from = UpdateEditValueClientAction.class)
    public GUpdateEditValueAction convertAction(UpdateEditValueClientAction action, FormDispatchServlet servlet) throws IOException {
        return new GUpdateEditValueAction(valuesConverter.convertOrCast(deserializeServerValue(action.value), servlet.getBLProvider()));
    }

    @Converter(from = AsyncGetRemoteChangesClientAction.class)
    public GAsyncGetRemoteChangesAction convertAction(AsyncGetRemoteChangesClientAction action) throws IOException {
        return new GAsyncGetRemoteChangesAction();
    }

    @Converter(from = LogOutClientAction.class)
    public GLogOutAction convertAction(LogOutClientAction action) {
        return new GLogOutAction();
    }

    @Converter(from = OpenUriClientAction.class)
    public GOpenUriAction convertAction(OpenUriClientAction action) {
        return new GOpenUriAction(action.uri.toString());
    }

    @Converter(from = EditNotPerformedClientAction.class)
    public GEditNotPerformedAction convertAction(EditNotPerformedClientAction action) {
        return new GEditNotPerformedAction();
    }

    @Converter(from = OpenFileClientAction.class)
    public GOpenFileAction convertAction(OpenFileClientAction action) {
        return new GOpenFileAction(FileUtils.saveFile(action.file, action.extension));
    }

    @Converter(from = ExportFileClientAction.class)
    public GExportFileAction convertAction(ExportFileClientAction action) {
        ArrayList<String> filePaths = new ArrayList<String>();
        for (String fileName : action.files.keySet()) {
            filePaths.add(FileUtils.saveFile(fileName, action.files.get(fileName)));
        }
        return new GExportFileAction(filePaths);
    }
}
