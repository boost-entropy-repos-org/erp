package platform.client.form.cell;

import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.table.AbstractTableModel;

final class SingleCellTableModel extends AbstractTableModel {
    private final ClientGroupObjectValue columnKey;
    private ClientPropertyDraw property;
    private Object value;
    private boolean readOnly;

    public SingleCellTableModel(ClientGroupObjectValue columnKey) {
        this.columnKey = columnKey;
    }

    public ClientPropertyDraw getProperty() {
        return property;
    }

    public void setProperty(ClientPropertyDraw property) {
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = property.baseType.transformServerValue(value);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public ClientGroupObjectValue getColumnKey() {
        return columnKey;
    }

    public int getRowCount() {
        return 1;
    }

    public int getColumnCount() {
        return 1;
    }

    public boolean isCellEditable(int row, int col) {
        return !readOnly;
    }

    public Object getValueAt(int row, int col) {
        return getValue();
    }

    public void setValueAt(Object value, int row, int col) {
        setValue(value);
    }
}
