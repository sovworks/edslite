package com.sovworks.eds.android.locations.closer.fragments;

import android.content.Context;

import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

public class OMLocationCloserFragment extends OpenableLocationCloserFragment
{
    public static void unmountAndClose(Context context, Location location, boolean forceClose) throws Exception
    {
        OpenableLocationCloserFragment.closeLocation(context, (Openable) location, forceClose);
    }
}
