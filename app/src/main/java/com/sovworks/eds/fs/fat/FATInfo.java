package com.sovworks.eds.fs.fat;

import android.os.Parcel;

import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;


public class FATInfo extends FileSystemInfo
{
    public static final Creator<FATInfo> CREATOR = new Creator<FATInfo>()
    {
        @Override
        public FATInfo createFromParcel(Parcel in)
        {
            return new FATInfo();
        }

        @Override
        public FATInfo[] newArray(int size)
        {
            return new FATInfo[size];
        }
    };

    public static final String NAME = "FAT";

    @Override
    public String getFileSystemName()
    {
        return NAME;
    }

    @Override
    public void makeNewFileSystem(RandomAccessIO img) throws IOException
    {
        FatFS.formatFat(img, img.length());
    }

    @Override
    public FileSystem openFileSystem(RandomAccessIO img, boolean readOnly) throws IOException
    {
        FatFS fs = FatFS.getFat(img);
        fs.setReadOnlyMode(readOnly);
        return fs;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
    }
}
