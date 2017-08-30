package com.sovworks.eds.fs.fat;

import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.fat.FatFS.ClusterChainIO;
import com.sovworks.eds.fs.fat.FatFS.FatPath;

import java.io.IOException;
import java.util.Date;

public class FileIO extends ClusterChainIO
{
	public FileIO(FatFS fat, DirEntry fileEntry,FatPath path,File.AccessMode mode,Object opTag) throws IOException
	{
		fat.super(fileEntry.startCluster,path,fileEntry.fileSize,mode);
		_fat = fat;		
		_fileEntry = fileEntry;
		_basePath = (FatPath)path.getParentPath();
		_opTag = opTag;
		if(mode == AccessMode.WriteAppend)
			seek(length());
		else if(mode == AccessMode.ReadWriteTruncate)
			setLength(0);
	}
	
	@Override
	public void flush() throws IOException
	{
		synchronized (_rwSync)
		{
			if(_isBufferDirty || !_addedClusters.isEmpty())
			{
				try
				{
					super.flush();
				}
				finally
				{
					updateFileEntry();
				}
			}
		}
	}
	
	@Override
	public void setLength(long newLength) throws IOException
	{		
		synchronized (_rwSync)
		{
			super.setLength(newLength);
			updateFileEntry();
		}
		
	}

	private final FatFS _fat;
	private final FatPath _basePath;
	private final DirEntry _fileEntry;
	private final Object _opTag;

	@Override
	protected void writeBuffer() throws IOException
	{
		super.writeBuffer();
		_fileEntry.lastModifiedDateTime = new Date();
	}

	private void updateFileEntry() throws IOException
	{
		if(_mode == AccessMode.Read)
			return;		
		_fileEntry.startCluster = _clusterChain.isEmpty() ? FatFS.LAST_CLUSTER : _clusterChain.get(0);
		_fileEntry.fileSize = _maxStreamPosition;
		_fileEntry.writeEntry(_fat, _basePath,_opTag);
		if(!_basePath.isRootDirectory())
		{
			FatPath parentPath = (FatPath) _basePath.getParentPath();
			if(parentPath!=null)
			{
				DirEntry entry = _fat.getDirEntry(_basePath, _opTag);
				if(entry!=null)
				{
					entry.lastModifiedDateTime = _fileEntry.lastModifiedDateTime;
					entry.writeEntry(_fat, parentPath, _opTag);
				}
			}
		}
	}
}
