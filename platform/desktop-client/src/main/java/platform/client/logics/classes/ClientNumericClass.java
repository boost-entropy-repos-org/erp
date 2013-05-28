package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.editor.DoublePropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

public class ClientNumericClass extends ClientDoubleClass {

    public final static ClientTypeClass type = new ClientTypeClass() {
        public byte getTypeId() {
            return Data.NUMERIC;
        }

        public ClientNumericClass getDefaultClass(ClientObjectClass baseClass) {
            return getDefaultType();
        }

        public ClientNumericClass getDefaultType() {
            return new ClientNumericClass(10, 2);
        }

        @Override
        public String toString() {
            return ClientResourceBundle.getString("logics.classes.number");
        }
    };

    public final int length;
    public final int precision;

    private String sID;

    public ClientNumericClass(int length, int precision) {
        this.length = length;
        this.precision = precision;
        sID = "NumericClass[" + length + "," + precision + "]";
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public ClientTypeClass getTypeClass() {
        return type;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    public Format getDefaultFormat() {
        NumberFormat format = (NumberFormat) super.getDefaultFormat();

        format.setMaximumIntegerDigits(length - precision - ((precision > 0) ? 1 : 0));
        format.setMaximumFractionDigits(precision);
        return format;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.number") + '[' + length + ',' + precision + ']';
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DoublePropertyEditor(value, (NumberFormat) property.getFormat(), property.design, BigDecimal.class);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return BigDecimal.valueOf(NumberFormat.getInstance().parse(s).doubleValue());
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.big.decimal"), 0);
        }
    }
}
