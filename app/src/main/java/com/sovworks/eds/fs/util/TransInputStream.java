package com.sovworks.eds.fs.util;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.DataInput;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public abstract class TransInputStream extends InputStream implements DataInput
{
    public static boolean ENABLE_DEBUG_LOG = false;

    public TransInputStream(InputStream base, int bufferSize)
    {
        _base = base;
        _bufferSize = bufferSize;
        _buffer = new byte[_bufferSize];
    }

    @Override
    public synchronized int read(byte[] buf, int offset, int count) throws IOException
    {
        log("read %d %d %d", buf.length, offset, count);
        byte[] currentBuffer = getCurrentBuffer();
        if(_bytesLeft <= 0)
            return -1;
        int avail = Math.min(getSpaceInBuffer(), _bytesLeft);
        int read = Math.min(avail, count);
        System.arraycopy(currentBuffer, getPositionInBuffer(), buf, offset, read);
        setCurrentBufferRead(read);
        //if(LOG_MORE)
        //Log.d("EDS ClusterChainIO",String.format("ClusterChainIO read: file=%s read %d bytes",_path.getPathString(),avail));
        return read;
    }

    @Override
    public int read() throws IOException
    {
        byte[] buf = new byte[1];
        return (read(buf, 0, 1) == 1) ? (buf[0] & 0xFF) : -1;
    }

    @Override
    public void close() throws IOException
    {
        close(true);
    }

    public void close(boolean closeBase) throws IOException
    {
        if(closeBase)
            _base.close();
    }

    protected final InputStream _base;
    protected final int _bufferSize;
    protected byte[] _buffer;
    protected long _currentPosition;
    protected int _bytesLeft;

    protected abstract int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException;

    protected byte[] getCurrentBuffer() throws IOException
    {
        if(_bytesLeft <= 0)
            _bytesLeft = readFromBaseAndTransformBuffer(_buffer, 0, _bufferSize, getBufferPosition());
        return _buffer;
    }

    protected int readFromBaseAndTransformBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
    {
        int br = readFromBase(buf, offset, count);
        return transformBufferFromBase(buf, offset, br, bufferPosition, buf);
    }

    protected int readFromBase(byte[] buf, int offset, int count) throws IOException
    {
        int t = 0;
        while(t<count)
        {
            int n = _base.read(buf, offset + t, count - t);
            if(n<0)
                return t;
            t+=n;
        }
        return t;
    }

    protected long getBufferPosition()
    {
        return _currentPosition - (_currentPosition % _bufferSize);
    }

    protected void log(String msg, Object... params)
    {
        if(ENABLE_DEBUG_LOG && GlobalConfig.isDebug())
            Logger.log(String.format("TransInputStream: " + msg, params));
    }

    protected void setCurrentBufferRead(int numBytes)
    {
        _currentPosition += numBytes;
        _bytesLeft -= numBytes;
    }

    protected int getPositionInBuffer()
    {
        return (int) (_currentPosition % _bufferSize);
    }

    protected int getSpaceInBuffer()
    {
        return _bufferSize - getPositionInBuffer();
    }

}
