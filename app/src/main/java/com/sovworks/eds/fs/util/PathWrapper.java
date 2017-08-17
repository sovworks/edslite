package com.sovworks.eds.fs.util;

import android.support.annotation.NonNull;

import java.io.IOException;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;

public abstract class PathWrapper implements Path
{
	public PathWrapper(FileSystem fs,Path path)
	{
		_fs = fs;
		_base = path;
	}

	@Override
	public FileSystem getFileSystem()
	{
		return _fs;
	}

	@Override
	public String getPathString()
	{
		return getBase().getPathString();
	}

	@Override
	public String getPathDesc()
	{
		return getBase().getPathDesc();
	}

	@Override
	public boolean isRootDirectory() throws IOException
	{
		return getBase().isRootDirectory();
	}

	@Override
	public boolean exists() throws IOException
	{
		return getBase().exists();
	}

	@Override
	public boolean isFile() throws IOException
	{
		return getBase().isFile();
	}

	@Override
	public boolean isDirectory() throws IOException
	{
		return getBase().isDirectory();
	}	

	@Override
	public Path combine(String part) throws IOException
	{
		return getPathFromBasePath(getBase().combine(part));
	}

	@Override
	public Directory getDirectory() throws IOException
	{
		return new DirectoryWrapper(this, getBase().getDirectory())
		{
			@Override
			protected Path getPathFromBasePath(Path basePath) throws IOException
			{
				return PathWrapper.this.getPathFromBasePath(basePath);
			}
		};
	}

	@Override
	public File getFile() throws IOException
	{
		return new FileWrapper(this, getBase().getFile())
		{
			@Override
			protected Path getPathFromBasePath(Path basePath) throws IOException
			{
				return PathWrapper.this.getPathFromBasePath(basePath);
			}
		};
	}

	@Override
	public Path getParentPath() throws IOException
	{
		return getPathFromBasePath(getBase().getParentPath());
	}
	
	public Path getBase()
	{
		return _base;
	}

	@Override
	public int compareTo(@NonNull Path another)
	{
		return getBase().compareTo(((PathWrapper)another).getBase());
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof PathWrapper && getBase().equals(((PathWrapper)o).getBase());
	}

	@Override
	public int hashCode()
	{
		return getBase().hashCode();
	}

	@Override
	public String toString()
	{
		return getBase().toString();
	}

	protected abstract Path getPathFromBasePath(Path basePath) throws IOException;

	private final FileSystem _fs;
	private final Path _base;

}
