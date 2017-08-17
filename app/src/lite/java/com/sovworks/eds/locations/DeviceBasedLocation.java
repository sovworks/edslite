package com.sovworks.eds.locations;

import android.net.Uri;

import com.sovworks.eds.fs.Path;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;

public class DeviceBasedLocation extends DeviceBasedLocationBase
{
	public DeviceBasedLocation(Settings settings)
	{
		this(settings, null,null);
	}
	
	public DeviceBasedLocation(Settings settings, String currentPath) throws IOException
	{
		this(settings, null,null);
		_currentPathString = currentPath;
	}
	
	public DeviceBasedLocation(Settings settings, Path currentPath) throws IOException
	{
		this(settings, null, currentPath.getFileSystem().getRootPath().getPathString());
		setCurrentPath(currentPath);
	}
	
	public DeviceBasedLocation(Settings settings, String title,String rootDir)
	{
		this(settings, title, rootDir, null);
	}
	
	public DeviceBasedLocation(Settings settings, String title,String rootDir,String currentPath)
	{
		super(settings, title, rootDir, currentPath);
	}
	
	public DeviceBasedLocation(Settings settings, Uri locationUri)
	{
		this(settings, locationUri.getQueryParameter("title"),getLocationId(locationUri),locationUri.getPath());
		loadFromUri(locationUri);
	}

	public DeviceBasedLocation(DeviceBasedLocation sibling)
	{
		super(sibling);
	}

	@Override
	public DeviceBasedLocation copy()
	{
		return new DeviceBasedLocation(this);
	}
}
