package com.sovworks.eds.settings;

import com.sovworks.eds.crypto.SecureBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public interface SettingsCommon
{
	int VERSION = 3;

	int USE_INTERNAL_IMAGE_VIEWER_NEVER = 0;
	int USE_INTERNAL_IMAGE_VIEWER_VIRT_FS = 1;
	int USE_INTERNAL_IMAGE_VIEWER_ALWAYS = 2;

	int FB_SORT_DATE_ASC = 2;
	int FB_SORT_DATE_DESC = 3;
	int FB_SORT_SIZE_ASC = 4;
	int FB_SORT_SIZE_DESC = 5;
	int FB_SORT_FILENAME_ASC = 0;
	int FB_SORT_FILENAME_DESC = 1;
	int FB_SORT_FILENAME_NUM_ASC = 6;
	int FB_SORT_FILENAME_NUM_DESC = 7;

	int THEME_DEFAULT = 0;
	int THEME_DARK = 1;


	class InvalidSettingsPassword extends Exception
    {

    }

	class LocationShortcutWidgetInfo
	{
		public String widgetTitle;
		public String locationUriString;		
		
		public String save() throws JSONException
		{
			JSONObject jo = new JSONObject();
			jo.put(SETTINGS_WIDGET_TITLE, widgetTitle);			
			jo.put(SETTINGS_LOCATION_URI, locationUriString);
			return jo.toString();
		}
		
		public void load(String data)
		{
			JSONObject jo;
			try
			{
				jo = new JSONObject(data);
				widgetTitle = jo.optString(SETTINGS_WIDGET_TITLE);
				locationUriString = jo.optString(SETTINGS_LOCATION_URI);				
			}
			catch (JSONException ignored)
			{
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
		
		private static final String SETTINGS_WIDGET_TITLE = "widget_title";
		private static final String SETTINGS_LOCATION_URI = "location_uri";		
	}

	class ExternalFileManagerInfo
	{
		public String packageName;
		public String className;
		public String action;
		public String mimeType;


		public void save(JSONObject jo) throws JSONException
		{
			jo.put(SETTINGS_PACKAGE_NAME, packageName);
			jo.put(SETTINGS_CLASS_NAME, className);
			jo.put(SETTINGS_ACTION, action);
			jo.put(SETTINGS_MIME_TYPE, mimeType);
		}

		public void load(JSONObject jo)
		{
			packageName = jo.optString(SETTINGS_PACKAGE_NAME);
			className = jo.optString(SETTINGS_CLASS_NAME);
			action= jo.optString(SETTINGS_ACTION);
			mimeType = jo.optString(SETTINGS_MIME_TYPE);
		}

		public void load(String s) throws JSONException
		{
			JSONObject jo = new JSONObject(s);
			load(jo);
		}

		public String save() throws JSONException
		{
			JSONObject jo = new JSONObject();
			save(jo);
			return jo.toString();
		}

		private static final String SETTINGS_PACKAGE_NAME = "package_name";
		private static final String SETTINGS_CLASS_NAME = "class_name";
		private static final String SETTINGS_ACTION = "action";
		private static final String SETTINGS_MIME_TYPE = "mime_type";
	}

	String getStoredLocations();
	void setStoredLocations(String locations);
	
	String getLocationSettingsString(String locationId);
	void setLocationSettingsString(String locationId, String data);
	int getMaxTempFileSize();
	boolean wipeTempFiles();
	boolean showPreviews();
	String getWorkDir();
	String getExtensionsMimeMapString();
	int getLastViewedPromoVersion();
	int getInternalImageViewerMode();
	LocationShortcutWidgetInfo getLocationShortcutWidgetInfo(int widgetId);
	void setLocationShortcutWidgetInfo(int widgetId, LocationShortcutWidgetInfo info);
	boolean disableLargeSceenLayouts();
	int getFilesSortMode();
	int getMaxContainerInactivityTime();
	boolean isImageViewerFullScreenModeEnabled();
	boolean isImageViewerAutoZoomEnabled();
	boolean neverSaveHistory();
	boolean disableDebugLog();
	boolean disableModifiedFilesBackup();
	List<String> getVisitedHintSections();
	boolean isHintDisabled();
    SecureBuffer getSettingsProtectionKey() throws InvalidSettingsPassword;
    boolean isFlagSecureEnabled();
	int getCurrentSettingsVersion();
	boolean alwaysForceClose();
	int getCurrentTheme();
	ExternalFileManagerInfo getExternalFileManagerInfo();
	boolean dontUseContentProvider();
	boolean forceTempFiles();
}
