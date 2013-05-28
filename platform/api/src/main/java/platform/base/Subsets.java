package platform.base;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * User: DAle
 * Date: 18.11.2010
 * Time: 14:09:19
 */

public class Subsets<E> implements Iterable<ImSet<E>> {
    private ImSet<E> objects;
    
    public Subsets(ImSet<E> objects) {
        this.objects = objects;
    }

    public Iterator<ImSet<E>> iterator() {
        return new SubsetsIterator();
    }

    public class SubsetsIterator implements Iterator<ImSet<E>> {
        long subsetIndex;
        long subsetsCnt;

        SubsetsIterator() {
            subsetIndex = 0;
            subsetsCnt = 1 << objects.size();
        }

        public boolean hasNext() {
            return subsetIndex < subsetsCnt;
        }

        public ImSet<E> next() {
            MSet<E> subset = SetFact.mSet();
            int index = 0;
            for (E object : objects) {
                if ((subsetIndex & (1 << index)) != 0) {
                    subset.add(object);
                }
                ++index;
            }
            ++subsetIndex;
            return subset.immutable();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
