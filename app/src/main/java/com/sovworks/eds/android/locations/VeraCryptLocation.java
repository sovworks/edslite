package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.settings.Settings;
import com.sovworks.eds.veracrypt.FormatInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class VeraCryptLocation extends ContainerBasedLocation
{
	public static final String URI_SCHEME = "vc";

    public VeraCryptLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        super(uri, lm, context, settings);
    }

	public VeraCryptLocation(Location location, Context context) throws IOException
	{
		this(location, null, context, UserSettings.getSettings(context));
	}

	public VeraCryptLocation(Location containerLocation, EdsContainer cont, Context context, Settings settings)
	{
		super(containerLocation, cont, context, settings);
	}

	public VeraCryptLocation(VeraCryptLocation sibling)
	{
		super(sibling);
	}

	@Override
	public List<ContainerFormatInfo> getSupportedFormats()
	{
		return Collections.singletonList(getContainerFormatInfo());
	}

	@Override
	public Uri getLocationUri()
	{
		return makeUri(URI_SCHEME).build();
	}

	@Override
	public VeraCryptLocation copy()
	{
		return new VeraCryptLocation(this);
	}

	@Override
	public ContainerFormatInfo getContainerFormatInfo()
	{
		return new FormatInfo();
	}

}
