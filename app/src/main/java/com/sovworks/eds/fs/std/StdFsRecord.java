package com.sovworks.eds.fs.std;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public abstract class StdFsRecord implements FSRecord
{	
	@Override
	public Date getLastModified() throws IOException
	{
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(_path.getJavaFile().lastModified());
		return cal.getTime();
	}

	@Override
	public void setLastModified(Date dt) throws IOException
	{
		if(!_path.getJavaFile().setLastModified(dt.getTime()))
			throw new IOException("Failed setting last modified date");
	}

	@Override
	public Path getPath()
	{
		return _path;		
	}	

	@Override
	public void delete() throws IOException
	{
		if(_path.exists() && !_path.getJavaFile().delete())
			throw new IOException(String.format("Failed deleting %s", _path.getPathString()));
	}

	@Override
	public String getName() throws IOException
	{
		return _path.getPathUtil().getFileName();
	}

	@Override
	public void rename(String newName) throws IOException
	{
		moveTo((StdFsPath) PathUtil.changeFileName(_path, newName));
	}

	@Override
	public void moveTo(Directory newParent) throws IOException
	{
		moveTo((StdFsPath) newParent.getPath().combine(getName()));
	}

	public void moveTo(StdFsPath newPath) throws IOException
	{
		if(!_path.getJavaFile().renameTo(newPath.getJavaFile()))
			throw new IOException(String.format("Failed renaming %s to %s", _path.getPathString(),newPath.getPathString()));
		_path = newPath;
	}
	
	protected StdFsPath _path;
	
	protected StdFsRecord(StdFsPath path)
	{
		_path = path;
	}	
	
}