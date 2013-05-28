package platform.base.col.implementations.order;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.ArIndexedMap;
import platform.base.col.implementations.ArIndexedSet;
import platform.base.col.implementations.ArMap;
import platform.base.col.implementations.ArSet;
import platform.base.col.implementations.abs.AMOrderMap;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class ArOrderIndexedMap<K, V> extends AMOrderMap<K, V> {
    
    public ArIndexedMap<K, V> arMap;
    private int[] order;

    public ArOrderIndexedMap(AddValue<K, V> addValue) {
        arMap = new ArIndexedMap<K, V>(addValue);
        order = new int[arMap.size()];
    }

    public ArOrderIndexedMap(ArOrderIndexedMap<K, V> orderMap, AddValue<K, V> addValue) {
        arMap = new ArIndexedMap<K, V>(orderMap.arMap, addValue);
        order = orderMap.order.clone();
    }

    public ArOrderIndexedMap(ArIndexedMap<K, V> arMap, int[] order) {
        this.arMap = arMap;
        this.order = order;
    }

    public ArOrderIndexedMap(int size, AddValue<K, V> addValue) {
        arMap = new ArIndexedMap<K, V>(size, addValue);
        order = new int[size];
    }

    // ImValueMap
    public ArOrderIndexedMap(ArOrderIndexedMap<K, ?> orderMap) {
        arMap = new ArIndexedMap<K, V>(orderMap.arMap);
        order = orderMap.order.clone();
    }

    public ArOrderIndexedMap(ArOrderIndexedSet<K> orderSet) {
        arMap = new ArIndexedMap<K, V>(orderSet.arSet);
        order = orderSet.order.clone();
    }

    public ImMap<K, V> getMap() {
        return arMap;
    }

    public V getValue(int i) {
        return arMap.getValue(order[i]);
    }

    public K getKey(int i) {
        return arMap.getKey(order[i]);
    }

    public int size() {
        return arMap.size();
    }

    public void add(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public void exclAdd(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public void mapValue(int i, V value) {
        arMap.mapValue(order[i], value);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderIndexedMap<K, M>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        if(arMap.size==0)
            return MapFact.EMPTYORDER();
        if(arMap.size==1)
            return MapFact.singletonOrder(singleKey(), singleValue());

        if(arMap.size < SetFact.useArrayMax) {
            Object[] keys = new Object[arMap.size];
            Object[] values = new Object[arMap.size];
            for(int i=0;i<arMap.size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            return new ArOrderMap<K, V>(new ArMap<K, V>(arMap.size, keys, values));
        }

        if(arMap.keys.length > arMap.size * SetFact.factorNotResize) {
            Object[] newKeys = new Object[arMap.size];
            System.arraycopy(arMap.keys, 0, newKeys, 0, arMap.size);
            arMap.keys = newKeys;
            Object[] newValues = new Object[arMap.size];
            System.arraycopy(arMap.values, 0, newValues, 0, arMap.size);
            arMap.values = newValues;
        }

        return this;
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new ArOrderIndexedSet<K>(new ArIndexedSet<K>(arMap.size, arMap.keys), order);
    }
}
