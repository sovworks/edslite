package com.sovworks.eds.fs.util;

import java.io.IOException;
import java.io.InputStream;

import com.sovworks.eds.fs.DataInput;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.RandomStorageAccess;

public class RandomAccessInputStream extends InputStream implements DataInput, RandomStorageAccess
{

	public RandomAccessInputStream(RandomAccessIO io)
	{
		_io = io;
	}
	
	@Override
	public int read() throws IOException
	{
		return _io.read();
	}
	
	@Override
	public int read(byte[] b,int off,int len) throws IOException
	{
		return _io.read(b, off, len);
	}	

	@Override
	public void close() throws IOException
	{
		_io.close();
	}
	
	public void seek(long position) throws IOException
	{
		_io.seek(position);
	}

	public long getFilePointer() throws IOException
	{
		return _io.getFilePointer();
	}
	
	@Override
	public long length() throws IOException
	{
		return _io.length(); 
	}
	
	private final RandomAccessIO _io;	
}
