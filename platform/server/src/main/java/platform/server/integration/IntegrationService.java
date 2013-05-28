package platform.server.integration;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.server.Message;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.IntegerClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcPropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.Collection;

public class IntegrationService {
    private ImportTable table;
    private Collection<ImportProperty<?>> properties;
    private Collection<? extends ImportKey<?>> keys;
    private DataSession session;
    private Collection<ImportDelete> deletes;

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties) {
        this(session, table, keys, properties, null);
    }

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties, Collection<ImportDelete> deletes) {
        this.session = session;
        this.table = table;
        this.properties = properties;
        this.keys = keys;
        this.deletes = deletes;
    }

    public void synchronize() throws SQLException {
        synchronize(false);
    }

    public void synchronize(boolean replaceNull) throws SQLException {
        synchronize(replaceNull, true);
    }

    @Message("message.synchronize")
    public void synchronize(boolean replaceNull, boolean replaceEqual) throws SQLException {
        SingleKeyTableUsage<ImportField> importTable = new SingleKeyTableUsage<ImportField>(IntegerClass.instance, table.fields, ImportField.typeGetter);
        
        MExclMap<ImMap<String, DataObject>, ImMap<ImportField, ObjectValue>> mRows = MapFact.mExclMap();
        int counter = 0;
        for (final PlainDataTable.Row row : table)
            mRows.exclAdd(MapFact.singleton("key", new DataObject(counter++)), table.fields.getSet().mapValues(new GetValue<ObjectValue, ImportField>() {
                public ObjectValue getMapValue(ImportField value) {
                    return ObjectValue.getValue(row.getValue(value), value.getFieldClass());
                }}));
        importTable.writeRows(session.sql, mRows.immutable());

        if (deletes != null) {
            deleteObjects(importTable);
        }

        // приходится через addKeys, так как synchronize сам не может resolv'ить сессию на добавление
        MExclMap<ImportKey<?>, SinglePropertyTableUsage<?>> mAddedKeys = MapFact.mExclMapMax(keys.size());
        for (ImportKey<?> key : keys)
            if(!key.skipKey && key.keyClass instanceof ConcreteCustomClass)
                mAddedKeys.exclAdd(key, key.synchronize(session, importTable));
        ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys = mAddedKeys.immutable();

        DataChanges propertyChanges = DataChanges.EMPTY;
        for (ImportProperty<?> property : properties)
            propertyChanges = propertyChanges.add(property.synchronize(session, importTable, addedKeys, replaceNull, replaceEqual));
        
        session.change(propertyChanges);

        for(SinglePropertyTableUsage<?> addedTable : addedKeys.valueIt())
            addedTable.drop(session.sql);

        importTable.drop(session.sql);
    }

    private <P extends PropertyInterface> void deleteObjects(SingleKeyTableUsage<ImportField> importTable) throws SQLException {
        for (ImportDelete delete : deletes) {
            KeyExpr keyExpr = new KeyExpr("key");

            Where deleteWhere = Where.TRUE;

            // выражения для полей в импортируемой таблице
            ImMap<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

            // фильтруем только те, которых нету в ImportTable
            if (!delete.deleteAll)
                deleteWhere = deleteWhere.and(GroupExpr.create(MapFact.singleton("key",
                                           delete.key.getExpr(importExprs, session.getModifier())),
                                           Where.TRUE,
                                           MapFact.singleton("key", keyExpr)).getWhere().not());

            ImRevMap<P, KeyExpr> intraKeyExprs = delete.deleteProperty.property.getMapKeys(); // генерим ключи (использовать будем только те, что не в DataObject
            KeyExpr groupExpr = null;
            ImMap<P, ImportDeleteInterface> deleteMapping = ((CalcPropertyImplement<P, ImportDeleteInterface>) delete.deleteProperty).mapping;
            ImValueMap<P,Expr> mvDeleteExprs = deleteMapping.mapItValues();
            for (int i=0,size=deleteMapping.size();i<size;i++) {
                KeyExpr intraKeyExpr = intraKeyExprs.get(deleteMapping.getKey(i));
                ImportDeleteInterface deleteInterface = deleteMapping.getValue(i);
                if (delete.key.equals(deleteInterface)) {
                    groupExpr = intraKeyExpr; // собственно группируем по этому ключу
                    mvDeleteExprs.mapValue(i, groupExpr);
                } else
                    mvDeleteExprs.mapValue(i, deleteInterface.getDeleteExpr(importTable, intraKeyExpr, session.getModifier()));
            }

            deleteWhere = deleteWhere.and(GroupExpr.create(MapFact.singleton("key", groupExpr),
                                       delete.deleteProperty.property.getExpr(mvDeleteExprs.immutableValue(), session.getModifier()),
                                       GroupType.ANY, MapFact.singleton("key", keyExpr)).getWhere());

            session.changeClass(new ClassChange(keyExpr, deleteWhere, CaseExpr.NULL));
        }
    }
}
