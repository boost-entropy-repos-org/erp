package lsfusion.erp.integration;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

public class DefaultExportTXTActionProperty extends DefaultExportActionProperty {

    protected final int LEFT = 0;
    protected final int RIGHT = 1;
    protected final int CENTER = 2;
    
    public DefaultExportTXTActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public DefaultExportTXTActionProperty(ScriptingLogicsModule LM, ValueClass valueClass) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, valueClass);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected String fillSymbolsLine(char c, int length) {
        return fillSymbolsLine("", c, length);
    }

    protected static String fillSymbolsLine(String postfix, char c, int length) {
        postfix = postfix == null ? "" : postfix;
        while (postfix.length() < length)
            postfix = c + postfix;
        return postfix;
    }
    
    protected String formatString(String input, String encoding, int length, int position) throws UnsupportedEncodingException {
        return formatString(input, encoding, length, position, false);
    }

    protected String formatString(String input, String encoding, int length, int position, boolean newLine) throws UnsupportedEncodingException {
        input = input == null ? "" : input.substring(0, Math.min(input.length(), length));
        while (input.length() < length) {
            switch (position) {
                case LEFT:
                    input = input + " ";
                    break;
                case RIGHT:
                    input = " " + input;
                    break;
                case CENTER:
                    input = " " + input;
                    if (input.length() < length)
                        input = input + " ";
                    break;
            }
        }
        return new String((input + (newLine ? "\n" : "")).getBytes(encoding), encoding);
    }
}