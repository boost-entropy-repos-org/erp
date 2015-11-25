package lsfusion.erp.integration;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DefaultIntegrationActionProperty extends ScriptingActionProperty {

    public DefaultIntegrationActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public DefaultIntegrationActionProperty(ScriptingLogicsModule LM, ValueClass valueClass) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, valueClass);
    }

    public DefaultIntegrationActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    static final Locale RU_LOCALE = new Locale("ru");
    static final DateFormatSymbols RU_SYMBOLS = new DateFormatSymbols(RU_LOCALE);
    static final String[] RU_MONTHS = {"января", "февраля", "марта", "апреля", "мая",
            "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    static {
        RU_SYMBOLS.setMonths(RU_MONTHS);
    }

    protected static Date parseDate(String value) throws ParseException {
        return parseDate(value, null);    
    }
    
    protected static Date parseDate(String value, Date defaultValue) throws ParseException {
        if (value == null) return defaultValue;
        value = value.trim();
        if (value.isEmpty() || value.replace(".", "").trim().isEmpty()) return defaultValue;
        if (value.length() == 8 && !value.contains(".")) {
            try {
                //чит для отличия ddMMyyyy от yyyyMMdd
                Integer intValue = Integer.parseInt(value.substring(4, 6));
                if(intValue > 12)
                return new Date(DateUtils.parseDate(value, new String[]{"ddMMyyyy"}).getTime());
            } catch(Exception e) {
                return defaultValue;
            }            
        } 
        if (value.contains("г")) {
            //чит для даты с месяцем прописью
            return new Date(new SimpleDateFormat("dd MMMM yyyy г.", RU_SYMBOLS).parse(value.toLowerCase()).getTime());
        }
        switch (value.length()) {
            case 4:
                return new Date(DateUtils.parseDate(value, new String[]{"MMyy"}).getTime());
            case 5:
                return new Date(DateUtils.parseDate(value, new String[]{"MM.yy"}).getTime());
            case 6:
                return new Date(DateUtils.parseDate(value, new String[]{"MM,yy_", "d.MM.yy"}).getTime());
            case 7:
                return new Date(DateUtils.parseDate(value, new String[]{"MM.yyyy", "MM-yyyy"}).getTime());
            case 8:
                return new Date(DateUtils.parseDate(value, new String[]{"yyyyMMdd", "dd.MM.yy", "dd/MM/yy"}).getTime());
            case 10:
                return new Date(DateUtils.parseDate(value, new String[]{"dd.MM.yyyy", "dd/MM/yyyy", "yyyy-MM-dd"}).getTime());
            case 16:
                return new Date(DateUtils.parseDate(value, new String[]{"dd.MM.yyyy HH:mm"}).getTime());
            case 19:
                return new Date(DateUtils.parseDate(value, new String[]{"dd.MM.yyyy HH:mm:ss"}).getTime());
        }
        return new Date(DateUtils.parseDate(value, new String[]{"MM,yy_", "d.MM.yy", "MM.yyyy", "MM-yyyy", "MMyy", "MM.yy", "yyyyMMdd",
                "dd.MM.yy", "dd/MM/yy", "dd.MM.yyyy", "dd/MM/yyyy", "dd.MM.yyyy HH:mm", "dd.MM.yyyy HH:mm:ss"}).getTime());
    }

    protected BigDecimal safeAdd(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else return (operand1 == null ? operand2 : (operand2 == null ? operand1 : operand1.add(operand2)));
    }

    protected BigDecimal safeSubtract(int operand1, BigDecimal operand2) {
        return safeSubtract(BigDecimal.valueOf(operand1), operand2);
    }
    
    protected BigDecimal safeSubtract(BigDecimal operand1, int operand2) {
        return safeSubtract(operand1, BigDecimal.valueOf(operand2));
    }

    protected BigDecimal safeSubtract(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null && operand2 == null)
            return null;
        else
            return (operand1 == null ? operand2.negate() : (operand2 == null ? operand1 : operand1.subtract((operand2))));
    }

    protected BigDecimal safeMultiply(BigDecimal operand1, int operand2) {
        return safeMultiply(operand1, BigDecimal.valueOf(operand2));
    }

    protected BigDecimal safeMultiply(BigDecimal operand1, BigDecimal operand2) {
        if (operand1 == null || operand1.doubleValue() == 0 || operand2 == null || operand2.doubleValue() == 0)
            return null;
        else return operand1.multiply(operand2);
    }

    protected BigDecimal safeDivide(BigDecimal dividend, int quotient) {
        return safeDivide(dividend, BigDecimal.valueOf(quotient));
    }

    protected BigDecimal safeDivide(BigDecimal dividend, BigDecimal quotient) {
        return safeDivide(dividend, quotient, 3);
    }

    protected BigDecimal safeDivide(BigDecimal dividend, int quotient, int scale) {
        return safeDivide(dividend, BigDecimal.valueOf(quotient), scale);
    }
    
    protected BigDecimal safeDivide(BigDecimal dividend, BigDecimal quotient, int scale) {
        if (dividend == null || quotient == null || quotient.doubleValue() == 0)
            return null;
        return dividend.divide(quotient, scale, RoundingMode.HALF_UP);
    }

    protected BigDecimal safeNegate(BigDecimal operand) {
        return operand == null ? null : operand.negate();
    }

    protected String trim(String input) {
        return input == null ? null : input.trim();
    }

    protected String trim(String input, String defaultValue) {
        return input == null ? defaultValue : input.trim();
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }

    protected String nullIfEmpty(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
    
    protected boolean notNullNorEmpty(String value) {
        return value != null && !value.isEmpty();
    }
    
    protected boolean notNullNorEmpty(List value) {
        return value != null && !value.isEmpty();
    }

    protected String upper(String value) {
        return value == null ? null : value.toUpperCase();
    }

    protected void checkFileExistence(String filePath) {
        if (!(new File(filePath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + filePath + " не найден");
    }
}