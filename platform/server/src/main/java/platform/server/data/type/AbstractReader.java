package platform.server.data.type;

import platform.base.col.interfaces.mutable.MExclMap;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractReader<T> implements Reader<T> {

    public String writeDeconc(SQLSyntax syntax, TypeEnvironment env) {
        return "?";
    }

    public void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv) {
        mResult.exclAdd(name, source);
    }

    public T read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getObject(name));
    }

}
