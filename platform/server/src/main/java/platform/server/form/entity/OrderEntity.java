package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.OrderInstance;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Set;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, ServerCustomSerializable {
    void fillObjects(Set<ObjectEntity> objects);

    /**
     * Возвращает OrderEntity, которая заменяет все старые ObjectEntities, на их текущие значения, взятые из instanceFactory,
     * кроме oldObject, который заменяется на newObject.
     *
     * По сути фиксирует текущие значения всех ObjectEntities, кроме oldObject.
     */
    OrderEntity<T> getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);
}
