package lsfusion.erp.region.by.masterdata;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.json.JSONException;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

public class ImportNBRBExchangeRateLastDaysActionProperty extends ImportNBRBExchangeRateActionProperty {
    private final ClassPropertyInterface currencyInterface;

    public ImportNBRBExchangeRateLastDaysActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        currencyInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject currencyObject = context.getDataKeyValue(currencyInterface);

            String shortNameCurrency = (String) findProperty("shortName[Currency]").read(context, currencyObject);
            long currentTime = Calendar.getInstance().getTimeInMillis();
            Integer days = (Integer) findProperty("importNBRBExchangeRateDaysCount[]").read(context);
            if (shortNameCurrency != null && days != null && days > 0) {
                Date separationDate = new Date(2016 - 1900, 5, 30); //30.06.2016
                Date denominationDate = new Date(2016 - 1900, 6, 1); //01.07.2016
                Date dateFrom = new Date(currentTime - (long) days * 24 * 3600 * 1000);
                Date dateTo = new Date(currentTime + (long) 24 * 3600 * 1000);
                if(dateFrom.getTime() <= separationDate.getTime() && dateTo.getTime() > separationDate.getTime()) {
                    importExchanges(dateFrom, separationDate, shortNameCurrency, context, true);
                    importExchanges(denominationDate, dateTo, shortNameCurrency, context, false);
                } else {
                    importExchanges(dateFrom, dateTo, shortNameCurrency, context, dateFrom.getTime() < separationDate.getTime());
                }

            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException | ParseException | JSONException e) {
            throw Throwables.propagate(e);
        }

    }
}