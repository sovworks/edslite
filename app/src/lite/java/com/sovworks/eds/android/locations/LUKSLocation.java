package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.settings.Settings;

public class LUKSLocation extends LUKSLocationBase
{
	public LUKSLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        super(uri, lm, context, settings);
    }

	public LUKSLocation(Location containerLocation, EdsContainer cont, Context context, Settings settings)
	{
		super(containerLocation, cont, context, settings);
	}

	private LUKSLocation(LUKSLocation sibling)
	{
		super(sibling);
	}

	@Override
	public LUKSLocation copy()
	{
		return new LUKSLocation(this);
	}

}
