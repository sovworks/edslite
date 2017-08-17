package com.sovworks.eds.fs.exfat;

import com.sovworks.eds.exceptions.NativeError;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.FileStat;
import com.sovworks.eds.fs.util.PathBase;

import java.io.IOException;



class ExFatPath extends PathBase implements Path
{
    ExFatPath(ExFat fs, String pathString)
    {
        super(fs);
        _pathString = pathString;
    }

    @Override
    public String getPathString()
    {
        return _pathString;
    }

    @Override
    public boolean exists() throws IOException
    {
        return getAttr() != null;
    }

    @Override
    public boolean isFile() throws IOException
    {
        FileStat attr = getAttr();
        return attr!=null && !attr.isDir;
    }

    @Override
    public boolean isDirectory() throws IOException
    {
        FileStat attr = getAttr();
        return attr!=null && attr.isDir;
    }

    @Override
    public Directory getDirectory() throws IOException
    {
        return new ExFatDirectory(getFileSystem(), this);
    }

    @Override
    public File getFile() throws IOException
    {
        return new ExFatFile(getFileSystem(), this);
    }

    @Override
    public ExFat getFileSystem()
    {
        return (ExFat) super.getFileSystem();
    }

    private final String _pathString;

    FileStat getAttr() throws IOException
    {
        ExFat ef = getFileSystem();
        synchronized (ef._sync)
        {
            FileStat stat = new FileStat();
            int res = ef.getAttr(stat, _pathString);
            if (res == NativeError.ENOENT)
                return null;
            if (res != 0)
                throw new IOException("getAttr failed. Error code = " + res);
            return stat;
        }
    }
}
