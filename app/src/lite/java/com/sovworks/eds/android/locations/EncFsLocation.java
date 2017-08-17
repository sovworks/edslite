package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.encfs.FS;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;

public class EncFsLocation extends EncFsLocationBase
{
    public EncFsLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
		super(uri, lm, context, settings);
    }

	public EncFsLocation(Location location, Context context) throws IOException
	{
		this(location, null, context, UserSettings.getSettings(context));
	}

	public EncFsLocation(Location containerLocation, FS encFs, Context context, Settings settings)
	{
		super(containerLocation, encFs, context, settings);
	}

    public EncFsLocation(EncFsLocationBase sibling)
    {
        super(sibling);
    }


	@Override
	public EncFsLocation copy()
	{
        return new EncFsLocation(this);
	}

}
