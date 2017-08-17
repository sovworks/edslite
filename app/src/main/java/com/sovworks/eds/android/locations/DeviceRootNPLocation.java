package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.DeviceBasedLocation;
import com.sovworks.eds.settings.Settings;

public class DeviceRootNPLocation extends DeviceBasedLocation
{
	public static String getLocationId()
	{
		return URI_SCHEME;
	}

	public static final String URI_SCHEME = "rootfsnp";

	public DeviceRootNPLocation(Context context)
	{
		this(context,"");		
	}

	public DeviceRootNPLocation(Context context, String currentPath)
	{
		super(UserSettings.getSettings(context), context.getString(R.string.device),null,currentPath);
		_context = context;
	}
	
	public DeviceRootNPLocation(Context context, Settings settings, Uri locationUri)
	{
		super(settings, locationUri);
		_context = context;
	}

	@Override
	public Uri getLocationUri()
	{
		return makeFullUri().build();
	}

	@Override
	public Uri.Builder makeFullUri()
	{
		return super.makeFullUri().scheme(URI_SCHEME);
	}
	
	@Override
	public String getId()
	{
		return getLocationId();
	}

	@Override
	public DeviceRootNPLocation copy()
	{
		return new DeviceRootNPLocation(_context, _currentPathString);
	}

	private final Context _context;

}
