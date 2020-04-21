package com.sovworks.eds.locations;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.providers.MainContentProvider;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.std.StdFs;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.settings.Settings;

import java.io.File;
import java.io.IOException;

public abstract class DeviceBasedLocationBase implements Location, Cloneable
{
	public static String getId(String chrootPath)
	{
		return "stdfs" + chrootPath;
	}

	public static String getLocationId(Uri locationUri)
	{
		return locationUri.getQueryParameter("root_dir");
	}

	public static final String URI_SCHEME = "file";


	public DeviceBasedLocationBase(Settings settings, String title, String rootDir, String currentPath)
	{
		_settings = settings;
		_sharedData = new SharedData(rootDir == null ? "" : rootDir, title);
		_currentPathString = currentPath;
	}

	public DeviceBasedLocationBase(DeviceBasedLocationBase sibling)
	{
		_settings = sibling._settings;
		_sharedData = sibling.getSharedData();
		_currentPathString = sibling._currentPathString;
	}

	@Override
	public void loadFromUri(Uri uri)
	{
		_currentPathString = uri.getPath();
	}

	@Override
	public String getTitle()
	{
		try
		{
			String title = getSharedData().title;
			return title == null ? getCurrentPath().getPathDesc() : title;
		}
		catch (IOException e)
		{
			return "";
		}
	}

	@Override
	public synchronized FileSystem getFS() throws IOException
	{
		SharedData sd = getSharedData();
		if(sd.fs == null)
			sd.fs = createFS();
		return sd.fs;
	}

	@Override
	public Path getCurrentPath() throws IOException
	{
		return _currentPathString == null ? getFS().getRootPath() : getFS().getPath(_currentPathString);
	}

	@Override
	public void setCurrentPath(Path path)
	{
		_currentPathString = path == null ? null : path.getPathString();
	}

	@Override
	public Uri getLocationUri()
	{
		return makeSimpleUri().build();
	}

    @Override
    public Uri getDeviceAccessibleUri(Path path)
    {
		if(_settings.dontUseContentProvider() && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
		{
			StringPathUtil pu = new StringPathUtil(getRootPath()).combine(path.getPathString());
			return Uri.fromFile(new File(pu.toString()));
		}
		return MainContentProvider.getContentUriFromLocation(this, path);
    }

	@Override
	public synchronized ExternalSettings getExternalSettings()
	{
		SharedData sd = getSharedData();
		if(sd.externalSettings == null)
			sd.externalSettings = loadExternalSettings();
		return sd.externalSettings;
	}

	@Override
	public void saveExternalSettings()
	{

	}

	@Override
	public Intent getExternalFileManagerLaunchIntent()
	{
		return null;
	}

	@Override
	public String getId()
	{
		return getId(getSharedData().chroot);
	}
	
	@Override
	public String toString()
	{
		try
		{
			return new StringPathUtil(getRootPath()).combine(getCurrentPath().getPathString()).toString();
		}
		catch (IOException e)
		{
			return "wrong path";
		}
	}

	@Override
	public void closeFileSystem(boolean force) throws IOException
	{
		FileSystem fs = getSharedData().fs;
		if(fs!=null && getRootPath().length() != 0)
			fs.close(force);
		getSharedData().fs = null;
	}

	@Override
	public boolean isFileSystemOpen()
	{
		return getSharedData().fs != null;
	}

	@Override
	public boolean isReadOnly()
	{
		return false;
	}
	
	@Override
	public boolean isEncrypted()
	{
		return false;
	}

	@Override
	public boolean isDirectlyAccessible()
	{
		return true;
	}

	public String getRootPath()
	{
		return getSharedData().chroot;
	}

	public Uri.Builder makeFullUri()
	{
		try
		{
			Uri u = Uri.fromFile(new File(getCurrentPath().getPathString()));
			Uri.Builder b = u.buildUpon();
			StringPathUtil cr = new StringPathUtil(getRootPath());
			if(!cr.isEmpty())
				b.appendQueryParameter("root_dir", cr.toString());
			if(getSharedData().title!=null)
				b.appendQueryParameter("title", getSharedData().title);
			return b;
		}
		catch(IOException e)
		{
			Logger.log(e);
			return null;
		}
	}

	public Uri.Builder makeSimpleUri()
	{
		try
		{
			Uri.Builder ub = new Uri.Builder();
			StringPathUtil pu = new StringPathUtil(getRootPath()).combine(getCurrentPath().getPathString());
			ub.path(pu.toString());
			return ub;
		}
		catch(IOException e)
		{
			Logger.log(e);
			return null;
		}

	}

	/*@Override
	public boolean equals(Object obj)
	{
		try
		{
			return (obj instanceof DeviceBasedLocation) &&
					getId().equals(((DeviceBasedLocation) obj).getId()) &&
					getCurrentPath().equals(((DeviceBasedLocation) obj).getCurrentPath());
		}
		catch (Exception e)
		{
			return false;
		}
	}*/

	protected final Settings _settings;
	private final SharedData _sharedData;
	protected String _currentPathString;

	protected static class SharedData
	{
		public SharedData(String chroot, String title)
		{
			this.chroot = chroot;
			this.title = title;
		}

		final String chroot, title;
		FileSystem fs;
		ExternalSettings externalSettings;
	}

	protected SharedData getSharedData()
	{
		return _sharedData;
	}
	
	protected FileSystem createFS() throws IOException
	{		
		return StdFs.getStdFs(getRootPath());
	}

	protected ExternalSettings loadExternalSettings()
	{
		return new DefaultExternalSettings();
	}

}
