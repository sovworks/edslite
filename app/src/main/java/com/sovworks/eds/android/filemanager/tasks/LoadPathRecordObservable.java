package com.sovworks.eds.android.filemanager.tasks;

import android.content.Context;

import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import io.reactivex.Single;

public class LoadPathRecordObservable
{
    public static Single<BrowserRecord> create(Context context, Location targetLocation, DirectorySettings dirSettings)
    {
        return Single.create(s -> {
            Path p = targetLocation.getCurrentPath();
            s.onSuccess(ReadDir.getBrowserRecordFromFsRecord(context, targetLocation, p, dirSettings));
        });
    }
}
