package com.sovworks.eds.android.helpers;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.functions.Cancellable;

public class CancellableProgressReporter implements ProgressReporter, Cancellable
{
    @Override
    public void setText(CharSequence text)
    {

    }

    @Override
    public void setProgress(int progress)
    {

    }

    @Override
    public boolean isCancelled()
    {
        return _isCancelled.get();
    }

    @Override
    public void cancel() throws Exception
    {
        _isCancelled.set(true);
    }

    private AtomicBoolean _isCancelled = new AtomicBoolean();
}
