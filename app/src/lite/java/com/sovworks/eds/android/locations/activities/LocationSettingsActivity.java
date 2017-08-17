package com.sovworks.eds.android.locations.activities;

import android.app.Fragment;
import android.net.Uri;

import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.locations.EncFsLocationBase;
import com.sovworks.eds.android.locations.LUKSLocation;
import com.sovworks.eds.android.locations.TrueCryptLocation;
import com.sovworks.eds.android.locations.VeraCryptLocation;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragment;
import com.sovworks.eds.android.locations.fragments.EncFsSettingsFragment;

public class LocationSettingsActivity extends SettingsBaseActivity
{

    @Override
    protected Fragment getSettingsFragment()
    {
        return getCreateLocationFragment();
    }

    private Fragment getCreateLocationFragment()
    {
        Uri uri = getIntent().getData();
        if(uri == null || uri.getScheme() == null)
            throw new RuntimeException("Location uri is not set");
        switch (uri.getScheme())
        {
            case EncFsLocationBase.URI_SCHEME:
                return new EncFsSettingsFragment();
            case VeraCryptLocation.URI_SCHEME:
            case TrueCryptLocation.URI_SCHEME:
            case LUKSLocation.URI_SCHEME:
            case ContainerBasedLocation.URI_SCHEME:
                return new ContainerSettingsFragment();
            default:
                throw new RuntimeException("Unknown location type");
        }
    }

}
