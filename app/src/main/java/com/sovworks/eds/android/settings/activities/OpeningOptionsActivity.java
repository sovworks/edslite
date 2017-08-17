package com.sovworks.eds.android.settings.activities;

import android.app.Fragment;
import android.content.Intent;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.settings.PropertiesHostWithStateBundle;
import com.sovworks.eds.android.settings.fragments.OpeningOptionsFragment;

public class OpeningOptionsActivity extends SettingsBaseActivity
{
    @Override
    public void onBackPressed()
    {
        PropertiesHostWithStateBundle frag = (PropertiesHostWithStateBundle) getFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_TAG);
        if(frag!=null)
        {
            try
            {
                frag.getPropertiesView().saveProperties();
                Intent res = new Intent();
                res.putExtras(frag.getState());
                setResult(RESULT_OK, res);
                super.onBackPressed();
            }
            catch (Exception e)
            {
                Logger.showAndLog(this, e);
            }
        }
        else
            super.onBackPressed();
    }

    @Override
    protected Fragment getSettingsFragment()
    {
        return getOpeningOptionsFragment();
    }

    private Fragment getOpeningOptionsFragment()
    {
        return new OpeningOptionsFragment();
    }

}
