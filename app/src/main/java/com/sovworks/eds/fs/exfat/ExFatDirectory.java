package com.sovworks.eds.fs.exfat;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

class ExFatDirectory extends ExFatRecord implements Directory
{

    ExFatDirectory(ExFat exFat, ExFatPath path)
    {
        super(exFat, path);
    }

    @Override
    public Directory createDirectory(String name) throws IOException
    {
        String newPath = getPath().getPathUtil().combine(name).toString();
        synchronized (_exFat._sync)
        {
            int res = _exFat.makeDir(newPath);
            if (res != 0)
                throw new IOException("Failed making directory. Error code = " + res);
        }
        return new ExFatDirectory(_exFat, new ExFatPath(_exFat, newPath));
    }

    @Override
    public File createFile(String name) throws IOException
    {
        String newPath = getPath().getPathUtil().combine(name).toString();
        synchronized (_exFat._sync)
        {
            int res = _exFat.makeFile(newPath);
            if (res != 0)
                throw new IOException("Failed making directory. Error code = " + res);
        }
        return new ExFatFile(_exFat, new ExFatPath(_exFat, newPath));
    }

    @Override
    public Contents list() throws IOException
    {
        ArrayList<String> names = new ArrayList<>();
        synchronized (_exFat._sync)
        {
            int res = _exFat.readDir(_path.getPathString(), names);
            if (res != 0)
                throw new IOException("readDir failed. Error code = " + res);
        }
        final ArrayList<Path> paths = new ArrayList<>();
        StringPathUtil curPath = _path.getPathUtil();
        for(String name: names)
            paths.add(new ExFatPath(_exFat, curPath.combine(name).toString()));
        return new Contents()
        {
            @Override
            public void close() throws IOException
            {

            }

            @Override
            public Iterator<Path> iterator()
            {
                return paths.iterator();
            }
        };
    }

    @Override
    public void delete() throws IOException
    {
        synchronized (_exFat._sync)
        {
            int res = _exFat.rmdir(_path.getPathString());
            if (res != 0)
                throw new IOException("Delete failed. Error code = " + res);
        }
    }

    @Override
    public long getTotalSpace() throws IOException
    {
        synchronized (_exFat._sync)
        {
            long res = _exFat.getTotalSpace();
            if (res < 0)
                throw new IOException("Failed getting total space");
            return res;
        }
    }

    @Override
    public long getFreeSpace() throws IOException
    {
        synchronized (_exFat._sync)
        {
            long res = _exFat.getFreeSpace();
            if (res < 0)
                throw new IOException("Failed getting free space");
            return res;
        }
    }
}
