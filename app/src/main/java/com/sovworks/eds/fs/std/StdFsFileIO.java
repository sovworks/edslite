package com.sovworks.eds.fs.std;

import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;


public class StdFsFileIO extends RandomAccessFile implements RandomAccessIO
{	
	public StdFsFileIO(File f,AccessMode mode) throws IOException
	{
		super(f,mode == AccessMode.Read ? "r" : "rw");
		if(mode == AccessMode.ReadWriteTruncate)
			setLength(0);
		else if(mode == AccessMode.WriteAppend)
			seek(length());
	}	
	
	
	@Override
	public void close() throws IOException
	{
		try
		{
			FileDescriptor fd = getFD();
			if(fd!=null)
				fd.sync();
		}
		catch(SyncFailedException ignored) {}
		super.close();
	}	
	
	@Override
	public void flush() throws IOException
	{
				
	}	
}