package com.sovworks.eds.fs.util;


import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class BufferedRandomAccessIO extends RandomAccessIOWrapper
{
	public static boolean ENABLE_DEBUG_LOG = false;

	public BufferedRandomAccessIO(RandomAccessIO base, int bufferSize) throws FileNotFoundException
	{
		super(base);
		_bufferSize = bufferSize;
	}

	@Override
	public long getFilePointer() throws IOException
	{
		return _currentPosition;
	}

	@Override
	public void close() throws IOException
	{
		close(true);
	}

	public synchronized void close(boolean closeBase) throws IOException
	{
		if (closeBase)
			super.close();
	}

	@Override
	public synchronized int read(byte[] buf, int offset, int count) throws IOException
	{
		log("read %d %d %d", buf.length, offset, count);
		if (_currentPosition >= _length)
			return -1;
		if (count > 0)
		{
			byte[] currentBuffer = getCurrentBuffer();
			long avail = Math.min(getSpaceInBuffer(), _length - _currentPosition);
			int read = (int)Math.min(avail, count);
			System.arraycopy(currentBuffer, getPositionInBuffer(), buf, offset, read);
			setCurrentBufferRead(read);
			//if(LOG_MORE)
			//Log.d("EDS ClusterChainIO",String.format("ClusterChainIO read: file=%s read %d bytes",_path.getPathString(),avail));
			return read;
		}

		return 0;
	}

	@Override
	public int read() throws IOException
	{
		byte[] buf = new byte[1];
		return (read(buf, 0, 1) == 1) ? (buf[0] & 0xFF) : -1;
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
	public synchronized void seek(long position) throws IOException
	{
		log("seek %d", position);
		if (position < 0) throw new IllegalArgumentException();
		_currentPosition = position;
		//DEBUG
		//Log.d("EncryptedRAF.seek",String.format("current sector offset = %d, is_buffer_empty=%s, is_buffer_changed=%s ", _currentSectorOffset,_is_buffer_empty,_is_buffer_changed));		
	}

	@Override
	public long length() throws IOException
	{
		return _length;
	}

	@Override
	public synchronized void flush() throws IOException
	{
		writeCurrentBuffer();
		super.flush();
	}

	protected long _currentPosition, _length;
	protected final int _bufferSize;

	protected abstract byte[] getCurrentBuffer() throws IOException;
	protected abstract long getBufferPosition();

	protected void log(String msg, Object... params)
	{
		if (ENABLE_DEBUG_LOG && GlobalConfig.isDebug())
			Logger.log(String.format("EncryptedFile: " + msg, params));
	}

	protected void setCurrentBufferWritten(int numBytes)
	{
		_currentPosition += numBytes;
		if (_currentPosition > _length)
			_length = _currentPosition;
	}

	protected void setCurrentBufferRead(int numBytes)
	{
		_currentPosition += numBytes;
	}

	protected int getPositionInBuffer()
	{
		return (int) (_currentPosition % _bufferSize);
	}

	protected void writeCurrentBuffer() throws IOException
	{
		byte[] buf = getCurrentBuffer();
		RandomAccessIO base = getBase();
		base.seek(getBufferPosition());
		base.write(buf, 0, buf.length);
	}

	protected int getSpaceInBuffer()
	{
		return _bufferSize - getPositionInBuffer();
	}
}
