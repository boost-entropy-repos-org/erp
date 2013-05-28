package platform.server.data;

import platform.server.classes.DataClass;
import platform.server.classes.DateTimeClass;
import platform.server.classes.IntegerClass;
import platform.server.classes.LongClass;
import platform.server.data.query.CompileSource;

public enum Time {
    EPOCH, HOUR, MINUTE, DATETIME;

    public DataClass getConcreteValueClass() {
        switch (this) {
            case HOUR:
                return IntegerClass.instance;
            case MINUTE:
                return IntegerClass.instance;
            case EPOCH:
                return LongClass.instance;
            case DATETIME:
                return DateTimeClass.instance;
        }
        throw new RuntimeException("Unknown time");
    }

    public String getSource(CompileSource compile) {
        switch(this) {
            case HOUR:
                return compile.syntax.getHour();
            case MINUTE:
                return compile.syntax.getMinute();
            case EPOCH:
                return compile.syntax.getEpoch();
            case DATETIME:
                return compile.syntax.getDateTime();
        }
        throw new RuntimeException("Unknown time");
    }
}
