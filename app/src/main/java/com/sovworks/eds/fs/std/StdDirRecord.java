package com.sovworks.eds.fs.std;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.errors.DirectoryIsNotEmptyException;

class StdDirRecord extends StdFsRecord implements Directory
{		

	public StdDirRecord(StdFs stdFs, StdFsPath path) throws IOException
	{
		super(path);
		_stdFs = stdFs;
		if(path.exists() && !path.getJavaFile().isDirectory())
			throw new IllegalArgumentException("StdDirRecord error: file must be a directory");		
	}	

	@Override
	public long getTotalSpace() throws IOException
	{
		return _path.getJavaFile().getTotalSpace();
	}

	@Override
	public long getFreeSpace() throws IOException
	{
		//return _path.isRootDirectory() ? _path.getJavaFile().getFreeSpace() : _stdFs.getRootPath().getDirectory().getFreeSpace();
		return _path.getJavaFile().getFreeSpace();
	}

	@Override
	public void delete() throws IOException
	{
		if(_path.exists())
		{
			File[] ff = _path.getJavaFile().listFiles();
			if(ff!=null && ff.length>0)
				throw new DirectoryIsNotEmptyException("Directory is not empty: " + _path.getPathDesc());
		}
		super.delete();
	}

	@Override
	public Directory createDirectory(String name) throws IOException
	{
		StdFsPath newPath = (StdFsPath) _path.combine(name);
		if(!newPath.getJavaFile().mkdir())
			throw new IOException("Failed creating folder");
		return new StdDirRecord(_stdFs, newPath);
	}

	@Override
	public com.sovworks.eds.fs.File createFile(String name) throws IOException
	{
		StdFsPath newPath = (StdFsPath) _path.combine(name);
		if(!newPath.getJavaFile().createNewFile())
			throw new IOException("Failed creating file");
		return new StdFileRecord(newPath);
	}

	@Override
	public Directory.Contents list() throws IOException
	{
		File[] files = _path.getJavaFile().listFiles();
		final ArrayList<Path> res = files == null ? new ArrayList<Path>() : new ArrayList<Path>(files.length);
		if(files != null)		
			for(File f: files)
				res.add(_stdFs.getPath(f));
		
		return new Contents()
		{				
			@Override
			public void close() throws IOException
			{				
			}
			
			@Override
			public Iterator<Path> iterator()
			{
				return res.iterator();
			}
		};
	}
	
	private StdFs _stdFs;
}