package com.sovworks.eds.android.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.sovworks.eds.android.EdsApplication;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.settings.SettingsCommon;

import org.json.JSONException;

import java.security.SecureRandom;
import java.util.List;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
public abstract class UserSettingsCommon implements SettingsCommon
{
	public static final String LOCATION_SETTINGS_PREFIX= "location_settings_";
	public static final String LOCATIONS_LIST = "locations_list";
	public static final String MAX_FILE_SIZE_TO_OPEN = "max_file_size_to_open";
	public static final String WIPE_TEMP_FILES = "wipe_temp_files";
	public static final String SHOW_PREVIEWS = "show_previews";
	public static final String WORK_DIR = "work_dir";
	public static final String LAST_VIEWED_CHANGES = "last_viewed_changes";
	public static final String USE_INTERNAL_IMAGE_VIEWER = "use_internal_image_viewer";
	public static final String LOCATION_SHORTCUT_WIDGET_PREFIX = "location_shortcut_widget_";
	public static final String DISABLE_WIDE_SCREEN_LAYOUTS = "disable_wide_screen_layouts";
	public static final String FILE_BROWSER_SORT_MODE = "file_browser_sort_mode";
	public static final String MAX_INACTIVITY_TIME = "max_inactivity_time";
	public static final String EXTENSIONS_MIME = "extensions_mime";
	public static final String IMAGE_VIEWER_FULL_SCREEN_ENABLED = "image_viewer_full_screen_enabled";
	public static final String IMAGE_VIEWER_AUTO_ZOOM_ENABLED = "image_viewer_auto_zoom_enabled";
	public static final String NEVER_SAVE_HISTORY = "never_save_history";
	public static final String DISABLE_DEBUG_LOG = "disable_debug_log";
	public static final String VISITED_HINT_SECTIONS = "visited_hint_sections";
	public static final String DISABLE_HINTS = "disable_hints";
	public static final String DISABLE_MODIFIED_FILES_BACKUP = "disable_modified_files_backup";
	public static final String IS_FLAG_SECURE_ENABLED = "is_flag_secure_enabled";
	public static final String FORCE_UNMOUNT = "force_unmount";
	public static final String CURRENT_SETTINGS_VERSION = "current_settings_version";
	public static final String SETTINGS_PROTECTION_KEY_OLD = "settings_protection_key";
	public static final String SETTINGS_PROTECTION_KEY_USER = "settings_protection_key_user";
	public static final String SETTINGS_PROTECTION_KEY_AUTO = "settings_protection_key_auto";
	public static final String THEME = "theme";
	public static final String EXTERNAL_FILE_MANAGER = "external_file_manager";
	public static final String DONT_USE_CONTENT_PROVIDER = "dont_use_content_provider";
	public static final String FORCE_TEMP_FILES = "force_temp_files";

	public static final String SETTINGS_PROTECTION_KEY_CHECK = "protection_key_check";

	public static final String PREFS_NAME = "com.sovworks.eds.PREFERENCES";


