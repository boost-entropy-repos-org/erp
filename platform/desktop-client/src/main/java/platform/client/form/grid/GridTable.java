package platform.client.form.grid;

import com.google.common.base.Throwables;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientPropertyTable;
import platform.client.form.GroupObjectController;
import platform.client.form.grid.groupchange.GroupChangeAction;
import platform.client.form.sort.MultiLineHeaderRenderer;
import platform.client.form.sort.TableSortableHeaderManager;
import platform.client.logics.ClientForm;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.form.ServerResponse;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static java.lang.Math.max;
import static platform.client.ClientResourceBundle.getString;

public class GridTable extends ClientPropertyTable {

    public static final String GOTO_LAST_ACTION = "gotoLastRow";
    public static final String GOTO_FIRST_ACTION = "gotoFirstRow";
    public static final String GROUP_CORRECTION_ACTION = "groupPropertyCorrection";

    private final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();

    private List<ClientGroupObjectValue> rowKeys = new ArrayList<ClientGroupObjectValue>();
    private Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> captions = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> readOnlyValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientGroupObjectValue, Object> rowBackground = new HashMap<ClientGroupObjectValue, Object>();
    private Map<ClientGroupObjectValue, Object> rowForeground = new HashMap<ClientGroupObjectValue, Object>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellBackgroundValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellForegroundValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    private ClientGroupObjectValue currentObject;

    private final GridTableModel model;

    private Action moveToNextCellAction = null;

    //нужно для отключения поиска свободных мест при навигации по таблице
    private boolean hasFocusableCells;

    private boolean isInternalNavigating = false;

    private boolean isLayouting;

    private final GridView gridView;
    private final GridController gridController;
    private final GroupObjectController groupController;

    // пока пусть GridTable напрямую общается с формой, а не через Controller, так как ей много о чем надо с ней говорить, а Controller будет просто бюрократию создавать
    private final ClientFormController form;
    private boolean tabVertical = false;

    private int viewMoveInterval = 0;
    private ClientGroupObject groupObject;
    private TableSortableHeaderManager<Pair<ClientPropertyDraw, ClientGroupObjectValue>> sortableHeaderManager;

    //для вдавливаемости кнопок
    private int pressedCellRow = -1;
    private int pressedCellColumn = -1;
    private int previousSelectedRow = 0;

    private GridSelectionController selectionController = new GridSelectionController(this);
    private KeyController keyController = new KeyController(this);

