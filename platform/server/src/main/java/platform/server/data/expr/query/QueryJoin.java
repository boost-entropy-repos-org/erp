package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

// query именно Outer а не Inner, потому как его контекст "связан" с group, и его нельзя прозрачно подменять
public abstract class QueryJoin<K extends Expr,I extends OuterContext<I>, T extends QueryJoin<K, I, T, OC>,
        OC extends QueryJoin.QueryOuterContext<K,I,T,OC>> extends AbstractInnerContext<T> implements InnerJoin<K, T> {

    protected final I query;
    public final ImMap<K, BaseExpr> group; // вообще гря не reverseable

    public abstract static class QueryOuterContext<K extends Expr,I extends OuterContext<I>, T extends QueryJoin<K, I, T, OC>,
            OC extends QueryJoin.QueryOuterContext<K,I,T,OC>> extends AbstractOuterContext<OC> {

        protected final T thisObj;
        protected QueryOuterContext(T thisObj) {
            this.thisObj = thisObj;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(final HashContext hashContext) {
            return new QueryInnerHashContext<K, I>(thisObj) {
                protected int hashOuterExpr(BaseExpr outerExpr) {
                    return outerExpr.hashOuter(hashContext);
                }
            }.hashValues(hashContext.values);
        }

        public T getThis() {
            return thisObj;
        }

        @Override
        public ImSet<OuterContext> calculateOuterDepends() {
            return BaseUtils.immutableCast(thisObj.group.values().toSet());
        }

        @Override
        public ImSet<Value> getValues() {
            return super.getValues().merge(thisObj.getInnerValues());
        }

        @Override
        public ImSet<StaticValueExpr> getOuterStaticValues() {
            throw new RuntimeException("should not be");
        }

        public boolean twins(TwinImmutableObject o) {
            return thisObj.equals(thisObj);
        }

        public InnerExpr getInnerExpr(WhereJoin join) {
            return QueryJoin.getInnerExpr(thisObj, join);
        }
        public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
            return InnerExpr.getExprFollows(thisObj, recursive);
        }
        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(thisObj);
        }
        public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) {
            return InnerExpr.getFollowJoins(thisObj, upWheres, unionJoins);
        }

        public abstract T translateThis(MapTranslate translate);

        protected OC translate(MapTranslate translator) {
            return translateThis(translator).getOuter();
        }
    }
    protected abstract OC createOuterContext();
    protected OC outer;
    private OC getOuter() {
        if(outer==null)
            outer = createOuterContext();
        return outer;
    }
    public ImSet<ParamExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ExprEnumerator enumerator) {
        return getOuter().enumerate(enumerator);
    }
    protected long calculateComplexity(boolean outer) {
        return getOuter().getComplexity(outer);
    }
    public T translateOuter(MapTranslate translator) {
        return getOuter().translateOuter(translator).getThis();
    }

    public InnerExpr getInnerExpr(WhereJoin join) {
        return getOuter().getInnerExpr(join);
    }
    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return getOuter().getExprFollows(recursive);
    }
    public InnerJoins getInnerJoins() {
        return getOuter().getInnerJoins();
    }
    public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) {
        return getOuter().getJoinFollows(upWheres, unionJoins);
    }

    public ImMap<K, BaseExpr> getJoins() {
        return group;
    }

    // множественное наследование
    public static InnerExpr getInnerExpr(InnerJoin<?, ?> join, WhereJoin<?, ?> whereJoin) {
        ImSet<InnerExpr> set = NotNullExpr.getInnerExprs(whereJoin.getExprFollows(true), null);
        for(int i=0,size=set.size();i<size;i++) {
            InnerExpr expr = set.get(i);
            if(BaseUtils.hashEquals(join,expr.getInnerJoin()))
                return expr;
        }
        return null;
    }

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    protected final ImSet<KeyExpr> keys;
    private final ImSet<Value> values;

    public ImSet<ParamExpr> getKeys() {
        return BaseUtils.immutableCast(keys);
    }

    public ImSet<Value> getValues() {
        return values;
    }

    // дублируем аналогичную логику GroupExpr'а
    protected QueryJoin(T join, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(join.values);
        MapTranslate valueTranslator = mapValues.mapKeys();
        query = join.query.translateOuter(valueTranslator);
        group = valueTranslator.translateExprKeys(translator.translateDirect(join.group));
        keys = join.keys;
        values = mapValues.translateValues(join.values);
    }

    // для проталкивания
    protected QueryJoin(T join, I query) {
        // надо еще транслировать "внутренние" values
        this.query = query;
        group = join.group;
        keys = join.keys;
        values = join.values;
    }

    public QueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, I inner, ImMap<K, BaseExpr> group) {
        this.keys = keys;
        this.values = values;

        this.query = inner;
        this.group = group;
    }

    protected abstract static class QueryInnerHashContext<K extends Expr,I extends OuterContext<I>> extends AbstractInnerHashContext {

        protected final QueryJoin<K, I, ?, ?> thisObj;
        protected QueryInnerHashContext(QueryJoin<K, I, ?, ?> thisObj) {
            this.thisObj = thisObj;
        }

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(int i=0,size=thisObj.group.size();i<size;i++)
                hash += thisObj.group.getKey(i).hashOuter(hashContext) ^ hashOuterExpr(thisObj.group.getValue(i));
            hash = hash * 31;
            for(KeyExpr key : thisObj.keys)
                hash += hashContext.keys.hash(key);
            hash = hash * 31;
            for(Value value : thisObj.values)
                hash += hashContext.values.hash(value);
            return hash * 31 + thisObj.query.hashOuter(hashContext);
        }

        public ImSet<ParamExpr> getInnerKeys() {
            return BaseUtils.immutableCast(thisObj.keys);
        }
        public ImSet<Value> getInnerValues() {
            return thisObj.values;
        }
        protected boolean isComplex() {
            return thisObj.isComplex();
        }
    }
    private QueryInnerHashContext<K, I> innerHashContext = new QueryInnerHashContext<K, I>(this) { // по сути тоже множественное наследование, правда нюанс что своего же Inner класса
        protected int hashOuterExpr(BaseExpr outerExpr) {
            return outerExpr.hashCode();
        }
    };
    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return innerHashContext.hashInner(hashContext);
    }

    protected abstract T createThis(ImSet<KeyExpr> keys, ImSet<Value> values, I query, ImMap<K, BaseExpr> group);

    protected T translate(MapTranslate translator) {
        return createThis(translator.translateDirect(keys), translator.translateValues(values), query.translateOuter(translator), (ImMap<K,BaseExpr>) translator.translateExprKeys(group));
    }

    public boolean equalsInner(T object) {
        return getClass() == object.getClass() && BaseUtils.hashEquals(query, object.query) && BaseUtils.hashEquals(group,object.group);
    }

    @Override
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }
}
