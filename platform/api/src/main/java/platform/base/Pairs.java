package platform.base;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.util.List;

public class Pairs<T,V> extends Permutations<ImRevMap<T,V>> {

    private ImOrderSet<T> from;
    private ImOrderSet<V> to;
    public Pairs(ImOrderSet<T> from, ImOrderSet<V> to) {
        super(from.size()==to.size()?from.size():-1);
        this.from = from;
        this.to = to;
    }

    ImRevMap<T, V> getPermute(final PermuteIterator permute) {
        return from.mapOrderRevValues(new GetIndex<V>() {
            public V getMapValue(int i) {
                return to.get(permute.nums[i]);
            }});
    }

    boolean isEmpty() {
        return from.size()!=to.size();
    }
}
