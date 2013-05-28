package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.where.NotNullWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;

public abstract class NotNullExpr extends VariableSingleClassExpr {

    @Override
    public Where calculateOrWhere() {
        return Where.TRUE;
    }

    @Override
    public Where calculateNotNullWhere() { // assert result instanceof NotNull || result.isTrue()
        return Where.TRUE;
    }

    public abstract class NotNull extends NotNullWhere {
        protected BaseExpr getExpr() {
            return NotNullExpr.this;
        }
    }

    private ImSet<NotNullExpr> exprThisFollows = null;
    @ManualLazy
    public ImSet<NotNullExpr> getExprFollows(boolean includeThis, boolean recursive) {
        assert includeThis || recursive;
        if(recursive) {
            if(includeThis && hasNotNull()) {
                if(exprThisFollows==null)
                    exprThisFollows = SetFact.addExcl(getExprFollows(true), this);
                return exprThisFollows;
            } else
                return getExprFollows(true);
        } else // не кэшируем так как редко используется
            return SetFact.singleton(this);
    }

    public void fillFollowSet(MSet<DataWhere> fillSet) {
        assert hasNotNull();
        fillSet.add((DataWhere)getNotNullWhere());
    }

    public static ImSet<NotNullExpr> getExprFollows(ImCol<BaseExpr> exprs, boolean recursive) {
        MSet<NotNullExpr> set = SetFact.mSet();
        for(int i=0,size=exprs.size();i<size;i++)
            set.addAll(exprs.get(i).getExprFollows(true, recursive));
        return set.immutable();
    }

    public static ImSet<DataWhere> getFollows(ImSet<NotNullExpr> exprFollows) {
        MSet<DataWhere> result = SetFact.mSet();
        for(int i=0,size=exprFollows.size();i<size;i++)
            exprFollows.get(i).fillFollowSet(result);
        return result.immutable();
    }

    public static ImSet<InnerExpr> getInnerExprs(ImSet<NotNullExpr> set, Result<ImSet<UnionJoin>> unionJoins) {
        boolean hasNotInner = false;
        for(int i=0,size=set.size();i<size;i++) // оптимизация
            if(!(set.get(i) instanceof InnerExpr)) {
                hasNotInner = true;
                break;
            }
        if(!hasNotInner) {
            if(unionJoins!=null)
                unionJoins.set(SetFact.<UnionJoin>EMPTY());
            return BaseUtils.immutableCast(set);
        }

        MSet<UnionJoin> mUnionJoins = null;
        if(unionJoins!=null)
            mUnionJoins = SetFact.mSetMax(set.size());

        MSet<InnerExpr> mResult = SetFact.mSet();
        for(int i=0,size=set.size();i<size;i++) {
            NotNullExpr expr = set.get(i);
            if(expr instanceof InnerExpr)
                mResult.add((InnerExpr)expr);
            else {
                if(mUnionJoins!=null && !(expr instanceof CurrentEnvironmentExpr))
                    mUnionJoins.add(((UnionExpr)expr).getBaseJoin());
                mResult.addAll(getInnerExprs(expr.getExprFollows(false), unionJoins));
            }
        }

        if(unionJoins!=null)
            unionJoins.set(mUnionJoins.immutable());
        return mResult.immutable();
    }

}
