package com.sovworks.eds.fs.std;

import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;

import java.io.File;
import java.io.IOException;


public class StdFs implements FileSystem
{
	
	public static StdFsPath makePath(Object... elements) throws IOException
	{
		return (StdFsPath)Util.makePath(getStdFs(), elements);
	}
	
	public static StdFs getStdFs()
	{
		return getStdFs(null);
	}
	
	public static synchronized StdFs getStdFs(String rootDir)
	{
		if(rootDir==null || rootDir.length()==0 || rootDir.equals("/"))
		{
			if(_rootStdFs == null)
				_rootStdFs = new StdFs("");
			return _rootStdFs;
		}
		
		return new StdFs(rootDir);
	}
	
	@Override
	public com.sovworks.eds.fs.Path getPath(String pathString)
			throws IOException
	{
		return new StdFsPath(this, pathString);		
	}
	
	@Override
	public Path getRootPath()
	{
		try
		{
			return getPath("/");
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close(boolean force) throws IOException 
	{	
		
	}

	@Override
	public boolean isClosed()
	{
		return false;
	}

	public StringPathUtil getRootDir()
	{
		return _rootDir;
	}
		
	Path getPath(File f) throws IOException
	{
		StringPathUtil pu = new StringPathUtil(f.getPath());
		return getPath(pu.getSubPath(_rootDir).toString());
	}
	
	protected StdFs(String rootDir)
	{
		_rootDir = new StringPathUtil(rootDir);
	}
	
	private static StdFs _rootStdFs;
	
	private final StringPathUtil _rootDir;
	
}