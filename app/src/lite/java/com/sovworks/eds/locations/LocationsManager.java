package com.sovworks.eds.locations;

import android.content.Context;

import com.sovworks.eds.settings.Settings;

public class LocationsManager extends LocationsManagerBase
{
    public static boolean isOpen(Location loc)
    {
        return !(loc instanceof Openable) || ((Openable) loc).isOpen();
    }

    public static boolean isOpenableAndOpen(Location loc)
    {
        return loc instanceof Openable && isOpen(loc);
    }

    public LocationsManager(Context context, Settings settings)
    {
        super(context, settings);
    }
}
