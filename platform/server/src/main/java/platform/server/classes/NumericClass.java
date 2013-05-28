package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class NumericClass extends IntegralClass<BigDecimal> {

    public String toString() {
        return ServerResourceBundle.getString("classes.number")+" "+length+","+precision;
    }

    final byte length;
    final byte precision;

    private NumericClass(byte length, byte precision) {
        super(ServerResourceBundle.getString("classes.numeric"));
        this.length = length;
        this.precision = precision;
    }

    public Class getReportJavaClass() {
        return BigDecimal.class;
    }

    public byte getTypeID() {
        return Data.NUMERIC; 
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        super.serialize(outStream);

        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    int getWhole() {
        return length-precision;
    }

    int getPrecision() {
        return precision;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getNumericType(length,precision);
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getNumericSQL();
    }

    public BigDecimal read(Object value) {
        if(value==null) return null;
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setBigDecimal(num, (BigDecimal) value);
    }

    public static NumericClass get(int length, int precision) {
        return get((byte)length, (byte)precision);
    }

    private static Collection<NumericClass> instances = new ArrayList<NumericClass>();

    public static NumericClass get(byte length, byte precision) {
        for(NumericClass instance : instances)
            if(instance.length == length && instance.precision==precision)
                return instance;

        NumericClass instance = new NumericClass(length,precision);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    @Override
    public int getCharLength() {
        return length;
    }

    public Object getDefaultValue() {
        return new BigDecimal("0.0");
    }

    public BigDecimal parseString(String s) throws ParseException {
        try {
            return new BigDecimal(s.replace(',','.'));
        } catch (Exception e) {
            return new BigDecimal("0.0");
        }
    }

    public String getSID() {
        return "NumericClass[" + length + "," + precision + "]";
    }

    @Override
    public Number getInfiniteValue() {
        return BigDecimal.valueOf(Double.MAX_VALUE / 2);
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(10, length);
    }
}
