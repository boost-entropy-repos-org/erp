package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.SubQueryExpr;
import platform.server.data.query.IQuery;
import platform.server.data.query.QueryBuilder;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.OldProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyQueryType;
import platform.server.session.PropertyChanges;

@Aspect
public class WrapComplexityAspect {

    <K extends PropertyInterface> Expr wrapComplexity(Expr expr, Where where, CalcProperty<K> property, ImMap<K, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        Expr wrapExpr = expr;
        if(expr.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//            System.out.println("WRAP COMPLEX EXPR " + property + "(" + property.getSID() + ") : " + expr.getComplexity(true));
            wrapExpr = SubQueryExpr.create(expr);
        }
        if(where != null) {
            if(where.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//                System.out.println("WRAP COMPLEX WHERE " + property + " : " + where.getComplexity(true));
                where = SubQueryExpr.create(where.and(expr.getWhere().or(property.getExpr(joinImplement).getWhere())));
            }
            changedWhere.add(where);
        }
        return wrapExpr;
    }

    public <T extends PropertyInterface> Expr getJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty<T> property, ImMap<T, ? extends Expr> joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        if((Settings.get().isDisableWrapComplexity() && !property.complex && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity())) || !property.isFull()) // если ключей не хватает wrapp'ить нельзя
            return (Expr) thisJoinPoint.proceed();
        WhereBuilder cascadeWhere = CalcProperty.cascadeWhere(changedWhere);
        return wrapComplexity((Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propClasses, propChanges, cascadeWhere}),
                changedWhere!=null?cascadeWhere.toWhere():null, property, joinExprs, changedWhere);
    }
    @Around("execution(* platform.server.logics.property.CalcProperty.getJoinExpr(platform.base.col.interfaces.immutable.ImMap,boolean,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propClasses,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        return getJoinExpr(thisJoinPoint, property, joinExprs, propClasses, propChanges, changedWhere);
    }

    public <T extends PropertyInterface> IQuery<T, String> getQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) throws Throwable {
        assert property.isFull();
        IQuery<T, String> query = (IQuery<T, String>) thisJoinPoint.proceed();
        
        if(Settings.get().isDisableWrapComplexity() && !property.complex && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity()))
            return query;

        ImRevMap<T, KeyExpr> mapKeys = query.getMapKeys();
        Expr expr = query.getExpr("value");

        boolean changedWhere = queryType.needChange();
        Where where = changedWhere ? query.getExpr("changed").getWhere() : null;
        WhereBuilder wrapWhere = changedWhere ? new WhereBuilder() : null;
        Expr wrapExpr = wrapComplexity(expr, where, property, MapFact.addExcl(interfaceValues, mapKeys), wrapWhere);

        if(BaseUtils.hashEquals(expr, wrapExpr) && BaseUtils.nullHashEquals(where, changedWhere ? wrapWhere.toWhere() : null))
            return query;
        else {
            QueryBuilder<T, String> wrappedQuery = new QueryBuilder<T, String>(mapKeys);
            wrappedQuery.addProperty("value", wrapExpr);
            if(changedWhere)
                wrappedQuery.addProperty("changed", ValueExpr.get(wrapWhere.toWhere()));
            return wrappedQuery.getQuery();
        }
    }
    @Around("execution(* platform.server.logics.property.CalcProperty.getQuery(boolean,platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,platform.base.col.interfaces.immutable.ImMap)) " +
            "&& target(property) && args(propClasses, propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, ImMap interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, propClasses, propChanges, queryType, interfaceValues);
    }
}
