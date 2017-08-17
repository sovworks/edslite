package com.sovworks.eds.android.filemanager.tasks;

import android.os.Bundle;

public class CreateNewFileTask extends CreateNewFileTaskBase
{
    public static CreateNewFileTask newInstance(String filename, int fileType)
    {
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, filename);
        args.putInt(ARG_TYPE, fileType);
        CreateNewFileTask f = new CreateNewFileTask();
        f.setArguments(args);
        return f;
    }

}