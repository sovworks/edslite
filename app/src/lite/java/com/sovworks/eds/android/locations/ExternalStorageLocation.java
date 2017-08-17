package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.DeviceBasedLocation;
import com.sovworks.eds.locations.LocationBase;

import org.json.JSONException;
import org.json.JSONObject;

public class ExternalStorageLocation extends DeviceBasedLocation
{
	public static final String URI_SCHEME = "ext-st";

	public static class ExternalSettings extends LocationBase.ExternalSettings
	{
		public boolean dontAskWritePermission()
		{
			return _dontAskWritePermission;
		}
		public void setDontAskWritePermission(boolean val)
		{
			_dontAskWritePermission = val;
		}

		public String getDocumentsAPIUriString()
		{
			return _documentsAPIUriString;
		}

		public void setDocumentsAPIUriString(String documentsAPIUriString)
		{
			_documentsAPIUriString = documentsAPIUriString;
		}

		@Override
		public void saveToJSONObject(JSONObject jo) throws JSONException
		{
			super.saveToJSONObject(jo);
			jo.put(SETTINGS_DONT_ASK_WRITE_PERMISSION, _dontAskWritePermission);
			if(_documentsAPIUriString == null)
				jo.remove(SETTINGS_DOC_API_URI_STRING);
			else
				jo.put(SETTINGS_DOC_API_URI_STRING, _documentsAPIUriString);
		}

		@Override
		public void loadFromJSONOjbect(JSONObject jo) throws JSONException
		{
			super.loadFromJSONOjbect(jo);
			_dontAskWritePermission = jo.optBoolean(SETTINGS_DONT_ASK_WRITE_PERMISSION, false);
			_documentsAPIUriString = jo.optString(SETTINGS_DOC_API_URI_STRING, null);
		}

		private static final String SETTINGS_DOC_API_URI_STRING = "documents_api_uri_string";
		private static final String SETTINGS_DONT_ASK_WRITE_PERMISSION = "dont_ask_write_permission";

		private String _documentsAPIUriString;
		private boolean _dontAskWritePermission;
	}

	public ExternalStorageLocation(Context context, String label, String mountPath, String currentPath)
	{
		super(UserSettings.getSettings(context), label, mountPath, currentPath);
		_context = context;
	}

	public ExternalStorageLocation(Context context, Uri uri)
	{
		super(UserSettings.getSettings(context), uri);
		_context = context;
	}

	@Override
	public Uri.Builder makeFullUri()
	{
		return super.makeFullUri().scheme(URI_SCHEME);
	}

	@Override
	public synchronized ExternalSettings getExternalSettings()
	{
		return (ExternalSettings)super.getExternalSettings();
	}

	@Override
	public void saveExternalSettings()
	{
		try
		{
			UserSettings.getSettings(_context).setLocationSettingsString(getId(), getExternalSettings().save());
		}
		catch (JSONException e)
		{
			throw new RuntimeException("Settings serialization failed", e);
		}
	}

	@Override
	public ExternalStorageLocation copy()
	{
		return new ExternalStorageLocation(_context, getTitle(), getRootPath(), _currentPathString);
	}

	protected final Context _context;

	@Override
	protected ExternalSettings loadExternalSettings()
	{
		ExternalSettings res = new ExternalSettings();
		res.load(UserSettings.getSettings(_context),getId());
		return res;
	}
}
