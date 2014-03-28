package equ.clt.handler.lsterminal;

import com.hexiong.jdbf.JDBFException;
import com.hexiong.jdbf.JDBField;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OverJDBField extends JDBField {

    private char type = super.getType();

    public OverJDBField(String s, char c, int i, int j) throws JDBFException {
        super(s, c, i, j);
    }

    @Override
    public String format(Object obj) throws JDBFException {
        if (type == 'N' || type == 'F') {
            if (obj == null) {
                //obj = new Double(0.0D);
                String result = "";
                while (result.length() <= getLength())
                    result += " ";
                return result;
            }
            if (obj instanceof Number) {
                Number number = (Number) obj;
                StringBuffer stringbuffer = new StringBuffer(getLength());
                for (int i = 0; i < getLength(); i++) {
                    stringbuffer.append("#");

                }
                if (getDecimalCount() > 0) {
                    stringbuffer.setCharAt(getLength() - getDecimalCount() - 1, '.');
                }
                DecimalFormat decimalformat = new DecimalFormat(stringbuffer.toString());
                String s1 = decimalformat.format(number);
                int k = getLength() - s1.length();
                if (k < 0) {
                    throw new JDBFException("Value " + number +
                            " cannot fit in pattern: '" + stringbuffer +
                            "'.");
                }
                StringBuffer stringbuffer2 = new StringBuffer(k);
                for (int l = 0; l < k; l++) {
                    stringbuffer2.append(" ");

                }
                return stringbuffer2 + s1;
            } else {
                throw new JDBFException("Expected a Number, got " + obj.getClass() +
                        ".");
            }
        }
        if (type == 'C') {
            if (obj == null) {
                obj = "";
            }
            if (obj instanceof String) {
                String s = (String) obj;
                if (s.length() > getLength()) {
                    throw new JDBFException("'" + obj + "' is longer than " + getLength() +
                            " characters.");
                }
                StringBuffer stringbuffer1 = new StringBuffer(getLength() - s.length());
                for (int j = 0; j < getLength() - s.length(); j++) {
                    stringbuffer1.append(' ');

                }
                return s + stringbuffer1;
            } else {
                throw new JDBFException("Expected a String, got " + obj.getClass() +
                        ".");
            }
        }
        if (type == 'L') {
            if (obj == null) {
                //obj = new Boolean(false);
                return " ";
            }
            if (obj instanceof Boolean) {
                Boolean boolean1 = (Boolean) obj;
                return boolean1.booleanValue() ? "Y" : "N";
            } else {
                throw new JDBFException("Expected a Boolean, got " + obj.getClass() +
                        ".");
            }
        }
        if (type == 'D') {
            if (obj == null) {
                //obj = new Date();
                String result = "";
                while (result.length() <= 8)
                    result += " ";
                return result;
            }
            if (obj instanceof Date) {
                Date date = (Date) obj;
                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
                return simpledateformat.format(date);
            } else {
                throw new JDBFException("Expected a Date, got " + obj.getClass() + ".");
            }
        } else {
            throw new JDBFException("Unrecognized JDBFField type: " + type);
        }
    }


}
