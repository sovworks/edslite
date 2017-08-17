package com.sovworks.eds.fs.util;

import android.support.annotation.NonNull;

import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;

import java.io.IOException;

public abstract class PathBase implements Path
{
	@Override
	public FileSystem getFileSystem()
	{
		return _fs;
	}

	@Override
	public String getPathDesc()
	{
		return getPathString();
	}

	@Override
    public boolean isRootDirectory() throws IOException
    {
    	return isDirectory() && getParentPath() == null;
    }

	@Override
    public PathBase combine(String part) throws IOException
    {
    	return (PathBase) _fs.getPath(getPathUtil().combine(part).toString());
    }

    @Override
	public boolean equals(Object o)
	{
    	StringPathUtil pu = getPathUtil();
		if(o instanceof PathBase)
		{
			StringPathUtil opu = ((PathBase)o).getPathUtil();
			return /*((Path)o).getFileSystem().equals(getFileSystem()) && */((pu==null && opu==null) || (pu != null && pu.equals(opu)));
		}

		if(o instanceof String || o instanceof StringPathUtil)
			return pu.equals(o);

		return super.equals(o);
	}

	@Override
	public int hashCode()
	{
		StringPathUtil pu = getPathUtil();
		return pu==null ? 0 : pu.hashCode();
	}

	@Override
	public Path getParentPath() throws IOException
	{
		StringPathUtil pu = getPathUtil();
		return pu==null || pu.isEmpty() ? null : _fs.getPath(pu.getParentPath().toString());
	}

	@Override
	public String toString()
	{
		return getPathString();
	}

	@Override
	public int compareTo(@NonNull Path other)
	{
		return getPathUtil().compareTo(new StringPathUtil(other.getPathString()));
	}

	public StringPathUtil getPathUtil()
	{
		return new StringPathUtil(getPathString());
	}

    protected PathBase(FileSystem fs)
    {
    	_fs = fs;
    }

    private final FileSystem _fs;
}
