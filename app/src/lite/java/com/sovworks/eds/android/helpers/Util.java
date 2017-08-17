package com.sovworks.eds.android.helpers;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.settings.SettingsCommon;

import java.io.IOException;

public class Util extends UtilBase
{
    public static SecureBuffer getPassword(Bundle args, @SuppressWarnings("UnusedParameters") LocationsManager lm) throws IOException
    {
        return args.getParcelable(Openable.PARAM_PASSWORD);
    }

    public static void setTheme(Activity act)
    {
        int theme = UserSettings.getSettings(act.getApplicationContext()).getCurrentTheme();
        act.setTheme(theme == SettingsCommon.THEME_DARK ?
                R.style.Theme_EDS_Dark :
                R.style.Theme_EDS
        );
    }

    public static String getDefaultSettingsPassword(Context context)
    {
        try
        {
            return SimpleCrypto.calcStringMD5(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        }
        catch(Exception e)
        {
            Logger.log(e);
        }
        return "";
    }
}

