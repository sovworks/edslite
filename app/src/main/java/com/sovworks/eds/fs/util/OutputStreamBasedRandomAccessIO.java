package com.sovworks.eds.fs.util;


import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamBasedRandomAccessIO implements RandomAccessIO
{
    public OutputStreamBasedRandomAccessIO(OutputStream base)
    {
        _base = base;
    }

    @Override
    public void setLength(long newLength) throws IOException
    {
    }

    @Override
    public void close() throws IOException
    {
        _base.close();

    }

    @Override
    public int read() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int b) throws IOException
    {
        _base.write(b);
        _curPos++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        _base.write(b, off, len);
        _curPos += len;
    }

    @Override
    public void flush() throws IOException
    {
        _base.flush();
    }

    @Override
    public void seek(long position) throws IOException
    {
        if(_curPos != position)
            throw new UnsupportedOperationException();
    }

    @Override
    public long getFilePointer() throws IOException
    {
        return _curPos;
    }

    @Override
    public long length() throws IOException
    {
        return _curPos;
    }

    private final OutputStream _base;
    private long _curPos;
}
