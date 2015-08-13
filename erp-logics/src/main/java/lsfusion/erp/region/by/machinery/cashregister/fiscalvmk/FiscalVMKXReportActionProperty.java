package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class FiscalVMKXReportActionProperty extends ScriptingActionProperty {

    public FiscalVMKXReportActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String ip = (String) findProperty("ipCurrentCashRegister").read(context.getSession());
            Integer comPort = (Integer) findProperty("comPortCurrentCashRegister").read(context);
            Integer baudRate = (Integer) findProperty("baudRateCurrentCashRegister").read(context);
            String fiscalVMKZReportTitle = (String) findProperty("fiscalVMKReceiptTitle").read(context);
            
            String result = (String) context.requestUserInteraction(new FiscalVMKCustomOperationClientAction(ip, comPort, baudRate, 1, fiscalVMKZReportTitle));
            if (result == null) {
                context.apply();
            }
            else
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
        } catch (SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
