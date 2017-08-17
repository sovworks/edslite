package com.sovworks.eds.fs.util;


import com.sovworks.eds.fs.RandomAccessIO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class TransRandomAccessIO extends BufferedRandomAccessIO
{
	public TransRandomAccessIO(RandomAccessIO base, int bufferSize) throws FileNotFoundException
	{
		super(base, bufferSize);
		_buffer = new byte[_bufferSize];
	}

	public synchronized void close(boolean closeBase) throws IOException
	{
        try
        {
            writeCurrentBuffer();
			super.close(closeBase);
        }
        finally
        {
			Arrays.fill(_buffer, (byte) 0);
        }
	}

	@Override
	public synchronized void write(byte[] buf, int offset, int count) throws IOException
	{
		if(!_allowSkip && _currentPosition > _length)
			fillFreeSpace();
		super.write(buf, offset, count);
	}

	@Override
	public synchronized void setLength(long newLength) throws IOException
	{
		if(!_allowSkip && newLength > _length - 1)
		{
			seek(newLength - 1);
			write(0);
		}
		else
		{
			_length = newLength;
			super.setLength(calcBasePosition(newLength));
		}
	}

	public final void setAllowSkip(boolean val)
	{
		_allowSkip = val;
	}

	protected byte[] _buffer;
	protected boolean _allowSkip = false, _isBufferLoaded, _isBufferChanged;

    @Override
    protected void setCurrentBufferWritten(int numBytes)
    {
		super.setCurrentBufferWritten(numBytes);
		_isBufferChanged = true;
    }

    @Override
    protected byte[] getCurrentBuffer() throws IOException
    {
        if(_isBufferLoaded)
        {
			long dif = _currentPosition - _bufferPosition;
            if(dif<0 || dif >= _bufferSize)
            {
                writeCurrentBuffer();
				_bufferPosition = calcBufferPosition();
                _isBufferLoaded = false;
            }
        }
		else
			_bufferPosition = calcBufferPosition();
        loadCurrentBuffer();
        return _buffer;
    }

    @Override
    protected void writeCurrentBuffer() throws IOException
    {
        if(!_isBufferChanged)
			return;
        long bp = getBufferPosition();
		int count = Math.min((int) (_length - bp), _bufferSize);
		transformBufferAndWriteToBase(_buffer, 0, count, bp);
        _isBufferChanged = false;
    }

	protected void loadCurrentBuffer() throws IOException
	{
		if(_isBufferLoaded)
			return;
		long bp = getBufferPosition();
		int space = (int)Math.min(_length - bp, _bufferSize);
		if(space > 0)
		{
			int act = readFromBaseAndTransformBuffer(_buffer, 0, space, bp);
			Arrays.fill(_buffer, act, _bufferSize, (byte) 0);
		}
		_isBufferChanged = false;
		_isBufferLoaded = true;
	}

	protected int readFromBaseAndTransformBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		int bc = readFromBase(buf, offset, count, bufferPosition);
		return transformBufferFromBase(buf, offset, bc, bufferPosition, buf);
	}

	protected void transformBufferAndWriteToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		transformBufferToBase(buf, offset, count , bufferPosition, buf);
		writeToBase(buf, offset, count, bufferPosition);
	}

	protected void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException
	{

	}

	protected int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException
	{
		return count;
	}

    protected void writeToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
    {
        getBase().seek(calcBasePosition(bufferPosition));
        getBase().write(buf, offset, count);
    }

	protected int readFromBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		getBase().seek(calcBasePosition(bufferPosition));
		return readFullyEncrypted(buf, offset, count);
		//if(bc != count)
		//	throw new IOException("Got " + bc + " bytes instead of " + count);
	}

	protected long calcBasePosition(long position)
	{
		return position;
	}

	protected long calcVirtPosition(long basePosition)
	{
		return basePosition;
	}

	protected long getBufferPosition()
	{
		return _bufferPosition;
	}

	protected int readFullyEncrypted(byte[] buf, int off,int len) throws IOException
	{
		int t = 0;
		while(t<len)
		{
			int n = getBase().read(buf, off + t, len - t);
			if(n<0)
				return t;
			t+=n;
		}
		return t;
	}

	protected void fillFreeSpace() throws IOException
	{
		long pos = _length;
		int rem = (int) (_length % _bufferSize);
		if(rem != 0)
			pos += _bufferSize - rem;
		byte[] tbuf = new byte[_bufferSize];
		//this method should be called when the current buffer is modified so
		// the ending position would be the start position of the current buffer
		for(long bp = getBufferPosition();pos<bp;pos+= _bufferSize)
			transformBufferAndWriteToBase(tbuf, 0, _bufferSize, pos);
	}

    private long _bufferPosition;

	private long calcBufferPosition()
	{
		return _currentPosition  - (_currentPosition % _bufferSize);
	}

}
