package com.sovworks.eds.fs.util;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.Path;

import java.io.IOException;
import java.util.Date;

public abstract class FSRecordWrapper implements FSRecord
{
	public FSRecordWrapper(Path path, FSRecord base)
	{
		_base = base;
		_path = path;
	}

	@Override
	public Path getPath()
	{
		return _path;
	}

	@Override
	public String getName() throws IOException
	{
		return _base.getName();
	}

	@Override
	public void rename(String newName) throws IOException
	{
		_base.rename(newName);
		setPath(getPathFromBasePath(_base.getPath()));
	}

	@Override
	public Date getLastModified() throws IOException
	{
		return _base.getLastModified();
	}

	@Override
	public void setLastModified(Date dt) throws IOException
	{
		_base.setLastModified(dt);
	}

	@Override
	public void delete() throws IOException
	{
		_base.delete();
	}

	@Override
	public void moveTo(Directory newParent) throws IOException
	{
		_base.moveTo(((DirectoryWrapper)newParent).getBase());
		setPath(getPathFromBasePath(_base.getPath()));
	}
	
	public FSRecord getBase()
	{
		return _base;
	}

	protected abstract Path getPathFromBasePath(Path basePath) throws IOException;

	protected void setPath(Path path)
	{
		_path = path;
	}
	
	private final FSRecord _base;
	private Path _path;

}
