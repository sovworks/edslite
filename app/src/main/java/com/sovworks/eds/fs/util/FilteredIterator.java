package com.sovworks.eds.fs.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class FilteredIterator<T> implements Iterator<T>
{
    public FilteredIterator(Iterator<? extends T> base)
    {
        _base = base;
    }

    /**
     * Returns true if there is at least one more element, false otherwise.
     *
     * @see #next
     */
    @Override
    public synchronized boolean hasNext()
    {
        if(!_hasNext)
            setNext();
        return _hasNext;
    }

    /**
     * Returns the next object and advances the iterator.
     *
     * @return the next object.
     * @throws NoSuchElementException if there are no more elements.
     * @see #hasNext
     */
    @Override
    public synchronized T next()
    {
        if(!hasNext())
            throw new NoSuchElementException();
        _hasNext = false;
        return _nextItem;
    }

    /**
     * Removes the last object returned by {@code next} from the collection.
     * This method can only be called once between each call to {@code next}.
     * Do not call {@code hasNext} between calls to {@code next} and {@code remove}
     *
     * @throws UnsupportedOperationException if removing is not supported by the collection being
     *                                       iterated.
     * @throws IllegalStateException         if {@code next} has not been called, or {@code remove} has
     *                                       already been called after the last call to {@code next}.
     */
    @Override
    public synchronized void remove()
    {
        if(!_hasNext)
            throw new IllegalStateException();
        _base.remove();
    }

    public Iterator<? extends T> getBaseIterator()
    {
        return _base;
    }

    protected abstract boolean isValidItem(T item);

    private boolean _hasNext;
    private T _nextItem;
    private final Iterator<? extends T> _base;

    private void setNext()
    {
        _hasNext = false;
        while(_base.hasNext())
        {
            T nextItem = _base.next();
            if(isValidItem(nextItem))
            {
                _nextItem = nextItem;
                _hasNext = true;
                break;
            }
        }
    }
}
