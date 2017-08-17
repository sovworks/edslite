package com.sovworks.eds.settings;

import java.io.File;

public abstract class SystemConfigCommon
{
    public abstract File getCacheFolderPath();
    public abstract File getTmpFolderPath();
    public abstract File getPrivateExecFolderPath();
    public abstract File getFSMFolderPath();
}
