package platform.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.shared.actions.navigator.GenerateID;
import platform.gwt.form.shared.actions.navigator.GenerateIDResult;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;

public class GenerateIDHandler extends SimpleActionHandlerEx<GenerateID, GenerateIDResult, RemoteLogicsInterface> {
    public GenerateIDHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws DispatchException, IOException {
        return new GenerateIDResult(servlet.getLogics().generateID());
    }
}
