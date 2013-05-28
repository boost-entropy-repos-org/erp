package platform.server.context;

import org.apache.log4j.Logger;
import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.interop.action.LogMessageClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsInstance;
import platform.server.logics.ObjectValue;
import platform.server.logics.SecurityManager;
import platform.server.remote.RemoteForm;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static platform.base.BaseUtils.padLeft;
import static platform.base.BaseUtils.replicate;
import static platform.server.ServerLoggers.systemLogger;

public class LogicsInstanceContext extends AbstractContext {
    private static final Logger logger = Logger.getLogger(LogicsInstanceContext.class);

    private final LogicsInstance logicsInstance;

    public LogicsInstanceContext(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        DataObject serverComputer = logicsInstance.getDbManager().getServerComputerObject();
        return new FormInstance(formEntity,
                                logicsInstance, session, SecurityManager.serverSecurityPolicy, null, null,
                                serverComputer,
                                null, mapObjects, isModal, sessionScope.isManageSession(), checkOnOk, showDrop, interactive, null);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm(formInstance, logicsInstance.getRmiManager().getExportPort(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        if (!proccessClientAction(action)) {
            super.delayUserInteraction(action);
        }
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        if (proccessClientAction(action)) {
            return null;
        }
        return super.requestUserInteraction(action);
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        for (ClientAction action : actions) {
            if (!proccessClientAction(action)) {
                throw new UnsupportedOperationException("requestUserInteraction is not supported for action" + (action == null ? "" : ": " + action.getClass()));
            }
        }
        return new Object[actions.length];
    }

    private boolean proccessClientAction(ClientAction action) {
        if (action instanceof LogMessageClientAction) {
            LogMessageClientAction logAction = (LogMessageClientAction) action;
            if (logAction.failed) {
                throw new RuntimeException("Server error: " + logAction.message + "\n" + errorDataToTextTable(logAction.titles, logAction.data));
            } else {
                logger.error(logAction.message);
            }
            return true;
        } else if (action instanceof MessageClientAction) {
            MessageClientAction msgAction = (MessageClientAction) action;
            systemLogger.info("Server message: " + msgAction.message);
            return true;
        }
        return false;
    }

    private String errorDataToTextTable(List<String> titles, List<List<String>> data) {
        if (titles.size() == 0) {
            return "";
        }

        int rCount = data.size() + 1;
        int cCount = titles.size();

        ArrayList<List<String>> all = new ArrayList<List<String>>();
        all.add(titles);
        all.addAll(data);

        int columnWidths[] = new int[cCount];
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                columnWidths[j] = Math.max(columnWidths[j], cellText == null ? 0 : cellText.trim().length());
            }
        }

        int tableWidth = cCount + 1; //рамки
        for (int j = 0; j < cCount; ++j) {
            tableWidth += columnWidths[j];
        }

        String br = replicate('-', tableWidth) + "\n";

        StringBuilder result = new StringBuilder(br);
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            result.append("|");
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                result.append(padLeft(cellText, columnWidths[j])).append("|");
            }
            result.append("\n");
            if (i == 0) {
                result.append(br);
            }
        }
        result.append(br);

        return result.toString();
    }
}
