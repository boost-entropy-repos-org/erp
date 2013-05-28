package platform.server.classes;

import org.apache.poi.POIXMLDocument;
import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ExcelClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "ExcelClass";
    }

    private static Collection<ExcelClass> instances = new ArrayList<ExcelClass>();

    public static ExcelClass get(boolean multiple, boolean storeName) {
        for (ExcelClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        ExcelClass instance = new ExcelClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ExcelClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.excel.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ExcelClass ? this : null;
    }

    public byte getTypeID() {
        return Data.EXCEL;
    }

    public String getOpenExtension(byte[] file) {
        try {
            return POIXMLDocument.hasOOXMLHeader(new ByteArrayInputStream(file)) ? "xlsx" : "xls";
        } catch (IOException e) {
            return "xls";
        }
    }
}
