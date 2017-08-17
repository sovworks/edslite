package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.luks.FormatInfo;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LUKSLocation extends ContainerBasedLocation
{
	public static final String URI_SCHEME = "luks";

    public LUKSLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        super(uri, lm, context, settings);
    }

	public LUKSLocation(Location location, Context context) throws IOException
	{
		this(location, null, context, UserSettings.getSettings(context));
	}

	public LUKSLocation(Location containerLocation, EdsContainer cont, Context context, Settings settings)
	{
		super(containerLocation, cont, context, settings);
	}

	public LUKSLocation(LUKSLocation sibling)
	{
		super(sibling);
	}


	@Override
	public Uri getLocationUri()
	{
		return makeUri(URI_SCHEME).build();
	}

	@Override
	public LUKSLocation copy()
	{
		return new LUKSLocation(this);
	}

	@Override
	public List<ContainerFormatInfo> getSupportedFormats()
	{
		return Collections.singletonList(getContainerFormatInfo());
	}

	@Override
	protected ContainerFormatInfo getContainerFormatInfo()
	{
		return new FormatInfo();
	}
}
