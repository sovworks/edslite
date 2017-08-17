package com.sovworks.eds.android.locations.closer.fragments;


import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

public class ClosersRegistry
{
    public static LocationCloserBaseFragment getDefaultCloserForLocation(Location location)
    {
        return location instanceof Openable ?
                new OpenableLocationCloserFragment()
                :
                new LocationCloserBaseFragment();
    }
}
