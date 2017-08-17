package com.sovworks.eds.fs.util;

import java.util.Iterator;

public class CombinedIterator<T> implements Iterator<T>
{
    public CombinedIterator(Iterable<Iterator<T>> iters)
    {
        _iterIter = iters.iterator();
    }

    @Override
    public boolean hasNext()
    {
        return getCurrentIter()!=null;
    }

    @Override
    public T next()
    {
        Iterator<T> iter = getCurrentIter();
        if(iter == null)
            throw new IllegalStateException("No more elements");
        return iter.next();
    }

    @Override
    public void remove()
    {
        if(_iter == null)
            throw new IllegalStateException("Current element is not set");
        _iter.remove();
    }

    private Iterator<T> getCurrentIter()
    {
        if(_iter == null || !_iter.hasNext())
        {
            _iter = null;
            while(_iterIter.hasNext())
            {
                _iter = _iterIter.next();
                if(_iter.hasNext())
                    break;
                else
                    _iter = null;
            }

        }
        return _iter;
    }

    private final Iterator<Iterator<T>> _iterIter;
    private Iterator<T> _iter;
}
