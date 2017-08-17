package com.sovworks.eds.fs.util;

import java.io.IOException;
import java.io.InputStream;

public class DirectPipedInputStream extends InputStream
{

	@Override
	public synchronized int read() throws IOException
	{
		int res = read(_oneByteBuffer, 0, 1);
		return res < 0 ? res : _oneByteBuffer[0];	
	}
	
	@Override
	public int read(byte[] buf, int offset, int len) throws IOException
	{
		synchronized (_sync)
		{
			if(_finWrite)
				return -1;
			_actualBytes = -1;
			_buffer = buf;
			_offset = offset;
			_requestedBytes = len;			
			_sync.notify();
		}	
		while(true)
		{
			synchronized (_sync)
			{
				if(_actualBytes > 0)												
					return _actualBytes;
				
				if(_finWrite)
					return -1;
				try
				{
					_sync.wait();
				}
				catch (InterruptedException e)
				{
				}			
			}
		}
	}
	
	@Override
	public void close() throws IOException
	{
		synchronized (_sync)
		{
			_finRead = true;
			_sync.notify();			
		}
	}
	
	byte[] getBuffer()
	{
		while(!_finRead)		
		{						
			synchronized (_sync)
			{
				if(_buffer != null)
					break;
				try
				{
					_sync.wait();
				}
				catch (InterruptedException e)
				{					
				}				
			}			
		}
		return _buffer;		
	}
	
	int getRequestedBytes()
	{
		return _requestedBytes;
	}
	
	int getOffset()
	{
		return _offset;
	}
	
	void releaseBuffer(int availableBytes)
	{
		synchronized (_sync)
		{
			_buffer = null;
			_actualBytes = availableBytes;
			_sync.notify();
		}
	}
	
	
	void finWrite()
	{
		synchronized (_buffer)
		{
			_finWrite = true;
			_buffer.notify();			
		}		
	}
	
	private final Object _sync = new Object();
		
	private byte[] _buffer;	
	private final byte[] _oneByteBuffer = new byte[1];
	private int _offset, _requestedBytes, _actualBytes;
	
	private boolean _finWrite, _finRead;	
		
	
}