	public static boolean isWideScreenLayout(SettingsCommon settings, Activity activity)
	{
		return !settings.disableLargeSceenLayouts() && activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	protected UserSettingsCommon(Context context, SettingsCommon defaultSettings)
	{
		_context = context;
		_defaultSettings = defaultSettings;
		_prefs = GlobalConfig.isDebug() ?
					context.getSharedPreferences("debug", 0)					
				:
					context.getSharedPreferences(PREFS_NAME, 0);
					
	}
	
	@Override
	public String getLocationSettingsString(String locationId)
	{
		return _prefs.getString(
				LOCATION_SETTINGS_PREFIX + locationId,
				_defaultSettings.getLocationSettingsString(locationId)
		);
	}
	
	@SuppressLint("CommitPrefEdits")
	@Override
	public void setLocationSettingsString(String locationId, String data)
	{
		SharedPreferences.Editor e = _prefs.edit();
		if(data == null)
			e.remove(LOCATION_SETTINGS_PREFIX + locationId);
		else
			e.putString(LOCATION_SETTINGS_PREFIX + locationId, data);
		e.commit();
	}
	
	@Override
	public String getStoredLocations()
	{
		return _prefs.getString(
				LOCATIONS_LIST,
				_defaultSettings.getStoredLocations()
		);
	}

	@SuppressLint("CommitPrefEdits")
	@Override
	public void setStoredLocations(String locations)
	{	
		SharedPreferences.Editor e = _prefs.edit();
		e.putString(LOCATIONS_LIST, locations);
		e.commit();
	}
	
	@Override
	public int getMaxTempFileSize()
	{
		return _prefs.getInt(MAX_FILE_SIZE_TO_OPEN,
				_defaultSettings.getMaxTempFileSize()
		);
	}
	
	@Override
	public boolean wipeTempFiles()
	{
		return _prefs.getBoolean(WIPE_TEMP_FILES, _defaultSettings.wipeTempFiles());
	}

	@Override
	public boolean showPreviews()
	{		
		return _prefs.getBoolean(SHOW_PREVIEWS, _defaultSettings.showPreviews());
	}
	
	@Override
	public String getWorkDir()
	{
		return _prefs.getString(WORK_DIR, _defaultSettings.getWorkDir());
	}

	@Override
	public int getLastViewedPromoVersion()
	{
		return _prefs.getInt(LAST_VIEWED_CHANGES,_defaultSettings.getLastViewedPromoVersion());
	}

	@Override
	public int getInternalImageViewerMode()
	{
		return _prefs.getInt(USE_INTERNAL_IMAGE_VIEWER, _defaultSettings.getInternalImageViewerMode());
	}
	
	@Override
	public LocationShortcutWidgetInfo getLocationShortcutWidgetInfo(int widgetId)
	{
		String data = _prefs.getString(LOCATION_SHORTCUT_WIDGET_PREFIX + widgetId, null);
		if(data == null)
			return _defaultSettings.getLocationShortcutWidgetInfo(widgetId);
		LocationShortcutWidgetInfo i = new LocationShortcutWidgetInfo();
		i.load(data);
		return i;
	}
	
	@SuppressLint("CommitPrefEdits")
	@Override
	public void setLocationShortcutWidgetInfo(int widgetId,
			LocationShortcutWidgetInfo info)
	{
		SharedPreferences.Editor editor = _prefs.edit();
		if(info == null)
			editor.remove(LOCATION_SHORTCUT_WIDGET_PREFIX + widgetId);
		else
			editor.putString(LOCATION_SHORTCUT_WIDGET_PREFIX + widgetId, info.toString());
		editor.commit();
	}
	
	@Override
	public boolean disableLargeSceenLayouts()
	{		
		return _prefs.getBoolean(DISABLE_WIDE_SCREEN_LAYOUTS, _defaultSettings.disableLargeSceenLayouts());
	}

	@Override
	public int getFilesSortMode() 
	{
		return _prefs.getInt(FILE_BROWSER_SORT_MODE, _defaultSettings.getFilesSortMode());
	}

	@Override
	public int getMaxContainerInactivityTime() 
	{		
		return _prefs.getInt(MAX_INACTIVITY_TIME, _defaultSettings.getMaxContainerInactivityTime());
	}
		
	@Override
	public String getExtensionsMimeMapString()
	{
		return _prefs.getString(EXTENSIONS_MIME, _defaultSettings.getExtensionsMimeMapString());
	}
	
	@Override
	public boolean isImageViewerFullScreenModeEnabled()
	{		
		return _prefs.getBoolean(IMAGE_VIEWER_FULL_SCREEN_ENABLED, _defaultSettings.isImageViewerFullScreenModeEnabled());
	}

	@Override
	public boolean isImageViewerAutoZoomEnabled()
	{
		return _prefs.getBoolean(IMAGE_VIEWER_AUTO_ZOOM_ENABLED, _defaultSettings.isImageViewerAutoZoomEnabled());
	}

	@Override
	public boolean neverSaveHistory()
	{
		return _prefs.getBoolean(NEVER_SAVE_HISTORY, _defaultSettings.neverSaveHistory());
	}
	
	@Override
	public boolean disableDebugLog()
	{		
		return _prefs.getBoolean(DISABLE_DEBUG_LOG, _defaultSettings.disableDebugLog());
	}
	
	@Override
	public List<String> getVisitedHintSections()
	{
		String s = _prefs.getString(VISITED_HINT_SECTIONS, null);
		if(s == null)
			return _defaultSettings.getVisitedHintSections();
			
		try
		{
			return Util.loadStringArrayFromString(s);
		}
		catch (JSONException e)
		{
			return _defaultSettings.getVisitedHintSections();
		}
	}

	@Override
	public boolean isHintDisabled()
	{
		return _prefs.getBoolean(DISABLE_HINTS, _defaultSettings.isHintDisabled());
	}

	@Override
	public boolean disableModifiedFilesBackup()
	{		
		return _prefs.getBoolean(DISABLE_MODIFIED_FILES_BACKUP, _defaultSettings.disableModifiedFilesBackup());
	}

	@Override
	public boolean isFlagSecureEnabled()
	{
		return _prefs.getBoolean(IS_FLAG_SECURE_ENABLED, _defaultSettings.isFlagSecureEnabled());
	}

	@Override
	public boolean alwaysForceClose()
	{
		return _prefs.getBoolean(FORCE_UNMOUNT, _defaultSettings.alwaysForceClose());
	}

	@Override
	public int getCurrentSettingsVersion()
	{
		return _prefs.getInt(CURRENT_SETTINGS_VERSION , -1);
	}

	public SharedPreferences getSharedPreferences()
	{
		return _prefs;
	}

    public String getProtectedString(String key) throws InvalidSettingsPassword
    {
		byte[] pd = getProtectedData(key);
        return pd == null ? null : new String(pd);
    }

	public byte[] getProtectedData(String key) throws InvalidSettingsPassword
	{
		if(_resetProtectedSettings)
			return null;
		String encryptedString = _prefs.getString(key, null);
		if(encryptedString == null)
			return null;
		try
		{
			return SimpleCrypto.decrypt(getSettingsProtectionKey(), encryptedString);
		}
		catch (Exception e)
		{
			throw new InvalidSettingsPassword();
		}
	}

    @SuppressLint("CommitPrefEdits")
    public void setProtectedField(String key, String value) throws InvalidSettingsPassword
    {
		_prefs.edit().putString(
				key,
				SimpleCrypto.encrypt(getSettingsProtectionKey(), value.getBytes())
		).commit();
	}

	@SuppressLint("CommitPrefEdits")
	public void setProtectedField(String key, byte[] value) throws InvalidSettingsPassword
	{
		_prefs.edit().putString(
				key,
				SimpleCrypto.encrypt(getSettingsProtectionKey(), value)
		).commit();
	}

    @Override
    public synchronized SecureBuffer getSettingsProtectionKey() throws InvalidSettingsPassword
    {
        if(_settingsProtectionKey == null)
        {
			boolean isUser = false;
			String encryptedString = _prefs.getString(SETTINGS_PROTECTION_KEY_USER, null);
			if(encryptedString == null)
			{
				encryptedString = _prefs.getString(SETTINGS_PROTECTION_KEY_AUTO, null);
				if(encryptedString == null)
				{
					encryptedString = _prefs.getString(SETTINGS_PROTECTION_KEY_OLD, null);
					isUser = true;
				}
			}
			else
				isUser = true;
            if(encryptedString == null)
                saveSettingsProtectionKey();
            else
            {
				try
                {
					byte[] settingsPassword = getSettingsPassword();
					try
					{
						_settingsProtectionKey = new SecureBuffer(SimpleCrypto.decryptWithPassword(settingsPassword, encryptedString));
						if (!isSettingsPasswordValid())
						{
							clearSettingsProtectionKey();
							if(isUser)
								throw new InvalidSettingsPassword();
							else
								saveSettingsProtectionKey();
						}
					}
					finally
					{
						SecureBuffer.eraseData(settingsPassword);
					}
				}
                catch (Exception e)
                {
                    clearSettingsProtectionKey();
                    InvalidSettingsPassword e1 = new InvalidSettingsPassword();
                    e1.initCause(e);
                    throw e1;
                }

            }
        }
		return _settingsProtectionKey;
	}

    public boolean isSettingsPasswordValid()
    {
        String pf;
        try
        {
            pf = getProtectedString(SETTINGS_PROTECTION_KEY_CHECK);
        }
        catch (InvalidSettingsPassword ignored)
        {
            return false;
        }
        return CHECK_PHRASE.equals(pf);
    }

	@Override
	public int getCurrentTheme()
	{
		return _prefs.getInt(THEME, _defaultSettings.getFilesSortMode());
	}

	@Override
	public ExternalFileManagerInfo getExternalFileManagerInfo()
	{
		String data = _prefs.getString(EXTERNAL_FILE_MANAGER, null);
		if(data == null)
			return _defaultSettings.getExternalFileManagerInfo();
		ExternalFileManagerInfo i = new ExternalFileManagerInfo();
		try
		{
			i.load(data);
		}
		catch (JSONException e)
		{
			return null;
		}
		return i;
	}

	@Override
	public boolean dontUseContentProvider()
	{
		return _prefs.getBoolean(DONT_USE_CONTENT_PROVIDER, _defaultSettings.dontUseContentProvider());
	}

	@Override
	public boolean forceTempFiles()
	{
		return _prefs.getBoolean(FORCE_TEMP_FILES, _defaultSettings.forceTempFiles());
	}

	public synchronized void saveSettingsProtectionKey() throws InvalidSettingsPassword
    {
        if(_settingsProtectionKey == null)
        {
            SecureRandom sr = new SecureRandom();
			byte[] k = new byte[32];
            sr.nextBytes(k);
			_settingsProtectionKey = new SecureBuffer(k);
        }
		byte[] key = _settingsProtectionKey.getDataArray();
		if(key == null)
			throw new InvalidSettingsPassword();
		try
		{
			byte[] pass = getUserSettingsPassword();
			if(pass != null)
			{
				try
				{
					_prefs.edit().putString(
							SETTINGS_PROTECTION_KEY_USER,
							SimpleCrypto.encryptWithPassword(pass, key)
					).
							remove(SETTINGS_PROTECTION_KEY_AUTO).
							remove(SETTINGS_PROTECTION_KEY_OLD).
							commit();
				}
				finally
				{
					SecureBuffer.eraseData(pass);
				}
			}
			else
			{
				pass = getAutoSettingsPassword();
				try
				{
					_prefs.edit().putString(
							SETTINGS_PROTECTION_KEY_AUTO,
							SimpleCrypto.encryptWithPassword(pass, key)
					).
							remove(SETTINGS_PROTECTION_KEY_USER).
							remove(SETTINGS_PROTECTION_KEY_OLD).
							commit();
				}
				finally
				{
					SecureBuffer.eraseData(pass);
				}
			}
		}
		finally
		{
			SecureBuffer.eraseData(key);
		}
		_resetProtectedSettings = false;
		setProtectedField(SETTINGS_PROTECTION_KEY_CHECK, CHECK_PHRASE);
    }

    public synchronized void clearSettingsProtectionKey()
    {
        if(_settingsProtectionKey!=null)
        {
            _settingsProtectionKey.close();
            _settingsProtectionKey = null;
        }
	}

	protected final SharedPreferences _prefs;

    private static final String CHECK_PHRASE = "valid pass";
	protected final Context _context;
	private final SettingsCommon _defaultSettings;
    private SecureBuffer _settingsProtectionKey;
	private boolean _resetProtectedSettings;

	private byte[] getUserSettingsPassword()
	{
		SecureBuffer mp = EdsApplication.getMasterPassword();
		if(mp != null)
		{
			byte[] mpd = mp.getDataArray();
			if(mpd!=null)
				return mpd;
		}
		return null;
	}

	private byte[] getAutoSettingsPassword()
	{
		return Util.getDefaultSettingsPassword(_context).getBytes();
	}

    private byte[] getSettingsPassword()
	{
        byte[] res = getUserSettingsPassword();
        return res == null ? getAutoSettingsPassword() : res;
    }
}