package com.github.nvlled.paperglass;

import java.util.*;

public class LimitedList<T> extends ArrayList<T> {
    private final int maxSize;

    public LimitedList(int n) {
        maxSize = n;
    }

    @Override
    public boolean add(T obj) {
        if (size() < maxSize) {
            return super.add(obj);
        }
        return false;
    }

    @Override
    public void add(int i, T obj) {
        if (size() < maxSize) {
            super.add(i, obj);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> coll) {
        if (size()+coll.size() < maxSize) {
            return super.addAll(coll);
        }
        return false;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> coll) {
        if (size()+coll.size() < maxSize) {
            return super.addAll(i, coll);
        }
        return false;
    }
}
