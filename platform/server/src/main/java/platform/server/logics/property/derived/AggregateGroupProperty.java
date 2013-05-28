package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MList;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChanges;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface> extends CycleGroupProperty<T ,PropertyInterface> {

    private final CalcPropertyInterfaceImplement<T> whereProp;
    private final T aggrInterface;
    private final ImSet<CalcPropertyInterfaceImplement<T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> property, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
        CalcPropertyMapImplement<?, T> and = DerivedProperty.createAnd(innerInterfaces, aggrInterface, property);
        and.property.caption = caption + "(аггр.)";
        assert groupProps.toSet().containsAll(innerInterfaces.removeIncl(aggrInterface));
        return create(sID, caption, and, groupProps, innerInterfaces, property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, CalcPropertyInterfaceImplement<T> and, ImCol<CalcPropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
        return new AggregateGroupProperty<T>(sID, caption, and, groupInterfaces, innerInterfaces, whereProp, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(String sID, String caption, CalcPropertyInterfaceImplement<T> and, ImCol<CalcPropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
        super(sID, caption, innerInterfaces, groupInterfaces, and, null);

        this.whereProp = whereProp;
        this.aggrInterface = aggrInterface;
        this.groupProps = groupProps;
    }

    // для этого во многом и делалось
    @Override
    protected boolean noIncrement() {
        return false;
    }

    @Override
    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<T>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.ifElse(changedExpr.getWhere(), prevExpr.and(changedPrevExpr.getWhere().not()));
    }

    @Override
    public ActionPropertyMapImplement<?, Interface<T>> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            PropertyInterface addedObject = new PropertyInterface();
            ImRevMap<CalcPropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = getMapInterfaces().toRevExclMap().reverse();

            ImRevMap<T, PropertyInterface> propValues = MapFact.addRevExcl(MapFact.singletonRev(aggrInterface, addedObject), // aggrInterface = aggrObject, остальные из row'а читаем
                    aggrInterfaces.filterInclRev(innerInterfaces.removeIncl(aggrInterface))); // assert что будут все в aggrInterfaces

            MList<ActionPropertyMapImplement<?, PropertyInterface>> mActions = ListFact.mList();
            if(whereProp instanceof CalcPropertyMapImplement)
                mActions.add(((CalcPropertyMapImplement<?, T>) whereProp).getSetNotNullAction(true).map(propValues));
            for(int i=0,size= aggrInterfaces.size();i<size;i++) {
                CalcPropertyInterfaceImplement<T> keyImplement = aggrInterfaces.getKey(i);
                if(keyImplement instanceof CalcPropertyMapImplement) {
                    CalcPropertyMapImplement<?, PropertyInterface> change = ((CalcPropertyMapImplement<?, T>) keyImplement).map(propValues);
                    Interface<T> valueInterface = aggrInterfaces.getValue(i);
                    ImSet<PropertyInterface> usedInterfaces = change.mapping.valuesSet().addExcl(valueInterface); // assert что не будет
                    mActions.add(DerivedProperty.createSetAction(usedInterfaces, change, (PropertyInterface) valueInterface));
                }
            }

            ImSet<PropertyInterface> setInnerInterfaces = SetFact.addExcl(interfaces, addedObject);
            return BaseUtils.immutableCast(DerivedProperty.createForAction(setInnerInterfaces, BaseUtils.<ImSet<PropertyInterface>>immutableCast(interfaces),
                    DerivedProperty.createListAction(setInnerInterfaces, mActions.immutableList()), addedObject, null, false));
        } else
            return super.getSetNotNullAction(notNull);
    }

}



