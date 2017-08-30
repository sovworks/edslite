package com.sovworks.eds.android.filemanager.tasks;

import android.content.Context;
import android.os.Bundle;

import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.ExecutableFileRecord;
import com.sovworks.eds.android.filemanager.records.FolderRecord;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.io.IOException;
import java.util.ArrayList;

public class ReadDirTask extends ReadDirTaskBase
{
	public static BrowserRecord createBrowserRecordFromFile(Context context, @SuppressWarnings("UnusedParameters") Location loc, Path path, DirectorySettings directorySettings) throws IOException
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

	public static ReadDirTask newInstance(Location target, boolean addShowRootFolderLinks, int scrollPosition)
	{
		Bundle args = new Bundle();
		args.putParcelable(LocationsManager.PARAM_LOCATION_URI, target.getLocationUri());
		if(addShowRootFolderLinks)
			args.putBoolean(ARG_SHOW_ROOT_FOLDER_LINK, true);
		if(scrollPosition > 0)
			args.putInt(ARG_SCROLL_POSITION, scrollPosition);
		ReadDirTask f = new ReadDirTask();
		f.setArguments(args);
		return f;
	}

    public static ReadDirTask newInstance(Bundle args)
    {
        if(!args.containsKey(LocationsManager.PARAM_LOCATION_URI))
            throw new IllegalArgumentException("Location uri is not specified");
        ReadDirTask f = new ReadDirTask();
        f.setArguments(args);
        return f;
    }
}