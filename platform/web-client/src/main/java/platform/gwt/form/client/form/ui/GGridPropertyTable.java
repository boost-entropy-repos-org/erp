package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.Header;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.CellPreviewEvent;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.NativeEditEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class GGridPropertyTable<T extends GridDataRecord> extends GPropertyTable<T> {
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    protected Map<GGroupObjectValue, Object> rowBackgroundValues = new HashMap<GGroupObjectValue, Object>();
    protected Map<GGroupObjectValue, Object> rowForegroundValues = new HashMap<GGroupObjectValue, Object>();

    protected ArrayList<GGridPropertyTableHeader> headers = new ArrayList<GGridPropertyTableHeader>();

    protected boolean needToRestoreScrollPosition = true;
    protected GGroupObjectValue oldKey = null;
    protected int oldRowScrollTop;

    public GGridSortableHeaderManager sortableHeaderManager;

    public interface GGridPropertyTableResource extends Resources {
        @Source("GGridPropertyTable.css")
        GGridPropertyTableStyle style();
    }

    public interface GGridPropertyTableStyle extends Style {}

    public static final GGridPropertyTableResource GGRID_RESOURCES = GWT.create(GGridPropertyTableResource.class);

    public GGridPropertyTable(GFormController iform) {
        super(iform, GGRID_RESOURCES);

        setTableBuilder(new GGridPropertyTableBuilder<T>(this));
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (event.getTypeInt() == Event.ONDBLCLICK) {
            if (form.isDialog() &&
                    getFocusHolderElement().isOrHasChild(Node.as(event.getEventTarget())) && // не включаем header, чтобы сортировка работала
                    !isEditable(getCurrentCellContext())) {
                stopPropagation(event);
                form.okPressed();
            }
        } else if (GKeyStroke.isAddFilterEvent(event)) {
            getGroupController().addFilter();
        } else if (GKeyStroke.isRemoveAllFiltersEvent(event)) {
            getGroupController().removeFilters();
        } else if (GKeyStroke.isReplaceFilterEvent(event)) {
            getGroupController().replaceFilter();
        } else if (GKeyStroke.isPossibleStartFilteringEvent(event)) {
            if (!isEditable(getCurrentCellContext())) {
                stopPropagation(event);
                quickFilter(new NativeEditEvent(event));
            }
        } else if (BrowserEvents.KEYDOWN.equals(event.getType()) && KeyCodes.KEY_ESCAPE == event.getKeyCode()) {
            GAbstractGroupObjectController goController = getGroupController();
            if (goController.filter != null && goController.filter.hasConditions()) {
                stopPropagation(event);
                goController.removeFilters();
                return;
            }
        }
        super.onBrowserEvent2(event);
    }

    public Cell.Context getCurrentCellContext() {
        return new Cell.Context(getKeyboardSelectedRow(), getKeyboardSelectedColumn(), getKeyboardSelectedRowValue());
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        changeBorder("black");
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        changeBorder("lightGrey");
    }

    public void changeBorder(String color) {
        getElement().getStyle().setBorderColor(color);
    }

    public GPropertyDraw getSelectedProperty() {
        return getProperty(getCurrentCellContext());
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        rowBackgroundValues = values;
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        rowForegroundValues = values;
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
    }

    public void headerClicked(GGridPropertyTableHeader header, boolean withCtrl) {
        sortableHeaderManager.headerClicked(getHeaderIndex(header), withCtrl);
        refreshHeaders();
    }

    public int getHeaderIndex(Header header) {
        return headers.indexOf(header);
    }

    public Boolean getSortDirection(Header header) {
        return sortableHeaderManager.getSortDirection(getHeaderIndex(header));
    }

    public abstract GGroupObjectValue getCurrentKey();
    public abstract GridPropertyTableKeyboardSelectionHandler getKeyboardSelectionHandler();
    public abstract void quickFilter(EditEvent event);
    public abstract GAbstractGroupObjectController getGroupController();
    abstract String getCellBackground(GridDataRecord rowValue, int row, int column);
    abstract String getCellForeground(GridDataRecord rowValue, int row, int column);

    void storeScrollPosition() {
        int selectedRow = getKeyboardSelectedRow();
        GridDataRecord selectedRecord = getKeyboardSelectedRowValue();
        if (selectedRecord != null) {
            oldKey = selectedRecord.getKey();
            TableRowElement childElement = getChildElement(selectedRow);
            if (childElement != null) {
                oldRowScrollTop = childElement.getAbsoluteTop() - getTableDataScroller().getAbsoluteTop();
            }
        }
    }

    void restoreScrollPosition() {
        if (needToRestoreScrollPosition && oldKey != null && oldRowScrollTop != -1) {
            int currentInd = getKeyboardSelectedRow();
            GGroupObjectValue currentKey = getCurrentKey();
            if (currentKey != null && currentKey.equals(oldKey) && isRowWithinBounds(currentInd)) {
                int rowTop = currentInd * getRowHeight();
                int newVerticalScrollPosition = max(0, rowTop - oldRowScrollTop);

                setDesiredVerticalScrollPosition(newVerticalScrollPosition);

                oldKey = null;
                oldRowScrollTop = -1;
                needToRestoreScrollPosition = false;
            }
        }
    }


    @Override
    public void selectNextCellInColumn(boolean down) {
        getKeyboardSelectionHandler().selectNextCellInColumn(down);
    }

    public static class GridPropertyTableKeyboardSelectionHandler<T extends GridDataRecord> extends DataGridKeyboardSelectionHandler<T> {
        public GridPropertyTableKeyboardSelectionHandler(DataGrid<T> table) {
            super(table);
        }

        @Override
        public boolean handleKeyEvent(CellPreviewEvent<T> event) {
            NativeEvent nativeEvent = event.getNativeEvent();

            assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

            int keyCode = nativeEvent.getKeyCode();
            boolean ctrlPressed = nativeEvent.getCtrlKey();
            if (keyCode == KeyCodes.KEY_HOME && !ctrlPressed) {
                getDisplay().setKeyboardSelectedColumn(0);
                return true;
            } else if (keyCode == KeyCodes.KEY_END && !ctrlPressed) {
                getDisplay().setKeyboardSelectedColumn(getDisplay().getColumnCount() - 1);
                return true;
            } else if (!ctrlPressed && !nativeEvent.getAltKey() && keyCode == KeyCodes.KEY_ENTER) {
                if (nativeEvent.getShiftKey()) {
                    nextRow(false);
                } else {
                    nextColumn(true);
                }
                return true;
            }
            return super.handleKeyEvent(event);
        }

        public void selectNextCellInColumn(boolean down) {
            nextRow(down);
        }
    }
}
