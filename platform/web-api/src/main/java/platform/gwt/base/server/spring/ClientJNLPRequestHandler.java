package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.HttpRequestHandler;
import platform.base.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

public class ClientJNLPRequestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(ClientJNLPRequestHandler.class);

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        try {
            StringBuffer requestURL = request.getRequestURL();
            String codebaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/"));
            handleJNLPRequest(request, response, codebaseUrl, "client.jnlp", "lsFusion Client", request.getServerName(), blProvider.getRegistryPort(), blProvider.getExportName());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration can't be found.");
        }
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String registryHost,
                                     int registryPort,
                                     String exportName) throws ServletException, IOException {
        logger.debug("Generating jnlp response.");

        try {
            Properties properties = new Properties();
            properties.put("jnlp.codebase", codebaseUrl);
            properties.put("jnlp.url", jnlpUrl);
            properties.put("jnlp.appName", appName);
            properties.put("jnlp.registryHost", registryHost);
            properties.put("jnlp.registryPort", String.valueOf(registryPort));
            properties.put("jnlp.exportName", exportName);

            String content = stringResolver.replacePlaceholders(
                    IOUtils.readStreamToString(getClass().getResourceAsStream("/client.jnlp")), properties
            );

            response.setContentType("application/x-java-jnlp-file");
            response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration can't be found.");
        }
    }
}
