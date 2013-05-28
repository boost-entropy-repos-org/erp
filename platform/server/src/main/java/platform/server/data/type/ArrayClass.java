package platform.server.data.type;

import platform.server.classes.DataClass;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;

public class ArrayClass<T> extends DataClass<T[]> {

    private final Type<T> type;

    private ArrayClass(Type<T> type) {
        super(ServerResourceBundle.getString("classes.array")+" " + type);
        this.type = type;
    }

    public Format getReportFormat() {
        throw new RuntimeException("not supported");
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return type.getDB(syntax, typeEnv) + "[]";
    }

    public int getSQL(SQLSyntax syntax) {
        return Types.ARRAY;
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        if(1==1) throw new RuntimeException("not supported"); // не совсем понятно что с TypeEnvironment делать
        statement.setArray(num, statement.getConnection().createArrayOf(type.getDB(syntax, null), (Object[]) value)); // not tested
    }

    private static Collection<ArrayClass> arrays = new ArrayList<ArrayClass>();

    public static <T extends Type> ArrayClass<T> get(Type<T> type) {
        for(ArrayClass array : arrays)
            if(array.type.equals(type))
                return array;
        ArrayClass<T> array = new ArrayClass<T>(type);
        arrays.add(array);
//        DataClass.storeClass(array.getObjectSID(), array);
        return array;
    }
    
    public DataClass getCompatible(DataClass compClass) {
        if(compClass.equals(this))
            return this;
        return null;
    }

    public byte getTypeID() {
        throw new RuntimeException("not supported");
    }

    protected Class getReportJavaClass() {
        throw new RuntimeException("not supported"); 
    }

    public T[] parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public T[] read(Object value) {
        throw new RuntimeException("not supported");
    }

    public String getSID() { // закомментил DataClass.storeClass
        throw new RuntimeException("not supported");
    }

    public Object getDefaultValue() {
        throw new RuntimeException("not supported");
    }
}
