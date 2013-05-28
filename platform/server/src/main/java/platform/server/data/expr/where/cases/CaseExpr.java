package platform.server.data.expr.where.cases;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClassSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ClassReader;
import platform.server.data.type.NullReader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassField;

import java.util.HashSet;
import java.util.Set;

public class CaseExpr extends Expr {

    private final ExprCaseList cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    public CaseExpr(ExprCaseList cases) {
        this.cases = cases;
        assert !(this.cases.size()==1 && this.cases.get(0).where.isTrue());
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return cases;
    }

    public String getSource(CompileSource compile) {

        if (compile instanceof ToString) {
            String result = "";
            for (ExprCase exprCase : cases) {
                result = (result.length() == 0 ? "" : result + ",") + exprCase.toString();
            }
            return "CE(" + result + ")";
        }

        if (cases.size() == 0) {
            return SQLSyntax.NULL;
        }

        String source = "CASE";
        boolean hasElse = true;
        for (int i = 0; i < cases.size(); i++) {
            ExprCase exprCase = cases.get(i);
            String caseSource = exprCase.data.getSource(compile);

            if (i == cases.size() - 1 && exprCase.where.isTrue()) {
                source = source + " ELSE " + caseSource;
                hasElse = false;
            } else {
                source = source + " WHEN " + exprCase.where.getSource(compile) + " THEN " + caseSource;
            }
        }
        return source + (hasElse ? " ELSE " + SQLSyntax.NULL : "") + " END";
    }

    public ConcreteClass getStaticClass() {
        ConcreteClass result = null;
        for(ExprCase exprCase : cases) {
            ConcreteClass staticClass = exprCase.data.getStaticClass();
            if(staticClass != null) {
                if(result == null)
                    result = staticClass;
                else
                    if(!BaseUtils.hashEquals(result, staticClass))
                        return null;
            }
        }
        return result;
    }

    public Type getType(KeyType keyType) {
        Type type = null;
        for(ExprCase exprCase : cases) {
            Type caseType = exprCase.data.getType(keyType);
            if(caseType!=null) {
                if(type==null) {
                    if(!(caseType instanceof DataClass))
                        return caseType;
                    type = caseType;
                } else
                    type = ((DataClass)type).getCompatible((DataClass) caseType); // для того чтобы выбрать максимальную по длине
            }
        }
        return type;
    }
    public Stat getTypeStat(Where fullWhere) {
        Stat stat = null;
        for(ExprCase exprCase : cases) {
            Stat caseStat = exprCase.data.getTypeStat(fullWhere.and(exprCase.where));
            if(caseStat!=null)
                return caseStat;
        }
        return stat;
    }

    public ClassReader getReader(KeyType keyType) {
        Type type = getType(keyType);
        if(type==null) return NullReader.instance;
        return type;
    }

    protected CaseExpr translate(MapTranslate translator) {
        return new CaseExpr(cases.translateOuter(translator));
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        MExprCaseList translatedCases = new MExprCaseList(cases.exclusive);
        for(ExprCase exprCase : cases)
            translatedCases.add(exprCase.where.translateQuery(translator),exprCase.data.translateQuery(translator));
        return translatedCases.getFinal();
    }

    public Expr followFalse(Where where, boolean pack) {
        if(where.isFalse() && !pack) return this;

        MExprCaseList mCases = new MExprCaseList(where, pack, cases.exclusive);
        for(ExprCase exprCase : cases)
            mCases.add(exprCase.where, exprCase.data);
        return mCases.getFinal();
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        MSet<OuterContext> result = SetFact.mSet();
        for(ExprCase exprCase : cases) {
            result.add(exprCase.where);
            result.add(exprCase.data);
        }
        return result.immutable();
    }

    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        // здесь по-хорошему надо andNot(верхних) но будет тормозить
        for(ExprCase exprCase : cases) {
            exprCase.where.fillJoinWheres(joins, andWhere);
            exprCase.data.fillJoinWheres(joins, andWhere.and(exprCase.where));
        }
    }

    public boolean twins(TwinImmutableObject obj) {
        return cases.equals(((CaseExpr)obj).cases);
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashContext hashContext) {
        return cases.hashOuter(hashContext) + 5;
    }

    // получение Where'ов

    public Where calculateWhere() {
        return cases.getWhere(new GetValue<Where, Expr> (){
            public Where getMapValue(Expr cCase) {
                return cCase.getWhere();
            }
        });
    }

    public Where isClass(final ValueClassSet set) {
        return cases.getWhere(new GetValue<Where, Expr>(){
            public Where getMapValue(Expr cCase) {
                return cCase.isClass(set);
            }
        });
    }

    public Where compareBase(final BaseExpr expr, final Compare compareBack) {
        return cases.getWhere(new GetValue<Where, Expr>() {
            public Where getMapValue(Expr cCase) {
                return cCase.compareBase(expr, compareBack);
            }
        });
    }
    public Where compare(final Expr expr, final Compare compare) {
        return cases.getWhere(new GetValue<Where, Expr>(){
            public Where getMapValue(Expr cCase) {
                return cCase.compare(expr,compare);
            }
        });
    }

    // получение выражений

/*
    public Expr scale(int coeff) {
        if(coeff==1) return this;
        
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where, exprCase.props.scale(coeff)); // new ExprCase(exprCase.where,exprCase.props.scale(coeff))
        return result.getExpr();
    }

    public Expr sum(Expr expr) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.props.sum(expr));
        result.add(Where.TRUE,expr); // если null то expr
        return result.getExpr();
    }*/

    public Expr classExpr(ImSet<ClassField> classes) {
        MExprCaseList result = new MExprCaseList(cases.exclusive);
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.data.classExpr(classes));
        return result.getFinal();
    }

    public Where getBaseWhere() {
        return cases.get(0).where;
    }

    private int whereDepth = -1;
    @ManualLazy
    public int getWhereDepth() {
        if(whereDepth<0) {
            for(ExprCase exprCase : cases)
                whereDepth = BaseUtils.max(whereDepth, exprCase.data.getWhereDepth());
            whereDepth = whereDepth + 1;
        }
        return whereDepth;
    }

    public Set<BaseExpr> getBaseExprs() {
        Set<BaseExpr> result = new HashSet<BaseExpr>();
        for(ExprCase exprCase : cases)
            result.addAll(exprCase.data.getBaseExprs());
        return result;
    }

    @Override
    public ObjectValue getObjectValue() {
        if(cases.size()==0)
            return NullValue.instance;
        return super.getObjectValue();
    }
}
