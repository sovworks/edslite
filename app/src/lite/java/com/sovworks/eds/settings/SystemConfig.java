package com.sovworks.eds.settings;

public abstract class SystemConfig extends SystemConfigCommon
{
    public static SystemConfig getInstance()
    {
        return _instance;
    }

    public static void setInstance(SystemConfig systemConfig)
    {
        _instance = systemConfig;
    }

    private static SystemConfig _instance;
}
