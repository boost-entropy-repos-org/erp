package platform.server.logics.property.derived;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PullChangeProperty;
import platform.server.session.PropertyChanges;

// определяет не максимум изменения, а для конкретных входов
public class OnChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, OnChangeProperty.Interface<T, P>> {

    public abstract static class Interface<T extends PropertyInterface, P extends PropertyInterface> extends PropertyInterface<Interface<T, P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceEntity getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectEntity valueObject);
    }

    public static class KeyOnInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {
        T propertyInterface;

        public KeyOnInterface(T propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return mapOnValues.get(propertyInterface);
        }
    }

    public static class KeyToInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {

        P propertyInterface;

        public KeyToInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return mapToValues.get(propertyInterface);
        }
    }

    public static class ValueInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {

        CalcProperty<P> toChange;

        public ValueInterface(CalcProperty<P> toChange) {
            super(1000);

            this.toChange = toChange;
        }

        public Expr getExpr() {
            return toChange.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return valueObject;
        }
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> ImOrderSet<Interface<T, P>> getInterfaces(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        return onChange.getOrderInterfaces().mapOrderSetValues(new GetValue<Interface<T, P>, T>() {
            public Interface<T, P> getMapValue(T value) {
                return new KeyOnInterface<T, P>(value);
            }
        }).addOrderExcl(toChange.getOrderInterfaces().mapOrderSetValues(new GetValue<Interface<T, P>, P>() {
            public Interface<T, P> getMapValue(P value) {
                return new KeyToInterface<T, P>(value);
            }
        })).addOrderExcl(new ValueInterface<T, P>(toChange));
    }

    public OnChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(onChange.getSID() +"_ONCH_"+ toChange.getSID(),onChange.caption+" по ("+toChange.caption+")", getInterfaces(onChange, toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<T, P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses) // пока так
            propClasses = false;

        ImFilterValueMap<Interface<T, P>, Expr> mvMapExprs = interfaces.mapFilterValues();
        MExclMap<T, Expr> mOnChangeExprs = MapFact.mExclMapMax(interfaces.size());
        for(int i=0,size=interfaces.size();i<size;i++) {
            Interface<T, P> propertyInterface = interfaces.get(i);
            if(propertyInterface instanceof KeyOnInterface)
                mOnChangeExprs.exclAdd(((KeyOnInterface<T, P>) propertyInterface).propertyInterface, joinImplement.get(propertyInterface));
            else
                mvMapExprs.mapValue(i, propertyInterface.getExpr());
        }
        ImMap<Interface<T, P>, Expr> mapExprs = mvMapExprs.immutableValue();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(mOnChangeExprs.immutable(),
                propClasses, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.ANY, joinImplement.filterIncl(mapExprs.keys()));
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public CalcPropertyObjectEntity<Interface<T, P>> getPropertyObjectEntity(final ImMap<T, DataObject> mapOnValues, final ImMap<P, DataObject> mapToValues, final ObjectEntity valueObject) {
        ImMap<Interface<T, P>, PropertyObjectInterfaceEntity> interfaceImplement = interfaces.mapValues(new GetValue<PropertyObjectInterfaceEntity, Interface<T, P>>() {
            public PropertyObjectInterfaceEntity getMapValue(Interface<T, P> value) {
                return value.getInterface(mapOnValues, mapToValues, valueObject);
            }});
        return new CalcPropertyObjectEntity<Interface<T, P>>(this,interfaceImplement);
    }
}
