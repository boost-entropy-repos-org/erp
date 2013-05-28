package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.data.expr.Expr;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.expr.formula.FormulaImpl;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class FormulaImplProperty extends FormulaProperty<FormulaImplProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }
        });
    }

    private final FormulaImpl formula;

    public FormulaImplProperty(String sID, String caption, int intCount, FormulaImpl formula) {
        super(sID, caption, getInterfaces(intCount));

        this.formula = formula;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return FormulaExpr.create(formula, getOrderInterfaces().mapList(joinImplement));
    }
}
