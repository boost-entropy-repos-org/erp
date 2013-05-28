package platform.server.form.entity;

import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.identity.IdentityObject;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.data.type.Type;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.ExplicitActionProperty;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static platform.interop.form.ServerResponse.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, ServerIdentitySerializable {

    private PropertyEditType editType = PropertyEditType.EDITABLE;

    public PropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, String> contextMenuBindings;
    private Map<String, ActionPropertyObjectEntity<?>> editActions;

    private boolean drawToToolbar = false;

    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean shouldBeLast = false;
    public ClassViewType forceViewType = null;
    public String eventID = null;

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public Object columnGroupObjects = SetFact.mOrderExclSet();
    private boolean finalizedColumnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public CalcPropertyObjectEntity<?> propertyCaption;
    public CalcPropertyObjectEntity<?> propertyReadOnly;
    public CalcPropertyObjectEntity<?> propertyFooter;
    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P, ?> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        setSID("propertyDraw" + ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public DataClass getRequestInputType(FormEntity form) {
        return getRequestInputType(CHANGE, form);
    }

    public DataClass getRequestInputType(String actionSID, FormEntity form) {
        Type type = null;
        if (propertyObject instanceof CalcPropertyObjectEntity) {
            ActionPropertyObjectEntity<?> changeAction = getEditAction(actionSID, form);

            if (changeAction != null) {
                type = changeAction.property.getSimpleRequestInputType();
            }
        }

        assert type == null || type instanceof DataClass;

        return (DataClass) type;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form) {
        ActionPropertyObjectEntity<A> changeAction = (ActionPropertyObjectEntity<A>) getEditAction(CHANGE, form);
        if(changeAction!=null)
            return changeAction.getAddRemove(form);
        return null;
    }

    public ActionPropertyObjectEntity<?> getEditAction(String actionId, FormEntity entity) {
        // ?? тут или нет
        if (isReadOnly() &&
                (CHANGE.equals(actionId)
                        || CHANGE_WYS.equals(actionId)
                        || EDIT_OBJECT.equals(actionId)
                        || GROUP_CHANGE.equals(actionId))) {
            return null;
        }

        if (editActions != null) {
            ActionPropertyObjectEntity editAction = editActions.get(actionId);
            if (editAction != null) {
                return editAction;
            }
        }

        Property<P> property = propertyObject.property;
        if (GROUP_CHANGE.equals(actionId)) {
            return getEditAction(CHANGE, entity).getGroupChange();
        }

        if (isSelector()) {
            return getSelectorAction(property, entity);
        }

        if (CHANGE_WYS.equals(actionId)) {
            //если CHANGE_WYS не переопределён на уровне Property и CHANGE request'ает DataClass, то возвращаем CHANGE
            if (!propertyObject.property.isChangeWYSOverriden() && getRequestInputType(entity) != null) {
                return getEditAction(CHANGE, entity);
            }
        }

        ActionPropertyMapImplement<?, P> editActionImplement = propertyObject.property.getEditAction(actionId);
        return editActionImplement == null ? null : editActionImplement.mapObjects(propertyObject.mapping);
    }

    private ActionPropertyObjectEntity<?> getSelectorAction(Property<P> property, FormEntity entity) {
        ImMap<P, ObjectEntity> groupObjects = propertyObject.mapping.filterValues(getToDraw(entity).getObjects()); // берем нижний объект в toDraw
        for (ObjectEntity objectInstance : groupObjects.valueIt()) {
            if (objectInstance.baseClass instanceof CustomClass) {
                ExplicitActionProperty dialogAction = objectInstance.getChangeAction(property);
                return new ActionPropertyObjectEntity<ClassPropertyInterface>(
                        dialogAction,
                        MapFact.singleton(dialogAction.interfaces.single(), (PropertyObjectInterfaceEntity) objectInstance)
                );
            }
        }
        return null;
    }

    public void setPropertyObject(PropertyObjectEntity<P, ?> propertyObject) {
        this.propertyObject = propertyObject;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<KeyStroke, String>();
        }
        keyBindings.put(ks, actionSID);
    }

    public void setContextMenuAction(String actionSID, String caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<String, String>();
        }
        contextMenuBindings.put(actionSID, caption);
    }

    public void setEditAction(String actionSID, ActionPropertyObjectEntity<?> editAction) {
        if(editActions==null) {
            editActions = new HashMap<String, ActionPropertyObjectEntity<?>>();
        }
        editActions.put(actionSID, editAction);
    }


    public OrderedMap<String, String> getContextMenuBindings() {
        ImOrderMap<String, String> propertyContextMenuBindings = propertyObject.property.getContextMenuBindings();
        if (propertyContextMenuBindings.isEmpty()) {
            return contextMenuBindings;
        }

        OrderedMap<String, String> result = new OrderedMap<String, String>();
        for (int i = 0; i < propertyContextMenuBindings.size(); ++i) {
            result.put(propertyContextMenuBindings.getKey(i), propertyContextMenuBindings.getValue(i));
        }

        if (contextMenuBindings == null) {
            return result;
        }

        result.putAll(contextMenuBindings);
        return result;
    }

    public Map<KeyStroke, String> getKeyBindings() {
        ImMap<KeyStroke, String> propertyKeyBindings = propertyObject.property.getKeyBindings();
        if (propertyKeyBindings.isEmpty()) {
            return keyBindings;
        }

        if (keyBindings == null) {
            return propertyKeyBindings.toJavaMap();
        }
        Map<KeyStroke, String> result = propertyKeyBindings.toJavaMap();
        result.putAll(keyBindings);
        return result;
    }

    public String getMouseBinding() {
        return mouseBinding != null ? mouseBinding : propertyObject.property.getMouseBinding();
    }

    @LongMutable
    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        if(!finalizedColumnGroupObjects) {
            finalizedColumnGroupObjects = true;
            columnGroupObjects = ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).immutableOrder();
        }

        return (ImOrderSet<GroupObjectEntity>)columnGroupObjects;
    }
    public void setColumnGroupObjects(ImOrderSet<GroupObjectEntity> columnGroupObjects) {
        assert !finalizedColumnGroupObjects;
        finalizedColumnGroupObjects = true;
        this.columnGroupObjects = columnGroupObjects;
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        assert !finalizedColumnGroupObjects;
        ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).exclAdd(columnGroupObject);
    }

    public void setPropertyCaption(CalcPropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption;
    }

    public void setPropertyFooter(CalcPropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyBackground(CalcPropertyObjectEntity propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(CalcPropertyObjectEntity propertyForeground) {
        this.propertyForeground = propertyForeground;
    }

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public boolean isSelector() {
        return editType == PropertyEditType.SELECTOR;
    }

    public boolean isReadOnly() {
        return editType == PropertyEditType.READONLY;
    }

    public boolean isEditable() {
        return editType == PropertyEditType.EDITABLE;
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView defaultView) {
        propertyObject.property.proceedDefaultDesign(propertyView, defaultView);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, getColumnGroupObjects().toJavaList());

        //не сериализуем эти property*, если они были созданы без привязки к BL, через DerivedProperty.*
        //потому что иначе не сможем десериализовать для preview
        serializeIfDeserializable(propertyCaption, pool, outStream, serializationType);
        serializeIfDeserializable(propertyReadOnly, pool, outStream, serializationType);
        serializeIfDeserializable(propertyFooter, pool, outStream, serializationType);
        serializeIfDeserializable(propertyBackground, pool, outStream, serializationType);
        serializeIfDeserializable(propertyForeground, pool, outStream, serializationType);

        outStream.writeBoolean(shouldBeLast);

        outStream.writeBoolean(editType != null);
        if (editType != null)
            outStream.writeByte(editType.serialize());

        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null)
            pool.writeString(outStream, forceViewType.name());

        //todo: serialization/deserialzation
//        pool.writeString(outStream, mouseBinding);
//        outStream.writeInt(keyBinding == null ? 0 : keyBinding.size());
//        if (keyBinding != null) {
//            for (Map.Entry<KeyStroke, String> e : keyBinding.entrySet()) {
//                pool.writeObject(outStream, e.getKey());
//                pool.writeString(outStream, e.getValue());
//            }
//        }
    }

    private void serializeIfDeserializable(CalcPropertyObjectEntity<?> propertyObject, ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        if (propertyObject != null && pool.context.BL.getProperty(propertyObject.property.getSID()) == null) {
            propertyObject = null;
        }
        pool.serializeObject(outStream, propertyObject, serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        propertyObject = pool.deserializeObject(inStream);
        toDraw = pool.deserializeObject(inStream);
        setColumnGroupObjects(SetFact.fromJavaOrderSet(pool.<GroupObjectEntity>deserializeList(inStream)));
        propertyCaption = pool.deserializeObject(inStream);
        propertyReadOnly = pool.deserializeObject(inStream);
        propertyFooter = pool.deserializeObject(inStream);
        propertyBackground = pool.deserializeObject(inStream);
        propertyForeground = pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean())
            editType = PropertyEditType.deserialize(inStream.readByte());
        if (inStream.readBoolean())
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));
    }

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null?form.getApplyObject(propertyObject.getObjectInstances()):toDraw;        
    }

    public boolean isDrawToToolbar() {
        return drawToToolbar;
    }

    public void setDrawToToolbar(boolean drawToToolbar) {
        this.drawToToolbar = drawToToolbar;
        if (drawToToolbar) {
            forceViewType = ClassViewType.PANEL;
        }
    }
}
