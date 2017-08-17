package com.sovworks.eds.util;

import java.util.Iterator;

public abstract class IteratorConverter<S, T> implements Iterator<T>
{
    @Override
    public boolean hasNext()
    {
        return _srcIterator.hasNext();
    }

    @Override
    public T next()
    {
        return convert(_srcIterator.next());
    }

    @Override
    public void remove()
    {
        _srcIterator.remove();
    }

    public Iterator<? extends S> getSrcIter()
    {
        return _srcIterator;
    }

    protected IteratorConverter(Iterator<? extends S> srcIterator)
    {
        _srcIterator = srcIterator;
    }

    protected abstract T convert(S src);

    private final Iterator<? extends S> _srcIterator;

}
