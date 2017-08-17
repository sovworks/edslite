package com.sovworks.eds.fs.exfat;

import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;

class ExFatRAIO implements RandomAccessIO
{
    ExFatRAIO(ExFat exfat, long fileHandle, long startPosition, File.AccessMode mode)
    {
        _exfat = exfat;
        _fileHandle = fileHandle;
        _position = startPosition;
        _mode = mode;
    }


    @Override
    public void seek(long position) throws IOException
    {
        synchronized (_exfat._sync)
        {
            _position = position;
        }
    }

    @Override
    public long getFilePointer() throws IOException
    {
        return _position;
    }

    @Override
    public long length() throws IOException
    {
        synchronized (_exfat._sync)
        {
            long res = _exfat.getSize(_fileHandle);
            if (res < 0)
                throw new IOException("Failed getting node size.");
            return res;
        }
    }

    @Override
    public void setLength(long newLength) throws IOException
    {
        if(_mode == File.AccessMode.Read)
            throw new IOException("Read-only mode");
        synchronized (_exfat._sync)
        {
            int res = _exfat.truncate(_fileHandle, newLength);
            if (res != 0)
                throw new IOException("Truncate failed. Error code = " + res);
            if (_position > newLength)
                _position = newLength;
        }
    }

    @Override
    public synchronized void write(int b) throws IOException
    {
        _obBuf[0] = (byte)(b & 0xFF);
        write(_obBuf,0,1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        if(_mode == File.AccessMode.Read)
            throw new IOException("Read-only mode");
        synchronized (_exfat._sync)
        {
            int res = _exfat.write(_fileHandle, b, off, len, _position);
            if (res < 0)
                throw new IOException("Write failed. Result = " + res);
            _position += res;
        }
    }

    @Override
    public void flush() throws IOException
    {
        synchronized (_exfat._sync)
        {
            int res = _exfat.flush(_fileHandle);
            if (res != 0)
                throw new IOException("Flush failed. Error code = " + res);
        }
    }

    @Override
    public synchronized int read() throws IOException
    {
        int cnt = read(_obBuf,0,1);
        return cnt <= 0 ? -1 : (_obBuf[0] & 0xFF);
    }

    @Override
    public int read(byte[] b, int off, int count) throws IOException
    {
        synchronized (_exfat._sync)
        {
            int res = _exfat.read(_fileHandle, b, off, count, _position);
            if (res < 0)
                throw new IOException("Read failed. Result = " + res);
            if (res == 0 && count > 0)
                return -1;
            _position += res;
            return res;
        }
    }

    @Override
    public void close() throws IOException
    {
        synchronized (_exfat._sync)
        {
            if(_fileHandle != 0)
            {
                int res = _exfat.closeFile(_fileHandle);
                if (res != 0)
                    throw new IOException("Close failed. Error code = " + res);
                _fileHandle = 0;
            }
        }

    }

    private final byte[] _obBuf = new byte[1];
    private final ExFat _exfat;
    private long _fileHandle;
    private final File.AccessMode _mode;
    private long _position;
}
