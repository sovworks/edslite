package com.sovworks.eds.crypto;

import java.io.File;
import java.io.IOException;

import com.sovworks.eds.crypto.modes.XTS;
import com.sovworks.eds.fs.RandomAccessIO;

public class LocalEncryptedFileXTS implements RandomAccessIO
{
	public LocalEncryptedFileXTS(String pathToFile, boolean readOnly, long dataOffset, XTS xts) throws IOException
	{
		_file = new File(pathToFile);
		_dataOffset = dataOffset;
		_contextPointer = initContext(pathToFile, readOnly, xts.getXTSContextPointer());
		if(_contextPointer == 0)
			throw new IOException("Context initialization failed");
		
		seek(0);
	}

	@Override
	public void close() throws IOException
	{
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		close(_contextPointer);
		_contextPointer = 0;
	}

	@Override
	public void seek(long position) throws IOException
	{
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		
		seek(_contextPointer,_dataOffset + position);
	}

	@Override
	public long getFilePointer() throws IOException
	{
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		
		return getPosition(_contextPointer) - _dataOffset;
	}

	@Override
	public long length() throws IOException
	{
		return _file.length() - _dataOffset;
	}

	@Override
	public int read() throws IOException
	{
		 return (read(_oneByteBuf, 0, 1) != -1) ? _oneByteBuf[0] & 0xff : -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if(off + len > b.length)
			throw new IndexOutOfBoundsException();
		
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		
		int res = read(_contextPointer,b,off,len);
		if(res<0)
			throw new IOException("Failed reading data");
		return res;
	}

	@Override
	public void write(int b) throws IOException
	{
		_oneByteBuf[0] = (byte)(b & 0xFF);
		write(_oneByteBuf,0,1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if(off + len > b.length)
			throw new IndexOutOfBoundsException();
		
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		
		if(write(_contextPointer,b,off,len)!=0)
			throw new IOException("Failed writing data");
	}

	@Override
	public void flush() throws IOException
	{	
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		flush(_contextPointer);
	}

	@Override
	public void setLength(long newLength) throws IOException
	{
		if(_contextPointer == 0)
			throw new IOException("File is closed");
		
		if (newLength < 0) 
            throw new IllegalArgumentException("newLength < 0");		
		        
        if(ftruncate(_contextPointer, newLength + _dataOffset)!=0)
        	throw new IOException("Failed truncating file");        	

        long filePointer = getFilePointer();
        if (filePointer > newLength) 
            seek(newLength);
	}
	
	private final byte[] _oneByteBuf = new byte[1];
	private final long _dataOffset;
	private long _contextPointer;
	private final File _file;
	
	static
	{
		System.loadLibrary("localxts");
	}
	
	private static native void flush(long contextPointer);
	private static native void close(long contextPointer);
	private static native long getPosition(long contextPointer);
	private static native void seek(long contextPointer, long newPosition);
	private static native int ftruncate(long contextPointer, long newLength);
	private static native long initContext(String pathToFile, boolean readOnly, long xtsContext); 
	private static native int read(long contextPointer, byte[] buf, int off, int len);
	private static native int write(long contextPointer, byte[] buf, int off, int len);

}
