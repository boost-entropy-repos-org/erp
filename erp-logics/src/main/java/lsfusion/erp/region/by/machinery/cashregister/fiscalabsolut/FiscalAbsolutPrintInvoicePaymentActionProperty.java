package lsfusion.erp.region.by.machinery.cashregister.fiscalabsolut;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Iterator;

public class FiscalAbsolutPrintInvoicePaymentActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface invoiceInterface;
    private final ClassPropertyInterface paymentInterface;

    public FiscalAbsolutPrintInvoicePaymentActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingModuleErrorLog.SemanticError {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        invoiceInterface = i.next();
        paymentInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            DataObject invoiceObject = context.getDataKeyValue(invoiceInterface);
            DataObject paymentObject = context.getDataKeyValue(paymentInterface);

            String logPath = (String) findProperty("logPathCurrentCashRegister[]").read(context);
            Integer comPort = (Integer) findProperty("comPortCurrentCashRegister[]").read(context);
            Integer baudRate = (Integer) findProperty("baudRateCurrentCashRegister[]").read(context);
            Integer placeNumber = (Integer) findProperty("nppMachineryCurrentCashRegister[]").read(context);
            BigDecimal maxSum = (BigDecimal) findProperty("maxSumCurrentCashRegister[]").read(context);

            BigDecimal sumPayment = (BigDecimal) findProperty("sum[Payment.Payment]").read(context, paymentObject);
            Integer typePayment = (Integer) findProperty("fiscalType[Payment.Payment]").read(context, paymentObject);
            boolean saveCommentOnFiscalTape = findProperty("saveCommentOnFiscalTapeAbsolut[]").read(context) != null;

            if (sumPayment != null && typePayment != null) {
                if (maxSum != null && sumPayment.compareTo(maxSum) > 0) {
                    context.requestUserInteraction(new MessageClientAction("Сумма платежа превышает " + maxSum.intValue() + " рублей", "Ошибка!"));
                    return;
                }
            }
            
            Object result = context.requestUserInteraction(new FiscalAbsolutPrintInvoicePaymentClientAction(logPath, comPort, baudRate, placeNumber, null, sumPayment, typePayment, true, saveCommentOnFiscalTape));
            if(result != null)
                ServerLoggers.systemLogger.error("FiscalAbsolutPrintInvoicePayment Error: " + result);
            findProperty("printReceiptResult[]").change(result == null ? new DataObject(true) : NullValue.instance, context);
            
        } catch (SQLException | ScriptingModuleErrorLog.SemanticError e) {
            throw Throwables.propagate(e);
        }


    }
}
