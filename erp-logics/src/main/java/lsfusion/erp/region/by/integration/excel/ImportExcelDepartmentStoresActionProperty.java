package lsfusion.erp.region.by.integration.excel;

import lsfusion.erp.integration.*;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelDepartmentStoresActionProperty extends ImportExcelActionProperty {

    public ImportExcelDepartmentStoresActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {

                    ImportData importData = new ImportData();

                    importData.setDepartmentStoresList(importDepartmentStores(file));

                    new ImportActionProperty(LM).makeImport(importData, context);

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BiffException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<DepartmentStore> importDepartmentStores(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<DepartmentStore> data = new ArrayList<DepartmentStore>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String idDepartmentStore = parseString(sheet.getCell(0, i).getContents());
            String nameDepartmentStore = parseString(sheet.getCell(1, i).getContents());
            String idStore = parseString(sheet.getCell(2, i).getContents());

            data.add(new DepartmentStore(idDepartmentStore, nameDepartmentStore, idStore));
        }

        return data;
    }
}