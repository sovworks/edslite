package com.sovworks.eds.android.locations.fragments;

import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.locations.EncFsLocationBase;
import com.sovworks.eds.android.locations.opener.fragments.EDSLocationOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.android.tasks.ChangeEncFsPasswordTask;

public class EncFsSettingsFragment extends EDSLocationSettingsFragment
{
    @Override
    public EncFsLocationBase getLocation()
    {
        return (EncFsLocationBase) super.getLocation();
    }

    @Override
    protected TaskFragment createChangePasswordTaskInstance()
    {
        return new ChangeEncFsPasswordTask();
    }

    @Override
    protected LocationOpenerBaseFragment getLocationOpener()
    {
        return new EDSLocationOpenerFragment();
    }
}
