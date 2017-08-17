package com.sovworks.eds.fs.std;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.util.PathBase;
import com.sovworks.eds.fs.util.StringPathUtil;

import java.io.File;
import java.io.IOException;

public class StdFsPath extends PathBase
{		
	public StdFsPath(StdFs stdFs, String pathString)
	{
		super(stdFs);
		_pathString = pathString;
		_stdFS = stdFs;
	}

	@Override
	public boolean exists() throws IOException
	{
		return getJavaFile().exists();
	}

	@Override
	public boolean isFile() throws IOException
	{
		return getJavaFile().isFile();
	}

	@Override
	public boolean isDirectory() throws IOException
	{
		return getJavaFile().isDirectory();
	}

	@Override
	public Directory getDirectory() throws IOException
	{
		return new StdDirRecord(_stdFS, this);
	}

	@Override
	public com.sovworks.eds.fs.File getFile() throws IOException
	{
		return new StdFileRecord(this);
	}


	public File getJavaFile() throws IOException
	{
		return new File(_stdFS.getRootDir().combine(_pathString).toString());
	}	
	
	@Override
	public boolean equals(Object o)
	{
		return (o instanceof StdFsPath) ?
			getAbsPath().equals(((StdFsPath)o).getAbsPath()) :			
			super.equals(o);		
	}
	
	@Override
	public String getPathString()
	{
		return _pathString;
	}	
	
	private final String _pathString;
	private final StdFs _stdFS;
	
	private StringPathUtil getAbsPath()
	{
		try
		{
			File f = getJavaFile();
			return new StringPathUtil(f.exists() ? f.getCanonicalPath() : f.getAbsolutePath());
		}
		catch(IOException e)
		{
			return getPathUtil();				
		}
	}	
	
}