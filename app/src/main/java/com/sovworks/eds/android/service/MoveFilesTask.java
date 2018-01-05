package com.sovworks.eds.android.service;

import android.content.Intent;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.errors.NoFreeSpaceLeftException;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.locations.Location;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MoveFilesTask extends CopyFilesTask
{
    @Override
	protected String getErrorMessage(Throwable ex)
	{
		return _context.getString(R.string.move_failed);
	}
		
	@Override
	protected Intent getOverwriteRequestIntent(SrcDstCollection filesToOverwrite) throws IOException, JSONException
	{
		return FileManagerActivity.getOverwriteRequestIntent(
				_context,
				true,
				filesToOverwrite
		);
	}

	@Override
	protected void processSrcDstCollection(SrcDstCollection col) throws Exception
	{
		super.processSrcDstCollection(col);
		for(Directory dir: _foldersToDelete)
			deleteEmptyDir(dir);
	}

	@Override
	protected boolean processRecord(SrcDst record) throws Exception
	{
		try
		{
			Location srcLocation = record.getSrcLocation();
			Location dstLocation = record.getDstLocation();
			if(dstLocation == null)
				throw new IOException("Failed to determine destination location for " + srcLocation.getLocationUri());
			_wipe = !srcLocation.isEncrypted() && dstLocation.isEncrypted();
			if (tryMove(record))
				return true;
			copyFiles(record);
			Path srcPath = srcLocation.getCurrentPath();
			if (srcPath.isDirectory())
				_foldersToDelete.add(0, srcPath.getDirectory());

		}
		catch(NoFreeSpaceLeftException e)
		{
			throw new com.sovworks.eds.android.errors.NoFreeSpaceLeftException(_context);
		}
		catch (IOException e)
		{
			setError(e);
		}
		return true;
	}

	private boolean tryMove(SrcDst srcDst) throws IOException
	{
		Location srcLocation = srcDst.getSrcLocation();
		Path srcPath = srcLocation.getCurrentPath();
		Location dstLocation = srcDst.getDstLocation();
		if(dstLocation == null)
			throw new IOException("Failed to determine destination location for " + srcLocation.getLocationUri());
		Path dstPath = dstLocation.getCurrentPath();
		if (srcPath.getFileSystem() == dstPath.getFileSystem())
		{
			if(srcPath.isFile())
			{
				if(tryMove(srcPath.getFile(), dstPath.getDirectory()))
				{
					ExtendedFileInfoLoader.getInstance().discardCache(srcLocation, srcPath);
					return true;
				}
			}
			else if(srcPath.isDirectory())
				return tryMove(srcPath.getDirectory(), dstPath.getDirectory());
		}
		return false;
	}

	private boolean tryMove(FSRecord srcFile, Directory newParent) throws IOException
	{
		try
		{
			Path dstRec = calcDstPath(srcFile, newParent);
			if(dstRec!=null)
			{
				if (dstRec.exists())
					return false;
			}
			srcFile.moveTo(newParent);
			return true;
		}
		catch(UnsupportedOperationException e)
		{
			return false;
		}
	}

	@Override
	protected boolean copyFile(SrcDst record) throws IOException
	{
		if(super.copyFile(record))
		{
			Location srcLoc = record.getSrcLocation();
			ExtendedFileInfoLoader.getInstance().discardCache(srcLoc, srcLoc.getCurrentPath());
			return true;
		}
		return false;
	}

	@Override
	protected boolean copyFile(File srcFile, File dstFile) throws IOException
	{
		if(super.copyFile(srcFile, dstFile))
		{
			deleteFile(srcFile);
			return true;
		}
		return false;
	}

	private void deleteFile(File file) throws IOException
	{
		com.sovworks.eds.android.helpers.WipeFilesTask.wipeFile(
				file,
				_wipe,
				new com.sovworks.eds.android.helpers.WipeFilesTask.ITask()
				{
					@Override
					public void progress(int sizeInc)
					{
						//incProcessedSize(sizeInc);
					}

					@Override
					public boolean cancel()
					{
						return isCancelled();
					}
				}
		);
	}

	private boolean deleteEmptyDir(Directory startDir) throws IOException
	{
		Directory.Contents dc = startDir.list();
		try
		{
			if(dc.iterator().hasNext())
				return false;
		}
		finally
		{
			dc.close();
		}
		startDir.delete();
		return true;
	}
/*
	protected boolean deleteEmptyDirsRec(Directory startDir) throws IOException
	{
		Directory.Contents dc = startDir.list();
		try
		{
			for(Path p: dc)
			{
				if(p.isDirectory())
				{
					if(!deleteEmptyDirsRec(p.getDirectory()))
						return false;
				}
				else
					return false;
			}
		}
		finally
		{
			dc.close();
		}
		startDir.delete();
		return true;
	}
*/
	private boolean _wipe;
	private List<Directory> _foldersToDelete = new ArrayList<>();
}