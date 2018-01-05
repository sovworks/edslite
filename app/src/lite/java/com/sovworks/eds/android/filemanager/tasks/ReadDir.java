package com.sovworks.eds.android.filemanager.tasks;


import android.content.Context;

import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.ExecutableFileRecord;
import com.sovworks.eds.android.filemanager.records.FolderRecord;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ReadDir extends ReadDirBase
{
    static BrowserRecord createBrowserRecordFromFile(Context context, @SuppressWarnings("UnusedParameters") Location loc, Path path, DirectorySettings directorySettings) throws IOException
    {
        if (directorySettings != null)
        {
            StringPathUtil pu;
            if(path.isFile())
                pu = new StringPathUtil(path.getFile().getName());
            else if(path.isDirectory())
                pu = new StringPathUtil(path.getDirectory().getName());
            else
                pu = new StringPathUtil(path.getPathString());
            ArrayList<String> masks = directorySettings.getHiddenFilesMasks();
            if(masks != null)
                for (String mask : masks)
                {
                    if (pu.getFileName().matches(mask))
                        return null;
                }
        }

        return path.isDirectory() ?
                new FolderRecord(context)
                :
                new ExecutableFileRecord(context);
    }

    ReadDir(Context context, Location targetLocation, Collection<Path> selectedFiles, DirectorySettings dirSettings, boolean showRootFolderLink)
    {
        super(context, targetLocation, selectedFiles, dirSettings, showRootFolderLink);
    }
}
