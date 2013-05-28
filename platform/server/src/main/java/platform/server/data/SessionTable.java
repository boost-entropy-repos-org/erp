package platform.server.data;

import platform.base.BaseUtils;
import platform.base.GlobalObject;
import platform.base.Pair;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.DataClass;
import platform.server.context.ThreadLocalContext;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PropStat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.IQuery;
import platform.server.data.query.QueryBuilder;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.StringParseInterface;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;

import static platform.base.BaseUtils.hashEquals;

public class SessionTable extends Table implements ValuesContext<SessionTable>, Value {// в явную хранимые ряды

    public final int count; // вообще должен быть точным, или как минимум пессимистичным, чтобы в addObjects учитываться

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, count);
    }

    public Value removeBig(MAddSet<Value> usedValues) {
        return null;
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this, count);
    }

    // предполагается вызов через SQLSession
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, int count) {
        super(name, keys, properties, classes, propertyClasses);
        this.count = count;
    }

    // создает таблицу batch'ем
    public static SessionTable create(final SQLSession session, final ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, final ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        // прочитаем классы
        return session.createTemporaryTable(keys, properties, rows.size(), new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                session.insertBatchRecords(name, keys, rows);
                return null;
            }
        }, SessionRows.getClasses(properties, rows), owner);
    }

    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);

        this.count = count;
    }
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> tableClasses) {
        this(name, keys, properties, count, tableClasses.first, tableClasses.second);
    }

    public ImSet<PropertyField> getProperties() {
        return properties;
    }

    @Override
    public String getName(SQLSyntax syntax) {
        return syntax.getSessionTableName(name);
    }

    @Override
    public String getQueryName(CompileSource source) {
        assert source.params.containsKey(this);
        return source.params.get(this);
    }

    protected Table translate(MapTranslate translator) {
        return translateValues(translator.mapValues());
    }

    protected int hash(HashContext hashContext) {
        return hashValues(hashContext.values);
    }

    public ImSet<Value> getValues() {
        return getContextValues();
    }

    public SessionTable translateValues(MapValuesTranslate mapValues) {
        return mapValues.translate(this);
    }

    public SessionTable translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }

    public int hashValues(HashValues hashValues) {
        return hashValues.hash(this);
    }

    public ImSet<Value> getContextValues() {
        return SetFact.<Value>singleton(this);
    }

    public ParseInterface getParseInterface() {
        return new StringParseInterface() {
            public String getString(SQLSyntax syntax) {
                return getName(syntax);
            }
        };
    }

    // теоретически достаточно только
    private static class Struct extends TwinImmutableObject implements GlobalObject {

        public final ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        public final ImCol<PropertyField> properties;
        protected final ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
        protected final ImMap<PropertyField, ClassWhere<Field>> propertyClasses;

        protected final StatKeys<KeyField> statKeys;
        protected final ImMap<PropertyField, PropStat> statProps;

        private Struct(ImOrderSet<KeyField> keys, ImCol<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, StatKeys<KeyField> statKeys, ImMap<PropertyField, PropStat> statProps) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;

            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public boolean twins(TwinImmutableObject o) {
            return classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses) && statKeys.equals(((Struct)o).statKeys) && statProps.equals(((Struct)o).statProps);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode()) + statKeys.hashCode()) + statProps.hashCode();
        }
    }

    private Struct struct = null;

    @ManualLazy
    public Struct getValueClass() {
        if (struct == null) {
            struct = ThreadLocalContext.getLogicsInstance().twinObject(new Struct(keys, properties, classes, propertyClasses, getStatKeys(), getStatProps()));
        }
        return struct;
    }

    public boolean twins(TwinImmutableObject o) {
        return name.equals(((SessionTable) o).name) && getValueClass().equals(((SessionTable) o).getValueClass());
    }

    public int immutableHashCode() {
        return name.hashCode() * 31 + getValueClass().hashCode();
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses) {
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.merge(orClasses.second, ClassWhere.<PropertyField, Field>addOr());
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes.or(orClasses.first), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, final ImMap<PropertyField, ObjectValue> propFields) {
        return orFieldsClassWheres(DataObject.getMapClasses(keyFields), ObjectValue.getMapClasses(propFields), classes, propertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(final ImMap<KeyField, ConcreteClass> keyFields, final ImMap<PropertyField, ConcreteClass> propFields, ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {

        assert propertyClasses.keys().containsAll(propFields.keys());
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
            public ClassWhere<Field> getMapValue(PropertyField propField, ClassWhere<Field> existedPropertyClasses) {
                ConcreteClass propClass = propFields.get(propField);
                if (propClass != null)
                    existedPropertyClasses = existedPropertyClasses.or(new ClassWhere<Field>(
                            MapFact.addExcl(keyFields, propField, propClass)));
                return existedPropertyClasses;
            }});
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(
                classes.or(new ClassWhere<KeyField>(keyFields)), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> andFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) {
        // определяем новые классы чтобы создать таблицу
        final ClassWhere<KeyField> addKeyClasses = new ClassWhere<KeyField>(DataObject.getMapClasses(keyFields));

        final ClassWhere<KeyField> andKeyClasses = classes.and(addKeyClasses);

        ImMap<PropertyField, ClassWhere<Field>> andPropertyClasses = propertyClasses.mapValues(new GetValue<ClassWhere<Field>, ClassWhere<Field>>() {
            public ClassWhere<Field> getMapValue(ClassWhere<Field> value) {
                return value.and(BaseUtils.<ClassWhere<Field>>immutableCast(addKeyClasses));
            }}).addExcl(
                propFields.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ObjectValue>() {
                    public ClassWhere<Field> getMapValue(PropertyField key, ObjectValue value) {
                        return !(value instanceof DataObject)?ClassWhere.<Field>FALSE():
                                new ClassWhere<Field>(MapFact.<Field, ConcreteClass>singleton(key, ((DataObject) value).objectClass)).
                                        and(BaseUtils.<ClassWhere<Field>>immutableCast(andKeyClasses));
                    }}));
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(andKeyClasses, andPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> removeFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, final ImSet<KeyField> keyFields, ImSet<PropertyField> propFields) {
        if(keyFields.isEmpty())
            return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes, propertyClasses.remove(propFields));
        else {
            ImMap<PropertyField, ClassWhere<Field>> removePropClasses = propertyClasses.remove(propFields).mapValues(new GetValue<ClassWhere<Field>, ClassWhere<Field>>() {
                public ClassWhere<Field> getMapValue(ClassWhere<Field> value) {
                    return value.remove(keyFields);
                }});
            return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes.remove(keyFields), removePropClasses);
        }
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getFieldsClassWheres(ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> data) {
        ClassWhere<KeyField> keysClassWhere = ClassWhere.<KeyField>FALSE();
        ImMap<PropertyField, ClassWhere<Field>> propertiesClassWheres = null;
        for (int i=0,size=data.size();i<size;) {
            final ImMap<KeyField, ConcreteClass> rowKeyClasses = DataObject.getMapClasses(data.getKey(i));

            keysClassWhere = keysClassWhere.or(new ClassWhere(rowKeyClasses));

            ImMap<PropertyField, ClassWhere<Field>> rowClasses = data.getValue(i).mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ObjectValue>() {
                public ClassWhere<Field> getMapValue(PropertyField key, ObjectValue value) {
                    return new ClassWhere<Field>(MapFact.addExcl(rowKeyClasses, key, ((DataObject) value).objectClass));
                }
            });

            if(propertiesClassWheres==null)
                propertiesClassWheres = rowClasses;
            else
                propertiesClassWheres = propertiesClassWheres.mapAddValues(rowClasses, ClassWhere.<PropertyField, Field>addOr());
        }
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(keysClassWhere, propertiesClassWheres);
    }

    public SessionTable modifyRecord(final SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, final Object owner) throws SQLException {

        if(type==Modify.DELETE) { // статистику пока не учитываем
            return new SessionTable(name, keys, properties, count - deleteRecords(session, keyFields), classes, propertyClasses).
                    updateStatistics(session, count, owner);
        }

        boolean update = (type== Modify.UPDATE);
        if(type== Modify.MODIFY || type== Modify.LEFT) {
            if(session.isRecord(this, keyFields)) {
                if(type== Modify.MODIFY)
                    update = true;
                else
                    return this;
            }
        }

        if(update)
            session.updateRecords(this, keyFields, propFields);
        else
            session.insertRecord(this, keyFields, propFields);

        return new SessionTable(name, keys, properties, count + (update?0:1),
                        orFieldsClassWheres(classes, propertyClasses, keyFields, propFields)).
                            updateStatistics(session, count, owner);
    }

    public SessionTable modifyRows(SQLSession session, IQuery<KeyField, PropertyField> query, Modify type, QueryEnvironment env, Object owner) throws SQLException {

        if(query.isEmpty()) // оптимизация
            return this;

        ModifyQuery modify = new ModifyQuery(this, query, env);
        int inserted;
        switch (type) {
            case MODIFY:
                inserted = session.modifyRecords(modify);
                break;
            case LEFT:
                inserted = session.insertLeftSelect(modify, true, false);
                break;
            case ADD:
                inserted = session.insertSelect(modify);
                break;
            case UPDATE:
                session.updateRecords(modify);
                inserted = 0;
                break;
            case DELETE:
                return new SessionTable(name, keys, properties, count - session.deleteRecords(modify), classes, propertyClasses).
                        updateStatistics(session, count, owner);
            default:
                throw new RuntimeException("should not be");
        }
        return new SessionTable(name, keys, properties, count + inserted,
                        orFieldsClassWheres(classes, propertyClasses, SessionData.getQueryClasses(query))).
                            updateStatistics(session, count, owner);
    }
    public void updateAdded(SQLSession session, BaseClass baseClass, PropertyField field, Pair<Integer, Integer>[] shifts) throws SQLException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> join = join(query.getMapExprs());

        String formula = ""; String aggsh = "";
        MExclMap<String, Expr> mParams = MapFact.mExclMap(1 + 2 * shifts.length);
        mParams.exclAdd("prm1", join.getExpr(field));
        for(int i=0;i<shifts.length;i++) {
            String idsh = "prm" + (2*i+2);
            String countsh = "prm" + (2*i+3);

            if(i==0) {
                formula = idsh;
                aggsh = countsh;
            } else {
                formula = "WHEN prm1 > (" + aggsh + ") THEN " + idsh + " - (" + aggsh + ") " + (i==1?"ELSE ":"") + formula;
                aggsh += "+" + countsh;
            }
            mParams.exclAdd(idsh, new ValueExpr(shifts[i].first, ObjectType.idClass));
            if(i!=shifts.length-1) // последний параметр не нужен
                mParams.exclAdd(countsh, new ValueExpr(shifts[i].second, ObjectType.idClass));
        }
        if(shifts.length > 1)
            formula = "CASE " + formula + " END";

        query.addProperty(field, FormulaExpr.createCustomFormula("prm1+" + formula, baseClass.unknown, mParams.immutable()));
        query.and(join.getWhere());
        session.updateRecords(new ModifyQuery(this, query.getQuery()));
    }

    public SessionTable updateCurrentClasses(DataSession session) throws SQLException {
        final ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        platform.server.data.query.Join<PropertyField> join = join(mapKeys);

        MExclMap<Field, Expr> mMapExprs = MapFact.mExclMapMax(keys.size()+properties.size());
        MExclMap<Field, DataClass> mMapData = MapFact.mExclMapMax(keys.size()+properties.size());
        ClassWhere<KeyField> updatedClasses = ClassWhere.FALSE();
        for(KeyField key : keys)
            if(key.type instanceof ObjectType)
                mMapExprs.exclAdd(key, mapKeys.get(key));
            else
                mMapData.exclAdd(key, (DataClass) key.type);
        for(PropertyField property : properties)
            if(property.type instanceof ObjectType)
                mMapExprs.exclAdd(property, join.getExpr(property));
            else
                mMapData.exclAdd(property, (DataClass) property.type);
        ImMap<Field, Expr> mapExprs = mMapExprs.immutable();
        ImMap<Field, DataClass> mapData = mMapData.immutable();
        ImMap<PropertyField, ClassWhere<Field>> updatedPropertyClasses = properties.toMap(ClassWhere.<Field>FALSE());

        // пока исходим из assertion'а что не null, потом надо будет разные делать
        for(ImMap<Field, ConcreteObjectClass> diffClasses : session.readDiffClasses(join.getWhere(), MapFact.<Field, Expr>EMPTY(), mapExprs)) {
            final ImMap<Field, ConcreteClass> result = MapFact.addExcl(diffClasses, mapData);
            updatedClasses = updatedClasses.or(new ClassWhere<KeyField>(result.filterIncl(getTableKeys())));
            
            updatedPropertyClasses = updatedPropertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
                public ClassWhere<Field> getMapValue(PropertyField key, ClassWhere<Field> value) {
                    return value.or(new ClassWhere<Field>(result.filterIncl(SetFact.addExcl(getTableKeys(), key))));
                }});
        }
        return new SessionTable(name, keys, properties, count, updatedClasses, updatedPropertyClasses);
    }

    public SessionTable updateStatistics(final SQLSession session, int prevCount, final Object owner) throws SQLException {
        if(!SQLTemporaryPool.getDBStatistics(count).equals(SQLTemporaryPool.getDBStatistics(prevCount))) {
            return session.createTemporaryTable(keys, properties, count, new FillTemporaryTable() {
                public Integer fill(String name) throws SQLException {
                    QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(SessionTable.this);
                    platform.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs());
                    session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                    session.returnTemporaryTable(SessionTable.this, owner);
                    return null;
                }
            },new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes, propertyClasses), owner);
        }
        return this;
    }

    public int deleteRecords(SQLSession session, ImMap<KeyField, DataObject> keys) throws SQLException {
        return session.deleteKeyRecords(this, DataObject.getMapValues(keys));
    }


    public SessionTable addFields(final SQLSession session, final ImOrderSet<KeyField> keys, final ImMap<KeyField, DataObject> addKeys, final ImMap<PropertyField, ObjectValue> addProps, final Object owner) throws SQLException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        return session.createTemporaryTable(keys, properties.addExcl(addProps.keys()), count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                ImSet<KeyField> tableKeys = getTableKeys();
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(tableKeys.addExcl(addKeys.keys()), addKeys);
                platform.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs().filterIncl(tableKeys));
                moveData.and(prevJoin.getWhere());
                moveData.addProperties(prevJoin.getExprs());
                moveData.addProperties(DataObject.getMapExprs(addProps));
                session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, andFieldsClassWheres(classes, propertyClasses, addKeys, addProps), owner);
    }

    public SessionTable removeFields(final SQLSession session, ImSet<KeyField> removeKeys, ImSet<PropertyField> removeProps, final Object owner) throws SQLException {
        if(removeKeys.isEmpty() && removeProps.isEmpty())
            return this;

        // assert что удаляемые ключи с одинаковыми значениями, но вообще может использоваться как слияние
        final ImOrderSet<KeyField> remainOrderKeys = keys.removeOrder(removeKeys);
        final ImSet<KeyField> remainKeys = remainOrderKeys.getSet();
        final ImSet<PropertyField> remainProps = properties.remove(removeProps);
        return session.createTemporaryTable(remainOrderKeys, remainProps, count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(remainKeys);

                if(remainKeys.size()==keys.size()) { // для оптимизации
                    platform.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs().filterIncl(remainProps));
                } else {
                    ImRevMap<KeyField, KeyExpr> tableKeys = getMapKeys();
                    platform.server.data.query.Join<PropertyField> prevJoin = join(tableKeys);
                    ImRevMap<KeyField, KeyExpr> groupKeys = tableKeys.filterInclRev(remainKeys);
                    moveData.and(GroupExpr.create(groupKeys, prevJoin.getWhere(), moveData.getMapExprs()).getWhere());
                    for(PropertyField prop : remainProps)
                        moveData.addProperty(prop, GroupExpr.create(groupKeys, prevJoin.getExpr(prop), GroupType.ANY, moveData.getMapExprs()));
                }
                session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, removeFieldsClassWheres(classes, propertyClasses, removeKeys, removeProps), owner);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if (components == null)
            components = AbstractValuesContext.getComponents(this);
        return components;
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.returnTemporaryTable(this, owner);
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
        session.rollReturnTemporaryTable(this, owner);
    }

    // см. usage
    public SessionTable fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        assert propertyClasses.isEmpty();
        ClassWhere<KeyField> fixedClasses = classes.and(fixClasses);
        if(hashEquals(fixedClasses, classes))
            return this;
        else
            return new SessionTable(name, keys, properties, count, fixedClasses, propertyClasses);
    }
}
