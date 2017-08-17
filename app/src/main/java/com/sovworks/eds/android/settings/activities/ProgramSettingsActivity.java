package com.sovworks.eds.android.settings.activities;

import android.app.Fragment;

import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.settings.fragments.ProgramSettingsFragment;

public class ProgramSettingsActivity extends SettingsBaseActivity
{
    @Override
    protected Fragment getSettingsFragment()
    {
        return new ProgramSettingsFragment();
    }
}
