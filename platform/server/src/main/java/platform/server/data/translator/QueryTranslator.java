package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.ParamExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.query.SourceJoin;

// в отдельный класс для allKeys и для аспектов
public class QueryTranslator extends TwinImmutableObject {

    public final ImMap<ParamExpr,? extends Expr> keys;

    private final boolean allKeys;

    private GetValue<SourceJoin, SourceJoin> trans;
    private <V extends SourceJoin> GetValue<V, V> TRANS() {
        if(trans==null) {
            trans = new GetValue<SourceJoin, SourceJoin>() {
                public SourceJoin getMapValue(SourceJoin value) {
                    return value.translateQuery(QueryTranslator.this);
                }};
        }
        return (GetValue<V, V>)trans;
    }

    public QueryTranslator translateRemoveValues(MapValuesTranslate translate) {
        return new QueryTranslator(translate.mapKeys().translate(keys), allKeys);
    }

    protected QueryTranslator(ImMap<ParamExpr, ? extends Expr> keys, boolean allKeys) {
        this.keys = keys;

        this.allKeys = allKeys;
    }

    public QueryTranslator(ImMap<KeyExpr, ? extends Expr> joinImplement) {
        this(BaseUtils.<ImMap<ParamExpr, ? extends KeyExpr>>immutableCast(joinImplement), true);
    }

    public <K> ImMap<K, Expr> translate(ImMap<K, ? extends Expr> map) {
        return ((ImMap<K, Expr>)map).mapValues(this.<Expr>TRANS());
    }

    public <K> ImOrderMap<Expr, K> translate(ImOrderMap<? extends Expr, K> map) {
        return ((ImOrderMap<Expr, K>)map).mapOrderKeys(this.<Expr>TRANS());
    }

    public ImList<Expr> translate(ImList<? extends Expr> list) {
        return ((ImList<Expr>)list).mapListValues(this.<Expr>TRANS());
    }

    public ImSet<Expr> translate(ImSet<? extends Expr> set) {
        return ((ImSet<Expr>)set).mapSetValues(this.<Expr>TRANS());
    }

    public Expr translate(ParamExpr key) {
        Expr transExpr = keys.get(key);
        if(transExpr==null) {
            if(allKeys)
                assert key instanceof PullExpr; // не должно быть
            return key;
        } else
            return transExpr;
    }

    public boolean twins(TwinImmutableObject o) {
        return keys.equals(((QueryTranslator)o).keys);
    }

    public int immutableHashCode() {
        return keys.hashCode();
    }
}
