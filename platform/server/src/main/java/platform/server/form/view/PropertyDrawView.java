package platform.server.form.view;

import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.form.ReportConstants;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.server.classes.ActionClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassType;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.table.MapKeysTable;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Map;

import static platform.interop.form.ServerResponse.CHANGE_WYS;
import static platform.server.logics.ServerResourceBundle.getString;

public class PropertyDrawView extends ComponentView {

    public PropertyDrawEntity<?> entity;

    /**
     * Usage example:
     * <pre><code>
     *  LP someLP = ...;
     *  ...
     *  PropertyDrawEntity propertyDraw = getPropertyDraw(someLP);
     *  design.get(propertyDraw).autoHide = true;
     *  </code></pre>
     */
    public boolean autoHide;
    public boolean showTableFirst;
    public boolean editOnSingleClick;
    public boolean hide;
    public String regexp;
    public String regexpMessage;
    public boolean echoSymbols;

    private int minimumCharWidth;
    private int maximumCharWidth;
    private int preferredCharWidth;

    public KeyStroke editKey;
    public boolean showEditKey = true;

    public boolean drawAsync = false;

    public Format format;

    public Boolean focusable;

    public boolean panelLabelAbove = false;

    public ExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints = new ExternalScreenConstraints();

    public String caption;
    public boolean clearText;

    @SuppressWarnings({"UnusedDeclaration"})
    public PropertyDrawView() {

    }

    public PropertyDrawView(PropertyDrawEntity entity) {
        super(entity.ID);
        this.entity = entity;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getPropertyDrawDefaultConstraints(super.getDefaultConstraints());
    }

    public Type getType() {
        return entity.propertyObject.property.getType();
    }

    public Type getChangeType(FormEntity form) {
        return entity.getRequestInputType(form);
    }
    
    public Type getChangeWYSType(FormEntity form) {
        return entity.getRequestInputType(CHANGE_WYS, form);
    }

