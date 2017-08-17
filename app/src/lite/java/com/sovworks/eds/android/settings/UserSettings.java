package com.sovworks.eds.android.settings;

import android.content.Context;

import com.sovworks.eds.settings.DefaultSettings;
import com.sovworks.eds.settings.Settings;

public class UserSettings extends UserSettingsCommon implements Settings
{
    public static synchronized UserSettings getSettings(Context context)
    {
        if(_instance == null)
            _instance = new UserSettings(context);

        return _instance;
    }

    public synchronized static void closeSettings()
    {
        if(_instance!=null)
            _instance.clearSettingsProtectionKey();
        _instance = null;
    }

    public UserSettings(Context context)
    {
        super(context, _defaultSettings);
    }

    private static final Settings _defaultSettings = new DefaultSettings();

    private static UserSettings _instance;
}
