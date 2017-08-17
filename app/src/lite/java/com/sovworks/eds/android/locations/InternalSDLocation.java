package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.DeviceBasedLocation;

public class InternalSDLocation extends DeviceBasedLocation
{

	public static final String URI_SCHEME = "intmem";

	public InternalSDLocation(Context context)
	{
		this(context,Environment.getExternalStorageDirectory().getPath());		
	}	
	
	public InternalSDLocation(Context context,String currentPath)
	{
		this(context, context.getString(R.string.built_in_memory_card), null, currentPath);
	}

	public InternalSDLocation(Context context, String title, String rootDir,String currentPath)
	{
		super(UserSettings.getSettings(context), title, rootDir, currentPath);
		_context = context;
	}

    public InternalSDLocation(Context context, Uri locationUri)
    {
        super(UserSettings.getSettings(context), locationUri);
		_context = context;
    }

	@Override
	public Uri.Builder makeFullUri()
	{
		return super.makeFullUri().scheme(URI_SCHEME);
	}

	@Override
	public InternalSDLocation copy()
	{
		return new InternalSDLocation(_context, getTitle(), getRootPath(), _currentPathString);
	}

	private final Context _context;
}
