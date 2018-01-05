package com.sovworks.eds.fs.util;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.tasks.ReadDir;
import com.sovworks.eds.fs.FileSystem;

import java.io.IOException;
import java.util.HashMap;

public class ContainerFSWrapper extends ActivityTrackingFSWrapper
{
    public ContainerFSWrapper(FileSystem baseFs)
    {
        super(baseFs);
    }

    @Override
    public void setChangesListener(ChangeListener listener)
    {
        throw new UnsupportedOperationException();
    }

    public synchronized DirectorySettings getDirectorySettings(com.sovworks.eds.fs.Path path)
    {
        if(path == null)
            return null;
        if(_dirSettingsCache.containsKey(path))
            return _dirSettingsCache.get(path);
        DirectorySettings ds = null;
        try
        {
            ds = ReadDir.loadDirectorySettings(path);
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        _dirSettingsCache.put(path, ds);
        return ds;
    }

    private final HashMap<com.sovworks.eds.fs.Path, DirectorySettings> _dirSettingsCache = new HashMap<>();

}
