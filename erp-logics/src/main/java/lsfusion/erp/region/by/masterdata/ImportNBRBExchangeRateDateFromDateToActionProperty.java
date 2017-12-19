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
import java.util.Iterator;

public class ImportNBRBExchangeRateDateFromDateToActionProperty extends ImportNBRBExchangeRateActionProperty {
    private final ClassPropertyInterface currencyInterface;

    public ImportNBRBExchangeRateDateFromDateToActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        currencyInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject currencyObject = context.getDataKeyValue(currencyInterface);

            String shortNameCurrency = (String) findProperty("shortName[Currency]").read(context, currencyObject);
            Date nbrbDateFrom = (Date) findProperty("importNBRBExchangeRateDateFrom[]").read(context);
            Date nbrbDateTo = (Date) findProperty("importNBRBExchangeRateDateTo[]").read(context);


            if (nbrbDateFrom != null && nbrbDateTo != null && shortNameCurrency != null) {
                Date separationDate = new Date(2016 - 1900, 5, 30); //30.06.2016
                Date denominationDate = new Date(2016 - 1900, 6, 1); //01.07.2016
                if(nbrbDateFrom.getTime() <= separationDate.getTime() && nbrbDateTo.getTime() > separationDate.getTime()) {
                    importExchanges(nbrbDateFrom, separationDate, shortNameCurrency, context, true);
                    importExchanges(denominationDate, nbrbDateTo, shortNameCurrency, context, false);
                } else {
                    importExchanges(nbrbDateFrom, nbrbDateTo, shortNameCurrency, context,  nbrbDateFrom.getTime() < separationDate.getTime());
                }
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException | ParseException | JSONException e) {
            throw Throwables.propagate(e);
        }

    }
}