package lsfusion.erp.region.ru.masterdata;

import com.google.common.base.Throwables;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.physics.dev.integration.service.*;
import lsfusion.server.logics.action.session.DataSession;
import org.apache.commons.lang3.time.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ImportCBRFExchangeRateAction extends InternalAction {
    private final ClassPropertyInterface currencyInterface;

    public ImportCBRFExchangeRateAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        currencyInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject currencyObject = context.getDataKeyValue(currencyInterface);

            String extraSIDCurrency = (String) findProperty("extraSID[Currency]").read(context, currencyObject);
            Date cbrfDateFrom = (Date) findProperty("importCBRFExchangeRateDateFrom[]").read(context);
            Date cbrfDateTo = (Date) findProperty("importCBRFExchangeRateDateTo[]").read(context);

            if (cbrfDateFrom != null && cbrfDateTo != null && extraSIDCurrency != null)
                importExchanges(cbrfDateFrom, cbrfDateTo, extraSIDCurrency, context);

        } catch (IOException | JDOMException | ParseException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    private void importExchanges(Date dateFrom, Date dateTo, String extraSIDCurrency, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, IOException, JDOMException, SQLException, ParseException, SQLHandledException {


        List<Exchange> exchangesList = importExchangesFromXML(dateFrom, dateTo, extraSIDCurrency, context);

        if (exchangesList != null) {

            ImportField typeExchangeRUField = new ImportField(findProperty("name[TypeExchange]"));
            ImportField typeExchangeForeignField = new ImportField(findProperty("name[TypeExchange]"));
            ImportField currencyField = new ImportField(findProperty("shortName[Currency]"));
            ImportField homeCurrencyField = new ImportField(findProperty("shortName[Currency]"));
            ImportField rateField = new ImportField(findProperty("rate[TypeExchange,Currency,DATE]"));
            ImportField foreignRateField = new ImportField(findProperty("rate[TypeExchange,Currency,DATE]"));
            ImportField dateField = new ImportField(DateClass.instance);

            ImportKey<?> typeExchangeRUKey = new ImportKey((ConcreteCustomClass) findClass("TypeExchange"),
                    findProperty("typeExchange[ISTRING[50]]").getMapping(typeExchangeRUField));

            ImportKey<?> typeExchangeForeignKey = new ImportKey((ConcreteCustomClass) findClass("TypeExchange"),
                    findProperty("typeExchange[ISTRING[50]]").getMapping(typeExchangeForeignField));

            ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) findClass("Currency"),
                    findProperty("currencyShortName[BPSTRING[3]]").getMapping(currencyField));

            ImportKey<?> homeCurrencyKey = new ImportKey((ConcreteCustomClass) findClass("Currency"),
                    findProperty("currencyShortName[BPSTRING[3]]").getMapping(homeCurrencyField));

            List<ImportProperty<?>> props = new ArrayList<>();

            props.add(new ImportProperty(typeExchangeRUField, findProperty("name[TypeExchange]").getMapping(typeExchangeRUKey)));
            props.add(new ImportProperty(homeCurrencyField, findProperty("currency[TypeExchange]").getMapping(typeExchangeRUKey),
                    object(findClass("Currency")).getMapping(homeCurrencyKey)));
            props.add(new ImportProperty(rateField, findProperty("rate[TypeExchange,Currency,DATE]").getMapping(typeExchangeRUKey, currencyKey, dateField)));

            props.add(new ImportProperty(typeExchangeForeignField, findProperty("name[TypeExchange]").getMapping(typeExchangeForeignKey)));
            props.add(new ImportProperty(currencyField, findProperty("currency[TypeExchange]").getMapping(typeExchangeForeignKey),
                    object(findClass("Currency")).getMapping(currencyKey)));
            props.add(new ImportProperty(foreignRateField, findProperty("rate[TypeExchange,Currency,DATE]").getMapping(typeExchangeForeignKey, homeCurrencyKey, dateField)));

            List<List<Object>> data = new ArrayList<>();
            for (Exchange e : exchangesList) {
                data.add(Arrays.asList((Object) "ЦБРФ (RUB)", "ЦБРФ (" + e.currencyID + ")", e.currencyID, e.homeCurrencyID, e.exchangeRate, new BigDecimal(1 / e.exchangeRate.doubleValue()), e.date));
            }
            ImportTable table = new ImportTable(Arrays.asList(typeExchangeRUField, typeExchangeForeignField, currencyField,
                    homeCurrencyField, rateField, foreignRateField, dateField), data);

            DataSession session = context.getSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(typeExchangeRUKey,
                    typeExchangeForeignKey, currencyKey, homeCurrencyKey), props);
            service.synchronize(true, false);
        }
    }

    private List<Exchange> importExchangesFromXML(Date dateFrom, Date dateTo, String extraSIDCurrency, ExecutionContext context) throws IOException, JDOMException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        SAXBuilder builder = new SAXBuilder();

        List<Exchange> exchangesList = new ArrayList<>();

        Document document = builder.build(new URL("http://www.cbr.ru/scripts/XML_val.asp?d=0").openStream());
        Element rootNode = document.getRootElement();
        List list = rootNode.getChildren("Item");

        for (Object aList : list) {

            Element node = (Element) aList;

            String id = node.getAttributeValue("ID");

            if (extraSIDCurrency.equals(id)) {
                Document exchangeDocument = builder.build(new URL("http://www.cbr.ru/scripts/XML_dynamic.asp?date_req1="
                        + new SimpleDateFormat("dd/MM/yyyy").format(dateFrom)
                        + "&date_req2=" + new SimpleDateFormat("dd/MM/yyyy").format(dateTo)
                        + "&VAL_NM_RQ=" + id).openStream());
                Element exchangeRootNode = exchangeDocument.getRootElement();
                List exchangeList = exchangeRootNode.getChildren("Record");

                String shortNameCurrency = (String) findProperty("shortName[Currency]").read(context, findProperty("currencyExtraSID[BPSTRING[6]]").readClasses(context, new DataObject(extraSIDCurrency)));

                for (Object anExchangeList : exchangeList) {

                    Element exchangeNode = (Element) anExchangeList;

                    BigDecimal value = new BigDecimal(Double.valueOf(exchangeNode.getChildText("Value").replace(",", ".")) / Double.valueOf(exchangeNode.getChildText("Nominal")));

                    exchangesList.add(new Exchange(shortNameCurrency, "RUB",
                            new Date(DateUtils.parseDate(exchangeNode.getAttributeValue("Date"), new String[]{"dd.MM.yyyy"}).getTime()),
                            value));
                }
                if (exchangesList.size() > 0)
                    return exchangesList;
            }
        }
        return exchangesList;
    }


}