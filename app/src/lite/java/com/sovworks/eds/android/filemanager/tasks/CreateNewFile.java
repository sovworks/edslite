package com.sovworks.eds.android.filemanager.tasks;


import android.content.Context;

import com.sovworks.eds.locations.Location;

public class CreateNewFile extends CreateNewFileBase
{
    CreateNewFile(Context context, Location location, String fileName, int fileType, boolean returnExisting)
    {
        super(context, location, fileName, fileType, returnExisting);
    }
}
