package lsfusion.erp.region.by.euroopt;

import com.google.common.base.Throwables;
import lsfusion.erp.integration.DefaultImportActionProperty;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.silvertunnel_ng.netlib.adapter.url.NetlibURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;

public class EurooptActionProperty extends DefaultImportActionProperty {

    String mainPage = "https://e-dostavka.by";
    String itemGroupPattern = "https:\\/\\/e-dostavka\\.by\\/catalog\\/\\d{4}\\.html";
    String itemPattern = "https:\\/\\/e-dostavka\\.by\\/catalog\\/item_\\d+\\.html";
    String userAgent = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1667.0 Safari/537.36";

    String logPrefix = "Import Euroopt: ";

    public EurooptActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected NetLayer getNetLayer() throws IOException {
        NetLayer lowerNetLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR);
        // wait until TOR is ready (optional):
        lowerNetLayer.waitUntilReady();
        return lowerNetLayer;
    }

    protected Document getDocument(NetLayer lowerNetLayer, String url) throws IOException {
        int count = 3;
        while (count > 0) {
            try {
                Thread.sleep(50);
                if (lowerNetLayer == null) {
                    Connection connection = Jsoup.connect(url);
                    connection.timeout(10000);
                    connection.userAgent(userAgent);
                    return connection.get();
                } else {
                    URLConnection urlConnection = getTorConnection(lowerNetLayer, url);
                    try (InputStream responseBodyIS = urlConnection.getInputStream()) {
                        return Jsoup.parse(responseBodyIS, "utf-8", "");
                    }
                }
            } catch (HttpStatusException e) {
                count--;
                if (count <= 0)
                    ServerLoggers.importLogger.error(logPrefix + "error for url " + url + ": ", e);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }
        return null;
    }

    protected URLConnection getTorConnection(NetLayer lowerNetLayer, String url) throws IOException {
        // prepare URL handling on top of the lowerNetLayer
        NetlibURLStreamHandlerFactory factory = new NetlibURLStreamHandlerFactory(false);
        // the following method could be called multiple times
        // to change layer used by the factory over the time:
        factory.setNetLayerForHttpHttpsFtp(lowerNetLayer);

        // send request with POST data
        URLConnection urlConnection = new URL(null, mainPage + url, factory.createURLStreamHandler("https")).openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.connect();
        return urlConnection;
    }
}