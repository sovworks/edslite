package com.sovworks.eds.android.settings;

import android.content.Context;

import java.io.File;

public abstract class SystemConfigCommon extends com.sovworks.eds.settings.SystemConfig
{
    public SystemConfigCommon(Context context)
    {
        _context = context;
    }

    @Override
    public File getTmpFolderPath()
    {
        return _context.getFilesDir();
    }

    @Override
    public File getCacheFolderPath()
    {
        return _context.getCacheDir();
    }

    @Override
    public File getPrivateExecFolderPath()
    {
        return _context.getFilesDir();
    }

    @Override
    public File getFSMFolderPath()
    {
        return new File(_context.getFilesDir(), "fsm");
    }

    protected final Context _context;
}
