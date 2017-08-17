package com.sovworks.eds.settings;


import com.sovworks.eds.android.BuildConfig;

class GlobalConfigCommon
{
	public static boolean isDebug()
	{
		return BuildConfig.DEBUG;
	}

    public static final int FB_PREVIEW_WIDTH = 40;
    public static final int FB_PREVIEW_HEIGHT = 40;
    public static final int CLEAR_MASTER_PASS_INACTIVITY_TIMEOUT = 20*60*1000;
    public static final String SUPPORT_EMAIL = "eds@sovworks.com";
    public static final String HELP_URL = "https://sovworks.com/eds/managing-containers.php";
    public static final String EXFAT_MODULE_URL = "https://github.com/sovworks/edsexfat";
}
