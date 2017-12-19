package lsfusion.erp.region.by.certificate.declaration;

import com.google.common.base.Throwables;
import lsfusion.erp.integration.DefaultImportActionProperty;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ImportDeclarationAdjustmentXMLActionProperty extends DefaultImportActionProperty {
    private final ClassPropertyInterface declarationInterface;

    public ImportDeclarationAdjustmentXMLActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы XML", "xml");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                DataObject declarationObject = context.getDataKeyValue(declarationInterface);
                for (byte[] file : fileList) {

                    List<List<Object>> data = new ArrayList<>();

                    SAXBuilder builder = new SAXBuilder();
                    Document document = builder.build(new ByteArrayInputStream(file));
                    Element rootNode = document.getRootElement();
                    Namespace ns = rootNode.getNamespace("KDTout");
                    Namespace gns = rootNode.getNamespace("catESAD_cu");
                    Namespace catRu = rootNode.getNamespace("cat_ru");

                    Element registryNumberNode = rootNode.getChild("GtdRegistryNumber", ns);
                    Date registryDate = parseDate(registryNumberNode.getChildText("RegistrationDate", catRu));

                    rootNode = rootNode.getChild("KDToutGoodsShipment", ns);

                    List list = rootNode.getChildren("KDToutGoods", ns);
                    for (Object aList : list) {
                        Element node = (Element) aList;
                        List payment = node.getChildren("KDToutCustomsPaymentCalculation", ns);

                        Double duty = null;
                        Double vat = null;
                        for (Object p : payment) {
                            String paymentModeCode = ((Element) p).getChildText("PaymentModeCode", gns);
                            if ("2010".equals(paymentModeCode)) {
                                duty = Double.valueOf(((Element) p).getChildText("PaymentAmount", gns));
                            } else if ("5010".equals(paymentModeCode))
                                vat = Double.valueOf(((Element) p).getChildText("PaymentAmount", gns));
                        }
                        Double sum = Double.valueOf(node.getChildText("CustomsCost", gns));
                        Integer number = Integer.valueOf(node.getChildText("GoodsNumeric", gns));

                        data.add(Arrays.<Object>asList(1, number, registryDate, duty, vat, sum));
                    }

                    List<ImportProperty<?>> properties = new ArrayList<>();
                    List<ImportField> fields = new ArrayList<>();
                    List<ImportKey<?>> keys = new ArrayList<>();

                    ImportField numberDeclarationAdjustmentField = new ImportField(findProperty("number[DeclarationAdjustment]"));
                    ImportKey<?> declarationAdjustmentKey = new ImportKey((ConcreteCustomClass) findClass("DeclarationAdjustment"),
                            findProperty("declarationAdjustment[Declaration,INTEGER]").getMapping(declarationObject, numberDeclarationAdjustmentField));
                    keys.add(declarationAdjustmentKey);
                    properties.add(new ImportProperty(numberDeclarationAdjustmentField, findProperty("number[DeclarationAdjustment]").getMapping(declarationAdjustmentKey)));
                    properties.add(new ImportProperty(declarationObject, findProperty("declaration[DeclarationAdjustment]").getMapping(declarationAdjustmentKey)));
                    fields.add(numberDeclarationAdjustmentField);

                    ImportField numberDeclarationDetailField = new ImportField(findProperty("number[DeclarationDetail]"));
                    ImportKey<?> declarationDetailKey = new ImportKey((ConcreteCustomClass) findClass("DeclarationDetail"),
                            findProperty("declarationDetail[Declaration,INTEGER]").getMapping(declarationObject, numberDeclarationDetailField));
                    keys.add(declarationDetailKey);
                    properties.add(new ImportProperty(declarationObject, findProperty("declaration[DeclarationDetail]").getMapping(declarationDetailKey)));
                    fields.add(numberDeclarationDetailField);

                    ImportField dateDeclarationAdjustment = new ImportField(findProperty("date[DeclarationAdjustment]"));
                    properties.add(new ImportProperty(dateDeclarationAdjustment, findProperty("date[DeclarationAdjustment]").getMapping(declarationAdjustmentKey)));
                    fields.add(dateDeclarationAdjustment);

                    ImportField dutySumDeclarationAdjustmentField = new ImportField(findProperty("dutySum[DeclarationAdjustment, DeclarationDetail]"));
                    properties.add(new ImportProperty(dutySumDeclarationAdjustmentField, findProperty("dutySum[DeclarationAdjustment, DeclarationDetail]").getMapping(declarationAdjustmentKey, declarationDetailKey)));
                    fields.add(dutySumDeclarationAdjustmentField);

                    ImportField VATSumDeclarationAdjustmentField = new ImportField(findProperty("VATSum[DeclarationAdjustment, DeclarationDetail]"));
                    properties.add(new ImportProperty(VATSumDeclarationAdjustmentField, findProperty("VATSum[DeclarationAdjustment, DeclarationDetail]").getMapping(declarationAdjustmentKey, declarationDetailKey)));
                    fields.add(VATSumDeclarationAdjustmentField);

                    ImportField homeSumDeclarationAdjustmentField = new ImportField(findProperty("homeSum[DeclarationAdjustment, DeclarationDetail]"));
                    properties.add(new ImportProperty(homeSumDeclarationAdjustmentField, findProperty("homeSum[DeclarationAdjustment, DeclarationDetail]").getMapping(declarationAdjustmentKey, declarationDetailKey)));
                    fields.add(homeSumDeclarationAdjustmentField);

                    IntegrationService integrationService = new IntegrationService(context.getSession(), new ImportTable(fields, data), keys, properties);
                    integrationService.synchronize(true, false);
                    context.requestUserInteraction(new MessageClientAction("Импорт успешно завершён", "Импорт КТС"));
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
