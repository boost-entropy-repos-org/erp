package platform.server.data;

import platform.base.col.interfaces.immutable.ImList;
import platform.server.data.type.Type;

import java.sql.SQLException;

public interface TypePool {

    void ensureRecursion(ImList<Type> types) throws SQLException;
    void ensureConcType(ImList<Type> types) throws SQLException;
}
