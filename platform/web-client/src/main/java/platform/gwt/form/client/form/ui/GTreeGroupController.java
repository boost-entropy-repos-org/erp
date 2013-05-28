package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.CellPanel;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.client.ui.ResizableLayoutPanel;
import platform.gwt.base.client.ui.ResizableVerticalPanel;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GFormChanges;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
import platform.gwt.form.shared.view.reader.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTreeGroupController extends GAbstractGroupObjectController implements DefaultFocusReceiver {
    private GTreeGroup treeGroup;

    private GTreeTable tree;

    public GGroupObject lastGroupObject;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm) {
        super(iFormController, iTreeGroup.toolbar);
        treeGroup = iTreeGroup;
        lastGroupObject = treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;

        tree = new GTreeTable(iFormController, iForm, this);

        CellPanel treeTableView = new ResizableVerticalPanel();
        treeTableView.setSize("100%", "100%");

        ResizableLayoutPanel panel = new ResizableLayoutPanel();
        panel.setStyleName("gridResizePanel");
        panel.setSize("100%", "100%");
        panel.setWidget(tree);

        treeTableView.add(panel);
        treeTableView.setCellHeight(panel, "100%");
        treeTableView.setCellWidth(panel, "100%");

        getFormLayout().add(treeGroup, treeTableView, 0);
        if (treeGroup.defaultComponent) {
            getFormLayout().addDefaultComponent(this);
        }

        addFilterButton();
    }

    public void processFormChanges(GFormChanges fc) {
        for (GGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            for (GPropertyReader propertyReader : fc.properties.keySet()) {
                if (propertyReader instanceof GPropertyDraw) {
                    GPropertyDraw property = (GPropertyDraw) propertyReader;
                    if (property.groupObject == group && !fc.updateProperties.contains(property)) {
                        addProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panel.containsProperty(property)) {
                            panel.updateColumnKeys(property, GGroupObjectValue.SINGLE_EMPTY_KEY_LIST);
                        }
                    }
                }
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                GPropertyReader propertyReader = readProperty.getKey();
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == group) {
                    propertyReader.update(this, readProperty.getValue(), fc.updateProperties.contains(propertyReader));
                }
            }

            if (fc.objects.containsKey(group)) {
                tree.setCurrentPath(fc.objects.get(group));
            }
        }
        update();
    }

    void restoreScrollPosition() {
        tree.restoreScrollPosition();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        panel.removeProperty(property);
        tree.removeProperty(group, property);
    }

    private void addProperty(GGroupObject group, GPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            addPanelProperty(group, property);
        } else {
            addGridProperty(group, property);
        }
    }

    private void addPanelProperty(GGroupObject group, GPropertyDraw property) {
        if (tree != null)
            tree.removeProperty(group, property);
        panel.addProperty(property);
    }

    private void addGridProperty(GGroupObject group, GPropertyDraw property) {
        if (tree != null)
            tree.addProperty(group, property);
        panel.removeProperty(property);
    }

    private void update() {
        if (tree != null) {
            tree.update();
        }
        panel.update();
    }

    @Override
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, updateKeys);
        } else {
            tree.updatePropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, values);
        } else {
            tree.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, values);
        } else {
            tree.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, values);
        } else {
            tree.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateReadOnlyValues(property, values);
        } else {
            tree.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowBackgroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowBackgroundValue(values.values().iterator().next());
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowForegroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowForegroundValue(values.values().iterator().next());
    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return tree.getCurrentKey();
    }

    @Override
    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        tree.changeOrder(property, modiType);
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        GTreeGridRecord record = tree.getSelectedRecord();
        return record != null
                ? record.getGroup()
                : treeGroup.groups.get(0);
    }

    @Override
    public List<GPropertyDraw> getGroupObjectProperties() {
        GGroupObject currentGroupObject = getSelectedGroupObject();

        ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
        for (GPropertyDraw property : formController.getPropertyDraws()) {
            if (currentGroupObject != null && currentGroupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    @Override
    public GPropertyDraw getSelectedProperty() {
        GPropertyDraw defaultProperty = lastGroupObject.filterProperty;
        return defaultProperty != null
                ? defaultProperty
                : tree.getCurrentProperty();
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property) {
        return tree.getSelectedValue(property);
    }

    public boolean focusFirstWidget() {
        if (GwtClientUtils.isVisible(tree)) {
            tree.setFocus(true);
            return true;
        }

        return panel.focusFirstWidget();
    }

    @Override
    protected void changeFilter(List<GPropertyFilter> conditions) {
        formController.changeFilter(treeGroup, conditions);
    }

    @Override
    public boolean focus() {
        return focusFirstWidget();
    }
}
