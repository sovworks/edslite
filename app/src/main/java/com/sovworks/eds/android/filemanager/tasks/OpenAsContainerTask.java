package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.os.Bundle;

import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Location;

public class OpenAsContainerTask extends CheckStartPathTask
{
    public static OpenAsContainerTask newInstance(Location locationLocation, boolean storeLink)
    {
        Bundle args = new Bundle();
        args.putBoolean(ARG_STORE_LINK, storeLink);
        LocationsManager.storePathsInBundle(args, locationLocation, null);
        OpenAsContainerTask f = new OpenAsContainerTask();
        f.setArguments(args);
        return f;
    }

    @Override
    protected TaskCallbacks getTaskCallbacks(Activity activity)
    {
        FileListViewFragment f = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
        return f == null ? null : f.getOpenAsContainerTaskCallbacks();
    }
}
