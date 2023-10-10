package net.susnjar.paniql.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

public class IdentityHashSet<T> implements Set<T> {
    private final IdentityHashMap<T,T> backingMap = new IdentityHashMap<>();

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backingMap.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return backingMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return backingMap.keySet().toArray(a);
    }

    @Override
    public boolean add(T t) {
        return backingMap.put(t, t) != t;
    }

    @Override
    public boolean remove(Object o) {
        return backingMap.remove((T)o) != o;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingMap.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (final T element: c) {
            if (add(element)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backingMap.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backingMap.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        backingMap.clear();
    }
}
