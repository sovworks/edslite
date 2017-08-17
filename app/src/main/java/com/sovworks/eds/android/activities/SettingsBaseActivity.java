package com.sovworks.eds.android.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.settings.UserSettings;

public abstract class SettingsBaseActivity extends Activity
{
    public static final String SETTINGS_FRAGMENT_TAG = "com.sovworks.eds.android.locations.SETTINGS_FRAGMENT";

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        if(UserSettings.getSettings(this).isFlagSecureEnabled())
            CompatHelper.setWindowFlagSecure(this);
        if(savedInstanceState == null)
            getFragmentManager().
                beginTransaction().
                add(android.R.id.content, getSettingsFragment(), SETTINGS_FRAGMENT_TAG).
                commit();
    }

    protected abstract Fragment getSettingsFragment();
}
