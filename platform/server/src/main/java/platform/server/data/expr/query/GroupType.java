package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.Settings;
import platform.server.classes.StringClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.property.*;

public enum GroupType implements AggrType {
    SUM, MAX, MIN, ANY, STRING_AGG, AGGAR_SETADD, LAST;

    public <T extends PropertyInterface> GroupProperty<T> createProperty(String sID, String caption, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> property, ImSet<? extends CalcPropertyInterfaceImplement<T>> interfaces) {
        switch (this) {
            case MAX:
                return new MaxGroupProperty<T>(sID, caption, innerInterfaces, interfaces, property, false);
            case MIN:
                return new MaxGroupProperty<T>(sID, caption, innerInterfaces, interfaces, property, true);
            case SUM:
                return new SumGroupProperty<T>(sID, caption, innerInterfaces, interfaces, property);
        }
        throw new RuntimeException("not supported");
    }

    public Expr add(Expr op1, Expr op2) {
        switch (this) {
            case MAX:
                return op1.max(op2);
            case MIN:
                return op1.min(op2);
            case SUM:
                return op1.sum(op2);
            case ANY: // для этого ANY и делается
                return op1.nvl(op2);
        }
        throw new RuntimeException("can not be");
    }

    public boolean isSelect() {
        return this==MAX || this==MIN || this==ANY || this==LAST;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isSelectNotInWhere() { // в общем то оптимизационная вещь потом можно убрать
//        assert isSelect();
        return this == LAST;
    }
    public Where getWhere(ImList<Expr> exprs) {
        if(this==LAST) {
            assert exprs.size()==2;
            return exprs.get(0).getWhere();
        }
        return Expr.getWhere(exprs);
    }

    public Expr getMainExpr(ImList<Expr> exprs) {
        return getSingleExpr(exprs, null);
    }

    public Expr getSingleExpr(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
        if(this==LAST) {
            assert exprs.size()==2;
            return exprs.get(1);
        }
        return exprs.get(0);
    }

    public boolean hasAdd() {
        return this!=STRING_AGG && this!=AGGAR_SETADD && this!=LAST;
    }

    // если не комутативен и не инвариантен к появляению в выборке null'а
    public boolean nullsNotAllowed() {
        return this == LAST;
    }

    public boolean splitExprCases() {
        assert hasAdd();
        return isSelect() && Settings.get().isSplitGroupSelectExprcases();
    }

    public boolean splitInnerJoins() {
        assert hasAdd();
        return isSelect() && Settings.get().isSplitSelectGroupInnerJoins();
    }

    public boolean splitInnerCases() {
        assert hasAdd();
        return false;
    }

    public boolean exclusive() {
        assert hasAdd();
        return !isSelect();
    }

    public String getSource(ImList<String> exprs, ImOrderMap<String, Boolean> orders, ImSet<String> ordersNotNull, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
        String orderClause = BaseUtils.clause("ORDER BY", Query.stringOrder(orders, ordersNotNull, syntax));

        switch (this) {
            case MAX:
                assert exprs.size()==1 && orders.size()==0;
                return (type instanceof ConcatenateType ? "MAXC" : "MAX") + "(" + exprs.get(0) + ")";
            case MIN:
                assert exprs.size()==1 && orders.size()==0;
                return (type instanceof ConcatenateType ? "MINC" : "MIN") + "(" + exprs.get(0) + ")";
            case ANY:
                assert exprs.size()==1 && orders.size()==0;
                return "ANYVALUE(" + exprs.get(0) + ")";
            case SUM:
                assert exprs.size()==1 && orders.size()==0;
                return "notZero(SUM(" + exprs.get(0) + "))";
            case STRING_AGG:
                assert exprs.size()==2;
                return type.getCast("STRING_AGG(" + exprs.get(0) + "," + exprs.get(1) + orderClause + ")", syntax, typeEnv, false); // тут точная ширина не нужна главное чтобы не больше
            case AGGAR_SETADD:
                assert exprs.size()==1;
                return "AGGAR_SETADD(" + exprs.get(0) + orderClause + ")";
            case LAST:
                assert exprs.size()==2;
                return "LAST(" + exprs.get(1) + orderClause + ")";
            default:
                throw new RuntimeException("can not be");
        }
    }

    public int numExprs() {
        if(this==STRING_AGG || this==LAST)
            return 2;
        else
            return 1;
    }

    public Type getType(Type exprType) {
        if(this==STRING_AGG)
            return ((StringClass)exprType).extend(10);
        else
            return exprType;
    }
}
