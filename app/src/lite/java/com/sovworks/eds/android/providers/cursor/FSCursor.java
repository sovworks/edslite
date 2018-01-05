package com.sovworks.eds.android.providers.cursor;

import android.content.Context;

import com.drew.lang.annotations.NotNull;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.locations.Location;

import io.reactivex.Observable;

public class FSCursor extends FSCursorBase
{
    public FSCursor(Context context, Location location, @NotNull String[] projection, String selection, String[] selectionArgs, boolean listDir)
    {
        super(context, location, projection, selection, selectionArgs, listDir);
    }

    @Override
    protected Observable<CachedPathInfo> createObservable() throws Exception
    {
        SelectionChecker sc = new SelectionChecker(_location, _selection, _selectionArgs);
        return ListDirObservable.create(_location, _listDir).filter(sc).cache();
    }
}
