package com.sovworks.eds.android.filemanager.tasks;


import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CachedPathInfoBase;
import com.sovworks.eds.locations.Location;

import io.reactivex.Single;

public class LoadPathInfoObservable
{
    public static Single<CachedPathInfo> create(Location loc)
    {
        return Single.create(emitter -> {
            CachedPathInfo cachedPathInfo = new CachedPathInfoBase();
            cachedPathInfo.init(loc.getCurrentPath());
            emitter.onSuccess(cachedPathInfo);
        });
    }
}
