package com.sovworks.eds.locations;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.providers.ContainersDocumentProviderBase;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.std.StdFs;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.settings.DefaultSettings;
import com.sovworks.eds.settings.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class LocationBase implements Location, Cloneable
{			
	public static class ExternalSettings implements Location.ExternalSettings
	{
		public ExternalSettings()
		{
			
		}	

		@Override
		public void setProtectionKeyProvider(ProtectionKeyProvider p)
		{
			_protectionKeyProvider = p;
		}
		
		public String getTitle()
		{
			return _title;
		}
		
		public void setTitle(String title)
		{
			_title = title;
		}

		@Override
		public boolean isVisibleToUser()
		{
			return _isVisibleToUser;
		}

		@Override
		public void setVisibleToUser(boolean val)
		{
			_isVisibleToUser = val;
		}

		@Override
		public boolean useExtFileManager()
		{
			return _useExtFileManager;
		}

		@Override
		public void setUseExtFileManager(boolean val)
		{
			_useExtFileManager = val;
		}

		public String save() throws JSONException
		{
			JSONObject jo = new JSONObject();
			saveToJSONObject(jo);			
			return jo.toString();
		}
		
		public void load(Settings settings, String locationId)
		{
			load(settings.getLocationSettingsString(locationId));
		}
		
		public void load(String data)
		{
			JSONObject jo;
			try
			{
				jo = new JSONObject(data);			
			}
			catch (Exception e)
			{			
				jo = new JSONObject();
			}	
			try
			{
				loadFromJSONOjbect(jo);
			}
			catch (Exception e)
			{
				Logger.log(e);
			}			
		}
		
		@Override
		public String toString()
		{
			try
			{
				return save();
			}
			catch (JSONException e)
			{
				return "error";
			}
		}		
		
		public void saveToJSONObject(JSONObject jo) throws JSONException
		{		
			if(_title!=null)
				jo.put(SETTINGS_TITLE, _title);
			jo.put(SETTINGS_VISIBLE_TO_USER, _isVisibleToUser);
			jo.put(SETTINGS_USE_EXT_FILE_MANAGER, useExtFileManager());
		}
		
		public void loadFromJSONOjbect(JSONObject jo) throws JSONException
		{
			_title = jo.optString(SETTINGS_TITLE,null);
			_isVisibleToUser = jo.optBoolean(SETTINGS_VISIBLE_TO_USER, false);
			_useExtFileManager = jo.optBoolean(SETTINGS_USE_EXT_FILE_MANAGER, true);
		}
		
		protected void storeProtectedField(JSONObject jo,String key,String data)
		{
			try
			{
				if(data != null)
					jo.put(key, _protectionKeyProvider == null ?
							data :
							SimpleCrypto.encrypt(_protectionKeyProvider.getProtectionKey(), data));
			}
			catch (Exception ignored)
			{
			}
		}

		protected void storeProtectedField(JSONObject jo, String key, byte[] data)
		{
			try
			{
				if(data != null)
					jo.put(key, encryptAndEncode(data));
			}
			catch (Exception ignored)
			{
			}
		}

		protected String encryptAndEncode(byte[] data)
		{
			return _protectionKeyProvider == null ?
					SimpleCrypto.toHexString(data) :
					SimpleCrypto.encrypt(_protectionKeyProvider.getProtectionKey(), data);
		}
		
		protected String loadProtectedString(JSONObject jo, String key)
		{
			byte[] d = loadProtectedData(jo, key);
			return d == null ? null : new String(d);
		}

		protected byte[] loadProtectedData(JSONObject jo, String key)
		{
			try
			{
				String data = jo.optString(key, null);
				if(data == null)
					return null;
				return decodeAndDecrypt(data);
			}
			catch (Exception e)
			{
				Logger.log(e);
			}
			return null;
		}

		protected byte[] decodeAndDecrypt(String data)
		{
			return _protectionKeyProvider == null ?
					data.getBytes()
					:
					SimpleCrypto.decrypt(_protectionKeyProvider.getProtectionKey(), data);
		}
		
		private static final String SETTINGS_TITLE = "title";
		public static final String SETTINGS_VISIBLE_TO_USER = "visible_to_user";
		private static final String SETTINGS_USE_EXT_FILE_MANAGER = "use_ext_file_manager";

		private String _title;
		private boolean _isVisibleToUser = false, _useExtFileManager;
		private ProtectionKeyProvider _protectionKeyProvider;
	}

	protected LocationBase(LocationBase sibling)
	{
		this(sibling._globalSettings, sibling.getSharedData());
		_currentPathString = sibling._currentPathString;
	}

	protected LocationBase(Settings settings, SharedData sharedData)
	{	
		_globalSettings = settings == null ? new DefaultSettings() : settings;
		_sharedData = sharedData;
	}

	@Override
	public final String getId()
	{
		return getSharedData().locationId;
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
	public String getTitle()
	{
		return getExternalSettings().getTitle();
	}	

	@Override
	public ExternalSettings getExternalSettings()
	{
		SharedData sd = getSharedData();
		if(sd.externalSettings == null)
			sd.externalSettings = loadExternalSettings();
		return sd.externalSettings;
	}	

	@Override
	public void saveExternalSettings()
	{
		try
		{
			_globalSettings.setLocationSettingsString(getId(), getExternalSettings().save());
		}
		catch (JSONException e)
		{
			throw new RuntimeException("Settings serialization failed", e);
		}
	}

	@Override
	public void loadFromUri(Uri uri)
	{

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
		return false;
	}

	@Override
	public Uri getDeviceAccessibleUri(Path path)
	{
		return null;
	}

	@Override
	public Intent getExternalFileManagerLaunchIntent()
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return null;
		Settings.ExternalFileManagerInfo exFmInfo = _globalSettings.getExternalFileManagerInfo();
		if(exFmInfo == null || !DocumentsContract.Document.MIME_TYPE_DIR.equals(exFmInfo.mimeType))
			return null;

		try
		{
			Intent intent = new Intent(exFmInfo.action);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setClassName(exFmInfo.packageName, exFmInfo.className);
			Uri uri = ContainersDocumentProviderBase.getUriFromLocation(this);
			if (exFmInfo.mimeType != null)
				intent.setDataAndType(uri, exFmInfo.mimeType);
			else
				intent.setData(uri);
			return intent;
		}
		catch(Exception e)
		{
			Logger.log(e);
			return null;
		}
	}

	@Override
	public String toString()
	{
		String res = getTitle();
		try
		{
			if(isFileSystemOpen() && getCurrentPath()!=null)
				res += new StringPathUtil(getCurrentPath().getPathDesc());
		}
		catch (Exception ignored)
		{
		}
		return res;
	}	

	@Override
	public void closeFileSystem(boolean force) throws IOException
	{
		try
		{
			if(getSharedData().fs != null)
			{
				getSharedData().fs.close(force);
				getSharedData().fs = null;
			}			
		}
		catch(Throwable e)
		{
			if(!force)
				throw new IOException(e);
			else
				Logger.log(e);
		}
	}

	@Override
	public boolean isFileSystemOpen()
	{
		return getSharedData().fs != null;
	}

	/*@Override
	public boolean equals(Object obj)
	{
		try
		{
			return (obj instanceof LocationBase) &&
					getId().equals(((LocationBase) obj).getId()) &&
					getCurrentPath().equals(((LocationBase) obj).getCurrentPath());
		}
		catch (Exception e)
		{
			return false;
		}
	}*/

	protected static class SharedData
	{
		protected SharedData(String id)
		{
			locationId = id;
		}
		public FileSystem fs;
		public ExternalSettings externalSettings;
		public final String locationId;
	}

	protected final Settings _globalSettings;
	private final SharedData _sharedData;

	protected  String _currentPathString;

	protected SharedData getSharedData()
	{
		return _sharedData;
	}	
	
	protected ArrayList<Path> loadPaths(Collection<String> paths) throws IOException
	{
		ArrayList<Path> res = new ArrayList<>();
		for(String path: paths)
			res.add(StdFs.getStdFs().getPath(path));
		return res;
	}
	
	protected ExternalSettings loadExternalSettings()
	{		
		return new ExternalSettings();
	}
	
}
