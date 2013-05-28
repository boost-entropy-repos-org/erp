package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.Settings;
import platform.server.data.expr.query.Stat;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

// выделяется в отдельный объект так как синхронизироваться должен
public class SQLTemporaryPool {
    private final Map<FieldStruct, Set<String>> tables = MapFact.mAddRemoveMap();
    private final Map<String, Object> stats = MapFact.mAddRemoveMap();
    private final Map<String, FieldStruct> structs = MapFact.mAddRemoveMap(); // чтобы удалять таблицы, не имея структры
    private int counter = 0;

    public boolean isEmpty(Map<String, WeakReference<Object>> used) {
        return tables.isEmpty();
    }

    public String getTable(SQLSession session, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, FillTemporaryTable data, Integer count, Result<Integer> resultActual, Map<String, WeakReference<Object>> used, Result<Boolean> isNew) throws SQLException {
        FieldStruct fieldStruct = new FieldStruct(keys, properties, count);
        resultActual.set(count);

        synchronized(tables) {
            Set<String> matchTables = tables.get(fieldStruct);
            if(matchTables==null) {
                matchTables = SetFact.mAddRemoveSet();
                tables.put(fieldStruct, matchTables);
            }

            for(String matchTable : matchTables) // ищем нужную таблицу
                if(!used.containsKey(matchTable)) { // если не используется
                    session.truncate(matchTable); // удаляем старые данные
                    Integer actual = data.fill(matchTable); // заполняем
                    assert (actual!=null)==(count==null);
                    if(Settings.get().isAutoAnalyzeTempStats())
                        session.analyzeSessionTable(matchTable);
                    else {
                        Object actualStatistics = getDBStatistics(actual);
                        if(!actualStatistics.equals(stats.get(matchTable))) {
                            session.analyzeSessionTable(matchTable);
                            stats.put(matchTable, actualStatistics);
                        }
                    }
                    if(count==null)
                        resultActual.set(actual);

                    isNew.set(false);
                    return matchTable;
                }

            // если нет, "создаем" таблицу
            String table = "t_" + (counter++);
            session.createTemporaryTable(table, keys, properties);
            Integer actual = data.fill(table); // заполняем
            assert (actual!=null)==(count==null);
            session.analyzeSessionTable(table); // выполняем ее analyze, чтобы сказать постгре какие там будут записи
            if(count==null) { // обновляем реальной статистикой
                if(!Settings.get().isAutoAnalyzeTempStats())
                    stats.put(table, getDBStatistics(actual));
                resultActual.set(actual);
            }
            structs.put(table, fieldStruct);

            matchTables.add(table);

            isNew.set(true);
            return table;
        }
    }

    public void removeTable(String table) {
        synchronized (tables) {
            if(!Settings.get().isAutoAnalyzeTempStats())
                stats.remove(table);
            FieldStruct fieldStruct = structs.remove(table);
            Set<String> structTables = tables.get(fieldStruct);
            structTables.remove(table);
            if(structTables.isEmpty())
                tables.remove(fieldStruct);
        }
    }

    private static class FieldStruct {

        public final ImOrderSet<KeyField> keys;
        public final ImSet<PropertyField> properties;

        private final Object statistics;

        public FieldStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count) {
            this.keys = keys;
            this.properties = properties;

            if(Settings.get().isAutoAnalyzeTempStats() || count==null)
                this.statistics = null;
            else
                this.statistics = getDBStatistics(count);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof FieldStruct && keys.equals(((FieldStruct) o).keys) && properties.equals(((FieldStruct) o).properties) && BaseUtils.nullEquals(statistics, ((FieldStruct) o).statistics);

        }

        @Override
        public int hashCode() {
            return 31 * (31 * keys.hashCode() + properties.hashCode()) + BaseUtils.nullHash(statistics);
        }
    }

    public static Object getDBStatistics(int count) {
        return new Stat(count);
    }
}
