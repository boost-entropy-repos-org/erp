package lsfusion.erp.region.by.certificate.declaration;

import lsfusion.server.data.SQLHandledException;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.*;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportTNVEDClassifierActionProperty extends ScriptingActionProperty {

    public ImportTNVEDClassifierActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            ObjectValue countryBelarus = findProperty("country[STRING[3]]").readClasses(context, new DataObject("112", StringClass.get(3)));
            findProperty("defaultCountry[]").change(countryBelarus, context);
            context.getSession().apply(context);

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы DBF", "dbf");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {
                    importGroups(context, file);
                    importParents(context, file);
                }
            }
        } catch (xBaseJException | IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importGroups(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<List<Object>> data = new ArrayList<>();

        File tempFile = null;
        DBF dbfFile = null;
        try {

            tempFile = File.createTempFile("tempTnved", ".dbf");
            IOUtils.putFileBytes(tempFile, fileBytes);

            dbfFile = new DBF(tempFile.getPath());
            int recordCount = dbfFile.getRecordCount();

            BigDecimal defaultVAT = new BigDecimal(20);
            Date defaultDate = new Date(2001 - 1900, 0, 1);


            for (int i = 1; i <= recordCount; i++) {
                dbfFile.read();

                String groupID = new String(dbfFile.getField("KOD").getBytes(), "Cp866").trim();
                String name = new String(dbfFile.getField("NAIM").getBytes(), "Cp866").trim();
                String extraName = new String(dbfFile.getField("KR_NAIM").getBytes(), "Cp866").trim();

                Boolean hasCode = true;
                if (groupID.equals("··········")) {
                    groupID = "-" + i;
                    hasCode = null;
                }
                data.add(Arrays.asList((Object) groupID, name + extraName, i, "БЕЛАРУСЬ", hasCode, defaultVAT, defaultDate));
            }
        } finally {
            if(dbfFile != null)
                dbfFile.close();
            if(tempFile != null && !tempFile.delete())
                tempFile.deleteOnExit();
        }
        
        ImportField codeCustomsGroupField = new ImportField(findProperty("code[CustomsGroup]"));
        ImportField nameCustomsGroupField = new ImportField(findProperty("name[CustomsGroup]"));
        ImportField numberCustomsGroupField = new ImportField(findProperty("number[CustomsGroup]"));
        ImportField nameCustomsZoneField = new ImportField(findProperty("name[CustomsZone]"));
        ImportField hasCodeCustomsGroupField = new ImportField(findProperty("hasCode[CustomsGroup]"));
        ImportField vatField = new ImportField(findProperty("dataValueSupplierVAT[CustomsGroup,DATE]"));
        ImportField dateField = new ImportField(DateClass.instance);

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) findClass("CustomsGroup"), findProperty("customsGroup[STRING[10]]").getMapping(codeCustomsGroupField));
        ImportKey<?> customsZoneKey = new ImportKey((CustomClass) findClass("CustomsZone"), findProperty("customsZone[VARISTRING[50]]").getMapping(nameCustomsZoneField));
        ImportKey<?> VATKey = new ImportKey((CustomClass) findClass("Range"), findProperty("valueCurrentVATDefault[NUMERIC[10,5]]").getMapping(vatField));

        List<ImportProperty<?>> properties = new ArrayList<>();
        properties.add(new ImportProperty(codeCustomsGroupField, findProperty("code[CustomsGroup]").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(nameCustomsGroupField, findProperty("name[CustomsGroup]").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(numberCustomsGroupField, findProperty("number[CustomsGroup]").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(nameCustomsZoneField, findProperty("name[CustomsZone]").getMapping(customsZoneKey)));
        properties.add(new ImportProperty(nameCustomsZoneField, findProperty("customsZone[CustomsGroup]").getMapping(customsGroupKey),
                object(findClass("CustomsZone")).getMapping(customsZoneKey)));
        properties.add(new ImportProperty(hasCodeCustomsGroupField, findProperty("hasCode[CustomsGroup]").getMapping(customsGroupKey)));

        ImportTable table = new ImportTable(Arrays.asList(codeCustomsGroupField, nameCustomsGroupField,
                numberCustomsGroupField, nameCustomsZoneField, hasCodeCustomsGroupField, vatField, dateField), data);

        try (DataSession session = context.createSession()) {
            IntegrationService service = new IntegrationService(session, table,
                    Arrays.asList(customsGroupKey, customsZoneKey, VATKey), properties);
            service.synchronize(true, false);
            session.apply(context);
        }
    }

    private void importParents(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<List<Object>> data = new ArrayList<>();
        File tempFile = null;
        DBF file = null;
        try {
            tempFile = File.createTempFile("tempTnved", ".dbf");
            IOUtils.putFileBytes(tempFile, fileBytes);

            file = new DBF(tempFile.getPath());

            List<String> groupIDsList = new ArrayList<>();
            int recordCount = file.getRecordCount();
            for (int i = 1; i <= recordCount; i++) {
                file.read();

                String groupID = new String(file.getField("KOD").getBytes(), "Cp866").trim();
                String parentID = null;
                if (!groupID.equals("··········"))
                    for (int j = groupID.length() - 1; j > 0; j--) {
                        if (groupIDsList.contains(groupID.substring(0, j))) {
                            parentID = groupID.substring(0, j);
                            break;
                        }
                    }

                if (groupID.equals("··········")) {
                    groupID = "-" + i;
                }

                data.add(Arrays.asList((Object) groupID, parentID));
                groupIDsList.add(groupID);
            }
        } finally {
            if(file != null)
                file.close();
            if(tempFile != null && !tempFile.delete())
                tempFile.deleteOnExit();
        }

        ImportField groupIDField = new ImportField(findProperty("code[CustomsGroup]"));
        ImportField parentIDField = new ImportField(findProperty("code[CustomsGroup]"));

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) findClass("CustomsGroup"), findProperty("customsGroup[STRING[10]]").getMapping(groupIDField));
        ImportKey<?> parentCustomsGroupKey = new ImportKey((CustomClass) findClass("CustomsGroup"), findProperty("customsGroup[STRING[10]]").getMapping(parentIDField));

        List<ImportProperty<?>> properties = new ArrayList<>();
        properties.add(new ImportProperty(parentIDField, findProperty("parent[CustomsGroup]").getMapping(customsGroupKey),
                object(findClass("CustomsGroup")).getMapping(parentCustomsGroupKey)));

        ImportTable table = new ImportTable(Arrays.asList(groupIDField, parentIDField), data);

        try (DataSession session = context.createSession()) {
            IntegrationService service = new IntegrationService(session, table,
                    Arrays.asList(customsGroupKey, parentCustomsGroupKey), properties);
            service.synchronize(true, false);
            session.apply(context);
        }
    }
}