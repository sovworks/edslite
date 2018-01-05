package com.sovworks.eds.android.service;


import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.sovworks.eds.android.R;
import com.sovworks.eds.fs.util.FilesOperationStatus;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;

import java.util.concurrent.CancellationException;

public abstract class FileOperationTaskBase extends	ServiceTaskWithNotificationBase
{
	public static class FileOperationParam
	{
		FileOperationParam(Intent i)
		{
			_intent = i;			
		}
		
		SrcDstCollection getRecords()
		{
			if(_records == null)
				_records = loadRecords(_intent);
			return _records;
		}
		
		protected SrcDstCollection loadRecords(Intent i)
		{
			return i.getParcelableExtra(FileOpsService.ARG_RECORDS);
		}

		protected Intent getIntent()
		{
			return _intent;
		}
		
		private SrcDstCollection _records;
		private final Intent _intent;
	}
	
	@Override	
	public Object doWork(Context context, Intent i) throws Throwable
	{
		super.doWork(context, i);
		_param = initParam(i);
        updateUIOnTime();
		_currentStatus = initStatus(_param.getRecords());
		processSrcDstCollection(_param.getRecords());
		if(_error!=null)
			throw _error;
		return null;
	}
	
	@Override
	public void onCompleted(Result result)
	{
        super.onCompleted(result);
        broadcastCompleted();
	}
	
	FilesOperationStatus _currentStatus;
	
	abstract protected FilesOperationStatus initStatus(SrcDstCollection records);
	abstract protected boolean processRecord(SrcDst record) throws Exception;

    protected FileOperationParam initParam(Intent i)
	{
		return new FileOperationParam(i);
	}
	
	@Override	
	protected void updateUI()
	{
        if(_currentStatus!=null)
		    updateNotificationProgress();
        super.updateUI();
	}

	protected int getNotificationMainTextId()
	{
		return R.string.copying_files;
	}
	
	private void updateNotificationProgress()
	{
        _notificationBuilder.setProgress(100, getProgress(), false);
        _notificationBuilder.setContentText(getNotificationText());
	}

    protected String getNotificationText()
    {
        String fn = _currentStatus.fileName;
        return _context.getString(R.string.processing_file, fn == null ? "" : fn,
                    _currentStatus.processed.filesCount + 1,
                    _currentStatus.total.filesCount
        );
    }

    protected int getProgress()
    {
		if (_currentStatus.total.totalSize == 0)
		{
			if (_currentStatus.total.filesCount != 0) return (int) (( _currentStatus.processed.filesCount / (float) _currentStatus.total.filesCount) * 100);
		}
		else
			return (int) ((_currentStatus.processed.totalSize / (float) _currentStatus.total.totalSize) * 100);
        return 0;
    }
	
	@Override
    protected NotificationCompat.Builder initNotification()
	{
        return super.initNotification().setContentText(_context.getText(getNotificationMainTextId())).setProgress(100, 0, false);
	}
	
	protected void processSrcDstCollection(SrcDstCollection col) throws Exception
	{
		for (SrcDst rec : col)
		{
			if (isCancelled()) throw new CancellationException();
			if(!processRecord(rec))
				break;
		}
	}

	void setError(Throwable err)
	{
		if(_error == null || err == null)
			_error = err;
	}
	
	void incProcessedSize(int inc)
	{
		//int prevPrc = (int) ((_currentStatus.processed.totalSize / (float) _currentStatus.total.totalSize) * 100);
		_currentStatus.processed.totalSize += inc;
		//int newPrc = (int) ((_currentStatus.processed.totalSize / (float) _currentStatus.total.totalSize) * 100);
		//if (prevPrc != newPrc)
		updateUIOnTime();
			
	}
	
	private void broadcastCompleted()
	{
		_context.sendBroadcast(new Intent(FileOpsService.BROADCAST_FILE_OPERATION_COMPLETED));
	}	
	
	protected FileOperationParam getParam()
	{
		return _param;
	}
	
	private FileOperationParam _param;
    private Throwable _error;


}
