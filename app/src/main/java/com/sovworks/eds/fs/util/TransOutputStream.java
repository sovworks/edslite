package com.sovworks.eds.fs.util;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.DataOutput;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.IOException;
import java.io.OutputStream;

public abstract class TransOutputStream extends OutputStream implements DataOutput
{
    public static boolean ENABLE_DEBUG_LOG = false;

    public TransOutputStream(OutputStream base, int bufferSize)
    {
        _base = base;
        _bufferSize = bufferSize;
        _buffer = new byte[_bufferSize];
    }

    @Override
    public void write(int b) throws IOException
    {
        byte[] buf = new byte[]{(byte) b};
        write(buf, 0, 1);
    }

    @Override
    public synchronized void write(byte[] buf, int offset, int count) throws IOException
    {
        log("write %d %d %d", buf.length, offset, count);
        while (count > 0)
        {
            byte[] currentBuffer = getCurrentBuffer();
            int written = Math.min(getSpaceInBuffer(), count);
            System.arraycopy(buf, offset, currentBuffer, getPositionInBuffer(), written);
            offset += written;
            count -= written;
            setCurrentBufferWritten(written);
        }
    }

    @Override
    public void close() throws IOException
    {
        close(true);
    }

    @Override
    public void flush() throws IOException
    {
        writeCurrentBuffer();
        _base.flush();
    }

    public void close(boolean closeBase) throws IOException
    {
        writeCurrentBuffer();
        if(closeBase)
            _base.close();
    }

    protected final OutputStream _base;
    protected final int _bufferSize;
    protected byte[] _buffer;
    protected long _bufferPosition;
    protected int _bytesWritten;

    protected abstract void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException;

    protected byte[] getCurrentBuffer() throws IOException
    {
        if(_bytesWritten >= _bufferSize)
        {
            transformBufferAndWriteToBase(_buffer, 0, _bytesWritten, getBufferPosition());
            _bufferPosition += _bytesWritten;
            _bytesWritten = 0;
        }
        return _buffer;
    }

    protected void writeCurrentBuffer() throws IOException
    {
        if(_bytesWritten > 0)
        {
            transformBufferAndWriteToBase(_buffer, 0, _bytesWritten, getBufferPosition());
            _bufferPosition += _bytesWritten;
            _bytesWritten = 0;
        }
    }

    protected void transformBufferAndWriteToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
    {
        transformBufferToBase(buf, offset, count , bufferPosition, buf);
        writeToBase(buf, offset, count);
    }

    protected void writeToBase(byte[] buf, int offset, int count) throws IOException
    {
        _base.write(buf, offset, count);
    }

    protected long getBufferPosition()
    {
        return _bufferPosition;
    }

    protected void log(String msg, Object... params)
    {
        if(ENABLE_DEBUG_LOG && GlobalConfig.isDebug())
            Logger.log(String.format("TransInputStream: " + msg, params));
    }

    protected void setCurrentBufferWritten(int numBytes)
    {
        _bytesWritten += numBytes;
    }

    protected int getPositionInBuffer()
    {
        return _bytesWritten;
    }

    protected int getSpaceInBuffer()
    {
        return _bufferSize - _bytesWritten;
    }

}
