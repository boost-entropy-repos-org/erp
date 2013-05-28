package platform.server.data.where;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.ManualLazy;
import platform.server.data.query.CompileSource;
import platform.server.data.where.classes.MeanClassWhere;
import platform.server.data.where.classes.MeanClassWheres;


abstract public class DataWhere extends ObjectWhere {

    public boolean directMeansFrom(AndObjectWhere where) {
        for(OrObjectWhere orWhere : where.getOr())
            if(orWhere instanceof DataWhere && ((DataWhere)orWhere).follow(this))
                return true;
        return false;
    }

    public NotWhere not = null;
    @ManualLazy
    public NotWhere not() {  // именно здесь из-за того что типы надо перегружать без generics
        if(not==null)
            not = new NotWhere(this);
        return not;
    }

    public boolean follow(DataWhere dataWhere) {
        return getEqualFollows().contains(dataWhere);
    }

    // возвращает себя и все зависимости
    private ImSet<DataWhere> equalFollows = null;
    @ManualLazy
    public ImSet<DataWhere> getEqualFollows() {
        if(equalFollows ==null)
            equalFollows = SetFact.addExcl(calculateFollows(), this);
        return equalFollows;
    }

    // определяет все
    protected abstract ImSet<DataWhere> calculateFollows();

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected String getNotSource(CompileSource compile) {
        return NotWhere.PREFIX + getSource(compile);
    }

    public MeanClassWheres calculateMeanClassWheres(boolean useNots) {
        return new MeanClassWheres(getMeanClassWhere(),this);
    }

    protected MeanClassWhere getMeanClassWhere() {
        return new MeanClassWhere(getClassWhere());
    }

    public static Where create(DataWhere where) {
        if(where.getClassWhere().isFalse())
            return Where.FALSE;
        else
            return where;
    }

    public boolean isNot() {
        return false;
    }
}
