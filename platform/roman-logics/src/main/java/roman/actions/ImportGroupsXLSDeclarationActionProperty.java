package roman.actions;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import platform.base.BaseUtils;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportGroupsXLSDeclarationActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface declarationInterface;
    String row;

    public ImportGroupsXLSDeclarationActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("Declaration")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        declarationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

            DataObject declaration = context.getDataKeyValue(declarationInterface);
            ObjectValue customsZone = LM.findLCPByCompoundName("customsZoneDeclaration").readClasses(context.getSession(), declaration);

            for (byte[] file : fileList) {

                Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
                Sheet sheet = Wb.getSheet(0);

                List<List<Object>> data = new ArrayList<List<Object>>();

                for (int i = 14; i < sheet.getRows(); i += 7) {
                    List<Object> row = new ArrayList<Object>();
                    String nameData = sheet.getCell(1, i).getContents();
                    List<String> articles = new ArrayList<String>();
                    Pattern pattern = Pattern.compile("арт\\..*-");
                    Matcher matcher = pattern.matcher(nameData);
                    while (matcher.find())
                        articles.add(matcher.group().substring(4, matcher.group().length() - 1));
                    row.add(Integer.valueOf(sheet.getCell(0, i).getContents())); //userNumberGroupDeclaration
                    row.add(nameData);   //nameDataGroupDeclaration
                    row.add(parseDouble(sheet.getCell(1, i + 5).getContents())); //sumDataGroupDeclarationField
                    row.add(parseDouble(sheet.getCell(3, i + 5).getContents())); //dutyDataGroupDeclaration
                    String customCategory10 = sheet.getCell(7, i).getContents(); //sidCustomCategory10
                    row.add(customCategory10); //sidCustomCategory10
                    row.add((customCategory10 != null && customCategory10.length() > 9) ? customCategory10.substring(0,9) : null); //sidCustomCategory9
                    data.add(row);
                }

                ImportField userNumberGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("userNumberGroupDeclaration"));
                ImportField nameDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("nameDataGroupDeclaration"));
                ImportField sumDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("sumDataGroupDeclaration"));
                ImportField dutyDataGroupDeclarationField = new ImportField(LM.findLCPByCompoundName("dutyDataGroupDeclaration"));
                ImportField customCategory10Field = new ImportField(LM.findLCPByCompoundName("sidCustomCategory10"));
                ImportField customCategory9Field = new ImportField(LM.findLCPByCompoundName("sidCustomCategory9"));


                List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

                ImportKey<?> groupDeclarationKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("GroupDeclaration"),
                        LM.findLCPByCompoundName("uniqueGroupDeclaration").getMapping(userNumberGroupDeclarationField, declaration));

                ImportKey<?> customCategory10Key = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CustomCategory10"),
                        getLCP("sidToCustomCategory10").getMapping(customCategory10Field, customsZone));

                ImportKey<?> customCategory9Key = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("CustomCategory9"),
                        getLCP("sidToCustomCategory9").getMapping(customCategory9Field, customsZone));

                //ImportKey<?> declarationDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("DeclarationDetail"),
                //        LM.findLCPByCompoundName("uniqueDeclarationDetail").getMapping(userNumberGroupDeclarationField, declarationDetailField));


                properties.add(new ImportProperty(userNumberGroupDeclarationField, LM.findLCPByCompoundName("userNumberGroupDeclaration").getMapping(groupDeclarationKey)));
                properties.add(new ImportProperty(nameDataGroupDeclarationField, LM.findLCPByCompoundName("nameDataGroupDeclaration").getMapping(groupDeclarationKey)));
                properties.add(new ImportProperty(sumDataGroupDeclarationField, LM.findLCPByCompoundName("sumDataGroupDeclaration").getMapping(groupDeclarationKey)));
                properties.add(new ImportProperty(dutyDataGroupDeclarationField, LM.findLCPByCompoundName("dutyDataGroupDeclaration").getMapping(groupDeclarationKey)));
                properties.add(new ImportProperty(declaration, getLCP("declarationGroupDeclaration").getMapping(groupDeclarationKey)));

                properties.add(new ImportProperty(customCategory10Field, getLCP("sidCustomCategory10").getMapping(customCategory10Key)));
                properties.add(new ImportProperty((DataObject) customsZone, getLCP("customsZoneCustomCategory10").getMapping(customCategory10Key)));
                properties.add(new ImportProperty(customCategory10Field, getLCP("customCategory10GroupDeclaration").getMapping(groupDeclarationKey),
                        LM.object(getClass("customCategory10")).getMapping(customCategory10Key)));

                properties.add(new ImportProperty(customCategory9Field, getLCP("sidCustomCategory9").getMapping(customCategory9Key)));
                properties.add(new ImportProperty(customCategory9Field, getLCP("sidCustomCategory9CustomCategory10").getMapping(customCategory10Key)));

                List<ImportField> fields = BaseUtils.toList(userNumberGroupDeclarationField, nameDataGroupDeclarationField,
                        sumDataGroupDeclarationField, dutyDataGroupDeclarationField, customCategory10Field, customCategory9Field);

                ImportKey<?>[] keysArray = new ImportKey<?>[]{groupDeclarationKey, customCategory10Key, customCategory9Key};

                IntegrationService integrationService = new IntegrationService(context.getSession(), new ImportTable(fields, data), Arrays.asList(keysArray), properties);
                integrationService.synchronize(true, false);
                context.getSession().apply(context.getBL());
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BiffException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Double parseDouble(String value) throws ParseException {
        if (value.endsWith("грн."))
            value = value.substring(0, value.length() - 4);
        return NumberFormat.getInstance().parse(value).doubleValue();
    }
}
