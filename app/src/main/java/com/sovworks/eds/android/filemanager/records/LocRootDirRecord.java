package com.sovworks.eds.android.filemanager.records;

import android.content.Context;

import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import java.io.IOException;

public class LocRootDirRecord extends FolderRecord
{
	public LocRootDirRecord(Context context) throws IOException
	{
        super(context);
	}

	@Override
	public void init(Location location, Path path) throws IOException
	{
		super.init(location, path);
		_rootFolderName = super.getName();
		if((_rootFolderName == null || _rootFolderName.isEmpty()) && location!=null)
			_rootFolderName = location.getTitle() + "/";
	}

	@Override
	public String getName()
	{
		return _rootFolderName;
	}	

    @Override
	public boolean isFile()
	{
		return false;
	}

	@Override
	public boolean isDirectory()
	{
		return true;
	}

	private String _rootFolderName;

}