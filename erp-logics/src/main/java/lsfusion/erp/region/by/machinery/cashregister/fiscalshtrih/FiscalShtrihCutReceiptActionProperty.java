package lsfusion.erp.region.by.machinery.cashregister.fiscalshtrih;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class FiscalShtrihCutReceiptActionProperty extends ScriptingActionProperty {

    public FiscalShtrihCutReceiptActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {

            Integer comPort = (Integer) getLCP("comPortCurrentCashRegister").read(context.getSession());
            Integer baudRate = (Integer) getLCP("baudRateCurrentCashRegister").read(context.getSession());
            Integer pass = (Integer) getLCP("operatorNumberCurrentCashRegisterCurrentUser").read(context.getSession());
            int password = pass == null ? 30000 : pass * 1000;

            String result = (String) context.requestUserInteraction(new FiscalShtrihCustomOperationClientAction(5, password, comPort, baudRate));
            if (result != null)
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }

    }
}