    public GridTable(GridView igridView, ClientFormController iform) {
        super(new GridTableModel());

        form = iform;
        gridView = igridView;
        gridController = gridView.getGridController();
        groupController = gridController.getGroupController();
        groupObject = groupController.getGroupObject();

        setName(groupObject.toString());

        model = getModel();

        setAutoCreateColumnsFromModel(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        sortableHeaderManager = new TableSortableHeaderManager<Pair<ClientPropertyDraw, ClientGroupObjectValue>>(this) {
            protected void orderChanged(Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey, Order modiType) {
                GridTable.this.orderChanged(columnKey, modiType);
            }

            @Override
            protected void ordersCleared(ClientGroupObject groupObject) {
                GridTable.this.ordersCleared(groupObject);
            }

            @Override
            protected Pair<ClientPropertyDraw, ClientGroupObjectValue> getColumnKey(int column) {
                return new Pair<ClientPropertyDraw, ClientGroupObjectValue>(model.getColumnProperty(column), model.getColumnKey(column));
            }

            @Override
            protected ClientPropertyDraw getColumnProperty(int column) {
                return model.getColumnProperty(column);
            }

        };

        tableHeader.setDefaultRenderer(new MultiLineHeaderRenderer(tableHeader.getDefaultRenderer(), sortableHeaderManager) {
            @Override
            public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                model.getColumnProperty(column).design.designHeader(comp);
                return comp;
            }
        });
        tableHeader.addMouseListener(sortableHeaderManager);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (groupObject != null) {
                    ClientForm.lastActiveGroupObject = groupObject;
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            private int pageSize = 50;

            public void componentResized(ComponentEvent ce) {
                //баг с прорисовкой хэдера

                // Listener срабатывает в самом начале, когда компонент еще не расположен
                // В таком случае нет смысла вызывать изменение pageSize
                if (getParent().getHeight() == 0) {
                    return;
                }

                int newPageSize = getParent().getHeight() / getRowHeight() + 1;
                if (newPageSize != pageSize) {
                    try {
                        form.changePageSize(groupObject, newPageSize);
                        pageSize = newPageSize;
                    } catch (IOException e) {
                        throw new RuntimeException(getString("errors.error.changing.page.size"), e);
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.getKeyText(e.getKeyCode()).equals("Shift") && getSelectedObject() != null) {
                    if (!keyController.isRecording) {
                        selectionController.recordingStarted(getSelectedColumn());
                    }
                    keyController.startRecording(getSelectedRow());
                }
            }

            public void keyReleased(KeyEvent e) {
                if (KeyEvent.getKeyText(e.getKeyCode()).equals("Shift")) {
                    keyController.stopRecording();
                    selectionController.recordingStopped();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int column = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());

                if ((column == -1 || row == -1)) {
                    changeSelection(getSelectedRow(), getSelectedColumn(), false, false);
                }

                if (getSelectedRow() == previousSelectedRow || isEditOnSingleClick(row, column)) {
                    pressedCellColumn = column;
                    pressedCellRow = row;
                    repaint();
                }
                previousSelectedRow = getSelectedRow();

                if (getSelectedColumn() != -1) {
                    if (MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Shift")) {
                        if (!keyController.isRecording) {//пока кривовато работает
                            keyController.startRecording(getSelectedRow());
                            selectionController.recordingStarted(getSelectedColumn());
                        }
                        keyController.completeRecording(getSelectedRow());
                        selectionController.submitShiftSelection(keyController.getValues());
                    } else {
                        selectionController.mousePressed(getSelectedColumn());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressedCellRow = -1;
                pressedCellColumn = -1;
                previousSelectedRow = getSelectedRow();

                if (!MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Shift")) {
                    selectionController.mouseReleased();
                }

                repaint();
            }
        });

        if (form.isDialog()) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        form.okPressed();
                    }
                }
            });
        }

        //имитируем продвижение фокуса вперёд, если изначально попадаем на нефокусную ячейку
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                moveToFocusableCellIfNeeded();
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                changeCurrentObjectLater();
                moveToFocusableCellIfNeeded();
            }
        });

        initializeActionMap();
    }

    private boolean isEditOnSingleClick(int row, int col) {
        return getProperty(row, col).editOnSingleClick;
    }

    private void orderChanged(Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey, Order modiType) {
        try {
            form.changePropertyOrder(columnKey.first, modiType, columnKey.second);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(getString("errors.error.changing.sorting"), e);
        }

        tableHeader.repaint();
    }

    private void ordersCleared(ClientGroupObject groupObject) {
        try {
            form.clearPropertyOrders(groupObject);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(getString("errors.error.changing.sorting"), e);
        }

        tableHeader.repaint();
    }

    private void initializeActionMap() {
        editBindingMap.setKeyAction(KeyStrokes.getGroupCorrectionKeyStroke(), ServerResponse.GROUP_CHANGE);

        final Action oldNextAction = getActionMap().get("selectNextColumnCell");
        final Action oldPrevAction = getActionMap().get("selectPreviousColumnCell");
        final Action oldFirstAction = getActionMap().get("selectFirstColumn");
        final Action oldLastAction = getActionMap().get("selectLastColumn");

        final Action nextAction = new GoToNextCellAction(true);
        final Action prevAction = new GoToNextCellAction(false);
        final Action firstAction = new GoToLastCellAction(oldFirstAction, oldNextAction);
        final Action lastAction = new GoToLastCellAction(oldLastAction, oldPrevAction);

        moveToNextCellAction = nextAction;

        ActionMap actionMap = getActionMap();
        // set left and right actions
        actionMap.put("selectNextColumn", nextAction);
        actionMap.put("selectPreviousColumn", prevAction);
        // set tab and shift-tab actions
        actionMap.put("selectNextColumnCell", nextAction);
        actionMap.put("selectPreviousColumnCell", prevAction);
        // set top and end actions
        actionMap.put("selectFirstColumn", firstAction);
        actionMap.put("selectLastColumn", lastAction);

        actionMap.put(GOTO_FIRST_ACTION, new ScrollToEndAction(Scroll.HOME));
        actionMap.put(GOTO_LAST_ACTION, new ScrollToEndAction(Scroll.END));
        actionMap.put(GROUP_CORRECTION_ACTION, new GroupChangeAction(this));

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStrokes.getCtrlHome(), GOTO_FIRST_ACTION);
        inputMap.put(KeyStrokes.getCtrlEnd(), GOTO_LAST_ACTION);
