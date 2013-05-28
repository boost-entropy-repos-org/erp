package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ClassType;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ChangeBarcodeActionProperty extends SystemExplicitActionProperty {

    // заполнять поле barcode значением префикс + 0000 + id
    private final CalcProperty barcode;
    private final CalcProperty barcodePrefix;

    public ChangeBarcodeActionProperty(CalcProperty<?> barcode, CalcProperty barcodePrefix) {
        super("CBAR" + barcode.getSID(), "sys", new ValueClass[]{barcode.getInterfaceClasses(ClassType.ASSERTFULL).singleValue()});
        this.barcode = barcode;
        this.barcodePrefix = barcodePrefix;
    }

    static String formatBarcode(String prefix, String id) {
        if (prefix == null) prefix = "";
        prefix = prefix.trim();

        String barcode12 = prefix + BaseUtils.replicate('0', Math.max(12 - prefix.length() - id.length(), 0)) + id;
        int evenSum = 0;
        int oddSum = 0;
        for (int i = 0; i < barcode12.length(); i++) {
            if ((i + 1) % 2 == 0)
                evenSum += Integer.parseInt(barcode12.substring(i, i + 1));
            else
                oddSum += Integer.parseInt(barcode12.substring(i, i + 1));
        }
        int checkDigit = (evenSum * 3 + oddSum) % 10 == 0 ? 0 : 10 - (evenSum * 3 + oddSum) % 10;
        return barcode12 + checkDigit;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject object = context.getSingleDataKeyValue();
        String prefix = null;
        if (barcodePrefix != null)
            prefix = (String) barcodePrefix.read(context);
        barcode.change(MapFact.singleton(barcode.interfaces.single(), object),
                context, ChangeBarcodeActionProperty.formatBarcode(prefix, object.getValue().toString()));
    }
}
