package com.sovworks.eds.fs;

import android.os.Parcelable;

import com.sovworks.eds.fs.exfat.ExFATInfo;
import com.sovworks.eds.fs.exfat.ExFat;
import com.sovworks.eds.fs.fat.FATInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public abstract class FileSystemInfo implements Parcelable
{
    public static List<FileSystemInfo> getSupportedFileSystems()
    {
        ArrayList<FileSystemInfo> res = new ArrayList<>();
        res.add(new FATInfo());
        if(ExFat.isModuleInstalled())
            res.add(new ExFATInfo());
        return res;
    }

    public abstract String getFileSystemName();
    public abstract void makeNewFileSystem(RandomAccessIO img) throws IOException;
    public abstract FileSystem openFileSystem(RandomAccessIO img, boolean readOnly) throws IOException;
}
