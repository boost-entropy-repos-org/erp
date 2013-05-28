package platform.server.logics.property;

import platform.base.Pair;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.form.entity.drilldown.XorUnionDrillDownFormEntity;
import platform.server.logics.BusinessLogics;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

public class XorUnionProperty extends IncrementUnionProperty {

    public XorUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces, ImList<CalcPropertyInterfaceImplement<Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final ImList<CalcPropertyInterfaceImplement<Interface>> operands; // list нужен чтобы порядок редактирования был

    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    protected Expr calculateNewExpr(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImList<Expr> operandExprs = operands.mapListValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});

        Where xorWhere = Where.FALSE;
        for(Expr operandExpr : operandExprs)
            xorWhere = xorWhere.xor(operandExpr.getWhere());
        return ValueExpr.get(xorWhere);
    }

    @Override
    protected Expr calculateIncrementExpr(final ImMap<Interface, ? extends Expr> joinImplement, final PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        ImList<Pair<Expr, Where>> operandExprs = operands.mapListValues(new GetValue<Pair<Expr, Where>, CalcPropertyInterfaceImplement<Interface>>() { // до непосредственно вычисления, для хинтов
            public Pair<Expr, Where> getMapValue(CalcPropertyInterfaceImplement<Interface> key) {
                WhereBuilder changedOperandWhere = new WhereBuilder();
                return new Pair<Expr, Where>(key.mapExpr(joinImplement, propChanges, changedOperandWhere), changedOperandWhere.toWhere());
            }
        });

        Where resultWhere = prevExpr.getWhere();
        for(int i=0,size=operands.size();i<size;i++) {
            CalcPropertyInterfaceImplement<Interface> operand = operands.get(i);
            Pair<Expr, Where> operandExpr = operandExprs.get(i);
            Where prevOperandWhere = operand.mapExpr(joinImplement).getWhere();
            resultWhere = resultWhere.xor(operandExpr.first.getWhere().xor(prevOperandWhere).and(operandExpr.second));
            if(changedWhere!=null) changedWhere.add(operandExpr.second);
        }
        return ValueExpr.get(resultWhere);
    }

    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return SetFact.add(propChanges.getUsedDataChanges(getDepends()), propChanges.getUsedChanges(getDepends()));
    }

    @Override
    @IdentityLazy
    public ImSet<DataProperty> getChangeProps() {
        MSet<DataProperty> result = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            result.addAll(operand.mapChangeProps());
        return result.immutable();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = DataChanges.EMPTY;
        for(CalcPropertyInterfaceImplement<Interface> operand : operands.reverseList()) {
            Where siblingWhere = Where.FALSE;
            for(CalcPropertyInterfaceImplement<Interface> siblingOperand : operands) // считаем where сиблингов и потом ими xor'им change
                if(siblingOperand!=operand)
                    siblingWhere = siblingWhere.xor(siblingOperand.mapExpr(change.getMapExprs(), propChanges).getWhere());
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapDataChanges(new PropertyChange<Interface>(change, ValueExpr.get(change.expr.getWhere().xor(siblingWhere))), operandWhere, propChanges));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return true;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new XorUnionDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.xor.union"), this, BL
        );
    }
}
