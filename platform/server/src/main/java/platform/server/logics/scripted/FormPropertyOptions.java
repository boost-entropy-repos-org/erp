package platform.server.logics.scripted;

import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.isRedundantString;
import static platform.base.BaseUtils.nvl;

public class FormPropertyOptions {
    private PropertyEditType editType;
    private Boolean hintNoUpdate;
    private Boolean hintTable;
    private Boolean drawToToolbar;
    private List<GroupObjectEntity> columns;
    private MappedProperty showIf;
    private CalcPropertyObjectEntity readOnlyIf;
    private CalcPropertyObjectEntity background;
    private CalcPropertyObjectEntity foreground;
    private CalcPropertyObjectEntity header;
    private CalcPropertyObjectEntity footer;
    private ClassViewType forceViewType;
    private GroupObjectEntity toDraw;
    private OrderedMap<String, String> contextMenuBindings;
    private Map<String, ActionPropertyObjectEntity> editActions;
    private String eventId;
    private PropertyDrawEntity neighbourPropertyDraw;
    private String neighbourPropertyText;
    private Boolean isRightNeighbour;

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public List<GroupObjectEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<GroupObjectEntity> columns) {
        this.columns = columns;
    }

    public void setShowIf(MappedProperty showIf) {
        this.showIf = showIf;
    }

    public MappedProperty getShowIf() {
        return showIf;
    }

    public CalcPropertyObjectEntity getReadOnlyIf() {
        return readOnlyIf;
    }

    public void setReadOnlyIf(CalcPropertyObjectEntity readOnlyIf) {
        this.readOnlyIf = readOnlyIf;
    }

    public CalcPropertyObjectEntity getBackground() {
        return background;
    }

    public void setBackground(CalcPropertyObjectEntity background) {
        this.background = background;
    }

    public CalcPropertyObjectEntity getForeground() {
        return foreground;
    }

    public void setForeground(CalcPropertyObjectEntity foreground) {
        this.foreground = foreground;
    }

    public CalcPropertyObjectEntity getHeader() {
        return header;
    }

    public void setHeader(CalcPropertyObjectEntity header) {
        this.header = header;
    }

    public CalcPropertyObjectEntity getFooter() {
        return footer;
    }

    public void setFooter(CalcPropertyObjectEntity footer) {
        this.footer = footer;
    }

    public void setForceViewType(ClassViewType forceViewType) {
        this.forceViewType = forceViewType;
    }

    public ClassViewType getForceViewType() {
        return forceViewType;
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public GroupObjectEntity getToDraw() {
        return toDraw;
    }

    public Boolean getHintNoUpdate() {
        return hintNoUpdate;
    }

    public void setHintNoUpdate(Boolean hintNoUpdate) {
        this.hintNoUpdate = hintNoUpdate;
    }

    public Boolean getHintTable() {
        return hintTable;
    }

    public void setHintTable(Boolean hintTable) {
        this.hintTable = hintTable;
    }

    public Boolean getDrawToToolbar() {
        return drawToToolbar;
    }

    public void setDrawToToolbar(Boolean drawToToolbar) {
        this.drawToToolbar = drawToToolbar;
    }

    public void addEditAction(String actionSID, ActionPropertyObjectEntity action) {
        if (action != null) {
            if (editActions == null) {
                editActions = new HashMap<String, ActionPropertyObjectEntity>();
            }
            editActions.put(actionSID, action);
        }
    }

    public void addContextMenuBinding(String actionSID, String caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<String, String>();
        }
        contextMenuBindings.put(actionSID, caption);
    }

    public void addContextMenuEditAction(String caption, ActionPropertyObjectEntity action) {
        if (action != null) {
            if (isRedundantString(caption)) {
                caption = action.property.caption;
            }
            if (isRedundantString(caption)) {
                caption = action.property.getSID();
            }

            addEditAction(action.property.getSID(), action);
            addContextMenuBinding(action.property.getSID(), caption);
        }
    }

    public OrderedMap<String, String> getContextMenuBindings() {
        return contextMenuBindings;
    }

    public void setContextMenuBindings(OrderedMap<String, String> contextMenuBindings) {
        this.contextMenuBindings = contextMenuBindings;
    }

    public Map<String, ActionPropertyObjectEntity> getEditActions() {
        return editActions;
    }

    public void setEditActions(Map<String, ActionPropertyObjectEntity> editActions) {
        this.editActions = editActions;
    }

    public PropertyDrawEntity getNeighbourPropertyDraw() {
        return neighbourPropertyDraw;
    }

    public String getNeighbourPropertyText() {
        return neighbourPropertyText;
    }

    public void setNeighbourPropertyDraw(PropertyDrawEntity neighbourPropertyDraw, String propText) {
        this.neighbourPropertyDraw = neighbourPropertyDraw;
        this.neighbourPropertyText = propText;
    }

    public Boolean isRightNeighbour() {
        return isRightNeighbour;
    }

    public void setNeighbourType(Boolean isRightNeighbour) {
        this.isRightNeighbour = isRightNeighbour;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public FormPropertyOptions overrideWith(FormPropertyOptions overrides) {
        FormPropertyOptions merged = new FormPropertyOptions();

        merged.setEditType(nvl(overrides.getEditType(), editType));
        merged.setHintNoUpdate(nvl(overrides.getHintNoUpdate(), hintNoUpdate));
        merged.setHintTable(nvl(overrides.getHintTable(), hintTable));
        merged.setDrawToToolbar(nvl(overrides.getDrawToToolbar(), drawToToolbar));
        merged.setColumns(nvl(overrides.getColumns(), columns));
        merged.setShowIf(nvl(overrides.getShowIf(), showIf));
        merged.setReadOnlyIf(nvl(overrides.getReadOnlyIf(), readOnlyIf));
        merged.setBackground(nvl(overrides.getBackground(), background));
        merged.setForeground(nvl(overrides.getForeground(), foreground));
        merged.setHeader(nvl(overrides.getHeader(), header));
        merged.setFooter(nvl(overrides.getFooter(), footer));
        merged.setForceViewType(nvl(overrides.getForceViewType(), forceViewType));
        merged.setToDraw(nvl(overrides.getToDraw(), toDraw));
        merged.setEditActions(nvl(overrides.getEditActions(), editActions));
        merged.setContextMenuBindings(nvl(overrides.getContextMenuBindings(), contextMenuBindings));
        merged.setEventId(nvl(overrides.getEventId(), eventId));
        merged.setNeighbourPropertyDraw(nvl(overrides.getNeighbourPropertyDraw(), neighbourPropertyDraw), nvl(overrides.getNeighbourPropertyText(), neighbourPropertyText));
        merged.setNeighbourType(nvl(overrides.isRightNeighbour(), isRightNeighbour));
        return merged;
    }
}
