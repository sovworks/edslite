package com.sovworks.eds.android.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.android.locations.activities.CloseLocationsActivity;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;

public class LocationsServiceBase extends Service
{
	public static final int NOTIFICATION_RUNNING_SERVICE = 1;

	public static void startService(Context context)
	{
		context.startService(new Intent(context, LocationsService.class));
	}

	public static void stopService(Context context)
	{
		context.stopService(new Intent(context, LocationsService.class));
	}

	public static final String ACTION_CHECK_INACTIVE_LOCATION = "com.sovworks.eds.android.CHECK_INACTIVE_LOCATION";

	public static class InactivityCheckReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				LocationsManager lm = LocationsManager.getLocationsManager(context, false);
				if(lm == null)
					return;
				Uri uri = intent.getParcelableExtra(LocationsManager.PARAM_LOCATION_URI);
				if(uri!=null)
				{
					EDSLocation loc = (EDSLocation) lm.getLocation(uri);
					closeIfInactive(context, loc);
				}

			}
			catch (Throwable e)
			{
				Logger.log(e);
			}
		}

		private void closeIfInactive(Context context, EDSLocation loc)
		{
			int tm = loc.getExternalSettings().getAutoCloseTimeout();
			Logger.debug("Checking if " + loc.getTitle() + " container is inactive");
			if(tm <= 0)
				return;
			long ct = SystemClock.elapsedRealtime();
			Logger.debug("Current time = " + ct);
			if(loc.isOpenOrMounted())
			{
				long lastActivityTime = loc.getLastActivityTime();
				Logger.debug("Container " + loc.getTitle() + " is open. Last activity time is " + lastActivityTime);
				if(ct - lastActivityTime > tm)
				{
					Logger.debug("Starting close container task for " + loc.getTitle() + " after inactivity timeout.");
					FileOpsService.closeContainer(context, loc);
					return;
				}
			}
			registerInactiveContainerCheck(context, loc);

		}
	}

	public static void registerInactiveContainerCheck(Context context, EDSLocation loc)
	{

        long triggerTime = loc.getExternalSettings().getAutoCloseTimeout();
        if(triggerTime == 0)
            return;
        triggerTime += SystemClock.elapsedRealtime();
        Intent i = new Intent(ACTION_CHECK_INACTIVE_LOCATION);
        i.putExtra(LocationsManager.PARAM_LOCATION_URI, loc.getLocationUri());
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                loc.getId().hashCode(),
                i,
                PendingIntent.FLAG_ONE_SHOT
        );
		LocationsService.setCheckTimer(context, pi, triggerTime);
	}

	protected static void setCheckTimer(Context context, PendingIntent pi, long triggerTime)
	{
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(
					AlarmManager.ELAPSED_REALTIME_WAKEUP,
					triggerTime,
					pi
			);
	}

	@Override
    public void onCreate()
	{
		super.onCreate();
        try
        {
            _locationsManager = LocationsManager.getLocationsManager(this, true);
            _settings = UserSettings.getSettings(this);
            _shutdownReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    Logger.debug("Device shutdown. Closing locations");
                    _locationsManager.closeAllLocations(true, false);
                }
            };
            registerReceiver(_shutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
			registerReceiver(_shutdownReceiver, new IntentFilter("android.intent.action.QUICKBOOT_POWEROFF"));
			_inactivityCheckReceiver = new InactivityCheckReceiver();
			registerReceiver(_inactivityCheckReceiver, new IntentFilter(ACTION_CHECK_INACTIVE_LOCATION));
        }
        catch (Exception e)
        {
            Logger.showAndLog(this, e);
        }
    }


	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{		
		super.onStartCommand(intent, flags, startId);
        if(hasOpenLocations())
        {
            startForeground(
                    NOTIFICATION_RUNNING_SERVICE,
                    getServiceRunningNotification());
            TempFilesMonitor.getMonitor(this).startChangesMonitor();
            return Service.START_STICKY;
        }
        stopSelf();
        return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		Logger.debug("LocationsService onDestroy");
		stopForeground(true);
		if(_shutdownReceiver!=null)
		{
			unregisterReceiver(_shutdownReceiver);
			_shutdownReceiver = null;
		}
		if(_inactivityCheckReceiver!=null)
		{
			unregisterReceiver(_inactivityCheckReceiver);
			_inactivityCheckReceiver = null;
		}
		TempFilesMonitor.getMonitor(this).stopChangesMonitor();
		_locationsManager.closeAllLocations(true, true);
		deleteMirror();
		_settings = null;
		_locationsManager = null;
		super.onDestroy();
	}

	protected LocationsManager _locationsManager;
    protected Settings _settings;
	protected BroadcastReceiver _shutdownReceiver, _inactivityCheckReceiver;

	private void deleteMirror()
	{
		try
		{
			Location l = FileOpsService.getSecTempFolderLocation(_settings.getWorkDir(),this);
			if(l!=null)		
				Util.deleteFiles(l.getCurrentPath());
		}
		catch (IOException e)
		{
			Logger.showAndLog(this, e);
		}
	}

	protected boolean hasOpenLocations()
	{
		LocationsManager lm = LocationsManager.getLocationsManager(this);
		return lm.hasOpenLocations();
	}
	
	private Notification getServiceRunningNotification()
	{
        Intent i = new Intent(this, FileManagerActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CompatHelper.getServiceRunningNotificationsChannelId(this))
                .setContentTitle(getString(R.string.eds_service_is_running))
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notification_new : R.drawable.ic_notification)
                .setContentText("")
                .setContentIntent(PendingIntent.getActivity(this, 0, i, 0))
                .setOngoing(true)
				.addAction(
						R.drawable.ic_action_cancel,
						getString(R.string.close_all_containers),
						PendingIntent.getActivity(
								this,
								0,
								new Intent(this, CloseLocationsActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT
						)
				);
        Notification n = builder.build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
		return n;
	}
}
