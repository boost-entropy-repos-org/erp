package lsfusion.erp.region.by.machinery.board.fiscalboard;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Iterator;

public class FiscalBoardPostPrintActionProperty extends FiscalBoardActionProperty {
    private final ClassPropertyInterface receiptInterface;

    public FiscalBoardPostPrintActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        DataObject receiptObject = context.getDataKeyValue(receiptInterface);

        try {
            Integer comPortBoard = (Integer) findProperty("comPortBoardCurrentCashRegister[]").read(context);
            Integer baudRateBoard = (Integer) findProperty("baudRateBoardCurrentCashRegister[]").read(context);
            boolean uppercase = findProperty("uppercaseBoardCurrentCashRegister[]").read(context) != null;

            BigDecimal sum = (BigDecimal) findProperty("sumPayment[Receipt]").read(context, (DataObject) receiptObject);
            BigDecimal change = (BigDecimal) findProperty("changePayment[Receipt]").read(context, receiptObject);

            String[] lines = generateText(sum, change);
            context.requestUserInteraction(new FiscalBoardDisplayTextClientAction(lines[0], lines[1], baudRateBoard, comPortBoard, uppercase, null));

        } catch (SQLException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private String[] generateText(BigDecimal sum, BigDecimal change) {
        String firstLine = "ПОЛУЧЕНО:" + fillSpaces(toStr(sum), lineLength - 9);
        String secondLine = "СДАЧА:" + fillSpaces(toStr(change), lineLength - 6);
        return new String[]{firstLine, secondLine};
    }
}