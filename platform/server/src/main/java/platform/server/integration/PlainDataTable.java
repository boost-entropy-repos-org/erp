package platform.server.integration;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 17.12.10
 * Time: 14:54
 */

public class PlainDataTable<T> implements Iterable<PlainDataTable.Row> {
    public final ImOrderSet<T> fields; // todo [dale]: отрефакторить, не нужен тут public
    public List<List<Object>> data;
    protected final Map<T, Integer> fieldIndex;

    public PlainDataTable(List<T> fields, List<List<Object>> data) {
        this.fields = SetFact.fromJavaOrderSet(fields);
        this.data = data;

        fieldIndex = new HashMap<T, Integer>();
        for (int i = 0; i < fields.size(); i++) {
            fieldIndex.put(fields.get(i), i);
        }
    }

    public void add(PlainDataTable<T> table) {
        assert fieldIndex.equals(table.fieldIndex) && fields.equals(table.fields);
        data.addAll(table.data);
    }

    public class Row {
        private final int rowNum;

        public Row(int rowNum) {
            assert rowNum < data.size();
            this.rowNum = rowNum;
        }

        public Object getValue(int index) {
            return data.get(rowNum).get(index);
        }

        public Object getValue(T field) {
            assert fieldIndex.containsKey(field);
            return getValue(fieldIndex.get(field));
        }
    }

    private class RowIterator implements Iterator<PlainDataTable.Row> {
        private int rowNum = 0;
        public RowIterator() {

        }

        public boolean hasNext() {
            return rowNum < data.size();
        }

        public Row next() {
            return new Row(rowNum++);
        }

        public void remove() { assert false; }
    }

    public Iterator<PlainDataTable.Row> iterator() {
        return new RowIterator();
    }
}
