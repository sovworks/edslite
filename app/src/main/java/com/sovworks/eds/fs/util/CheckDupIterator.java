package com.sovworks.eds.fs.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CheckDupIterator<T> extends FilteredIterator<T>
{
    public CheckDupIterator(Iterator<T> base)
    {
        super(base);
    }

    @Override
    protected boolean isValidItem(T item)
    {
        if(_previousItems.contains(item))
            return false;
        _previousItems.add(item);
        return true;
    }

    private final Set<T> _previousItems = new HashSet<>();
}
