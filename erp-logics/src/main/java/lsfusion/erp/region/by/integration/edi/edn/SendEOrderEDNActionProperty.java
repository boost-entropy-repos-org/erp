package lsfusion.erp.region.by.integration.edi.edn;

import com.google.common.base.Throwables;
import lsfusion.erp.region.by.integration.edi.SendEOrderActionProperty;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;

import java.io.IOException;
import java.sql.SQLException;

public class SendEOrderEDNActionProperty extends SendEOrderActionProperty {

    public SendEOrderEDNActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            String login = (String) findProperty("loginEDN[]").read(context);
            String password = (String) findProperty("passwordEDN[]").read(context);
            String host = (String) findProperty("hostEDN[]").read(context);
            Integer port = (Integer) findProperty("portEDN[]").read(context);
            if (login != null && password != null && host != null && port != null) {
                String url = String.format("https://%s:%s/topby/DmcService?wsdl", host, port);
                sendEOrder(context, url, login, password, host, port);
            } else {
                ServerLoggers.importLogger.info("EDN SendEOrder: не заданы имя пользователя / пароль / хост / порт");
                context.delayUserInteraction(new MessageClientAction("EDN Заказ не выгружен: не заданы имя пользователя / пароль / хост / порт", "Экспорт"));
            }

        } catch (ScriptingErrorLog.SemanticErrorException | IOException | JDOMException e) {
            throw Throwables.propagate(e);
        }
    }
}