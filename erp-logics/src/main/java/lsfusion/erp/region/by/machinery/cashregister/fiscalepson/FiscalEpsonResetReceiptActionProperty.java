package lsfusion.erp.region.by.machinery.cashregister.fiscalepson;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Iterator;

public class FiscalEpsonResetReceiptActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface receiptInterface;

    public FiscalEpsonResetReceiptActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject receiptObject = context.getDataKeyValue(receiptInterface);

            boolean skipReceipt = findProperty("fiscalSkip[Receipt]").read(context.getSession(), receiptObject) != null;
            if (!skipReceipt) {
                Integer comPort = (Integer) findProperty("comPortCurrentCashRegister[]").read(context.getSession());
                Integer baudRate = (Integer) findProperty("baudRateCurrentCashRegister[]").read(context.getSession());

                Integer numberReceipt = (Integer) findProperty("number[Receipt]").read(context.getSession(), receiptObject);
                BigDecimal totalSum = (BigDecimal) findProperty("sumReceiptDetail[Receipt]").read(context.getSession(), receiptObject);
                BigDecimal sumCash = (BigDecimal) findProperty("sumCashPayment[Receipt]").read(context.getSession(), receiptObject);
                BigDecimal sumCard = (BigDecimal) findProperty("sumCardPayment[Receipt]").read(context.getSession(), receiptObject);
                ScriptingLogicsModule giftCardLM = context.getBL().getModule("GiftCard");
                BigDecimal sumGiftCard = giftCardLM == null ? null : (BigDecimal) giftCardLM.findProperty("sumGiftCardPayment[Receipt]").read(context.getSession(), receiptObject);

                String result = (String) context.requestUserInteraction(new FiscalEpsonResetReceiptClientAction(comPort, baudRate, numberReceipt, totalSum, sumCash, sumCard, sumGiftCard, totalSum.doubleValue() > 0));
                if (result == null) {
                    findProperty("resetted[Receipt]").change(true, context, receiptObject);
                    if (!context.apply())
                        context.requestUserInteraction(new MessageClientAction("Ошибка при аннулировании чека", "Ошибка"));
                } else {
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
                }
            }
        } catch (SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}