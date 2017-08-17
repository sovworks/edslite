package com.sovworks.eds.android.locations.activities;

import android.app.Fragment;

import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.locations.TrueCryptLocation;
import com.sovworks.eds.android.locations.VeraCryptLocation;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragment;

public class CreateLocationActivity extends SettingsBaseActivity
{
    public static final String EXTRA_LOCATION_TYPE = "com.sovworks.eds.android.LOCATION_TYPE";

    @Override
    protected Fragment getSettingsFragment()
    {
        return getCreateLocationFragment();
    }

    private Fragment getCreateLocationFragment()
    {
        switch (getIntent().getStringExtra(EXTRA_LOCATION_TYPE))
        {
            case VeraCryptLocation.URI_SCHEME:
            case TrueCryptLocation.URI_SCHEME:
            case ContainerBasedLocation.URI_SCHEME:
                return new CreateContainerFragment();
            default:
                throw new RuntimeException("Unknown location type");
        }
    }

}
