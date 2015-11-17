package lsfusion.erp.region.by.integration.excel;

import com.google.common.base.Throwables;
import lsfusion.erp.integration.ImportActionProperty;
import lsfusion.erp.integration.ImportData;
import lsfusion.erp.integration.UserInvoiceDetail;
import jxl.Sheet;
import jxl.read.biff.BiffException;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelUserInvoicesActionProperty extends ImportExcelActionProperty {

    public ImportExcelUserInvoicesActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setUserInvoicesList(importUserInvoices(file));

                    new ImportActionProperty(LM).makeImport(importData, context);

                }
            }
        } catch (IOException | BiffException | ParseException e) {
            throw Throwables.propagate(e);
        }
    }

    protected static List<UserInvoiceDetail> importUserInvoices(byte[] file) throws IOException, BiffException, ParseException {

        Sheet sheet = getSheet(file, 13);

        List<UserInvoiceDetail> data = new ArrayList<>();

        for (int i = 1; i < sheet.getRows(); i++) {
            String seriesUserInvoice = parseString(sheet.getCell(0, i));
            String numberUserInvoice = parseString(sheet.getCell(1, i));
            Date dateUserInvoice = parseDateValue(sheet.getCell(2, i));
            String idItem = parseString(sheet.getCell(3, i));
            BigDecimal quantity = parseBigDecimal(sheet.getCell(4, i));
            String supplier = parseString(sheet.getCell(5, i));
            String customerWarehouse = parseString(sheet.getCell(6, i));
            String supplierWarehouse = parseString(sheet.getCell(7, i));
            BigDecimal price = parseBigDecimal(sheet.getCell(8, i));
            BigDecimal chargePrice = parseBigDecimal(sheet.getCell(9, i));
            BigDecimal retailPrice = parseBigDecimal(sheet.getCell(10, i));
            BigDecimal retailMarkup = parseBigDecimal(sheet.getCell(11, i));
            String textCompliance = parseString(sheet.getCell(12, i));
            String userInvoiceDetailSID = (seriesUserInvoice == null ? "" : seriesUserInvoice) + numberUserInvoice + idItem;

            data.add(new UserInvoiceDetail(seriesUserInvoice + numberUserInvoice, seriesUserInvoice, numberUserInvoice,
                    null, true, userInvoiceDetailSID, dateUserInvoice, idItem, false, quantity, supplier,
                    customerWarehouse, supplierWarehouse, price, null, null, chargePrice, null, null, null, null,
                    retailPrice, retailMarkup, null, textCompliance, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null));
        }

        return data;
    }
}