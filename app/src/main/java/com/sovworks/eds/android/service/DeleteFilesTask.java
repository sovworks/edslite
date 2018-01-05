package com.sovworks.eds.android.service;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.FilesCountAndSize;
import com.sovworks.eds.fs.util.FilesOperationStatus;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;

import java.io.IOException;
import java.util.concurrent.CancellationException;

class DeleteFilesTask extends FileOperationTaskBase
{
    @Override
	public void onCompleted(Result result)
	{
        try
		{
            result.getResult();
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
	protected FilesOperationStatus initStatus(SrcDstCollection records)
	{
		FilesOperationStatus status = new FilesOperationStatus();
		status.total = FilesCountAndSize.getFilesCount(records);
		return status;
	}

	@Override
	protected boolean processRecord(SrcDst record) throws Exception
	{		
		Path srcPath = record.getSrcLocation().getCurrentPath();
		try
		{
			if (srcPath.isFile())
			{
				deleteFile(srcPath.getFile());
				ExtendedFileInfoLoader.getInstance().discardCache(record.getSrcLocation(), srcPath);
			}
			else if (srcPath.isDirectory())
				deleteDir(srcPath.getDirectory());
		}
		catch (final IOException e)
		{
			setError(new IOException(String.format("Unable to delete record: %s", srcPath.getPathDesc()), e));
		}
		if (_currentStatus.processed.filesCount < _currentStatus.total.filesCount - 1) _currentStatus.processed.filesCount++;
		updateUIOnTime();
		return true;
	}

	protected void deleteFile(File file) throws IOException
	{
		_currentStatus.fileName = file.getName();
		updateUIOnTime();
		file.delete();
	}

	private void deleteDir(Directory dir) throws IOException
	{
		_currentStatus.fileName = dir.getName();
		updateUIOnTime();
		dir.delete();
	}

	@Override
	protected String getErrorMessage(Throwable ex)
	{
		return _context.getString(R.string.delete_failed);
	}
	
	@Override
	protected int getNotificationMainTextId()
	{
		return R.string.deleting_files;
	}
}