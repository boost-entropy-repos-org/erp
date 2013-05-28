package platform.server.data.expr.order;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.sql.SQLSyntax;

public class PartitionParam extends PartitionToken {

    public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax) {
        return sources.get(this);
    }

    public int getLevel() {
        return 0;
    }
}
