package com.sovworks.eds.android.service;

import android.content.Intent;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.fs.DocumentTreeFS;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.errors.NoFreeSpaceLeftException;
import com.sovworks.eds.fs.util.FilesCountAndSize;
import com.sovworks.eds.fs.util.FilesOperationStatus;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.fs.util.SrcDstPlain;
import com.sovworks.eds.locations.Location;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.CancellationException;

class CopyFilesTask extends FileOperationTaskBase
{	
	public static class CopyFilesTaskParam extends FileOperationTaskBase.FileOperationParam
	{
		CopyFilesTaskParam(Intent i)
		{
			super(i);
			_overwrite = i.getBooleanExtra(FileOpsService.ARG_OVERWRITE, false);
		}
		
		public boolean forceOverwrite()
		{
			return _overwrite;
		}
		
		SrcDstPlain getOverwriteTargetsStorage()
		{
			return _overwriteTargets;			
		}
		
		private final boolean _overwrite;
		private final SrcDstPlain _overwriteTargets = new SrcDstPlain();
	}
		
	@Override
	public void onCompleted(Result result)
	{
        try
		{
            result.getResult();
			CopyFilesTaskParam p = getParam();
            //noinspection ThrowableResultOfMethodCallIgnored
            if (p.getOverwriteTargetsStorage()!=null && !p.getOverwriteTargetsStorage().isEmpty())
				_context.startActivity(getOverwriteRequestIntent(p.getOverwriteTargetsStorage()));
		}
        catch(CancellationException ignored)
        {

        }
		catch (Throwable e)
		{
            reportError(e);
		}
        finally
		{
            super.onCompleted(result);
		}
	}	
	
	@Override
	protected CopyFilesTaskParam initParam(Intent i)
	{
		return new CopyFilesTaskParam(i);
	}
	
	@Override
	protected FilesOperationStatus initStatus(SrcDstCollection records)
	{
		FilesOperationStatus status = new FilesOperationStatus();
		status.total = FilesCountAndSize.getFilesCountAndSize(false,records);
		return status;
	}

    @Override
    protected String getNotificationText()
    {
        String txt = super.getNotificationText();
        long curTime = System.currentTimeMillis();
		long prevTime = _currentStatus.prevUpdateTime;
		if(curTime - prevTime > 1000)
		{
			if(prevTime>0)
			{
				float speed = 1000*(float)(_currentStatus.processed.totalSize - _currentStatus.prevProcSize)/(1024*1024*(curTime - prevTime));
                txt += ", " + _context.getString(R.string.speed, speed);
			}
			_currentStatus.prevProcSize = _currentStatus.processed.totalSize;
			_currentStatus.prevUpdateTime = curTime;
		}
        return txt;
    }

	@Override
	protected boolean processRecord(SrcDst record) throws Exception
	{
		try
		{
			copyFiles(record);
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

	protected boolean copyFiles(SrcDst record) throws IOException
	{
		boolean res = true;
		Path src = record.getSrcLocation().getCurrentPath();
		if (src.isFile())
		{
			if (!copyFile(record)) res = false;
			if (_currentStatus.processed.filesCount < _currentStatus.total.filesCount - 1) _currentStatus.processed.filesCount++;
		}
		else if (!makeDir(record))
			res = false;

		updateUIOnTime();
		return res;
	}

	private boolean makeDir(SrcDst record) throws IOException
	{
		Path src = record.getSrcLocation().getCurrentPath();
		Location dstLocation = record.getDstLocation();
		if(dstLocation == null)
			throw new IOException("Failed to determine destination folder for " + src.getPathDesc());
		return makeDir(src, dstLocation.getCurrentPath());
	}

	private boolean makeDir(Path srcPath, Path dstPath) throws IOException
	{
		return makeDir(srcPath.getDirectory(), dstPath.getDirectory());
	}

	private boolean makeDir(Directory srcFolder, Directory targetFolder) throws IOException
	{
		String srcName = srcFolder.getName();
		_currentStatus.fileName = srcName;
		updateUIOnTime();
		Path dstPath = calcDstPath(srcFolder, targetFolder);
        if(dstPath == null || !dstPath.exists())
		{
			targetFolder.createDirectory(srcName);
			return true;
		}
		return false;
	}

	protected boolean copyFile(SrcDst record) throws IOException
	{
		Path src = record.getSrcLocation().getCurrentPath();
		Location dstLocation = record.getDstLocation();
		if(dstLocation == null)
			throw new IOException("Failed to determine destination folder for " + src.getPathDesc());
		Path dst = dstLocation.getCurrentPath();
		if(copyFile(src, dst))
		{
			ExtendedFileInfoLoader.getInstance().discardCache(record.getDstLocation(), dst);
			return true;
		}
		else
        {
            getParam().getOverwriteTargetsStorage().add(record);
            return false;
        }
	}

	protected boolean copyFile(Path srcPath, Path dstPath) throws IOException
	{
		return copyFile(srcPath.getFile(), dstPath.getDirectory());
	}


    protected Path calcDstPath(FSRecord src, Directory dstFolder) throws IOException
    {
        return calcPath(dstFolder, src.getName());
    }

    Path calcPath(Directory dstFolder, String name)
    {
        try
        {
            return dstFolder.getPath().combine(name);
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

	protected boolean copyFile(File srcFile, Directory targetFolder) throws IOException
	{
		String srcName = srcFile.getName();
		_currentStatus.fileName = srcName;
		updateUIOnTime();
		Path dstPath = calcDstPath(srcFile, targetFolder);
        if(dstPath != null && !dstPath.exists())
            dstPath = null;
		if (!getParam().forceOverwrite() && dstPath!=null)
			return false;

		if (!(targetFolder instanceof DocumentTreeFS.Directory))
		{
			long size = srcFile.getSize();
			long space = targetFolder.getFreeSpace();
			if(space > 0 && size > space)
				throw new NoFreeSpaceLeftException();
		}
		return copyFile(srcFile, dstPath!=null ? dstPath.getFile() : targetFolder.createFile(srcName));
	}


	protected boolean copyFile(File srcFile, File dstFile) throws IOException
	{
		Date srcDate = srcFile.getLastModified();
		InputStream fin = null;
		OutputStream fout = null;
		final byte[] buffer = new byte[8*1024];
		int bytesRead;
		try
		{
			fin = srcFile.getInputStream();
			fout = dstFile.getOutputStream();
			while ((bytesRead = fin.read(buffer)) >= 0)
			{
				if (isCancelled()) throw new CancellationException();
				fout.write(buffer, 0, bytesRead);
				incProcessedSize(bytesRead);
			}
		}
		finally
		{
			if (fin != null) fin.close();
			if (fout != null) fout.close();
		}
		try
		{
			dstFile.setLastModified(srcDate);
		}
		catch (IOException e)
		{
			Logger.log(e);
		}
        return true;
	}

	@Override
	protected String getErrorMessage(Throwable ex)
	{
		return _context.getString(R.string.copy_failed);
	}

	protected Intent getOverwriteRequestIntent(SrcDstCollection filesToOverwrite) throws IOException, JSONException
	{
		return FileManagerActivity.
				getOverwriteRequestIntent(_context, false, filesToOverwrite);

	}

	@Override
	protected CopyFilesTaskParam getParam()
	{
		return (CopyFilesTaskParam)super.getParam();
	}
}