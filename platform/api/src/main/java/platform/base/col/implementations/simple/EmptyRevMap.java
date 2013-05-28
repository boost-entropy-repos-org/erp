package platform.base.col.implementations.simple;

import platform.base.col.implementations.abs.ARevMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class EmptyRevMap<K, V> extends ARevMap<K, V> implements ImValueMap<K, V>, ImRevValueMap<K, V> {

    private final static EmptyRevMap<Object, Object> instance = new EmptyRevMap<Object, Object>();
    public static <K,V> EmptyRevMap<K, V> INSTANCE() {
        return (EmptyRevMap<K, V>) instance;
    }
    private EmptyRevMap() {
    }

    public int size() {
        return 0;
    }

    public K getKey(int i) {
        throw new UnsupportedOperationException();
    }

    public V getValue(int i) {
        throw new UnsupportedOperationException();
    }

    public ImRevMap<K, V> immutableValueRev() {
        return this;
    }

    public void mapValue(int i, V value) {
        throw new UnsupportedOperationException();
    }

    public V getMapValue(int i) {
        throw new UnsupportedOperationException();
    }

    public K getMapKey(int i) {
        throw new UnsupportedOperationException();
    }

    public int mapSize() {
        return 0;
    }

    public ImMap<K, V> immutableValue() {
        return this;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return (ImValueMap<K, M>) this;
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return (ImRevValueMap<K, M>) this;
    }

    @Override
    public ImRevMap<V, K> reverse() {
        return (ImRevMap<V, K>)this;
    }
}
