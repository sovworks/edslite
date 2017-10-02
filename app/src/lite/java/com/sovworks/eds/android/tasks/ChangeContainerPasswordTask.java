package com.sovworks.eds.android.tasks;

import android.os.Bundle;

import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.LocationsManager;

public class ChangeContainerPasswordTask extends ChangeContainerPasswordTaskBase
{
    public static ChangeContainerPasswordTask newInstance(ContainerLocation container, Bundle passwordDialogResult)
    {
        Bundle args = new Bundle();
        args.putAll(passwordDialogResult);
        LocationsManager.storePathsInBundle(args, container, null);
        ChangeContainerPasswordTask f = new ChangeContainerPasswordTask();
        f.setArguments(args);
        return f;
    }
}
