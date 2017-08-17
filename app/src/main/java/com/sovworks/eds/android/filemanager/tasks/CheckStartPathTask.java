package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.locations.tasks.AddExistingContainerTaskFragment;
import com.sovworks.eds.locations.Location;

public class CheckStartPathTask extends AddExistingContainerTaskFragment
{
    public static CheckStartPathTask newInstance(Uri startUri, boolean storeLink)
    {
        Bundle args = new Bundle();
        args.putBoolean(ARG_STORE_LINK, storeLink);
        args.putParcelable(LocationsManager.PARAM_LOCATION_URI, startUri);
        CheckStartPathTask f = new CheckStartPathTask();
        f.setArguments(args);
        return f;
    }

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        LocationsManager lm = LocationsManager.getLocationsManager(_context);
        Location loc = lm.getFromBundle(getArguments(), null);
        if(loc.getCurrentPath().isFile())
            state.setResult(findOrCreateEDSLocation(lm, loc, getArguments().getBoolean(ARG_STORE_LINK)));
        else
            state.setResult(null);
    }

    @Override
    protected TaskCallbacks getTaskCallbacks(Activity activity)
    {
        return ((FileManagerActivity)activity).getCheckStartPathCallbacks();
    }
}
