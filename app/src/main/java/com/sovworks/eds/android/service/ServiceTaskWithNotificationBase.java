package com.sovworks.eds.android.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.activities.CancelTaskActivity;

public abstract class ServiceTaskWithNotificationBase implements Task
{
	@Override
	public Object doWork(Context context, Intent i) throws Throwable
	{
        initTask(context, i);
		return null;
	}

	@Override
	public void onCompleted(Result result)
	{
		removeNotification();
	}
	
	@Override
	public void cancel()
	{
		_isCancelled = true;
	}
	
	public boolean isCancelled()
	{
		return _isCancelled;
	}

	protected boolean _isCancelled;
	protected Context _context;
    protected NotificationCompat.Builder _notificationBuilder;
	protected long _prevUpdateTime;
	protected int _taskId;

    protected void initTask(Context context, Intent i) throws Exception
    {
        _context = context;
        _taskId = FileOpsService.getNewNotificationId();
        _notificationBuilder = initNotification();
    }

	protected void updateUI()
	{
		updateNotification();
	}

	protected String getErrorMessage(Throwable ex)
	{
		return Logger.getExceptionMessage(_context, ex);
	}

	protected String getErrorDetails(Throwable ex)
	{
		String msg = ex.getLocalizedMessage();
		if(msg == null)
		{
			msg = ex.getMessage();
			if(msg == null)
				msg = "";
		}
		return msg;
	}

	protected void reportError(Throwable err)
	{
		Logger.log(err);
		showNotificationMessage(getErrorMessage(err), getErrorDetails(err));
	}
	
	protected void showNotificationMessage(String title, String message)
	{
		if(title == null)
			return;
        NotificationCompat.Builder nb = new NotificationCompat.Builder(_context)
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notification_new : R.drawable.ic_notification)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message);
        //Gingerbread compatibility
        final Intent emptyIntent = new Intent();
        PendingIntent pi = PendingIntent.getActivity(_context, 0,emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(pi);

        NotificationManager nm = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(FileOpsService.getNewNotificationId(), nb.build());
	}
	
	protected ServiceTaskWithNotificationBase()
	{
	}	

    protected NotificationCompat.Builder initNotification()
    {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(_context)
				.setContentTitle(_context.getString(R.string.eds))
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notification_new : R.drawable.ic_notification)
                .setOngoing(true)
                .setAutoCancel(false)
				.addAction(
						R.drawable.ic_action_cancel,
						_context.getString(android.R.string.cancel),
						FileOpsService.getCancelTaskActionPendingIntent(_context, _taskId)
				);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			nb.setContentIntent(
					PendingIntent.getActivity(
							_context,
							_taskId,
							CancelTaskActivity.getCancelTaskIntent(_context, _taskId),
							PendingIntent.FLAG_UPDATE_CURRENT
					)
			);
        return nb;
    }

    protected void removeNotification()
    {
        NotificationManager nm = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(_taskId);
    }

    protected void updateNotification()
    {
        NotificationManager nm = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(_taskId, _notificationBuilder.build());
    }

	protected void updateUIOnTime()
	{
		long time = SystemClock.uptimeMillis();
		if(time-_prevUpdateTime>1000)
		{
			updateUI();			
			_prevUpdateTime = time;
		}
	}
}
