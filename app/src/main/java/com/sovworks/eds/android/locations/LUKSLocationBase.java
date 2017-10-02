package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.luks.FormatInfo;
import com.sovworks.eds.settings.Settings;

import java.util.Collections;
import java.util.List;

abstract class LUKSLocationBase extends ContainerBasedLocation
{
	public static final String URI_SCHEME = "luks";

    LUKSLocationBase(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        super(uri, lm, context, settings);
    }

	LUKSLocationBase(Location containerLocation, EdsContainer cont, Context context, Settings settings)
	{
		super(containerLocation, cont, context, settings);
	}

	LUKSLocationBase(LUKSLocationBase sibling)
	{
		super(sibling);
	}


	@Override
	public Uri getLocationUri()
	{
		return makeUri(URI_SCHEME).build();
	}


	@Override
	public List<ContainerFormatInfo> getSupportedFormats()
	{
		return Collections.singletonList(getContainerFormatInfo());
	}

	@Override
	public ContainerFormatInfo getContainerFormatInfo()
	{
		return new FormatInfo();
	}

}
