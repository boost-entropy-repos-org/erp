package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.convert.ClientActionToGwtConverter;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.actions.GAction;
import platform.interop.form.ServerResponse;

import java.io.IOException;

public abstract class ServerResponseActionHandler<A extends Action<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected ServerResponseActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(ServerResponse serverResponse) throws IOException {
        return getServerResponseResult(null, serverResponse);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) throws IOException {
        GAction[] resultActions;
        if (serverResponse.actions == null) {
            resultActions = null;
        } else {
            resultActions = new GAction[serverResponse.actions.length];
            for (int i = 0; i < serverResponse.actions.length; i++) {
                if (form != null) {
                    resultActions[i] = clientActionConverter.convertAction(serverResponse.actions[i], form, servlet);
                } else {
                    resultActions[i] = clientActionConverter.convertAction(serverResponse.actions[i], servlet);
                }
            }
        }

        return new ServerResponseResult(resultActions, serverResponse.resumeInvocation);
    }
}