//        inputMap.put(KeyStrokes.getGroupCorrectionDialogKeyStroke(), GROUP_CORRECTION_ACTION);
    }

    int getID() {
        return groupObject.getID();
    }

    @Override
    public int hashCode() {
        return getID();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GridTable && ((GridTable) o).getID() == this.getID();
    }

    public void updateTable() {
        model.updateColumns(properties, columnKeys, captions);

        model.updateRows(rowKeys, values, readOnlyValues, rowBackground, rowForeground, cellBackgroundValues, cellForegroundValues);

        refreshColumnModel();

        if (viewMoveInterval != 0) {
            selectionController.keysChanged(viewMoveInterval < 0);
        }

        // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
        // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
        setSize(getSize().width, getRowHeight() * getRowCount());

        adjustSelection();

        setPreferredScrollableViewportSize(getPreferredSize());
        if (groupObject.tableRowsCount >= 0) {
            int count = groupObject.tableRowsCount == 0 ? getRowCount() : groupObject.tableRowsCount;
            int height = count * (getRowHeight() + 1) + getTableHeader().getPreferredSize().height;
            gridView.pane.setMinimumSize(new Dimension(getMinimumSize().width, height));
            gridView.pane.setPreferredSize(new Dimension(getPreferredSize().width, height + 2));
            gridView.pane.setMaximumSize(new Dimension(getMaximumSize().width, height + 5));
        }

        if (groupObject.grid.minimumSize != null) {
            gridView.pane.setMinimumSize(
                    SwingUtils.getOverridedSize(gridView.pane.getMinimumSize(), groupObject.grid.minimumSize));
        }

        if (groupObject.grid.preferredSize != null) {
            gridView.pane.setPreferredSize(
                    SwingUtils.getOverridedSize(gridView.pane.getPreferredSize(), groupObject.grid.preferredSize));
        }

        if (groupObject.grid.maximumSize != null) {
            gridView.pane.setMaximumSize(
                    SwingUtils.getOverridedSize(gridView.pane.getMaximumSize(), groupObject.grid.maximumSize));
        }

        previousSelectedRow = getCurrentRow();
    }

    private void changeCurrentObjectLater() {
        final ClientGroupObjectValue selectedObject = getSelectedObject();
        if (!currentObject.equals(selectedObject) && selectedObject != null) {
            setCurrentObject(selectedObject);
            SwingUtils.invokeLaterSingleAction(
                    groupObject.getActionID(),
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            changeCurrentObject(selectedObject);
                        }
                    }, 50);
        }
    }

    private void changeCurrentObject(ClientGroupObjectValue selectedObject) {
        if (currentObject.equals(selectedObject)) {
            try {
                form.changeGroupObject(groupObject, selectedObject);
            } catch (IOException ioe) {
                throw new RuntimeException(getString("errors.error.changing.current.object"), ioe);
            }
        }
    }

    private void refreshColumnModel() {
        TableColumnModel columnModel = getColumnModel();
        int newColumnCount = model.getColumnCount();
        int oldColumnCount = columnModel.getColumnCount();
        if (newColumnCount > oldColumnCount) {
            while (newColumnCount > columnModel.getColumnCount()) {
                addColumn(new TableColumn(columnModel.getColumnCount()));
            }
        } else {
            while (newColumnCount < columnModel.getColumnCount()) {
                removeColumn(columnModel.getColumn(columnModel.getColumnCount() - 1));
            }
        }

        for (int i = 0; i < model.getColumnCount(); ++i) {
            if (model.getColumnProperty(i).widthUser != null)
                getColumnModel().getColumn(i).setPreferredWidth(model.getColumnProperty(i).widthUser);
        }

        int rowHeight = 0;
        hasFocusableCells = false;
        for (int i = 0; i < model.getColumnCount(); ++i) {
            ClientPropertyDraw cell = model.getColumnProperty(i);

            TableColumn column = getColumnModel().getColumn(i);
            if (newColumnCount != oldColumnCount) {
//                if (autoResizeMode == JTable.AUTO_RESIZE_OFF) {
//                    column.setPreferredWidth(cell.getMinimumWidth(this));
//                } else {
//                    column.setPreferredWidth(cell.getPreferredWidth(this));
//                }
                column.setMinWidth(cell.getMinimumWidth(this));
                column.setMaxWidth(cell.getMaximumWidth(this));
            }
            column.setHeaderValue(model.getColumnName(i));

            rowHeight = max(rowHeight, cell.getPreferredHeight(this));

            hasFocusableCells |= cell.focusable == null || cell.focusable;

            boolean samePropAsPrevious = i != 0 && cell == model.getColumnProperty(i - 1);
            final int index = i;
            if (!samePropAsPrevious && cell.editKey != null) {
                form.getComponent().addKeyBinding(cell.editKey, cell.groupObject, new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        int leadRow = getSelectionModel().getLeadSelectionIndex();
                        if (leadRow != -1 && !isEditing()) {
                            keyController.stopRecording();
                            editCellAt(leadRow, index);
                        }
                    }
                });
            }
        }

        if (newColumnCount != oldColumnCount)
            setOrResetPreferredColumnWidths();

        if (model.getColumnCount() != 0) {
            setRowHeight(rowHeight);
            tableHeader.resizeAndRepaint();
            gridController.setForceHidden(false);
        } else {
            gridController.setForceHidden(true);
        }
    }

    private void adjustSelection() {
        //надо сдвинуть ViewPort - иначе дергаться будет

        int currentRow = getCurrentRow();

        final int dltpos = viewMoveInterval * getRowHeight();
        final Rectangle viewRect = ((JViewport) getParent()).getViewRect();

        viewRect.y += dltpos;
        if (viewRect.y < 0) {
            viewRect.y = 0;
        }

        int currentRowTop = currentRow * getRowHeight();
        int currentRowBottom = currentRowTop + getRowHeight() - 1;

        if (currentRowTop < viewRect.getMinY()) {
            viewRect.y = currentRowTop;
        } else if (currentRowBottom > viewRect.getMaxY()) {
            viewRect.y = currentRowBottom - viewRect.height;
        }
        ((JViewport) getParent()).setViewPosition(viewRect.getLocation());

        selectRow(currentRow);

        viewMoveInterval = 0;
    }

    protected void selectRow(int rowNumber) {
        if (rowNumber < 0 || rowNumber > getRowCount()) {
            return;
        }

        final int colSel = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        // scrollRectToVisible обязательно должен идти до setLeadSelectionIndex
        // иначе, если объект за пределами текущего "окна", сработает JViewport.changeListener
        // и он изменит текущий объект на другой (firstRow или lastRow)
        scrollRectToVisible(getCellRect(rowNumber, (colSel == -1) ? 0 : colSel, true));

        if (colSel == -1) {
            isInternalNavigating = true;
            changeSelection(rowNumber, 0, false, false);
            isInternalNavigating = false;
            moveToFocusableCellIfNeeded();
        } else {
            if (rowNumber != getSelectedRow()) {
                isInternalNavigating = true;
                changeSelection(rowNumber, colSel, false, false);
                isInternalNavigating = false;
            }
            getSelectionModel().setLeadSelectionIndex(rowNumber);
        }
    }

    public void setRowKeys(List<ClientGroupObjectValue> irowKeys) {
        int oldIndex = rowKeys.indexOf(currentObject);
        int newIndex = irowKeys.indexOf(currentObject);

        rowKeys = irowKeys;

        if (oldIndex != -1 && newIndex != -1) {
            viewMoveInterval = newIndex - oldIndex;
        }
        if (newIndex == -1) {
            selectionController.resetSelection();
            updateSelectionInfo();
        }
    }

    public void modifyGroupObject(ClientGroupObjectValue rowKey, boolean add) {
        if (add) {
            rowKeys.add(rowKey);
            setCurrentObject(rowKey);
        } else {
            if (currentObject.equals(rowKey))
                setCurrentObject(BaseUtils.getNearObject(BaseUtils.singleValue(rowKey), rowKeys));
            rowKeys.remove(rowKey);
        }
    }

    public void setCurrentObject(ClientGroupObjectValue value) {
        assert value == null || rowKeys.isEmpty() || rowKeys.contains(value);
        currentObject = value;
    }

    public ClientGroupObjectValue getCurrentObject() {
        return currentObject;
    }

    @Override
    public int getCurrentRow() {
        return rowKeys.indexOf(currentObject);
    }

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
    }

    public List<Pair<ClientPropertyDraw, ClientGroupObjectValue>> getVisibleProperties() {
        //возвращает все свойства, за исключеним тех, что формируют группы в колонки без единого значения
        List<Pair<ClientPropertyDraw, ClientGroupObjectValue>> props = new ArrayList<Pair<ClientPropertyDraw, ClientGroupObjectValue>>();
        for (ClientPropertyDraw property : properties) {
            for (ClientGroupObjectValue columnKey : columnKeys.get(property)) {
                if (model.getPropertyIndex(property, columnKey) != -1) {
                    props.add(new Pair<ClientPropertyDraw, ClientGroupObjectValue>(property, columnKey));
                }
            }
        }
        return props;
    }

    public List<ClientGroupObjectValue> getRowKeys() {
        return rowKeys;
    }

    private boolean fitWidth() {
        int minWidth = 0;
        TableColumnModel columnModel = getColumnModel();

        for (int i = 0; i < getColumnCount(); i++) {
            if (autoResizeMode == JTable.AUTO_RESIZE_OFF) {
                minWidth += columnModel.getColumn(i).getWidth();
            } else {
                minWidth += columnModel.getColumn(i).getMinWidth();
            }
        }

        // тут надо смотреть pane, а не саму table
        return (minWidth < getParent().getWidth());
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return fitWidth();
    }

    @Override
    public void doLayout() {
        int newAutoResizeMode = fitWidth()
                ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
                : JTable.AUTO_RESIZE_OFF;
        if (newAutoResizeMode != autoResizeMode) {
            autoResizeMode = newAutoResizeMode;
            setAutoResizeMode(newAutoResizeMode);

            setOrResetPreferredColumnWidths();
        }
        super.doLayout();
    }

    public void setOrResetPreferredColumnWidths() {
        if (getAutoResizeMode() == JTable.AUTO_RESIZE_OFF) {
            setPreferredColumnWidthsAsMinWidth();
        } else {
            resetPreferredColumnWidths();
        }
    }

    @Override
    public GridTableModel getModel() {
        return (GridTableModel) super.getModel();
    }

    private void setPreferredColumnWidthsAsMinWidth() {
        for (int i = 0; i < model.getColumnCount(); ++i) {
            getColumnModel().getColumn(i).setPreferredWidth(getColumnModel().getColumn(i).getMinWidth());
        }
    }

    private void resetPreferredColumnWidths() {
        for (int i = 0; i < model.getColumnCount(); ++i) {
            ClientPropertyDraw cell = model.getColumnProperty(i);
            getColumnModel().getColumn(i).setPreferredWidth(cell.getPreferredWidth(this));
        }
    }

    public ClientFormController getForm() {
        return form;
    }

    public ClientPropertyDraw getCurrentProperty() {
        ClientPropertyDraw selectedProperty = getSelectedProperty();
        return selectedProperty != null
                ? selectedProperty
                : model.getColumnCount() > 0
                ? model.getColumnProperty(0)
                : null;
    }

    public void pasteTable(List<List<String>> table) {
        boolean singleV = selectionController.hasSingleSelection();
        int selectedColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (selectedColumn == -1) {
            return;
        }
        int tableColumns = 0;
        if (!table.isEmpty()) {
            tableColumns = table.get(0).size();
        }
        boolean singleC = table.size() == 1 && tableColumns == 1;
        if (!singleV || !singleC) {
            int answer = SwingUtils.showConfirmDialog(form.getComponent(), getString("form.grid.sure.to.paste.multivalue"), "", JOptionPane.QUESTION_MESSAGE, 1);
            if (answer == JOptionPane.NO_OPTION) {
                return;
            }
        }

        try {
            if (singleV) {
                int columnsToInsert = Math.min(tableColumns, getColumnCount() - selectedColumn);

                List<ClientPropertyDraw> propertyList = new ArrayList<ClientPropertyDraw>();
                List<ClientGroupObjectValue> columnKeys = new ArrayList<ClientGroupObjectValue>();
                for (int i = 0; i < columnsToInsert; i++) {
                    ClientPropertyDraw propertyDraw = model.getColumnProperty(selectedColumn + i);
                    propertyList.add(propertyDraw);
                    columnKeys.add(model.getColumnKey(selectedColumn + i));
                }

                form.pasteExternalTable(propertyList, columnKeys, table, columnsToInsert);
            } else {
                Map<ClientPropertyDraw, List<ClientGroupObjectValue>> cells = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
                for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, List<ClientGroupObjectValue>> e : selectionController.getSelectedCells().entrySet()) {
                    ClientPropertyDraw property = e.getKey().first;
                    ClientGroupObjectValue columnKey = e.getKey().second;

                    List<ClientGroupObjectValue> cellKeys;
                    if (columnKey.isEmpty()) {
                        cellKeys = e.getValue();
                    } else {
                        cellKeys = new ArrayList<ClientGroupObjectValue>();
                        for (ClientGroupObjectValue rowKey : e.getValue()) {
                            ClientGroupObjectValue fullKey = rowKey.isEmpty() ? columnKey : new ClientGroupObjectValue(rowKey, columnKey);
                            cellKeys.add(fullKey);
                        }
                    }
                    cells.put(property, cellKeys);
                }

                form.pasteMulticellValue(cells, table.get(0).get(0));
                selectionController.resetSelection();
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    private boolean isCellFocusable(int row, int col) {
        //вообще говоря нужно скорректировать индексы к модели, но не актуально,
        //т.к. у нас всё равно не включено перемещение/сокрытие колонок
        return model.isCellFocusable(row, col);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (isInternalNavigating || isCellFocusable(rowIndex, columnIndex)) {
            if (!properties.isEmpty() && model.getColumnCount() > 0) {
                if (rowIndex >= getRowCount()) {
                    changeSelection(getRowCount() - 1, columnIndex, toggle, extend);
                    return;
                }
                selectionController.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
            super.changeSelection(rowIndex, columnIndex, toggle, extend);
            updateSelectionInfo();
            repaint();
        }
    }

    private void updateSelectionInfo() {
        int quantity = selectionController.getQuantity();
        if (quantity > 1) {
            int numbers = selectionController.getNumbersQuantity();
            if (numbers > 1) {
                Double sum = selectionController.getSum();
                NumberFormat format = java.text.NumberFormat.getNumberInstance();
                groupController.updateSelectionInfo(quantity, format.format(sum), format.format(sum / numbers));
                return;
            }
        }
        groupController.updateSelectionInfo(quantity, null, null);
    }

    public String getSelectedTable() throws ParseException {
        return selectionController.getSelectedTableString();
    }

    public boolean editCellAt(int row, int column, EventObject editEvent) {
        boolean edited = super.editCellAt(row, column, editEvent);
        if (!editPerformed) {
            assert !edited;
            if (KeyStrokes.isSuitableStartFilteringEvent(editEvent)) {
                groupController.quickEditFilter((KeyEvent) editEvent);
            }

            return false;
        }

        return edited;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (row < getRowCount() && column < getColumnCount()) {
            values.get(getProperty(column)).put(
                    new ClientGroupObjectValue(rowKeys.get(row), model.getColumnKey(column)),
                    value
            );
        }

        super.setValueAt(value, row, column);
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public boolean addProperty(final ClientPropertyDraw property) {
        if (!properties.contains(property)) {
            // конечно кривовато определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ins = BaseUtils.relativePosition(property, form.getPropertyDraws(), properties);
            properties.add(ins, property);
            selectionController.addProperty(property);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeProperty(ClientPropertyDraw property) {
        if (properties.contains(property)) {
            selectionController.removeProperty(property, columnKeys.get(property));
            properties.remove(property);
            readOnlyValues.remove(property);
            values.remove(property);
            captions.remove(property);
            columnKeys.remove(property);
            return true;
        }

        return false;
    }

    public void updateColumnCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        this.captions.put(property, captions);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        this.readOnlyValues.put(property, readOnlyValues);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues.put(property, cellBackgroundValues);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues.put(property, cellForegroundValues);
    }

    public void setColumnValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        BaseUtils.putUpdate(this.values, property, values, update);
    }

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys) {
        this.columnKeys.put(property, columnKeys);
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        this.rowBackground = rowBackground;
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        this.rowForeground = rowForeground;
    }

    public Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        return getSelectedValue(model.getPropertyIndex(property, columnKey));
    }

    private Object getSelectedValue(int col) {
        int row = getSelectedRow();
        if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount()) {
            return getValueAt(row, col);
        } else {
            return null;
        }
    }

    public ClientGroupObjectValue getSelectedObject() {
        int rowIndex = getSelectedRow();
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            return null;
        }
        rowIndex = convertRowIndexToModel(rowIndex);

        try {
            return rowKeys.get(rowIndex);
        } catch (IndexOutOfBoundsException be) {
            be.printStackTrace();
            return null;
        }
    }

    public ClientPropertyDraw getSelectedProperty() {
        int colView = getSelectedColumn();
        if (colView < 0 || colView >= getColumnCount()) {
            return null;
        }

        int colModel = convertColumnIndexToModel(colView);
        if (colModel < 0) {
            return null;
        }

        return model.getColumnProperty(colModel);
    }

    public boolean isPressed(int row, int column) {
        return pressedCellRow == row && pressedCellColumn == column;
    }

    public int getColumnCount() {
        return model.getColumnCount();
    }

    public ClientPropertyDraw getProperty(int row, int col) {
        return model.getColumnProperty(col);
    }

    public ClientPropertyDraw getProperty(int col) {
        return model.getColumnProperty(col);
    }

    public List<ClientPropertyDraw> getProperties() {
        return properties;
    }

    public ClientGroupObjectValue getColumnKey(int row, int col) {
        return model.getColumnKey(col);
    }

    public Pair<ClientPropertyDraw, ClientGroupObjectValue> getColumnProperty(int column) {
        return new Pair<ClientPropertyDraw, ClientGroupObjectValue>(getProperty(column), getColumnKey(0, column));
    }

    public boolean isSelected(int row, int column) {
        return selectionController.isCellSelected(getColumnProperty(column), rowKeys.get(row));
    }

    public Color getBackgroundColor(int row, int column) {
        return model.getBackgroundColor(row, column);
    }

    public Color getForegroundColor(int row, int column) {
        return model.getForegroundColor(row, column);
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        int ind = getMinPropertyIndex(property);
        sortableHeaderManager.changeOrder(new Pair<ClientPropertyDraw, ClientGroupObjectValue>(property, ind == -1 ? ClientGroupObjectValue.EMPTY : model.getColumnKey(ind)), modiType);
    }

    public void clearGridOrders(ClientGroupObject groupObject) throws IOException {
        sortableHeaderManager.clearOrders(groupObject);
    }

    public int getMinPropertyIndex(ClientPropertyDraw property) {
        return model.getPropertyIndex(property, null);
    }

    public GridTableModel getTableModel() {
        return model;
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new GridTableHeader(columnModel);
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        if (propertyDraw == null) {
            return;
        }

        int ind = getMinPropertyIndex(propertyDraw);
        if (ind != -1) {
            setColumnSelectionInterval(ind, ind);
        }
    }

    public void configureWheelScrolling(final JScrollPane pane) {
        assert pane.getViewport() == getParent();
        if (groupObject.pageSize != 0) {
            pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    int currRow = getCurrentRow();
                    if (currRow != -1) {
                        Rectangle viewRect = pane.getViewport().getViewRect();
                        int firstRow = rowAtPoint(new Point(0, viewRect.y + getRowHeight() - 1));
                        int lastRow = rowAtPoint(new Point(0, viewRect.y + viewRect.height - getRowHeight() + 1));

                        if (lastRow < firstRow) {
                            return;
                        }

                        if (isLayouting) {
                            selectRow(currRow);
                        } else {
                            if (currRow > lastRow) {
                                keyController.record(false, lastRow);
                                selectRow(lastRow);
                            } else if (currRow < firstRow) {
                                keyController.record(true, firstRow);
                                selectRow(firstRow);
                            }
                        }
                    }
                }
            });
        }
    }

    public void setLayouting(boolean isLayouting) {
        this.isLayouting = isLayouting;
    }

    private class GoToNextCellAction extends AbstractAction {
        private boolean forward;

        public GoToNextCellAction(boolean forward) {
            this.forward = forward;
        }

        private int moveNext(int row, int column, boolean forward) {

            if (forward) {
                if (tabVertical) {
                    row++;
                } else {
                    column++;
                }
            } else {
                if (tabVertical) {
                    row--;
                } else {
                    column--;
                }
            }
            int num;
            if (tabVertical) {
                num = column * getRowCount() + row;
            } else {
                num = row * getColumnCount() + column;
            }
            if (num < 0) {
                num += getColumnCount() * getRowCount();
            }
            if (num >= getColumnCount() * getRowCount()) {
                num = 0;
            }
            return num;
        }

        public void actionPerformed(ActionEvent e) {
            if (!hasFocusableCells || rowKeys.size() == 0) {
                return;
            }

            if (!form.commitCurrentEditing()) {
                return;
            }

            isInternalNavigating = true;

            int initRow = getSelectedRow();
            int initColumn = getSelectedColumn();

            int row = getSelectedRow();
            int column = getSelectedColumn();
            int oRow;
            int oColumn;
            if (row == -1 && column == -1) {
                changeSelection(0, 0, false, false);
            }
            do {
                int next = moveNext(row, column, forward);

                oRow = row;
                oColumn = column;

                if (tabVertical) {
                    column = next / getRowCount();
                    row = next % getRowCount();
                } else {
                    row = next / getColumnCount();
                    column = next % getColumnCount();
                }
                if (((row == 0 && column == 0 && forward) || (row == getRowCount() - 1 && column == getColumnCount() - 1 && (!forward)))
                        && isCellFocusable(initRow, initColumn)) {
                    row = initRow;
                    column = 0;
                    break;
                }
            } while ((oRow != row || oColumn != column) && !isCellFocusable(row, column));

            changeSelection(row, column, false, false);

            isInternalNavigating = false;
        }
    }

    private class GoToLastCellAction extends AbstractAction {
        private Action oldMoveLastAction;
        private Action oldMoveAction;

        public GoToLastCellAction(Action oldMoveLastAction, Action oldMoveAction) {
            this.oldMoveLastAction = oldMoveLastAction;
            this.oldMoveAction = oldMoveAction;
        }

        public void actionPerformed(ActionEvent e) {
            if (!hasFocusableCells || rowKeys.size() == 0) {
                return;
            }
            isInternalNavigating = true;

            oldMoveLastAction.actionPerformed(e);

            int row = getSelectedRow();
            int column = getSelectedColumn();
            int oRow = row + 1;
            int oColumn = column + 1;
            while ((oRow != row || oColumn != column) && !isCellFocusable(row, column)) {
                oldMoveAction.actionPerformed(e);
                oRow = row;
                oColumn = column;

                row = getSelectedRow();
                column = getSelectedColumn();
            }

            isInternalNavigating = false;
        }
    }

    private class ScrollToEndAction extends AbstractAction {
        private final Scroll direction;

        private ScrollToEndAction(Scroll direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (groupObject.pageSize != 0) {
                    form.changeGroupObject(groupObject, direction);
                } else if (!rowKeys.isEmpty()) {
                    switch (direction) {
                        case HOME:
                            selectRow(0);
                            break;
                        case END:
                            selectRow(rowKeys.size() - 1);
                            break;
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(getString("errors.error.moving.to.the.node"), ioe);
            }
        }
    }

    private class GridTableHeader extends JTableHeader {
        public GridTableHeader(TableColumnModel columnModel) {
            super(columnModel);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int index = columnModel.getColumnIndexAtX(e.getPoint().x);
            if (index == -1) {
                return super.getToolTipText(e);
            }
            int modelIndex = columnModel.getColumn(index).getModelIndex();

            return model.getColumnProperty(modelIndex).getTooltipText((String) columnModel.getColumn(index).getHeaderValue());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(columnModel.getTotalColumnWidth(), 34);
        }
    }

    public Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> getOrderDirections() {
        return sortableHeaderManager.getOrderDirections();
    }
}