package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.ActivityTrackingFSWrapper;
import com.sovworks.eds.fs.util.ContainerFSWrapper;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.locations.OMLocationBase;
import com.sovworks.eds.settings.Settings;
import com.sovworks.eds.settings.SettingsCommon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class EDSLocationBase extends OMLocationBase implements Cloneable, EDSLocation
{

	public static final String INTERNAL_SETTINGS_FILE_NAME = ".eds-settings";

	public static class ExternalSettings extends OMLocationBase.ExternalSettings implements EDSLocation.ExternalSettings
	{
		public ExternalSettings()
		{

		}

		@Override
		public boolean shouldOpenReadOnly()
		{
			return _openReadOnly;
		}

		@Override
		public void setOpenReadOnly(boolean val)
		{
			_openReadOnly = val;
		}

		@Override
		public int getAutoCloseTimeout()
		{
			return _autoCloseTimeout;
		}

		@Override
		public void setAutoCloseTimeout(int timeout)
		{
			_autoCloseTimeout = timeout;
		}

		@Override
		public void saveToJSONObject(JSONObject jo) throws JSONException
		{
			super.saveToJSONObject(jo);
			jo.put(SETTINGS_OPEN_READ_ONLY, shouldOpenReadOnly());
			if (_autoCloseTimeout > 0)
				jo.put(SETTINGS_AUTO_CLOSE_TIMEOUT, _autoCloseTimeout);
			else
				jo.remove(SETTINGS_AUTO_CLOSE_TIMEOUT);
		}

		@Override
		public void loadFromJSONOjbect(JSONObject jo) throws JSONException
		{
			super.loadFromJSONOjbect(jo);
			setOpenReadOnly(jo.optBoolean(SETTINGS_OPEN_READ_ONLY, false));
			_autoCloseTimeout = jo.optInt(SETTINGS_AUTO_CLOSE_TIMEOUT, 0);
		}

		private static final String SETTINGS_OPEN_READ_ONLY = "read_only";
		private static final String SETTINGS_AUTO_CLOSE_TIMEOUT = "auto_close_timeout";

		private boolean _openReadOnly;
		private int _autoCloseTimeout;
	}


	public static class InternalSettings implements EDSLocation.InternalSettings
	{
		public InternalSettings()
		{

		}

		public String save()
		{
			JSONObject jo = new JSONObject();
			try
			{
				save(jo);
			}
			catch (JSONException e)
			{
				throw new RuntimeException(e);
			}
			return jo.toString();
		}

		public void load(String data)
		{
			JSONObject jo;
			try
			{
				jo = new JSONObject(data);
			}
			catch (JSONException e)
			{
				jo = new JSONObject();
			}
			load(jo);
		}

		@Override
		public String toString()
		{
			return save();
		}

		protected void save(JSONObject jo) throws JSONException
		{
		}

		protected void load(JSONObject jo)
		{

		}
	}

	public static Location getContainerLocationFromUri(Uri locationUri, LocationsManagerBase lm) throws Exception
	{
		String uriString = locationUri.getQueryParameter(LOCATION_URI_PARAM);
		if (uriString == null)
			//maybe it's a legacy container
			uriString = locationUri.getQueryParameter("container_location");
		return lm.getLocation(Uri.parse(uriString));
	}

	protected EDSLocationBase(EDSLocationBase sibling)
	{
		super(sibling);
	}

	protected EDSLocationBase(Settings settings, SharedData sharedData)
	{
		super(settings, sharedData);
	}

	@Override
	public long getLastActivityTime()
	{
		if(isOpen())
		{
			ActivityTrackingFSWrapper fs;
			try
			{
				fs = getFS();
				return fs.getLastActivityTime();
			}
			catch (IOException e)
			{
				Logger.log(e);
			}
		}
		return 0;
	}

	@Override
	public synchronized ContainerFSWrapper getFS() throws IOException
	{
		if (getSharedData().fs == null)
		{
			if (!isOpenOrMounted())
				throw new IOException("Cannot access closed container.");
			boolean readOnly = getExternalSettings().shouldOpenReadOnly();
			try
			{
				getSharedData().fs = createFS(readOnly);
			}
			catch (UserException e)
			{
				throw new RuntimeException(e);
			}
			try
			{
				readInternalSettings();
				applyInternalSettings();
			}
			catch (IOException e)
			{
				Logger.showAndLog(getContext(), e);
			}
		}
		return (ContainerFSWrapper) getSharedData().fs;
	}

	@Override
	public String getTitle()
	{
		String title = super.getTitle();
		if (title == null)
		{
			Location containerLocation = getLocation();
			if(containerLocation.isFileSystemOpen())
			{
				try
				{
					title = new StringPathUtil(containerLocation.getCurrentPath().getPathString()).getFileNameWithoutExtension();

				}
				catch (Exception e)
				{
					title = "error";
				}
			}
			else
				title = containerLocation.toString();
		}
		return title;
	}

	@Override
	public boolean hasPassword()
	{
		return true;
	}

	@Override
	public boolean isEncrypted()
	{
		return true;
	}

	@Override
	public void readInternalSettings() throws IOException
	{
		Path settingsPath;
		try
		{
			settingsPath = getFS().getRootPath().combine(INTERNAL_SETTINGS_FILE_NAME);
		}
		catch (IOException e)
		{
			settingsPath = null;
		}
		if (settingsPath == null || !settingsPath.isFile())
			getSharedData().internalSettings.load("");
		else
			getSharedData().internalSettings.load(com.sovworks.eds.fs.util.Util.readFromFile(settingsPath));
	}

	@Override
	public InternalSettings getInternalSettings()
	{
		return getSharedData().internalSettings;
	}

	@Override
	public ExternalSettings getExternalSettings()
	{
		return (ExternalSettings) super.getExternalSettings();
	}

	@Override
	public void writeInternalSettings() throws IOException
	{
		FileSystem fs = getFS();
		com.sovworks.eds.fs.util.Util.writeToFile(fs.getRootPath().getDirectory(), INTERNAL_SETTINGS_FILE_NAME, getInternalSettings().save());
	}

	@Override
	public boolean isReadOnly()
	{
		return super.isReadOnly() || getExternalSettings().shouldOpenReadOnly();
	}

	@Override
	public void applyInternalSettings() throws IOException
	{

	}


	@Override
	public void saveExternalSettings()
	{
		if(!_globalSettings.neverSaveHistory())
			super.saveExternalSettings();
	}

	@Override
	public Location getLocation()
	{
		return getSharedData().containerLocation;
	}

	public Context getContext() { return getSharedData().context; }

	protected static class SharedData extends OMLocationBase.SharedData
	{
		public SharedData(String id, InternalSettings settings, Location location, Context ctx)
		{
			super(id);
			internalSettings = settings;
			containerLocation = location;
			context = ctx;
		}

		public final InternalSettings internalSettings;
		public final Location containerLocation;
		public final Context context;
	}

	protected static final String LOCATION_URI_PARAM = "location";

	protected abstract FileSystem createBaseFS(boolean readOnly) throws IOException, UserException;

	@Override
	protected SharedData getSharedData()
	{
		return (SharedData)super.getSharedData();
	}

	@Override
	protected ExternalSettings loadExternalSettings()
	{
		ExternalSettings res = new ExternalSettings();
		res.setProtectionKeyProvider(new ProtectionKeyProvider()
		{
			@Override
			public SecureBuffer getProtectionKey()
			{
				try
				{
					return UserSettings.getSettings(getContext()).getSettingsProtectionKey();
				}
				catch (SettingsCommon.InvalidSettingsPassword invalidSettingsPassword)
				{
					return null;
				}
			}
		});
		res.load(_globalSettings,getId());
		return res;
	}

	@Override
	protected ArrayList<Path> loadPaths(Collection<String> paths) throws IOException
	{
		return LocationsManager.getPathsFromLocations(
				LocationsManager.getLocationsManager(getContext()).getLocations(paths)
		);
	}

	protected Uri.Builder makeUri(String uriScheme)
	{
		Uri.Builder ub = new Uri.Builder();
		ub.scheme(uriScheme);
		if(_currentPathString!=null)
			ub.path(_currentPathString);
		else
			ub.path("/");
		ub.appendQueryParameter(LOCATION_URI_PARAM, getLocation().getLocationUri().toString());
		return ub;
	}

	protected FileSystem createFS(boolean readOnly) throws IOException, UserException
	{
		FileSystem baseFS = createBaseFS(readOnly);
		return new ContainerFSWrapper(baseFS);
	}

	protected static InternalSettings createInternalSettings()
	{
		return new InternalSettings();
	}

}
