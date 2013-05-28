package platform.server.mail;

import jasperapi.ReportGenerator;
import jasperapi.ReportHTMLExporter;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import org.apache.log4j.Logger;
import platform.base.ByteArray;
import platform.base.SystemUtils;
import platform.base.col.MapFact;
import platform.interop.action.MessageClientAction;
import platform.interop.form.ReportGenerationData;
import platform.server.ServerLoggers;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.DataObject;
import platform.server.logics.EmailLogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;
import platform.server.logics.property.actions.SystemExplicitActionProperty;
import platform.server.remote.RemoteForm;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.mail.Message.RecipientType.TO;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static platform.base.BaseUtils.nullTrim;
import static platform.base.BaseUtils.rtrim;
import static platform.server.logics.ServerResourceBundle.getString;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 11:53
 */

public class EmailActionProperty extends SystemExplicitActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;

    public static enum FormStorageType {INLINE, ATTACH}

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> fromAddress;
    private CalcPropertyInterfaceImplement<ClassPropertyInterface> subject;

    private List<CalcPropertyInterfaceImplement<ClassPropertyInterface>> recipients = new ArrayList<CalcPropertyInterfaceImplement<ClassPropertyInterface>>();
    private List<Message.RecipientType> recipientTypes = new ArrayList<Message.RecipientType>();

    private final List<FormEntity> forms = new ArrayList<FormEntity>();
    private final List<AttachmentFormat> formats = new ArrayList<AttachmentFormat>();
    private final List<FormStorageType> storageTypes = new ArrayList<FormStorageType>();
    private final List<Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>>> mapObjects = new ArrayList<Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>>>();
    private final List<CalcPropertyInterfaceImplement> attachmentProps = new ArrayList<CalcPropertyInterfaceImplement>();

    public EmailActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);

        askConfirm = true;
        setImage("email.png");
    }

    @Override
    protected boolean isVolatile() { // формы используются, не определишь getUsedProps
        return true;
    }

    public void setFromAddress(CalcPropertyInterfaceImplement<ClassPropertyInterface> fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setSubject(CalcPropertyInterfaceImplement<ClassPropertyInterface> subject) {
        this.subject = subject;
    }

    public <R extends PropertyInterface> void addRecipient(CalcPropertyInterfaceImplement<ClassPropertyInterface> recipient, Message.RecipientType type) {
        recipients.add(recipient);
        recipientTypes.add(type);
    }

    public void addInlineForm(FormEntity form, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objects) {
        forms.add(form);
        formats.add(AttachmentFormat.HTML);
        storageTypes.add(FormStorageType.INLINE);
        mapObjects.add(objects);
        attachmentProps.add(null);
    }

    public void addAttachmentForm(FormEntity form, AttachmentFormat format, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objects, CalcPropertyInterfaceImplement attachmentNameProp) {
        forms.add(form);
        formats.add(format);
        storageTypes.add(FormStorageType.ATTACH);
        mapObjects.add(objects);
        attachmentProps.add(attachmentNameProp);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        EmailLogicsModule emailLM = context.getBL().emailLM;
        try {
            if (emailLM.disableEmail.read(context) != null) {
                logger.error(getString("mail.sending.disabled"));
                return;
            }

            assert subject != null && fromAddress != null;

            List<EmailSender.AttachmentProperties> attachments = new ArrayList<EmailSender.AttachmentProperties>();
            List<String> inlineForms = new ArrayList<String>();
            Map<ByteArray, String> attachmentFiles = new HashMap<ByteArray, String>();

            assert forms.size() == storageTypes.size() && forms.size() == formats.size() && forms.size() == attachmentProps.size() && forms.size() == mapObjects.size();

            for (int i = 0; i < forms.size(); i++) {
                FormEntity form = forms.get(i);
                FormStorageType storageType = storageTypes.get(i);
                AttachmentFormat attachmentFormat = formats.get(i);
                CalcPropertyInterfaceImplement attachmentProp = attachmentProps.get(i);
                Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements = mapObjects.get(i);

                RemoteForm remoteForm = createReportForm(context, form, objectsImplements);

                // если объекты подошли
                if (remoteForm != null) {
                    String filePath = createReportFile(remoteForm, storageType == FormStorageType.INLINE, attachmentFormat, attachmentFiles);
                    if (storageType == FormStorageType.INLINE) {
                        inlineForms.add(filePath);
                    } else {
                        EmailSender.AttachmentProperties attachment = createAttachment(form, attachmentFormat, attachmentProp, context, filePath);
                        attachments.add(attachment);
                    }
                }
            }

            Map<String, Message.RecipientType> recipients = getRecipientEmails(context);

            String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionType.read(context);
            String smtpHost = (String) emailLM.smtpHost.read(context);
            String smtpPort = (String) emailLM.smtpPort.read(context);
            String fromAddress = (String) this.fromAddress.read(context, context.getKeys());
            String subject = (String) this.subject.read(context, context.getKeys());
            String userName = (String) emailLM.emailAccount.read(context);
            String password = (String) emailLM.emailPassword.read(context);

            sendEmail(context, smtpHost, smtpPort, userName, password, encryptedConnectionType, fromAddress, subject, recipients, inlineForms, attachments, attachmentFiles);
        } catch (Exception e) {
            String errorMessage = getString("mail.failed.to.send.mail") + " : " + e.toString();
            logger.error(errorMessage);
            context.delayUserInterfaction(new MessageClientAction(errorMessage, getString("mail.sending")));

            logError(context, getString("mail.failed.to.send.mail") + " : " + e.toString());
            e.printStackTrace();
        }
    }

    private void sendEmail(ExecutionContext context, String smtpHost, String smtpPort, String userName, String password, String encryptedConnectionType, String fromAddress, String subject, Map<String, Message.RecipientType> recipientEmails, List<String> inlineForms, List<EmailSender.AttachmentProperties> attachments, Map<ByteArray, String> attachmentFiles) throws MessagingException, IOException {
        if (smtpHost == null || fromAddress == null) {
            logError(context, getString("mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent"));
            return;
        }

        if (recipientEmails.isEmpty()) {
            logError(context, getString("mail.recipient.not.specified"));
            return;
        }

        EmailSender sender = new EmailSender(
                nullTrim(smtpHost),
                nullTrim(smtpPort),
                nullTrim(encryptedConnectionType),
                nullTrim(fromAddress),
                nullTrim(userName),
                nullTrim(password),
                recipientEmails
        );

        sender.sendMail(subject, inlineForms, attachments, attachmentFiles);
    }

    private Map<String, Message.RecipientType> getRecipientEmails(ExecutionContext context) throws SQLException {
        assert recipients.size() == recipientTypes.size();

        Map<String, Message.RecipientType> recipientEmails = new HashMap<String, Message.RecipientType>();
        for (int i = 0; i < recipients.size(); ++i) {
            CalcPropertyInterfaceImplement<ClassPropertyInterface> recipient = recipients.get(i);
            Message.RecipientType recipientType = recipientTypes.get(i);

            String recipientEmailList = (String) recipient.read(context, context.getKeys());
            if (recipientEmailList != null) {
                String[] emails = recipientEmailList.split(";");
                for (String email : emails) {
                    email = trimToNull(email);
                    if (email == null) {
                        continue;
                    }

                    // приоритет отдается TO, так как без него письмо не улетит
                    if (TO.equals(recipientType) || !recipientEmails.containsKey(email)) {
                        recipientEmails.put(email, recipientType);
                    }
                }
            }
        }
        return recipientEmails;
    }

    private EmailSender.AttachmentProperties createAttachment(FormEntity form, AttachmentFormat attachmentFormat, CalcPropertyInterfaceImplement attachmentNameProp, ExecutionContext context, String filePath) throws SQLException {
        assert attachmentFormat != null;

        String attachmentName = null;
        if (attachmentNameProp != null) {
            attachmentName = (String) attachmentNameProp.read(context, context.getKeys());
        }
        if (attachmentName == null) {
            attachmentName = form.caption;
        }
        attachmentName = rtrim(attachmentName.replace('"', '\''));

        // добавляем расширение, поскольку видимо не все почтовые клиенты правильно его определяют по mimeType
        attachmentName += attachmentFormat.getExtension();

        return new EmailSender.AttachmentProperties(filePath, attachmentName, attachmentFormat);
    }

    private RemoteForm createReportForm(ExecutionContext context, FormEntity form, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements) throws SQLException {
        Map<ObjectEntity, ObjectValue> objectValues = new HashMap<ObjectEntity, ObjectValue>();
        for (Map.Entry<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectImpl : objectsImplements.entrySet())
            objectValues.put(objectImpl.getKey(), objectImpl.getValue().readClasses(context, context.getKeys()));

        return context.createReportForm(form, MapFact.fromJavaMap(objectValues));
    }

    private String createReportFile(RemoteForm remoteForm, boolean inlineForm, AttachmentFormat attachmentFormat, Map<ByteArray, String> attachmentFiles) throws ClassNotFoundException, IOException, JRException {

        ReportGenerationData generationData = remoteForm.reportManager.getReportData();

        ReportGenerator report = new ReportGenerator(generationData, SystemUtils.getCurrentTimeZone());
        JasperPrint print = report.createReport(inlineForm, attachmentFiles);
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");
        try {
            String filePath = File.createTempFile("lsfReport", attachmentFormat != null ? attachmentFormat.getExtension() : null).getAbsolutePath();
            JRAbstractExporter exporter = createExporter(attachmentFormat);

            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, filePath);
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.exportReport();

            return filePath;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JRAbstractExporter createExporter(AttachmentFormat format) {
        JRAbstractExporter exporter;
        switch (format) {
            case PDF:
                exporter = new JRPdfExporter();
                break;
            case DOCX:
                exporter = new JRDocxExporter();
                break;
            case RTF:
                exporter = new JRRtfExporter();
                break;
            default:
                exporter = new ReportHTMLExporter();
                exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
                break;
        }
        return exporter;
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, getString("mail.sending")));
    }
}
