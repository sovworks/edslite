package com.sovworks.eds.fs.exfat;

import android.os.ParcelFileDescriptor;

import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.FileStat;
import com.sovworks.eds.fs.util.RandomAccessInputStream;
import com.sovworks.eds.fs.util.RandomAccessOutputStream;
import com.sovworks.eds.fs.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ExFatFile extends ExFatRecord implements File
{
    ExFatFile(ExFat exFat, ExFatPath path)
    {
        super(exFat, path);
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return new RandomAccessInputStream(getRandomAccessIO(AccessMode.Read));
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return new RandomAccessOutputStream(getRandomAccessIO(AccessMode.Write));
    }

    @Override
    public synchronized RandomAccessIO getRandomAccessIO(AccessMode accessMode) throws IOException
    {
        synchronized (_exFat._sync)
        {
            FileStat fs = _path.getAttr();
            if (accessMode == AccessMode.Read && (fs == null || fs.isDir))
                throw new FileNotFoundException();
            if (fs == null)
            {
                int res = _exFat.makeFile(_path.getPathString());
                if (res != 0)
                    throw new IOException("Failed creating file. Error code = " + res);
                fs = _path.getAttr();
                if (fs == null)
                    throw new IOException("File node is null");
            }
            long startPos = 0;
            if (accessMode == AccessMode.WriteAppend)
                startPos = fs.size;

            long handle = _exFat.openFile(_path.getPathString());
            if (handle == 0)
                throw new IOException("Failed getting file handle");
            if (accessMode == AccessMode.Write || accessMode == AccessMode.ReadWriteTruncate)
            {
                int res = _exFat.truncate(handle, 0);
                if (res != 0)
                {
                    _exFat.closeFile(res);
                    throw new IOException("Failed truncating file. Error code = " + res);
                }
            }
            return new ExFatRAIO(_exFat, handle, startPos, accessMode);
        }
    }

    @Override
    public void delete() throws IOException
    {
        synchronized (_exFat._sync)
        {
            int res = _exFat.delete(_path.getPathString());
            if (res != 0)
                throw new IOException("Delete failed. Error code = " + res);
        }
    }

    @Override
    public long getSize() throws IOException
    {
        return _path.getAttr().size;

    }

    @Override
    public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode) throws IOException
    {
        return null;
    }

    @Override
    public void copyToOutputStream(OutputStream output, long offset, long count, ProgressInfo progressInfo) throws IOException
    {
        Util.copyFileToOutputStream(output, this, offset, count, progressInfo);
    }

    @Override
    public void copyFromInputStream(InputStream input, long offset, long count, ProgressInfo progressInfo) throws IOException
    {
        Util.copyFileFromInputStream(input, this, offset, count, progressInfo);
    }
}
