package com.sovworks.eds.android.service;

import android.content.Context;
import android.content.Intent;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.util.FilesCountAndSize;
import com.sovworks.eds.fs.util.FilesOperationStatus;
import com.sovworks.eds.fs.util.SrcDstCollection;

import java.io.IOException;
import java.util.concurrent.CancellationException;

class WipeFilesTask extends DeleteFilesTask
{	
	WipeFilesTask(boolean wipe)
	{
		_wipe = wipe;
	}
	
	private final boolean _wipe;
	private TempFilesMonitor _mon;
	
	@Override 
	public Object doWork(Context context, Intent i) throws Throwable
	{		
		_mon = TempFilesMonitor.getMonitor(context);
		return super.doWork(context, i);
	}

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
	protected int getNotificationMainTextId()
	{
		return R.string.wiping_files;
	}	
	
	@Override
	protected FilesOperationStatus initStatus(SrcDstCollection records)
	{
		FilesOperationStatus status = new FilesOperationStatus();
		status.total = _wipe ? FilesCountAndSize.getFilesCountAndSize(false,records) : FilesCountAndSize.getFilesCount(records);
		return status;	
	}

	@Override
	protected void deleteFile(File file) throws IOException
	{
		synchronized (_mon.getSyncObject())
		{
			com.sovworks.eds.android.helpers.WipeFilesTask.wipeFile(
					file,
					_wipe,
					new com.sovworks.eds.android.helpers.WipeFilesTask.ITask()
					{
						@Override
						public void progress(int sizeInc)
						{
							incProcessedSize(sizeInc);

						}

						@Override
						public boolean cancel()
						{
							return isCancelled();
						}
					}
			);
		}
	}
}