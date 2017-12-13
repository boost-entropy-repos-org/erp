package lsfusion.erp.region.by.integration.edi;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;
import lsfusion.server.session.DataSession;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.xmlbeans.impl.util.Base64;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReceiveMessagesActionProperty extends EDIActionProperty {

    public ReceiveMessagesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    protected void receiveMessages(ExecutionContext context, String url, String login, String password, String host, int port, String provider, String archiveDir, boolean disableConfirmation, boolean sendReplies, boolean invoices)
            throws IOException, ScriptingModuleErrorLog.SemanticError, SQLHandledException, SQLException {
        if (context.getDbManager().isServer()) {
            Element rootElement = new Element("Envelope", soapenvNamespace);
            rootElement.setNamespace(soapenvNamespace);
            rootElement.addNamespaceDeclaration(soapenvNamespace);
            rootElement.addNamespaceDeclaration(topNamespace);

            Document doc = new Document(rootElement);
            doc.setRootElement(rootElement);

            //parent: rootElement
            Element headerElement = new Element("Header", soapenvNamespace);
            rootElement.addContent(headerElement);

            //parent: rootElement
            Element bodyElement = new Element("Body", soapenvNamespace);
            rootElement.addContent(bodyElement);

            //parent: bodyElement
            Element sendDocumentElement = new Element("GetDocuments", topNamespace);
            bodyElement.addContent(sendDocumentElement);

            addStringElement(topNamespace, sendDocumentElement, "username", login);
            addStringElement(topNamespace, sendDocumentElement, "password", password);

            String xml = new XMLOutputter().outputString(doc);
            HttpResponse httpResponse = sendRequest(host, port, login, password, url, xml, null);
            ServerLoggers.importLogger.info(provider + " ReceiveMessages request sent");
            String responseMessage = getResponseMessage(httpResponse);
            try {
                RequestResult requestResult = getRequestResult(httpResponse, responseMessage, "ReceiveMessages");
                switch (requestResult) {
                    case OK:
                        importMessages(context, url, login, password, host, port, provider, responseMessage, archiveDir, disableConfirmation, sendReplies, invoices);
                        break;
                    case AUTHORISATION_ERROR:
                        ServerLoggers.importLogger.error(provider + " ReceiveMessages: invalid login-password");
                        context.delayUserInteraction(new MessageClientAction(provider + " Сообщения не получены: ошибка авторизации", "Импорт"));
                        break;
                    case UNKNOWN_ERROR:
                        ServerLoggers.importLogger.error(provider + " ReceiveMessages: unknown error");
                        context.delayUserInteraction(new MessageClientAction(provider + " Сообщения не получены: неизвестная ошибка", "Импорт"));
                }
            } catch (JDOMException e) {
                ServerLoggers.importLogger.error(provider + " ReceiveMessages: invalid response", e);
                context.delayUserInteraction(new MessageClientAction(provider + " Сообщения не получены: некорректный ответ сервера", "Импорт"));
            }
        } else {
            context.delayUserInteraction(new MessageClientAction(provider + " Receive Messages disabled, change serverComputer() to enable", "Импорт"));
        }
    }

    private void importMessages(ExecutionContext context, String url, String login, String password, String host, Integer port,
                                String provider, String responseMessage, String archiveDir, boolean disableConfirmation, boolean sendReplies, boolean invoices)
            throws IOException, ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException, JDOMException {
        Map<String, Pair<String, String>> succeededMap = new HashMap<>();
        Map<String, DocumentData> orderMessages = new HashMap<>();
        Map<String, DocumentData> orderResponses = new HashMap<>();
        Map<String, DocumentData> despatchAdvices = new HashMap<>();
        Map<String, DocumentData> eInvoices = new HashMap<>();
        Map<String, DocumentData> invoiceMessages = new HashMap<>();

        Document document = new SAXBuilder().build(new ByteArrayInputStream(responseMessage.getBytes("utf-8")));
        Element rootNode = document.getRootElement();
        Namespace ns = rootNode.getNamespace();
        boolean failed = false;
        if (ns != null) {
            Element body = rootNode.getChild("Body", ns);
            if (body != null) {
                Element response = body.getChild("GetDocumentsResponse", topNamespace);
                if (response != null) {
                    Element result = response.getChild("GetDocumentsResult", topNamespace);
                    if (result != null) {
                        String successful = result.getChildText("Succesful", topNamespace);
                        if (successful != null && Boolean.parseBoolean(successful)) {

                            Element dataElement = result.getChild("Data", topNamespace);
                            for (Object documentDataObject : dataElement.getChildren("DocumentData", topNamespace)) {
                                Element documentData = (Element) documentDataObject;

                                String subXML = new String(Base64.decode(documentData.getChildText("Data", topNamespace).getBytes()));
                                String documentType = documentData.getChildText("DocumentType", topNamespace).toLowerCase();
                                String documentId = documentData.getChildText("Id", topNamespace);

                                try {
                                    Element subXMLRootNode = new SAXBuilder().build(new ByteArrayInputStream(subXML.getBytes("utf-8"))).getRootElement();
                                    switch (documentType) {
                                        case "systemmessage":
                                            if (!invoices)
                                                orderMessages.put(documentId, parseOrderMessage(subXMLRootNode, provider, documentId));
                                            break;
                                        case "ordrsp":
                                            if (!invoices)
                                                orderResponses.put(documentId, parseOrderResponse(subXMLRootNode, context, url, login, password,
                                                        host, port, provider, documentId, sendReplies, disableConfirmation));
                                            break;
                                        case "desadv":
                                            if (!invoices)
                                                despatchAdvices.put(documentId, parseDespatchAdvice(subXMLRootNode, context, url, login, password,
                                                        host, port, provider, documentId, sendReplies, disableConfirmation));
                                            break;
                                        case "blrwbl":
                                            if (invoices)
                                                eInvoices.put(documentId, parseEInvoice(subXMLRootNode));
                                            break;
                                        case "blrapn":
                                            if (invoices)
                                                invoiceMessages.put(documentId, parseInvoiceMessage(context, subXMLRootNode, provider, documentId));
                                            break;
                                    }
                                } catch (JDOMException | ParseException e) {
                                    ServerLoggers.importLogger.error(String.format("%s Parse Message %s error: ", provider, documentId), e);
                                }

                                if (archiveDir != null) {
                                    try {
                                        FileUtils.writeStringToFile(new File(archiveDir + "/" + documentId), subXML);
                                    } catch (Exception e) {
                                        ServerLoggers.importLogger.error("Archive file error: ", e);
                                    }
                                }
                            }
                        } else {
                            String message = result.getChildText("Message", topNamespace);
                            String errorCode = result.getChildText("ErrorCode", topNamespace);
                            context.delayUserInteraction(new MessageClientAction(String.format("Error %s: %s", errorCode, message), "Ошибка"));
                            failed = true;
                        }
                    }
                }
            }
        }

        if (!failed) {

            int orderMessagesSucceeded = 0;
            int orderMessagesFailed = 0;
            for (Map.Entry<String, DocumentData> message : orderMessages.entrySet()) {
                String documentId = message.getKey();
                DocumentData data = message.getValue();
                if (data.firstData != null) {
                    String error = importOrderMessages(context, data);
                    succeededMap.put(documentId, Pair.create(data.documentNumber, error));
                    if (error == null) {
                        ServerLoggers.importLogger.info(String.format("%s Import EOrderMessage %s succeeded", provider, documentId));
                        orderMessagesSucceeded++;
                    } else {
                        ServerLoggers.importLogger.error(String.format("%s Import EOrderMessage %s failed: %s", provider, documentId, error));
                        orderMessagesFailed++;
                    }
                } else {
                    succeededMap.put(documentId, Pair.create(data.documentNumber, String.format("%s Parsing EOrderMessage %s failed", provider, documentId)));
                    orderMessagesFailed++;
                }
            }

            int responsesSucceeded = 0;
            int responsesFailed = 0;
            for (Map.Entry<String, DocumentData> orderResponse : orderResponses.entrySet()) {
                String documentId = orderResponse.getKey();
                DocumentData data = orderResponse.getValue();
                String error = importOrderResponses(context, data);
                succeededMap.put(documentId, Pair.create(data.documentNumber, error));
                if (error == null) {
                    ServerLoggers.importLogger.info(String.format("%s Import EOrderResponse %s succeeded", provider, documentId));
                    responsesSucceeded++;
                } else {
                    ServerLoggers.importLogger.error(String.format("%s Import EOrderResponse %s failed: %s", provider, documentId, error));
                    responsesFailed++;
                }
            }

            int despatchAdvicesSucceeded = 0;
            int despatchAdvicesFailed = 0;
            for (Map.Entry<String, DocumentData> despatchAdvice : despatchAdvices.entrySet()) {
                String documentId = despatchAdvice.getKey();
                DocumentData data = despatchAdvice.getValue();
                String error = importDespatchAdvices(context, data);
                succeededMap.put(documentId, Pair.create(data.documentNumber, error));
                if (error == null) {
                    ServerLoggers.importLogger.info(String.format("%s Import EOrderDespatchAdvice %s succeeded", provider, documentId));
                    despatchAdvicesSucceeded++;
                } else {
                    ServerLoggers.importLogger.error(String.format("%s Import EOrderDespatchAdvice %s failed: %s", provider, documentId, error));
                    despatchAdvicesFailed++;
                }
            }

            int eInvoicesSucceeded = 0;
            int eInvoicesFailed = 0;
            for (Map.Entry<String, DocumentData> eInvoice : eInvoices.entrySet()) {
                String documentId = eInvoice.getKey();
                DocumentData data = eInvoice.getValue();
                String error = importEInvoices(context, data);
                succeededMap.put(documentId, Pair.create(data.documentNumber, error));
                if (error == null) {
                    ServerLoggers.importLogger.info(String.format("%s Import EInvoice %s succeeded", provider, documentId));
                    eInvoicesSucceeded++;
                } else {
                    ServerLoggers.importLogger.error(String.format("%s Import EInvoice %s failed: %s", provider, documentId, error));
                    eInvoicesFailed++;
                }
            }

            int invoiceMessagesSucceeded = 0;
            int invoiceMessagesFailed = 0;
            for (Map.Entry<String, DocumentData> message : invoiceMessages.entrySet()) {
                String documentId = message.getKey();
                DocumentData data = message.getValue();
                if (data.firstData != null) {
                    String error = importInvoiceMessages(context, data);
                    succeededMap.put(documentId, Pair.create(data.documentNumber, error));
                    if (error == null) {
                        ServerLoggers.importLogger.info(String.format("%s Import EInvoiceMessage %s succeeded", provider, documentId));
                        invoiceMessagesSucceeded++;
                    } else {
                        ServerLoggers.importLogger.error(String.format("%s Import EInvoiceMessage %s failed: %s", provider, documentId, error));
                        invoiceMessagesFailed++;
                    }
                } else {
                    succeededMap.put(documentId, Pair.create(data.documentNumber, String.format("%s Parsing EInvoiceMessage %s failed", provider, documentId)));
                    invoiceMessagesFailed++;
                }
            }

            String message = "";

            if (orderMessagesSucceeded > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Загружено сообщений по заказам: %s", orderMessagesSucceeded);
            if (orderMessagesFailed > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Не загружено сообщений по заказам: %s", orderMessagesFailed);

            if (responsesSucceeded > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Загружено ответов по заказам: %s", responsesSucceeded);
            if (responsesFailed > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Не загружено ответов по заказам: %s", responsesFailed);

            if (despatchAdvicesSucceeded > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Загружено уведомлений об отгрузке: %s", despatchAdvicesSucceeded);
            if (despatchAdvicesFailed > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Не загружено уведомлений об отгрузке: %s", despatchAdvicesFailed);

            if (eInvoicesSucceeded > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Загружено электронных накладных: %s", eInvoicesSucceeded);
            if (eInvoicesFailed > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Не загружено электронных накладных: %s", eInvoicesFailed);

            if (invoiceMessagesSucceeded > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Загружено сообщений по накладным: %s", invoiceMessagesSucceeded);
            if (invoiceMessagesFailed > 0)
                message += (message.isEmpty() ? "" : "\n") + String.format("Не загружено сообщений по накладным: %s", invoiceMessagesFailed);

            boolean succeeded = true;
            if (succeededMap.isEmpty())
                message += (message.isEmpty() ? "" : "\n") + "Не найдено новых сообщений";
            else if (!disableConfirmation) {

                for (Map.Entry<String, Pair<String, String>> succeededEntry : succeededMap.entrySet()) {
                    String documentId = succeededEntry.getKey();
                    Pair<String, String> documentNumberError = succeededEntry.getValue();
                    String documentNumber = documentNumberError.first;
                    String error = documentNumberError.second;
                    confirmDocumentReceived(context, documentId, url, login, password, host, port, provider);
                    if (error != null && sendReplies)
                        succeeded = succeeded && sendRecipientError(context, url, login, password, host, port, provider, documentId, documentNumber, error);
                }

            }

            if (succeeded)
                context.delayUserInteraction(new MessageClientAction(message, "Импорт"));
        }
    }

    private DocumentData parseOrderMessage(Element rootNode, String provider, String documentId) throws IOException, JDOMException {

        String documentNumber = rootNode.getChildText("documentNumber");
        Timestamp dateTime = parseTimestamp(rootNode.getChildText("documentDate"));

        Element reference = rootNode.getChild("reference");
        if (reference != null) {
            String documentType = reference.getChildText("documentType");
            if (documentType != null && documentType.equals("ORDERS")) {
                String orderNumber = reference.getChildText("documentNumber");
                String code = reference.getChildText("code");
                String description = reference.getChildText("description");
                if (description == null || description.isEmpty()) {
                    switch (code) {
                        case "1251":
                            description = "Сообщение прочитано получателем";
                            break;
                        case "1252":
                            description = "Сообщение принято учётной системой получателя";
                            break;
                        default:
                            description = null;
                            break;
                    }
                }
                return new DocumentData(documentNumber, Collections.singletonList(Arrays.asList((Object) documentNumber, dateTime, code, description, orderNumber)), null);
            } else
                ServerLoggers.importLogger.error(String.format("%s Parse Order Message %s error: incorrect documentType %s", provider, documentId, documentType));
        } else
            ServerLoggers.importLogger.error(String.format("%s Parse Order Message %s error: no reference tag", provider, documentId));
        return new DocumentData(documentNumber, null, null);
    }

    private String importOrderMessages(ExecutionContext context, DocumentData data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = null;
        List<List<Object>> importData = data == null ? null : data.firstData;
        if (importData != null && !importData.isEmpty()) {
            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField numberEOrderMessage = new ImportField(findProperty("number[EOrderMessage]"));
            ImportKey<?> eOrderMessageKey = new ImportKey((CustomClass) findClass("EOrderMessage"),
                    findProperty("eOrderMessage[VARSTRING[24]]").getMapping(numberEOrderMessage));
            keys.add(eOrderMessageKey);
            props.add(new ImportProperty(numberEOrderMessage, findProperty("number[EOrderMessage]").getMapping(eOrderMessageKey)));
            fields.add(numberEOrderMessage);

            ImportField dateTimeEOrderMessage = new ImportField(findProperty("dateTime[EOrderMessage]"));
            props.add(new ImportProperty(dateTimeEOrderMessage, findProperty("dateTime[EOrderMessage]").getMapping(eOrderMessageKey)));
            fields.add(dateTimeEOrderMessage);

            ImportField codeEOrderMessage = new ImportField(findProperty("code[EOrderMessage]"));
            props.add(new ImportProperty(codeEOrderMessage, findProperty("code[EOrderMessage]").getMapping(eOrderMessageKey)));
            fields.add(codeEOrderMessage);

            ImportField descriptionEOrderMessage = new ImportField(findProperty("description[EOrderMessage]"));
            props.add(new ImportProperty(descriptionEOrderMessage, findProperty("description[EOrderMessage]").getMapping(eOrderMessageKey)));
            fields.add(descriptionEOrderMessage);

            ImportField numberEOrderField = new ImportField(findProperty("number[EOrder]"));
            ImportKey<?> eOrderKey = new ImportKey((CustomClass) findClass("EOrder"),
                    findProperty("eOrder[VARSTRING[28]]").getMapping(numberEOrderField));
            eOrderKey.skipKey = true;
            keys.add(eOrderKey);
            props.add(new ImportProperty(numberEOrderField, findProperty("eOrder[EOrderMessage]").getMapping(eOrderMessageKey),
                    object(findClass("EOrder")).getMapping(eOrderKey)));
            fields.add(numberEOrderField);

            ImportTable table = new ImportTable(fields, importData);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("EDI_OM");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                message = session.applyMessage(context);
                session.popVolatileStats();
            } catch (Exception e) {
                ServerLoggers.importLogger.error("ImportOrderMessages Error: ", e);
                message = e.getMessage();
            }
        }
        return message;
    }

    private DocumentData parseOrderResponse(Element rootNode, ExecutionContext context, String url, String login, String password, String host,
                                            Integer port, String provider, String documentId, boolean sendReplies, boolean disableConfirmation) throws IOException, JDOMException, ScriptingModuleErrorLog.SemanticError, SQLHandledException, SQLException {
        List<List<Object>> firstData = new ArrayList<>();
        List<List<Object>> secondData = new ArrayList<>();

        String documentNumber = rootNode.getChildText("documentNumber");
        Timestamp dateTime = parseTimestamp(rootNode.getChildText("documentDate"));
        String responseType = rootNode.getChildText("function");
        String responseTypeObject = getResponseType(responseType);
        String buyerGLN = rootNode.getChildText("buyerGLN");
        String destinationGLN = rootNode.getChildText("destinationGLN");
        String supplierGLN = rootNode.getChildText("supplierGLN");
        String orderNumber = rootNode.getChildText("orderNumber");
        Timestamp deliveryDateTimeFirst = parseTimestamp(rootNode.getChildText("deliveryDateTimeFirst"));
        Timestamp deliveryDateTimeSecond = parseTimestamp(rootNode.getChildText("deliveryDateTimeSecond"));
        String note = rootNode.getChildText("comment");

        Map<String, String> orderBarcodesMap = getOrderBarcodesMap(context, url, login, password, host, port, provider, documentId,
                documentNumber, orderNumber, sendReplies, disableConfirmation);

        for (Object line : rootNode.getChildren("line")) {
            Element lineElement = (Element) line;
            String dataGTIN = trim(lineElement.getChildText("GTIN"));
            String GTIN;
            String barcode;
            if (orderBarcodesMap.containsKey(dataGTIN)) {
                barcode = orderBarcodesMap.get(dataGTIN);
                GTIN = null;
            } else {
                barcode = null;
                GTIN = dataGTIN;
            }
            String id = supplierGLN + "/" + documentNumber;
            String idDetail = id + "/" + dataGTIN;
            String action = lineElement.getChildText("action");
            String actionObject = getAction(action);
            BigDecimal quantityOrdered = parseBigDecimal(lineElement.getChildText("quantityOrdered"));
            BigDecimal quantityAccepted = parseBigDecimal(lineElement.getChildText("quantityAccepted"));
            BigDecimal price = parseBigDecimal(lineElement.getChildText("priceElement"));
            BigDecimal sumNoNDS = parseBigDecimal(lineElement.getChildText("priceNoNDS"));
            BigDecimal sumNDS = parseBigDecimal(lineElement.getChildText("priceNDS"));

            if (barcode != null)
                firstData.add(Arrays.<Object>asList(id, documentNumber, dateTime, responseTypeObject, note, supplierGLN, buyerGLN, destinationGLN, orderNumber,
                        deliveryDateTimeFirst, deliveryDateTimeSecond, idDetail, barcode, dataGTIN, actionObject, quantityOrdered, quantityAccepted, price,
                        sumNoNDS, sumNDS));
            else
                secondData.add(Arrays.<Object>asList(id, documentNumber, dateTime, responseTypeObject, note, supplierGLN, buyerGLN, destinationGLN, orderNumber,
                        deliveryDateTimeFirst, deliveryDateTimeSecond, idDetail, GTIN, dataGTIN, actionObject, quantityOrdered, quantityAccepted, price,
                        sumNoNDS, sumNDS));
        }
        return new DocumentData(documentNumber, firstData, secondData);
    }

    private String getResponseType(String id) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String value = id == null ? null : id.equals("4") ? "changed" : id.equals("27") ? "cancelled" : id.equals("29") ? "accepted" : null;
        return value == null ? null : ("EDI_EOrderResponseType." + value.toLowerCase());
    }

    private String getAction(String id) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String value = id == null ? null : id.equals("1") ? "added" : id.equals("3") ? "changed" : id.equals("5") ? "accepted" : id.equals("7") ? "cancelled" : null;
        return value == null ? null : ("EDI_EOrderResponseDetailAction." + value.toLowerCase());
    }

    private String importOrderResponses(ExecutionContext context, DocumentData data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = importOrderResponses(context, data, true);
        return message == null ? importOrderResponses(context, data, false) : message;
    }

    private String importOrderResponses(ExecutionContext context, DocumentData data, boolean first) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = null;
        List<List<Object>> importData = data == null ? null : (first ? data.firstData : data.secondData);
        if (importData != null && !importData.isEmpty()) {
            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField idEOrderResponseField = new ImportField(findProperty("id[EOrderResponse]"));
            ImportKey<?> eOrderResponseKey = new ImportKey((CustomClass) findClass("EOrderResponse"),
                    findProperty("eOrderResponse[VARSTRING[100]]").getMapping(idEOrderResponseField));
            keys.add(eOrderResponseKey);
            props.add(new ImportProperty(idEOrderResponseField, findProperty("id[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(idEOrderResponseField);

            ImportField numberEOrderResponseField = new ImportField(findProperty("number[EOrderResponse]"));
            props.add(new ImportProperty(numberEOrderResponseField, findProperty("number[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(numberEOrderResponseField);

            ImportField dateTimeEOrderResponseField = new ImportField(findProperty("dateTime[EOrderResponse]"));
            props.add(new ImportProperty(dateTimeEOrderResponseField, findProperty("dateTime[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(dateTimeEOrderResponseField);

            ImportField responseTypeField = new ImportField(findProperty("staticName[Object]"));
            ImportKey<?> responseTypeKey = new ImportKey((CustomClass) findClass("EOrderResponseType"),
                    findProperty("nameStatic[STRING[250]]").getMapping(responseTypeField));
            keys.add(responseTypeKey);
            props.add(new ImportProperty(responseTypeField, findProperty("responseType[EOrderResponse]").getMapping(eOrderResponseKey),
                    object(findClass("EOrderResponseType")).getMapping(responseTypeKey)));
            fields.add(responseTypeField);

            ImportField noteEOrderResponseField = new ImportField(findProperty("note[EOrderResponse]"));
            props.add(new ImportProperty(noteEOrderResponseField, findProperty("note[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(noteEOrderResponseField);

            ImportField GLNSupplierEOrderResponseField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> supplierKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityStockGLN[VARSTRING[13]]").getMapping(GLNSupplierEOrderResponseField));
            supplierKey.skipKey = true;
            keys.add(supplierKey);
            props.add(new ImportProperty(GLNSupplierEOrderResponseField, findProperty("supplier[EOrderResponse]").getMapping(eOrderResponseKey),
                    object(findClass("LegalEntity")).getMapping(supplierKey)));
            fields.add(GLNSupplierEOrderResponseField);

            ImportField GLNCustomerEOrderResponseField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> customerKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityGLN[VARSTRING[13]]").getMapping(GLNCustomerEOrderResponseField));
            customerKey.skipKey = true;
            keys.add(customerKey);
            props.add(new ImportProperty(GLNCustomerEOrderResponseField, findProperty("customer[EOrderResponse]").getMapping(eOrderResponseKey),
                    object(findClass("LegalEntity")).getMapping(customerKey)));
            fields.add(GLNCustomerEOrderResponseField);

            ImportField GLNCustomerStockEOrderResponseField = new ImportField(findProperty("GLN[Stock]"));
            ImportKey<?> customerStockKey = new ImportKey((CustomClass) findClass("Stock"),
                    findProperty("companyStockGLN[VARSTRING[13]]").getMapping(GLNCustomerStockEOrderResponseField));
            customerStockKey.skipKey = true;
            keys.add(customerStockKey);
            props.add(new ImportProperty(GLNCustomerStockEOrderResponseField, findProperty("customerStock[EOrderResponse]").getMapping(eOrderResponseKey),
                    object(findClass("Stock")).getMapping(customerStockKey)));
            fields.add(GLNCustomerStockEOrderResponseField);

            ImportField numberEOrderField = new ImportField(findProperty("number[EOrder]"));
            ImportKey<?> eOrderKey = new ImportKey((CustomClass) findClass("EOrder"),
                    findProperty("eOrder[VARSTRING[28]]").getMapping(numberEOrderField));
            eOrderKey.skipKey = true;
            keys.add(eOrderKey);
            props.add(new ImportProperty(numberEOrderField, findProperty("eOrder[EOrderResponse]").getMapping(eOrderResponseKey),
                    object(findClass("EOrder")).getMapping(eOrderKey)));
            fields.add(numberEOrderField);

            ImportField deliveryDateTimeEOrderResponseField = new ImportField(findProperty("deliveryDateTime[EOrderResponse]"));
            props.add(new ImportProperty(deliveryDateTimeEOrderResponseField, findProperty("deliveryDateTime[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(deliveryDateTimeEOrderResponseField);

            ImportField deliveryDateTimeSecondEOrderResponseField = new ImportField(findProperty("deliveryDateTimeSecond[EOrderResponse]"));
            props.add(new ImportProperty(deliveryDateTimeSecondEOrderResponseField, findProperty("deliveryDateTimeSecond[EOrderResponse]").getMapping(eOrderResponseKey)));
            fields.add(deliveryDateTimeSecondEOrderResponseField);

            ImportField idEOrderResponseDetailField = new ImportField(findProperty("id[EOrderResponseDetail]"));
            ImportKey<?> eOrderResponseDetailKey = new ImportKey((CustomClass) findClass("EOrderResponseDetail"),
                    findProperty("eOrderResponseDetail[VARSTRING[100]]").getMapping(idEOrderResponseDetailField));
            keys.add(eOrderResponseDetailKey);
            props.add(new ImportProperty(numberEOrderField, findProperty("orderResponse[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey),
                    object(findClass("EOrderResponse")).getMapping(eOrderResponseKey)));
            props.add(new ImportProperty(idEOrderResponseDetailField, findProperty("id[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(idEOrderResponseDetailField);

            if (first) {
                ImportField barcodeEOrderResponseDetailField = new ImportField(findProperty("id[Barcode]"));
                ImportKey<?> skuBarcodeKey = new ImportKey((CustomClass) findClass("Sku"),
                        findProperty("skuBarcode[VARSTRING[15]]").getMapping(barcodeEOrderResponseDetailField));
                skuBarcodeKey.skipKey = true;
                keys.add(skuBarcodeKey);
                props.add(new ImportProperty(barcodeEOrderResponseDetailField, findProperty("sku[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey),
                        object(findClass("Sku")).getMapping(skuBarcodeKey), true));
                fields.add(barcodeEOrderResponseDetailField);
            } else {
                ImportField GTINEOrderResponseDetailField = new ImportField(findProperty("id[Barcode]"));
                ImportKey<?> skuGTINKey = new ImportKey((CustomClass) findClass("Sku"),
                        findProperty("skuGTIN[VARSTRING[15]]").getMapping(GTINEOrderResponseDetailField));
                skuGTINKey.skipKey = true;
                keys.add(skuGTINKey);
                props.add(new ImportProperty(GTINEOrderResponseDetailField, findProperty("sku[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey),
                        object(findClass("Sku")).getMapping(skuGTINKey), true));
                fields.add(GTINEOrderResponseDetailField);
            }

            ImportField dataGTINEOrderResponseDetailField = new ImportField(findProperty("dataGTIN[EOrderResponseDetail]"));
            props.add(new ImportProperty(dataGTINEOrderResponseDetailField, findProperty("dataGTIN[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(dataGTINEOrderResponseDetailField);

            ImportField actionField = new ImportField(findProperty("staticName[Object]"));
            ImportKey<?> actionKey = new ImportKey((CustomClass) findClass("EOrderResponseDetailAction"),
                    findProperty("nameStatic[STRING[250]]").getMapping(actionField));
            keys.add(actionKey);
            props.add(new ImportProperty(actionField, findProperty("action[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey),
                    object(findClass("EOrderResponseDetailAction")).getMapping(actionKey)));
            fields.add(actionField);

            ImportField quantityOrderedEOrderResponseDetailField = new ImportField(findProperty("quantityOrdered[EOrderResponseDetail]"));
            props.add(new ImportProperty(quantityOrderedEOrderResponseDetailField, findProperty("quantityOrdered[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(quantityOrderedEOrderResponseDetailField);

            ImportField quantityAcceptedEOrderResponseDetailField = new ImportField(findProperty("quantityAccepted[EOrderResponseDetail]"));
            props.add(new ImportProperty(quantityAcceptedEOrderResponseDetailField, findProperty("quantityAccepted[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(quantityAcceptedEOrderResponseDetailField);

            ImportField priceEOrderResponseDetailField = new ImportField(findProperty("price[EOrderResponseDetail]"));
            props.add(new ImportProperty(priceEOrderResponseDetailField, findProperty("price[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(priceEOrderResponseDetailField);

            ImportField sumNoNDSEOrderResponseDetailField = new ImportField(findProperty("sumNoNDS[EOrderResponseDetail]"));
            props.add(new ImportProperty(sumNoNDSEOrderResponseDetailField, findProperty("sumNoNDS[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(sumNoNDSEOrderResponseDetailField);

            ImportField sumNDSEOrderResponseDetailField = new ImportField(findProperty("sumNDS[EOrderResponseDetail]"));
            props.add(new ImportProperty(sumNDSEOrderResponseDetailField, findProperty("sumNDS[EOrderResponseDetail]").getMapping(eOrderResponseDetailKey)));
            fields.add(sumNDSEOrderResponseDetailField);

            ImportTable table = new ImportTable(fields, importData);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("EDI_OR");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                message = session.applyMessage(context);
                session.popVolatileStats();
            } catch (Exception e) {
                ServerLoggers.importLogger.error("ImportOrderResponses Error: ", e);
                message = e.getMessage();
            }
        }
        return message;
    }

    private DocumentData parseDespatchAdvice(Element rootNode, ExecutionContext context, String url, String login, String password, String host,
                                             Integer port, String provider, String documentId, boolean sendReplies, boolean disableConfirmation)
            throws IOException, JDOMException, ScriptingModuleErrorLog.SemanticError, SQLHandledException, SQLException {
        List<List<Object>> firstData = new ArrayList<>();
        List<List<Object>> secondData = new ArrayList<>();

        String documentNumber = rootNode.getChildText("documentNumber");
        Timestamp dateTime = parseTimestamp(rootNode.getChildText("documentDate"));
        String deliveryNoteNumber = rootNode.getChildText("deliveryNoteNumber");
        Timestamp deliveryNoteDateTime = parseTimestamp(rootNode.getChildText("deliveryNoteDate"));
        String buyerGLN = rootNode.getChildText("buyerGLN");
        String destinationGLN = rootNode.getChildText("destinationGLN");
        String supplierGLN = rootNode.getChildText("supplierGLN");
        String orderNumber = rootNode.getChildText("orderNumber");
        Timestamp deliveryDateTimeFirst = parseTimestamp(rootNode.getChildText("deliveryDateTimeFirst"));
        String note = nullIfEmpty(rootNode.getChildText("comment"));

        Map<String, String> orderBarcodesMap = getOrderBarcodesMap(context, url, login, password, host, port, provider,
                documentId, documentNumber, orderNumber, sendReplies, disableConfirmation);

        int i = 1;
        for (Object line : rootNode.getChildren("line")) {
            Element lineElement = (Element) line;
            String dataGTIN = trim(lineElement.getChildText("GTIN"));
            String GTIN;
            String barcode;
            if (orderBarcodesMap.containsKey(dataGTIN)) {
                barcode = orderBarcodesMap.get(dataGTIN);
                GTIN = null;
            } else {
                barcode = null;
                GTIN = dataGTIN;
            }

            String id = supplierGLN + "/" + documentNumber + "/" + orderNumber;
            String idDetail = id + "/" + i++;
            BigDecimal quantityOrdered = parseBigDecimal(lineElement.getChildText("quantityOrdered"));
            BigDecimal quantityDespatch = parseBigDecimal(lineElement.getChildText("quantityDespatch"));
            BigDecimal valueVAT = parseBigDecimal(lineElement.getChildText("vat"));
            BigDecimal lineItemPrice = parseBigDecimal(lineElement.getChildText("lineItemPrice"));
            BigDecimal lineItemAmountWithoutCharges = parseBigDecimal(lineElement.getChildText("lineItemAmountWithoutCharges"));
            BigDecimal lineItemAmount = parseBigDecimal(lineElement.getChildText("lineItemAmount"));
            BigDecimal lineItemAmountCharges = parseBigDecimal(lineElement.getChildText("lineItemAmountCharges"));
            if (barcode != null)
                firstData.add(Arrays.<Object>asList(id, documentNumber, dateTime, deliveryNoteNumber, deliveryNoteDateTime, note, supplierGLN, buyerGLN, destinationGLN, orderNumber,
                        deliveryDateTimeFirst, idDetail, barcode, dataGTIN, quantityOrdered, quantityDespatch, valueVAT, lineItemPrice, lineItemAmountWithoutCharges,
                        lineItemAmount, lineItemAmountCharges));
            else
                secondData.add(Arrays.<Object>asList(id, documentNumber, dateTime, deliveryNoteNumber, deliveryNoteDateTime, note, supplierGLN, buyerGLN, destinationGLN, orderNumber,
                        deliveryDateTimeFirst, idDetail, GTIN, dataGTIN, quantityOrdered, quantityDespatch, valueVAT, lineItemPrice, lineItemAmountWithoutCharges,
                        lineItemAmount, lineItemAmountCharges));
        }
        return new DocumentData(documentNumber, firstData, secondData);
    }

    private String importDespatchAdvices(ExecutionContext context, DocumentData data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = importDespatchAdvices(context, data, true);
        return message == null ? importDespatchAdvices(context, data, false) : message;
    }

    private String importDespatchAdvices(ExecutionContext context, DocumentData data, boolean first) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = null;
        List<List<Object>> importData = data == null ? null : (first ? data.firstData : data.secondData);
        if (importData != null && !importData.isEmpty()) {
            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField idEOrderDespatchAdviceField = new ImportField(findProperty("id[EOrderDespatchAdvice]"));
            ImportKey<?> eOrderDespatchAdviceKey = new ImportKey((CustomClass) findClass("EOrderDespatchAdvice"),
                    findProperty("eOrderDespatchAdvice[VARSTRING[100]]").getMapping(idEOrderDespatchAdviceField));
            keys.add(eOrderDespatchAdviceKey);
            props.add(new ImportProperty(idEOrderDespatchAdviceField, findProperty("id[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(idEOrderDespatchAdviceField);

            ImportField numberEOrderDespatchAdviceField = new ImportField(findProperty("number[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(numberEOrderDespatchAdviceField, findProperty("number[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(numberEOrderDespatchAdviceField);

            ImportField dateTimeEOrderDespatchAdviceField = new ImportField(findProperty("dateTime[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(dateTimeEOrderDespatchAdviceField, findProperty("dateTime[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(dateTimeEOrderDespatchAdviceField);

            ImportField deliveryNoteNumberEOrderDespatchAdviceField = new ImportField(findProperty("deliveryNoteNumber[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(deliveryNoteNumberEOrderDespatchAdviceField, findProperty("deliveryNoteNumber[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(deliveryNoteNumberEOrderDespatchAdviceField);

            ImportField deliveryNoteDateTimeEOrderDespatchAdviceField = new ImportField(findProperty("deliveryNoteDateTime[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(deliveryNoteDateTimeEOrderDespatchAdviceField, findProperty("deliveryNoteDateTime[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(deliveryNoteDateTimeEOrderDespatchAdviceField);

            ImportField noteEOrderDespatchAdviceField = new ImportField(findProperty("note[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(noteEOrderDespatchAdviceField, findProperty("note[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(noteEOrderDespatchAdviceField);

            ImportField GLNSupplierEOrderDespatchAdviceField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> supplierKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityStockGLN[VARSTRING[13]]").getMapping(GLNSupplierEOrderDespatchAdviceField));
            supplierKey.skipKey = true;
            keys.add(supplierKey);
            props.add(new ImportProperty(GLNSupplierEOrderDespatchAdviceField, findProperty("supplier[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey),
                    object(findClass("LegalEntity")).getMapping(supplierKey)));
            fields.add(GLNSupplierEOrderDespatchAdviceField);

            ImportField GLNCustomerEOrderDespatchAdviceField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> customerKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityGLN[VARSTRING[13]]").getMapping(GLNCustomerEOrderDespatchAdviceField));
            customerKey.skipKey = true;
            keys.add(customerKey);
            props.add(new ImportProperty(GLNCustomerEOrderDespatchAdviceField, findProperty("customer[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey),
                    object(findClass("LegalEntity")).getMapping(customerKey)));
            fields.add(GLNCustomerEOrderDespatchAdviceField);

            ImportField GLNCustomerStockEOrderDespatchAdviceField = new ImportField(findProperty("GLN[Stock]"));
            ImportKey<?> customerStockKey = new ImportKey((CustomClass) findClass("Stock"),
                    findProperty("companyStockGLN[VARSTRING[13]]").getMapping(GLNCustomerStockEOrderDespatchAdviceField));
            customerStockKey.skipKey = true;
            keys.add(customerStockKey);
            props.add(new ImportProperty(GLNCustomerStockEOrderDespatchAdviceField, findProperty("customerStock[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey),
                    object(findClass("Stock")).getMapping(customerStockKey)));
            fields.add(GLNCustomerStockEOrderDespatchAdviceField);

            ImportField numberEOrderField = new ImportField(findProperty("number[EOrder]"));
            ImportKey<?> eOrderKey = new ImportKey((CustomClass) findClass("EOrder"),
                    findProperty("eOrder[VARSTRING[28]]").getMapping(numberEOrderField));
            eOrderKey.skipKey = true;
            keys.add(eOrderKey);
            props.add(new ImportProperty(numberEOrderField, findProperty("eOrder[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey),
                    object(findClass("EOrder")).getMapping(eOrderKey)));
            fields.add(numberEOrderField);

            ImportField deliveryDateTimeEOrderDespatchAdviceField = new ImportField(findProperty("deliveryDateTime[EOrderDespatchAdvice]"));
            props.add(new ImportProperty(deliveryDateTimeEOrderDespatchAdviceField, findProperty("deliveryDateTime[EOrderDespatchAdvice]").getMapping(eOrderDespatchAdviceKey)));
            fields.add(deliveryDateTimeEOrderDespatchAdviceField);

            ImportField idEOrderDespatchAdviceDetailField = new ImportField(findProperty("id[EOrderDespatchAdviceDetail]"));
            ImportKey<?> eOrderDespatchAdviceDetailKey = new ImportKey((CustomClass) findClass("EOrderDespatchAdviceDetail"),
                    findProperty("eOrderDespatchAdviceDetail[VARSTRING[100]]").getMapping(idEOrderDespatchAdviceDetailField));
            keys.add(eOrderDespatchAdviceDetailKey);
            props.add(new ImportProperty(numberEOrderField, findProperty("orderDespatchAdvice[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey),
                    object(findClass("EOrderDespatchAdvice")).getMapping(eOrderDespatchAdviceKey)));
            props.add(new ImportProperty(idEOrderDespatchAdviceDetailField, findProperty("id[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(idEOrderDespatchAdviceDetailField);

            if (first) {
                ImportField barcodeEOrderDespatchAdviceDetailField = new ImportField(findProperty("id[Barcode]"));
                ImportKey<?> skuBarcodeKey = new ImportKey((CustomClass) findClass("Sku"),
                        findProperty("skuBarcode[VARSTRING[15]]").getMapping(barcodeEOrderDespatchAdviceDetailField));
                skuBarcodeKey.skipKey = true;
                keys.add(skuBarcodeKey);
                props.add(new ImportProperty(barcodeEOrderDespatchAdviceDetailField, findProperty("sku[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey),
                        object(findClass("Sku")).getMapping(skuBarcodeKey), true));
                fields.add(barcodeEOrderDespatchAdviceDetailField);
            } else {
                ImportField GTINEOrderDespatchAdviceDetailField = new ImportField(findProperty("id[Barcode]"));
                ImportKey<?> skuGTINKey = new ImportKey((CustomClass) findClass("Sku"),
                        findProperty("skuGTIN[VARSTRING[15]]").getMapping(GTINEOrderDespatchAdviceDetailField));
                skuGTINKey.skipKey = true;
                keys.add(skuGTINKey);
                props.add(new ImportProperty(GTINEOrderDespatchAdviceDetailField, findProperty("sku[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey),
                        object(findClass("Sku")).getMapping(skuGTINKey), true));
                fields.add(GTINEOrderDespatchAdviceDetailField);
            }

            ImportField dataGTINEOrderDespatchAdviceDetailField = new ImportField(findProperty("dataGTIN[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(dataGTINEOrderDespatchAdviceDetailField, findProperty("dataGTIN[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(dataGTINEOrderDespatchAdviceDetailField);

            ImportField quantityOrderedEOrderDespatchAdviceDetailField = new ImportField(findProperty("quantityOrdered[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(quantityOrderedEOrderDespatchAdviceDetailField, findProperty("quantityOrdered[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(quantityOrderedEOrderDespatchAdviceDetailField);

            ImportField quantityDespatchEOrderDespatchAdviceDetailField = new ImportField(findProperty("quantityDespatch[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(quantityDespatchEOrderDespatchAdviceDetailField, findProperty("quantityDespatch[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(quantityDespatchEOrderDespatchAdviceDetailField);

            ImportField valueVATEOrderDespatchAdviceDetailField = new ImportField(findProperty("valueVAT[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(valueVATEOrderDespatchAdviceDetailField, findProperty("valueVAT[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(valueVATEOrderDespatchAdviceDetailField);

            ImportField lineItemPriceEOrderDespatchAdviceDetailField = new ImportField(findProperty("lineItemPrice[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(lineItemPriceEOrderDespatchAdviceDetailField, findProperty("lineItemPrice[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(lineItemPriceEOrderDespatchAdviceDetailField);

            ImportField lineItemAmountWithoutChargesEOrderDespatchAdviceDetailField = new ImportField(findProperty("lineItemAmountWithoutCharges[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(lineItemAmountWithoutChargesEOrderDespatchAdviceDetailField, findProperty("lineItemAmountWithoutCharges[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(lineItemAmountWithoutChargesEOrderDespatchAdviceDetailField);

            ImportField lineItemAmountEOrderDespatchAdviceDetailField = new ImportField(findProperty("lineItemAmount[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(lineItemAmountEOrderDespatchAdviceDetailField, findProperty("lineItemAmount[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(lineItemAmountEOrderDespatchAdviceDetailField);

            ImportField lineItemAmountChargesEOrderDespatchAdviceDetailField = new ImportField(findProperty("lineItemAmountCharges[EOrderDespatchAdviceDetail]"));
            props.add(new ImportProperty(lineItemAmountChargesEOrderDespatchAdviceDetailField, findProperty("lineItemAmountCharges[EOrderDespatchAdviceDetail]").getMapping(eOrderDespatchAdviceDetailKey)));
            fields.add(lineItemAmountChargesEOrderDespatchAdviceDetailField);

            ImportTable table = new ImportTable(fields, importData);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("EDI_DA");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                message = session.applyMessage(context);
                session.popVolatileStats();
            } catch (Exception e) {
                ServerLoggers.importLogger.error("ImportDespatchAdvice Error: ", e);
                message = e.getMessage();
            }
        }
        return message;
    }

    private DocumentData parseEInvoice(Element rootNode) throws IOException, JDOMException, ScriptingModuleErrorLog.SemanticError, SQLHandledException, SQLException, ParseException {
        List<List<Object>> data = new ArrayList<>();

        Element messageHeaderElement = rootNode.getChild("MessageHeader");
        String documentNumber = messageHeaderElement.getChildText("MessageID");

        Element deliveryNoteElement = rootNode.getChild("DeliveryNote");
        String deliveryNoteNumber = deliveryNoteElement.getChildText("DeliveryNoteID");
        Timestamp dateTime = parseTimestamp(deliveryNoteElement.getChildText("CreationDateTime"), "yyyyMMddHHmmss");
        String functionCode = deliveryNoteElement.getChildText("FunctionCode");
        Boolean isCancel = functionCode != null && functionCode.equals("1") ? true : null;

        Element shipperElement = deliveryNoteElement.getChild("Shipper");
        String supplierGLN = shipperElement.getChildText("GLN");

        Element receiverElement = deliveryNoteElement.getChild("Receiver");
        String buyerGLN = receiverElement.getChildText("GLN");

        Element shipToElement = deliveryNoteElement.getChild("ShipTo");
        String destinationGLN = shipToElement.getChildText("GLN");

        Element despatchAdviceLogisticUnitLineItemElement = deliveryNoteElement.getChild("DespatchAdviceLogisticUnitLineItem");

        for (Object line : despatchAdviceLogisticUnitLineItemElement.getChildren("LineItem")) {
            Element lineElement = (Element) line;
            Integer lineItemNumber = Integer.parseInt(lineElement.getChildText("LineItemNumber"));
            String lineItemID = lineElement.getChildText("LineItemID");
            String lineItemBuyerID = lineElement.getChildText("LineItemBuyerID");
            String lineItemName = lineElement.getChildText("LineItemName");

            String id = supplierGLN + "/" + documentNumber;
            String idDetail = id + "/" + lineItemNumber;
            BigDecimal quantityDespatched = parseBigDecimal(lineElement.getChildText("QuantityDespatched"));
            BigDecimal valueVAT = parseBigDecimal(lineElement.getChildText("TaxRate"));
            BigDecimal lineItemPrice = parseBigDecimal(lineElement.getChildText("LineItemPrice"));
            BigDecimal lineItemAmountWithoutCharges = parseBigDecimal(lineElement.getChildText("LineItemAmountWithoutCharges"));
            BigDecimal lineItemAmount = parseBigDecimal(lineElement.getChildText("LineItemAmount"));
            BigDecimal lineItemAmountCharges = parseBigDecimal(lineElement.getChildText("LineItemAmountCharges"));
            if (lineItemID != null || lineItemBuyerID != null)
                data.add(Arrays.<Object>asList(id, documentNumber, dateTime, deliveryNoteNumber, dateTime, supplierGLN,
                        buyerGLN, destinationGLN, isCancel, idDetail, lineItemID, lineItemBuyerID, lineItemName,
                        quantityDespatched, valueVAT, lineItemPrice, lineItemAmountWithoutCharges,
                        lineItemAmount, lineItemAmountCharges));
        }
        return new DocumentData(documentNumber, data, null);
    }

    private String importEInvoices(ExecutionContext context, DocumentData data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = null;
        List<List<Object>> importData = data == null ? null : data.firstData;
        if (importData != null && !importData.isEmpty()) {
            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField idEInvoiceField = new ImportField(findProperty("id[EInvoice]"));
            ImportKey<?> eInvoiceKey = new ImportKey((CustomClass) findClass("EInvoice"),
                    findProperty("eInvoice[VARSTRING[100]]").getMapping(idEInvoiceField));
            keys.add(eInvoiceKey);
            props.add(new ImportProperty(idEInvoiceField, findProperty("id[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(idEInvoiceField);

            ImportField numberEInvoiceField = new ImportField(findProperty("number[EInvoice]"));
            props.add(new ImportProperty(numberEInvoiceField, findProperty("number[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(numberEInvoiceField);

            ImportField dateTimeEInvoiceField = new ImportField(findProperty("dateTime[EInvoice]"));
            props.add(new ImportProperty(dateTimeEInvoiceField, findProperty("dateTime[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(dateTimeEInvoiceField);

            ImportField deliveryNoteNumberEInvoiceField = new ImportField(findProperty("deliveryNoteNumber[EInvoice]"));
            props.add(new ImportProperty(deliveryNoteNumberEInvoiceField, findProperty("deliveryNoteNumber[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(deliveryNoteNumberEInvoiceField);

            ImportField deliveryNoteDateTimeEInvoiceField = new ImportField(findProperty("deliveryNoteDateTime[EInvoice]"));
            props.add(new ImportProperty(deliveryNoteDateTimeEInvoiceField, findProperty("deliveryNoteDateTime[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(deliveryNoteDateTimeEInvoiceField);

            ImportField GLNSupplierEInvoiceField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> supplierKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityStockGLN[VARSTRING[13]]").getMapping(GLNSupplierEInvoiceField));
            supplierKey.skipKey = true;
            keys.add(supplierKey);
            props.add(new ImportProperty(GLNSupplierEInvoiceField, findProperty("supplier[EInvoice]").getMapping(eInvoiceKey),
                    object(findClass("LegalEntity")).getMapping(supplierKey)));
            fields.add(GLNSupplierEInvoiceField);

            ImportField GLNCustomerEInvoiceField = new ImportField(findProperty("GLN[LegalEntity]"));
            ImportKey<?> customerKey = new ImportKey((CustomClass) findClass("LegalEntity"),
                    findProperty("legalEntityGLN[VARSTRING[13]]").getMapping(GLNCustomerEInvoiceField));
            customerKey.skipKey = true;
            keys.add(customerKey);
            props.add(new ImportProperty(GLNCustomerEInvoiceField, findProperty("customer[EInvoice]").getMapping(eInvoiceKey),
                    object(findClass("LegalEntity")).getMapping(customerKey)));
            fields.add(GLNCustomerEInvoiceField);

            ImportField GLNCustomerStockEInvoiceField = new ImportField(findProperty("GLN[Stock]"));
            ImportKey<?> customerStockKey = new ImportKey((CustomClass) findClass("Stock"),
                    findProperty("companyStockGLN[VARSTRING[13]]").getMapping(GLNCustomerStockEInvoiceField));
            customerStockKey.skipKey = true;
            keys.add(customerStockKey);
            props.add(new ImportProperty(GLNCustomerStockEInvoiceField, findProperty("customerStock[EInvoice]").getMapping(eInvoiceKey),
                    object(findClass("Stock")).getMapping(customerStockKey)));
            fields.add(GLNCustomerStockEInvoiceField);

            ImportField isCancelEInvoiceField = new ImportField(findProperty("isCancel[EInvoice]"));
            props.add(new ImportProperty(isCancelEInvoiceField, findProperty("isCancel[EInvoice]").getMapping(eInvoiceKey)));
            fields.add(isCancelEInvoiceField);

            ImportField idEInvoiceDetailField = new ImportField(findProperty("id[EInvoiceDetail]"));
            ImportKey<?> eInvoiceDetailKey = new ImportKey((CustomClass) findClass("EInvoiceDetail"),
                    findProperty("eInvoiceDetail[VARSTRING[100]]").getMapping(idEInvoiceDetailField));
            keys.add(eInvoiceDetailKey);
            props.add(new ImportProperty(idEInvoiceDetailField, findProperty("eInvoice[EInvoiceDetail]").getMapping(eInvoiceDetailKey),
                    object(findClass("EInvoice")).getMapping(eInvoiceKey)));
            props.add(new ImportProperty(idEInvoiceDetailField, findProperty("id[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(idEInvoiceDetailField);

            ImportField lineItemIDEInvoiceDetailField = new ImportField(findProperty("lineItemID[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemIDEInvoiceDetailField, findProperty("lineItemID[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemIDEInvoiceDetailField);

            ImportField lineItemBuyerIDEInvoiceDetailField = new ImportField(findProperty("lineItemBuyerID[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemBuyerIDEInvoiceDetailField, findProperty("lineItemBuyerID[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemBuyerIDEInvoiceDetailField);

            ImportField lineItemNameEInvoiceDetailField = new ImportField(findProperty("lineItemName[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemNameEInvoiceDetailField, findProperty("lineItemName[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemNameEInvoiceDetailField);

            ImportField quantityDespatchedEInvoiceDetailField = new ImportField(findProperty("quantityDespatched[EInvoiceDetail]"));
            props.add(new ImportProperty(quantityDespatchedEInvoiceDetailField, findProperty("quantityDespatched[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(quantityDespatchedEInvoiceDetailField);

            ImportField valueVATEInvoiceDetailField = new ImportField(findProperty("valueVAT[EInvoiceDetail]"));
            props.add(new ImportProperty(valueVATEInvoiceDetailField, findProperty("valueVAT[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(valueVATEInvoiceDetailField);

            ImportField lineItemPriceEInvoiceDetailField = new ImportField(findProperty("lineItemPrice[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemPriceEInvoiceDetailField, findProperty("lineItemPrice[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemPriceEInvoiceDetailField);

            ImportField lineItemAmountWithoutChargesEInvoiceDetailField = new ImportField(findProperty("lineItemAmountWithoutCharges[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemAmountWithoutChargesEInvoiceDetailField, findProperty("lineItemAmountWithoutCharges[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemAmountWithoutChargesEInvoiceDetailField);

            ImportField lineItemAmountEInvoiceDetailField = new ImportField(findProperty("lineItemAmount[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemAmountEInvoiceDetailField, findProperty("lineItemAmount[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemAmountEInvoiceDetailField);

            ImportField lineItemAmountChargesEInvoiceDetailField = new ImportField(findProperty("lineItemAmountCharges[EInvoiceDetail]"));
            props.add(new ImportProperty(lineItemAmountChargesEInvoiceDetailField, findProperty("lineItemAmountCharges[EInvoiceDetail]").getMapping(eInvoiceDetailKey)));
            fields.add(lineItemAmountChargesEInvoiceDetailField);

            ImportTable table = new ImportTable(fields, importData);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("EDI_DA");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                message = session.applyMessage(context);
                session.popVolatileStats();
            } catch (Exception e) {
                ServerLoggers.importLogger.error("ImportEInvoice Error: ", e);
                message = e.getMessage();
            }
        }
        return message;
    }

    private DocumentData parseInvoiceMessage(ExecutionContext context, Element rootNode, String provider, String documentId) throws IOException, JDOMException, ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {

        Element acknowledgementElement = rootNode.getChild("Acknowledgement");

        String documentNumber = acknowledgementElement.getChildText("DocumentID");
        Timestamp dateTime = parseTimestamp(acknowledgementElement.getChildText("CreationDateTime"), "yyyyMMddHHmmss");

        Element referenceDocumentElement = acknowledgementElement.getChild("ReferenceDocument");
        if (referenceDocumentElement != null) {
            String type = referenceDocumentElement.getChildText("Type");
            if (type != null && (type.equals("BLRAPN") || type.equals("BLRWBR"))) {

                String invoiceNumber = referenceDocumentElement.getChildText("ID");
                if (type.equals("BLRAPN"))
                    invoiceNumber = (String) findProperty("numberEInvoiceBlrapn[VARSTRING[14]]").read(context, new DataObject(invoiceNumber));
                else if (type.equals("BLRWBR"))
                    invoiceNumber = (String) findProperty("numberEInvoiceBlrwbr[VARSTRING[14]]").read(context, new DataObject(invoiceNumber));

                Element errorOrAcknowledgementElement = acknowledgementElement.getChild("ErrorOrAcknowledgement");
                if (errorOrAcknowledgementElement != null) {
                    String code = errorOrAcknowledgementElement.getChildText("Code");
                    String description = errorOrAcknowledgementElement.getChildText("Description");
                    return new DocumentData(documentNumber, Collections.singletonList(Arrays.asList((Object) documentNumber, dateTime, code, description, invoiceNumber)), null);
                }
            }
        } else
            ServerLoggers.importLogger.error(String.format("%s Parse Invoice Message %s error: no reference tag", provider, documentId));
        return new DocumentData(documentNumber, null, null);
    }

    private String importInvoiceMessages(ExecutionContext context, DocumentData data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        String message = null;
        List<List<Object>> importData = data == null ? null : data.firstData;
        if (importData != null && !importData.isEmpty()) {
            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField numberEInvoiceMessageField = new ImportField(findProperty("number[EInvoiceMessage]"));
            ImportKey<?> eInvoiceMessageKey = new ImportKey((CustomClass) findClass("EInvoiceMessage"),
                    findProperty("eInvoiceMessage[VARSTRING[24]]").getMapping(numberEInvoiceMessageField));
            keys.add(eInvoiceMessageKey);
            props.add(new ImportProperty(numberEInvoiceMessageField, findProperty("number[EInvoiceMessage]").getMapping(eInvoiceMessageKey)));
            fields.add(numberEInvoiceMessageField);

            ImportField dateTimeEInvoiceMessageField = new ImportField(findProperty("dateTime[EInvoiceMessage]"));
            props.add(new ImportProperty(dateTimeEInvoiceMessageField, findProperty("dateTime[EInvoiceMessage]").getMapping(eInvoiceMessageKey)));
            fields.add(dateTimeEInvoiceMessageField);

            ImportField codeEInvoiceMessageField = new ImportField(findProperty("code[EInvoiceMessage]"));
            props.add(new ImportProperty(codeEInvoiceMessageField, findProperty("code[EInvoiceMessage]").getMapping(eInvoiceMessageKey)));
            fields.add(codeEInvoiceMessageField);

            ImportField descriptionEInvoiceMessageField = new ImportField(findProperty("description[EInvoiceMessage]"));
            props.add(new ImportProperty(descriptionEInvoiceMessageField, findProperty("description[EInvoiceMessage]").getMapping(eInvoiceMessageKey)));
            fields.add(descriptionEInvoiceMessageField);

            ImportField numberEInvoiceField = new ImportField(findProperty("number[EInvoice]"));
            ImportKey<?> eInvoiceKey = new ImportKey((CustomClass) findClass("EInvoice"),
                    findProperty("eInvoiceNumber[VARSTRING[28]]").getMapping(numberEInvoiceField));
            eInvoiceKey.skipKey = true;
            keys.add(eInvoiceKey);
            props.add(new ImportProperty(numberEInvoiceField, findProperty("eInvoice[EInvoiceMessage]").getMapping(eInvoiceMessageKey),
                    object(findClass("EInvoice")).getMapping(eInvoiceKey)));
            fields.add(numberEInvoiceField);

            ImportTable table = new ImportTable(fields, importData);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("EDI_IM");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                message = session.applyMessage(context);
                session.popVolatileStats();
            } catch (Exception e) {
                ServerLoggers.importLogger.error("ImportInvoiceMessages Error: ", e);
                message = e.getMessage();
            }
        }
        return message;
    }

    private void confirmDocumentReceived(ExecutionContext context, String documentId, String url, String login, String password,
                                         String host, Integer port, String provider) throws IOException, JDOMException {

        Element rootElement = new Element("Envelope", soapenvNamespace);
        rootElement.setNamespace(soapenvNamespace);
        rootElement.addNamespaceDeclaration(soapenvNamespace);
        rootElement.addNamespaceDeclaration(topNamespace);

        Document doc = new Document(rootElement);
        doc.setRootElement(rootElement);

        //parent: rootElement
        Element headerElement = new Element("Header", soapenvNamespace);
        rootElement.addContent(headerElement);

        //parent: rootElement
        Element bodyElement = new Element("Body", soapenvNamespace);
        rootElement.addContent(bodyElement);

        //parent: bodyElement
        Element confirmDocumentReceivedElement = new Element("ConfirmDocumentReceived", topNamespace);
        bodyElement.addContent(confirmDocumentReceivedElement);

        addStringElement(topNamespace, confirmDocumentReceivedElement, "username", login);
        addStringElement(topNamespace, confirmDocumentReceivedElement, "password", password);
        addStringElement(topNamespace, confirmDocumentReceivedElement, "documentId", documentId);

        String xml = new XMLOutputter().outputString(doc);
        HttpResponse httpResponse = sendRequest(host, port, login, password, url, xml, null);
        ServerLoggers.importLogger.info(String.format("%s ConfirmDocumentReceived document %s: request sent", provider, documentId));
        RequestResult requestResult = getRequestResult(httpResponse, getResponseMessage(httpResponse), "ConfirmDocumentReceived");
        switch (requestResult) {
            case OK:
                ServerLoggers.importLogger.info(String.format("%s ConfirmDocumentReceived document %s: request succeeded", provider, documentId));
                break;
            case AUTHORISATION_ERROR:
                ServerLoggers.importLogger.error(String.format("%s ConfirmDocumentReceived document %s: invalid login-password", provider, documentId));
                context.delayUserInteraction(new MessageClientAction(String.format("%s Документ %s не помечен как обработанный: ошибка авторизации", provider, documentId), "Импорт"));
                break;
            case UNKNOWN_ERROR:
                ServerLoggers.importLogger.error(String.format("%s ConfirmDocumentReceived document %s: unknown error", provider, documentId));
                context.delayUserInteraction(new MessageClientAction(String.format("%s Документ %s не помечен как обработанный", provider, documentId), "Импорт"));
        }
    }

    protected boolean sendRecipientError(ExecutionContext context, String url, String login, String password, String host, Integer port, String provider, String documentId, String documentNumber, String error) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException, IOException, JDOMException {
        boolean succeeded = false;
        String currentDate = formatDate(new Timestamp(Calendar.getInstance().getTime().getTime()));
        String contentSubXML = getErrorSubXML(documentId, documentNumber, error);

        Element rootElement = new Element("Envelope", soapenvNamespace);
        rootElement.setNamespace(soapenvNamespace);
        rootElement.addNamespaceDeclaration(soapenvNamespace);
        rootElement.addNamespaceDeclaration(topNamespace);

        Document doc = new Document(rootElement);
        doc.setRootElement(rootElement);

        //parent: rootElement
        Element headerElement = new Element("Header", soapenvNamespace);
        rootElement.addContent(headerElement);

        //parent: rootElement
        Element bodyElement = new Element("Body", soapenvNamespace);
        rootElement.addContent(bodyElement);

        //parent: bodyElement
        Element sendDocumentElement = new Element("SendDocument", topNamespace);
        bodyElement.addContent(sendDocumentElement);

        addStringElement(topNamespace, sendDocumentElement, "username", login);
        addStringElement(topNamespace, sendDocumentElement, "password", password);
        addStringElement(topNamespace, sendDocumentElement, "documentDate", currentDate);
        addStringElement(topNamespace, sendDocumentElement, "documentNumber", "error_" + documentId);

        addStringElement(topNamespace, sendDocumentElement, "documentType", "SYSTEMMESSAGE");
        addStringElement(topNamespace, sendDocumentElement, "content", contentSubXML);

        String xml = new XMLOutputter().outputString(doc);
        HttpResponse httpResponse = sendRequest(host, port, login, password, url, xml, null);
        ServerLoggers.importLogger.info(String.format("%s RecipientError %s request sent", provider, documentId));
        RequestResult requestResult = getRequestResult(httpResponse, getResponseMessage(httpResponse), "SendDocument");
        switch (requestResult) {
            case OK:
                succeeded = true;
                break;
            case AUTHORISATION_ERROR:
                ServerLoggers.importLogger.error(String.format("%s RecipientError %s: invalid login-password", provider, documentId));
                context.delayUserInteraction(new MessageClientAction(String.format("%s Сообщение об ошибке %s не выгружено: ошибка авторизации", provider, documentId), "Импорт"));
                break;
            case UNKNOWN_ERROR:
                ServerLoggers.importLogger.error(String.format("%s RecipientError %s: unknown error", provider, documentId));
                context.delayUserInteraction(new MessageClientAction(String.format("%s Сообщение об ошибке %s не выгружено: неизвестная ошибка", provider, documentId), "Импорт"));
        }
        return succeeded;
    }

    private String getErrorSubXML(String documentId, String documentNumber, String error) throws SQLException, ScriptingModuleErrorLog.SemanticError, SQLHandledException {
        Element rootElement = new Element("SYSTEMMESSAGE");
        Document doc = new Document(rootElement);
        doc.setRootElement(rootElement);

        addStringElement(rootElement, "documentNumber", "error_" + documentId);

        Element referenceElement = new Element("Reference");
        addStringElement(referenceElement, "documentNumber", documentNumber);
        addStringElement(referenceElement, "code", "1450");
        addStringElement(referenceElement, "description", error);
        rootElement.addContent(referenceElement);

        String xml = new XMLOutputter().outputString(doc);
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(xml.getBytes()));
    }

    private Map<String, String> getOrderBarcodesMap(ExecutionContext context, String url, String login, String password, String host, Integer port,
                                                    String provider, String documentId, String documentNumber, String orderNumber,
                                                    boolean sendReplies, boolean disableConfirmation)
            throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException, IOException, JDOMException {
        Map<String, String> orderBarcodesMap = new HashMap<>();
        if (orderNumber != null) {
            if (findProperty("eOrder[VARSTRING[28]]").read(context, new DataObject(orderNumber)) == null && sendReplies && !disableConfirmation) {
                sendRecipientError(context, url, login, password, host, port, provider, documentId, documentNumber, String.format("Заказ %s не найден)", orderNumber));
            }

            KeyExpr orderDetailExpr = new KeyExpr("EOrderDetail");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "eOrderDetail", orderDetailExpr);

            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            String[] names = new String[]{"idBarcode", "GTINBarcode"};
            LCP[] properties = findProperties("idBarcode[EOrderDetail]", "GTINBarcode[EOrderDetail]");
            for (int i = 0; i < properties.length; i++) {
                query.addProperty(names[i], properties[i].getExpr(context.getModifier(), orderDetailExpr));
            }
            query.and(findProperty("numberOrder[EOrderDetail]").getExpr(context.getModifier(), orderDetailExpr).compare(new DataObject(orderNumber), Compare.EQUALS));
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context);
            for (ImMap<Object, Object> entry : result.values()) {
                String idBarcode = (String) entry.get("idBarcode");
                String GTINBarcode = (String) entry.get("GTINBarcode");
                orderBarcodesMap.put(GTINBarcode, idBarcode);
            }
        }
        return orderBarcodesMap;
    }

    private Timestamp parseTimestamp(String value) {
        return parseTimestamp(value, "yyyy-MM-dd'T'HH:mm:ss");
    }

    private Timestamp parseTimestamp(String value, String pattern) {
        try {
            return new Timestamp(new SimpleDateFormat(pattern).parse(value).getTime());
        } catch (Exception e) {
            return null;
        }
    }

    private class DocumentData {
        String documentNumber;
        List<List<Object>> firstData;
        List<List<Object>> secondData;

        public DocumentData(String documentNumber, List<List<Object>> firstData, List<List<Object>> secondData) {
            this.documentNumber = documentNumber;
            this.firstData = firstData;
            this.secondData = secondData;
        }
    }
}
