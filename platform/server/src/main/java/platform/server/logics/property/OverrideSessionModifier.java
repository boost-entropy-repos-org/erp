package platform.server.logics.property;

import platform.base.FullFunctionSet;
import platform.base.FunctionSet;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.caches.ValuesContext;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.session.*;

import java.sql.SQLException;

import static platform.base.BaseUtils.merge;

public class OverrideSessionModifier extends SessionModifier {

    private final IncrementProps override;
    private final SessionModifier modifier;

    private final FunctionSet<CalcProperty> forceDisableHintIncrement;
    private final FunctionSet<CalcProperty> forceDisableNoUpdate;
    private final FunctionSet<CalcProperty> forceHintIncrement;
    private final FunctionSet<CalcProperty> forceNoUpdate;

    private Integer limitHintIncrementComplexity = null;
    private Integer limitHintIncrementStat = null;

    @Override
    public ImSet<CalcProperty> getHintProps() {
        return super.getHintProps().merge(modifier.getHintProps());
    }

    private FunctionSet<CalcProperty> pushHints() {
        return merge(override.getProperties(), forceHintIncrement, forceNoUpdate);
    }

//    private Map<CalcProperty, Boolean> pushHint = new HashMap<CalcProperty, Boolean>();
    @ManualLazy
    private boolean pushHint(CalcProperty property) { // частый вызов из-за кэширования хинтов, уже нет так как отрезается проверкой на complex
//        Boolean result = pushHint.get(property);
//        if(result==null) {
            return  !CalcProperty.dependsSet(property, override.getProperties(), forceHintIncrement, forceNoUpdate);
//            pushHint.put(property, result);
//        }
//        return result;
    }

    @Override
    public boolean allowHintIncrement(CalcProperty property) {
        if(forceDisableHintIncrement.contains(property))
            return false;

        if(pushHint(property))
            return modifier.allowHintIncrement(property);

        return super.allowHintIncrement(property);
    }

    @Override
    public boolean forceHintIncrement(CalcProperty property) {
        return forceHintIncrement.contains(property);
    }

    @Override
    public boolean forceNoUpdate(CalcProperty property) {
        return forceNoUpdate.contains(property);
    }

    @Override
    public boolean forceDisableNoUpdate(CalcProperty property) {
        boolean result = forceDisableNoUpdate.contains(property);
        // если здесь запрещено, то и в modifier'е должно быть запрещено
        assert !result || modifier.forceDisableNoUpdate(property);
        return result;
    }

    @Override
    public <P extends PropertyInterface> ValuesContext cacheAllowPrereadValues(CalcProperty<P> property) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return null;

        if(pushHint(property))
            return modifier.cacheAllowPrereadValues(property);

        return super.cacheAllowPrereadValues(property);
    }

    @Override
    public <P extends PropertyInterface> boolean allowPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) {
        if(!allowPropertyPrereadValues(property)) // оптимизация
            return false;

        if(pushHint(property))
            return modifier.allowPrereadValues(property, values);

        return super.allowPrereadValues(property, values);
    }


    @Override
    public int getLimitHintIncrementComplexity() {
        if(limitHintIncrementComplexity!=null)
            return limitHintIncrementComplexity;

        return super.getLimitHintIncrementComplexity();
    }

    @Override
    public int getLimitHintIncrementStat() {
        if(limitHintIncrementStat!=null)
            return limitHintIncrementStat;

        return super.getLimitHintIncrementStat();
    }

    @Override
    public void addHintIncrement(CalcProperty property) {
        assert allowHintIncrement(property);

        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addHintIncrement(property);
            return;
        }

        super.addHintIncrement(property);
    }

    @Override
    public <P extends PropertyInterface> void addPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) {
        // если не зависит от override'а проталкиваем внутрь
        if(pushHint(property)) {
            modifier.addPrereadValues(property, values);
            return;
        }

        super.addPrereadValues(property, values);
    }

    @Override
    public void clearHints(SQLSession session) throws SQLException {
        super.clearHints(session);
        modifier.clearHints(session);
    }

    @Override
    public void clearPrereads() throws SQLException {
        super.clearPrereads();
        modifier.clearPrereads();
    }

    @Override
    public void addNoUpdate(CalcProperty property) {
        assert allowNoUpdate(property);

        if(pushHint(property) && modifier.allowNoUpdate(property)) {
            modifier.addNoUpdate(property);
            return;
        }

        super.addNoUpdate(property);
    }

    // уведомляет что IncrementProps изменился
    public void eventIncrementChange(CalcProperty property) {
//        pushHint.remove(property);
        eventDataChange(property);

        // пробежим по increment'ам modifier, и уведомим что они могли изменится
        for(CalcProperty incrementProperty : modifier.getHintProps())
            if(CalcProperty.depends(incrementProperty, property))
                eventSourceChange(incrementProperty);
    }

    @Override
    public void clean(SQLSession sql) throws SQLException {
        super.clean(sql);
        override.unregisterView(this);
        modifier.unregisterView(this);
    }

    public OverrideSessionModifier(IncrementProps override, FunctionSet<CalcProperty> forceDisableHintIncrement, FunctionSet<CalcProperty> forceDisableNoUpdate, FunctionSet<CalcProperty> forceHintIncrement, FunctionSet<CalcProperty> forceNoUpdate, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this.override = override;
        this.modifier = modifier;
        this.forceDisableHintIncrement = forceDisableHintIncrement;
        this.forceDisableNoUpdate = forceDisableNoUpdate;
        this.forceHintIncrement = forceHintIncrement;
        this.forceNoUpdate = forceNoUpdate;

        // assert что modifier.forceDisableNoUpdate содержит все this.forceDisableNoUpdate
        // assert что forceDisableIncrement содержит все this.forceDisabeNoUpdate

        override.registerView(this);
        modifier.registerView(this);
    }

    public OverrideSessionModifier(IncrementProps override, FunctionSet<CalcProperty> forceDisableHintIncrement, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, forceDisableHintIncrement, FullFunctionSet.<CalcProperty>instance(), SetFact.<CalcProperty>EMPTY(), SetFact.<CalcProperty>EMPTY(), modifier);
    }

    public OverrideSessionModifier(IncrementProps override, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, Settings.get().isNoApplyIncrement(), modifier);

        limitHintIncrementComplexity = Settings.get().getLimitApplyHintIncrementComplexity();
        limitHintIncrementStat = Settings.get().getLimitApplyHintIncrementStat();
    }

    public OverrideSessionModifier(IncrementProps override, boolean disableHintIncrement, SessionModifier modifier) { // нужно clean вызывать после такого modifier'а
        this(override, disableHintIncrement ? FullFunctionSet.<CalcProperty>instance() : SetFact.<CalcProperty>EMPTY(), modifier);
    }

    @Override
    protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided) {
        PropertyChange<P> overrideChange = override.getPropertyChange(property);
        if(overrideChange!=null)
            return new ModifyChange<P>(overrideChange, true);
        return modifier.getModifyChange(property, preread, merge(overrided, CalcProperty.getDependsOnSet(override.getProperties()), forceDisableHintIncrement));
    }

    public ImSet<CalcProperty> calculateProperties() {
        return modifier.getProperties().merge(override.getProperties());
    }

    public SQLSession getSQL() {
        return modifier.getSQL();
    }

    public BaseClass getBaseClass() {
        return modifier.getBaseClass();
    }

    public QueryEnvironment getQueryEnv() {
        return modifier.getQueryEnv();
    }
}
