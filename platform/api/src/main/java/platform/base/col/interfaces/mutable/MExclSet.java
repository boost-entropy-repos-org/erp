package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImSet;

public interface MExclSet<K> {
    
    void exclAdd(K key);
    void exclAddAll(ImSet<? extends K> set);

    ImSet<K> immutable();

    // для MCaseList
    public int size();
    public K single();
}
