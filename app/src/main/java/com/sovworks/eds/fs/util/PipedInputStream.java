package com.sovworks.eds.fs.util;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class PipedInputStream extends InputStream
{

	@Override
	public synchronized int read() throws IOException
	{
		int res = read(_oneByteBuffer, 0, 1);
		return res < 0 ? res : _oneByteBuffer[0];	
	}
	
	@Override
	public int read(@NonNull byte[] buf, int offset, int len) throws IOException
	{
		synchronized (_buffer)
        {
            int avail = getAvailByteCountR();
            if (avail >= 0)
            {
                avail = Math.min(avail, len);
                System.arraycopy(_buffer, _rp, buf, offset, avail);
                incReadPos(avail);
                return avail;
            }
        }
		return -1;
		
	}
	
	@Override
	public void close() throws IOException
	{
		synchronized (_buffer)
		{
			_finRead = true;
			_buffer.notify();			
		}
	}
	
	int write(byte[] buf, int offset, int len)
	{
		synchronized (_buffer)
        {
            int avail = getAvailByteCountW();
            if (avail >= 0)
            {
                avail = Math.min(avail, len);
                System.arraycopy(buf, offset, _buffer, _wp, avail);
                incWritePos(avail);
                return avail;
            }
        }
		return -1;				
	}
	
	void notifyBuffer()
	{
		synchronized (_buffer)
		{
			_buffer.notify();			
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
	
	private final byte[] _oneByteBuffer = new byte[1];
	private final byte[] _buffer = new byte[200*1024];
	private int _rp, _wp, _nbr;
	private boolean _finWrite, _finRead;	
	
	private int getAvailByteCountW()
	{
		while(_nbr == _buffer.length) try
		{
			_buffer.wait();
			if(_finWrite || _finRead)
				return -1;
		}
		catch (InterruptedException e)
		{
			return -1;
		}		
		int dif = _rp - _wp;
		if(dif<=0)
			return _buffer.length - _wp;			
		return dif;				
	}
	
	private int getAvailByteCountR()
	{
		while(_nbr == 0 && !_finWrite) try
		{
			_buffer.wait();
			if(_finRead)			
				return -1;
		}
		catch (InterruptedException ignored)
		{
		}		
		if(_nbr == 0 && _finWrite)
			return -1;
		int dif = _wp - _rp;
		if(dif == 0)
			return Math.min(_nbr, _buffer.length - _rp);
		if(dif<0)
			return _buffer.length - _rp;			
		return dif;				
	}
	
	private void incWritePos(int count)
	{		
		_wp += count;		
		if(_wp >= _buffer.length)
			_wp -= _buffer.length;
		_nbr += count;
		if(_nbr == _buffer.length)
			_buffer.notify();
	}
	
	private void incReadPos(int count)
	{		
		_rp += count;
		if(_rp >= _buffer.length)
			_rp -= _buffer.length;
		_nbr -= count;
		_buffer.notify();		
	}	
}
