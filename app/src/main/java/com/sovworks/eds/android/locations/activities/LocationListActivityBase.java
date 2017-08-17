package com.sovworks.eds.android.locations.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.locations.DocumentTreeLocation;
import com.sovworks.eds.android.locations.fragments.ContainerListFragment;
import com.sovworks.eds.android.locations.fragments.DocumentTreeLocationsListFragment;
import com.sovworks.eds.android.locations.fragments.LocationListBaseFragment;
import com.sovworks.eds.android.settings.UserSettings;

public abstract class LocationListActivityBase extends Activity
{
    public static final String EXTRA_LOCATION_TYPE = "com.sovworks.eds.android.LOCATION_TYPE";

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        if(UserSettings.getSettings(this).isFlagSecureEnabled())
            CompatHelper.setWindowFlagSecure(this);
        if(savedInstanceState == null)
            getFragmentManager().
                beginTransaction().
                add(android.R.id.content, getCreateLocationFragment(), LocationListBaseFragment.TAG).
                commit();
    }

    protected Fragment getCreateLocationFragment()
    {
        switch (getIntent().getStringExtra(EXTRA_LOCATION_TYPE))
        {
            case ContainerBasedLocation.URI_SCHEME:
                return new ContainerListFragment();
            case DocumentTreeLocation.URI_SCHEME:
                return new DocumentTreeLocationsListFragment();
            default:
                throw new RuntimeException("Unknown location type");
        }
    }

}
