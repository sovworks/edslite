package com.sovworks.eds.android.helpers;

public interface ProgressReporter
{
    void setText(CharSequence text);
    void setProgress(int progress);
    boolean isCancelled();
}