    public Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form) {
        return entity.getAddRemove(form);        
    }

    public String getSID() {
        return entity.getSID();
    }

    public String getDefaultCaption() {
        return entity.propertyObject.property.caption;
    }

    public String getCaption() {
        return caption != null
                ? caption
                : getDefaultCaption();
    }

    public ReportDrawField getReportDrawField() {

        ReportDrawField reportField = new ReportDrawField(getSID(), getCaption());

        Type type = getType();

        reportField.minimumWidth = type.getMinimumWidth();
        reportField.setPreferredWidth(type.getPreferredWidth());

        if (getPreferredCharWidth() != 0) {
            reportField.fixedCharWidth = getPreferredCharWidth();
        }

        Format format = type.getReportFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat) format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat) format).toPattern();
        }

        reportField.hasColumnGroupObjects = !entity.getColumnGroupObjects().isEmpty();
        reportField.hasCaptionProperty = (entity.propertyCaption != null);
        reportField.hasFooterProperty = (entity.propertyFooter != null);

        // определяем класс заголовка
        if (reportField.hasCaptionProperty) {
            ReportDrawField captionField = new ReportDrawField(getSID() + ReportConstants.captionSuffix, "");
            entity.propertyCaption.property.getType().fillReportDrawField(captionField);
            reportField.captionClass = captionField.valueClass;
        } else {
            reportField.captionClass = java.lang.String.class;
        }

        // определяем класс футера
        if (reportField.hasFooterProperty) {
            ReportDrawField footerField = new ReportDrawField(getSID() + ReportConstants.footerSuffix, "");
            entity.propertyFooter.property.getType().fillReportDrawField(footerField);
            reportField.footerClass = footerField.valueClass;
        } else {
            reportField.footerClass = java.lang.String.class;
        }

        if (!getType().fillReportDrawField(reportField)) {
            return null;
        }

        return reportField;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, SerializationType.VISUAL_SETUP.equals(serializationType)
                ? caption
                : getCaption());
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        outStream.writeBoolean(echoSymbols);
        outStream.writeInt(getMinimumCharWidth());
        outStream.writeInt(getMaximumCharWidth());
        outStream.writeInt(getPreferredCharWidth());

        pool.writeObject(outStream, editKey);

        outStream.writeBoolean(showEditKey);

        outStream.writeBoolean(drawAsync);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(panelLabelAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null) {
            outStream.writeInt(externalScreen.getID());
        }
        pool.writeObject(outStream, externalScreenConstraints);

        outStream.writeBoolean(autoHide);
        outStream.writeBoolean(showTableFirst);
        outStream.writeBoolean(editOnSingleClick);
        outStream.writeBoolean(hide);

        //entity часть
        TypeSerializer.serializeType(outStream, getType());

        // асинхронные интерфейсы

        Type changeType = getChangeType(pool.context.view.entity);
        outStream.writeBoolean(changeType != null);
        if (changeType != null) {
            TypeSerializer.serializeType(outStream, changeType);
        }

        Type changeWYSType = getChangeWYSType(pool.context.view.entity);
        outStream.writeBoolean(changeWYSType != null);
        if (changeWYSType != null) {
            TypeSerializer.serializeType(outStream, changeWYSType);
        }

        Pair<ObjectEntity, Boolean> addRemove = getAddRemove(pool.context.view.entity);
        outStream.writeBoolean(addRemove != null);
        if(addRemove!=null) {
            pool.serializeObject(outStream, pool.context.view.getObject(addRemove.first));
            outStream.writeBoolean(addRemove.second);
        }

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage());

        pool.writeString(outStream, entity.getSID());
        pool.writeString(outStream, entity.propertyObject.property.toolTip);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(
                SerializationType.VISUAL_SETUP.equals(serializationType) ? entity.toDraw : entity.getToDraw(pool.context.view.entity)));

        ImOrderSet<GroupObjectEntity> columnGroupObjects = entity.getColumnGroupObjects();
        outStream.writeInt(columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(entity.propertyObject.property.checkEquals());
        outStream.writeBoolean(clearText);

        MapKeysTable<? extends PropertyInterface> mapTable = entity.propertyObject.property instanceof CalcProperty ?
                        ((CalcProperty<?>)entity.propertyObject.property).mapTable : null;
        pool.writeString(outStream, mapTable != null ? mapTable.table.name : null);

        ImMap<PropertyInterface, ValueClass> interfaceClasses = (ImMap<PropertyInterface, ValueClass>) entity.propertyObject.property.getInterfaceClasses(ClassType.FULL);
        ImMap<PropertyInterface, PropertyObjectInterfaceEntity> interfaceEntities = (ImMap<PropertyInterface, PropertyObjectInterfaceEntity>) entity.propertyObject.mapping;
        outStream.writeInt(entity.propertyObject.property.interfaces.size());
        for (PropertyInterface iFace : entity.propertyObject.property.interfaces) {
            pool.writeString(outStream, interfaceEntities.get(iFace).toString());
            interfaceClasses.get(iFace).serialize(outStream);
        }

        entity.propertyObject.property.getValueClass().serialize(outStream);
        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, entity.propertyObject.getCreationScript());
        pool.writeString(outStream, entity.propertyObject.getCreationPath());

        pool.writeString(outStream, entity.getMouseBinding());

        Map<KeyStroke, String> keyBindings = entity.getKeyBindings();
        outStream.writeInt(keyBindings == null ? 0 : keyBindings.size());
        if (keyBindings != null) {
            for (Map.Entry<KeyStroke, String> e : keyBindings.entrySet()) {
                pool.writeObject(outStream, e.getKey());
                pool.writeString(outStream, e.getValue());
            }
        }

        OrderedMap<String,String> contextMenuBindings = filterContextMenuItems(entity.getContextMenuBindings(), pool.context);
        outStream.writeInt(contextMenuBindings == null ? 0 : contextMenuBindings.size());
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                pool.writeString(outStream, contextMenuBindings.getKey(i));
                pool.writeString(outStream, contextMenuBindings.getValue(i));
            }
        }
    }

    private OrderedMap<String, String> filterContextMenuItems(OrderedMap<String, String> contextMenuBindings, ServerContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, String> contextMenuItems = new OrderedMap<String, String>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            String caption = contextMenuBindings.getValue(i);
            ActionPropertyObjectEntity<?> editAction = entity.getEditAction(actionSID, context.entity);
            if (context.securityPolicy.property.view.checkPermission(editAction.property)) {
                contextMenuItems.put(actionSID, caption);
            }
        }
        return contextMenuItems;
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
        echoSymbols = inStream.readBoolean();
        setMinimumCharWidth(inStream.readInt());
        setMaximumCharWidth(inStream.readInt());
        setPreferredCharWidth(inStream.readInt());

        editKey = pool.readObject(inStream);
        showEditKey = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = pool.context.BL.getExternalScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        autoHide = inStream.readBoolean();
        showTableFirst = inStream.readBoolean();
        editOnSingleClick = inStream.readBoolean();
        hide = inStream.readBoolean();

        entity = pool.context.entity.getPropertyDraw(inStream.readInt());
    }

    @Override
    public String toString() {
        return getCaption();
    }

    public int getMinimumCharWidth() {
        return minimumCharWidth != 0 ? minimumCharWidth : entity.propertyObject.property.minimumCharWidth;
    }

    public void setMinimumCharWidth(int minimumCharWidth) {
        this.minimumCharWidth = minimumCharWidth;
    }

    public int getMaximumCharWidth() {
        return maximumCharWidth != 0 ? maximumCharWidth : entity.propertyObject.property.maximumCharWidth;
    }

    public void setMaximumCharWidth(int maximumCharWidth) {
        this.maximumCharWidth = maximumCharWidth;
    }

    public int getPreferredCharWidth() {
        return preferredCharWidth != 0 ? preferredCharWidth : entity.propertyObject.property.preferredCharWidth;
    }

    public void setPreferredCharWidth(int preferredCharWidth) {
        this.preferredCharWidth = preferredCharWidth;
    }
    
    public String getAskConfirmMessage() {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        String msg;
        if (entity.propertyObject.property.getType() instanceof ActionClass) {
            msg = getString("form.instance.do.you.really.want.to.take.action");
        } else {
            msg = getString("form.instance.do.you.really.want.to.edit.property");
        }
        String caption = getCaption();
        if (caption != null) {
            msg += " \"" + caption + "\"?";
        }

        return msg;
    }
}