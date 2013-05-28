package platform.server.data.sql;

import org.apache.log4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;
import platform.server.data.AbstractConnectionPool;
import platform.server.data.TypePool;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

public abstract class DataAdapter extends AbstractConnectionPool implements SQLSyntax, TypePool {
    protected final static Logger logger = Logger.getLogger(DataAdapter.class);

    public String server;
    public String dataBase;
    public String userID;
    public String password;

    // для debuga
    protected DataAdapter() {
    }

    abstract void ensureDB() throws Exception, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String dataBase, String server, String userID, String password) throws Exception, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        this.dataBase = dataBase;
        this.server = server;
        this.userID = userID;
        this.password = password;

        ensureDB();
    }

    public String getStringType(int length) {
        return "char(" + length + ")";
    }
    public int getStringSQL() {
        return Types.CHAR;
    }

    @Override
    public String getVarStringType(int length) {
        return "varchar(" + length + ")";
    }
    @Override
    public int getVarStringSQL() {
        return Types.VARCHAR;
    }

    public String getNumericType(int length, int precision) {
        return "numeric(" + length + "," + precision + ")";
    }
    public int getNumericSQL() {
        return Types.NUMERIC;
    }

    public String getIntegerType() {
        return "integer";
    }
    public int getIntegerSQL() {
        return Types.INTEGER;
    }

    public String getDateType() {
        return "date";
    }
    public int getDateSQL() {
        return Types.DATE;
    }

    public String getDateTimeType() {
        return "timestamp";
    }
    public int getDateTimeSQL() {
        return Types.TIMESTAMP;
    }

    public String getTimeType() {
        return "time";
    }
    public int getTimeSQL() {
        return Types.TIME;
    }

    public String getLongType() {
        return "long";
    }
    public int getLongSQL() {
        return Types.BIGINT;
    }

    public String getDoubleType() {
        return "double precision";
    }
    public int getDoubleSQL() {
        return Types.DOUBLE;
    }

    public String getBitType() {
        return "integer";
    }
    public int getBitSQL() {
        return Types.INTEGER;
    }

    public String getTextType() {
        return "text";
    }
    public int getTextSQL() {
        return Types.VARCHAR;
    }

    public boolean hasDriverCompositeProblem() {
        return false;
    }

    public int getCompositeSQL() {
        return Types.BINARY;
    }

    public String getByteArrayType() {
        return "longvarbinary";
    }
    public int getByteArraySQL() {
        return Types.LONGVARBINARY;
    }

    public String getColorType() {
        return "integer";
    }

    public int getColorSQL() {
        return Types.INTEGER;
    }

    public String getBitString(Boolean value) {
        return (value ? "1" : "0");
    }

    public int updateModel() {
        return 0;
    }

    // по умолчанию
    public String getClustered() {
        return "CLUSTERED ";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return true;
    }

    public String getCommandEnd() {
        return "";
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE TEMPORARY TABLE " + tableName + " (" + declareString + ")";
    }

    public String getSessionTableName(String tableName) {
        return tableName;
    }

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return true;
    }

    public boolean orderUnion() {
        return false;
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE " + getSessionTableName(tableName);
    }

    public String getOrderDirection(boolean descending, boolean notNull) {
        return descending ? "DESC" : "ASC";
    }

    public boolean nullUnionTrouble() {
        return false;
    }

    public boolean inlineTrouble() {
        return false;
    }

    public String getHour() {
        return "EXTRACT(HOUR FROM CURRENT_TIME)";
    }

    public String getMinute() {
        return "EXTRACT(MINUTE FROM CURRENT_TIME)";
    }

    public String getEpoch() {
        return "EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)";
    }

    public String getDateTime() {
        return "CURRENT_TIMESTAMP";
    }

    public String typeConvertSuffix(Type oldType, Type newType, String name, TypeEnvironment typeEnv) {
        return "";
    }

    public String getInsensitiveLike() {
        return "LIKE";
    }

    public boolean supportGroupNumbers() {
        return false;
    }

    public String getCountDistinct(String field) {
        return "COUNT(DISTINCT " + field + ")";
    }
    public String getCount(String field) {
        return "COUNT(" + field + ")";
    }

    public boolean noDynamicSampling() {
        return true;
    }

    public boolean orderTopTrouble() {
        throw new RuntimeException("unknown");
    }

    public String backupDB(String dumpFileName) throws IOException, InterruptedException {
        return null;
    }

    public String getAnalyze(){
        return "";
    }

    public void useDLL(){
        /*try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            Connection connect = DriverManager.getConnection("jdbc:jtds:sqlserver://localhost:1433;instance=SQLEXPRESS;User=sa;Password=11111");

            InputStream dllStream = Main.class.getResourceAsStream("SQLUtils.dll");
            String dllName = "SQLUtils";

            connect.createStatement().execute("USE test");

            connect.createStatement().execute("IF OBJECT_ID(N'Concatenate', N'AF') is not null DROP Aggregate Concatenate;");

            PreparedStatement statement = connect.prepareStatement("IF EXISTS (SELECT * FROM sys.assemblies WHERE [name] = ?) DROP ASSEMBLY SQLUtils;");
            statement.setString(1, dllName);
            statement.execute();
            statement.clearParameters();

            statement = connect.prepareStatement("CREATE ASSEMBLY [SQLUtils] \n" +
                    "FROM  ? "+
                    "WITH permission_set = Safe;");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (dllStream.read(buffer) != -1) out.write(buffer);

            statement.setBytes(1, out.toByteArray());
            statement.execute();
            statement.clearParameters();

            connect.createStatement().execute("CREATE AGGREGATE [dbo].[Concatenate](@input nvarchar(4000))\n" +
                    "RETURNS nvarchar(4000)\n" +
                    "EXTERNAL NAME [SQLUtils].[Concatenate];");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
    }

    public static String genTypePostfix(ImList<Type> types) {
        String result = "";
        for(int i=0,size=types.size();i<size;i++)
            result = (result.length()==0?"":result + "_") + types.get(i).getSID();
        return result;
    }

    public static String genConcTypeName(ImList<Type> types) {
        return "T" + genTypePostfix(types);
    }

    public static String genRecursionName(ImList<Type> types) {
        return "recursion_" + genTypePostfix(types);
    }

    public static String genNRowName(ImList<Type> types) {
        return "NROW" + types.size();
    }

    private final TypeEnvironment recTypes = new TypeEnvironment() {
        public void addNeedRecursion(ImList<Type> types) {
            throw new UnsupportedOperationException();
        }

        public void addNeedType(ImList<Type> types) {
            try {
                ensureConcType(types);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    };

    protected Connection ensureConnection;

    protected void executeEnsure(String command) throws SQLException {
        Statement statement = ensureConnection.createStatement();
        try {
            statement.execute(command);
        } catch(SQLException e) {
        } finally {
            statement.close();
        }
    }

    private MCacheMap<ImList<Type>, Boolean> ensuredConcTypes = LRUCache.mBig();

    protected String notNullRowString;

    public synchronized void ensureConcType(ImList<Type> types) throws SQLException {

        Boolean ensured = ensuredConcTypes.get(types);
        if(ensured != null)
            return;

        // ensuring types
        String declare = "";
        for (int i=0,size=types.size();i<size;i++)
            declare = (declare.length() ==0 ? "" : declare + ",") + ConcatenateType.getFieldName(i) + " " + types.get(i).getDB(this, recTypes);

        executeEnsure("CREATE TYPE " + genConcTypeName(types) + " AS (" + declare + ")");

        ensuredConcTypes.exclAdd(types, true);
    }

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    protected String recursionString;

    private MCacheMap<ImList<Type>, Boolean> ensuredRecursion = LRUCache.mBig();

    public synchronized void ensureRecursion(ImList<Type> types) throws SQLException {

        Boolean ensured = ensuredRecursion.get(types);
        if(ensured != null)
            return;

        String declare = "";
        String using = "";
        for (int i=0,size=types.size();i<size;i++) {
            String paramName = "p" + i;
            Type type = types.get(i);
            declare = declare + ", " + paramName + " " + type.getDB(this, recTypes);
            using = (using.length() == 0 ? "USING " : using + ",") + paramName;
        }

        Properties properties = new Properties();
        properties.put("function.name", genRecursionName(types));
        properties.put("params.declare", declare);
        properties.put("params.usage", using);

        executeEnsure(stringResolver.replacePlaceholders(recursionString, properties));

        ensuredRecursion.exclAdd(types, true);
    }
}
