package com.sovworks.eds.android.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.Settings;

public class LocationShortcutWidget extends AppWidgetProvider
{	
	
	public static void setWidgetLayout(Context context,AppWidgetManager appWidgetManager,int widgetId,Settings.LocationShortcutWidgetInfo prefs,boolean isContainerOpen)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.widget);		
		views.setTextViewText(R.id.widgetTitleTextView, prefs.widgetTitle);
		//TypedValue typedValue = new TypedValue();
		//context.getTheme().resolveAttribute(isContainerOpen ? R.attr.widgetUnlockedIcon : R.attr.widgetLockedIcon, typedValue, true);
		//views.setImageViewResource(R.id.widgetLockImageButton, typedValue.resourceId);
		views.setImageViewResource(R.id.widgetLockImageButton, isContainerOpen ? R.drawable.widget_unlocked : R.drawable.widget_locked);
		
		Intent intent = new Intent(context, FileManagerActivity.class);
		intent.setData(Uri.parse(prefs.locationUriString));				
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetLockImageButton, pendingIntent);
		appWidgetManager.updateAppWidget(widgetId, views);
	}

    @Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
	{
        setWidgetsState(context, appWidgetManager, appWidgetIds, null);
	}
	
	/*@Override
	public void onEnabled (Context context)	
	{
		context.startService(new Intent(context, OperationsService.class));		
	}
	
	@Override
	public void onDisabled (Context context)
	{
		context.stopService(new Intent(context, OperationsService.class));
	}*/
	
	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent)
	{
		super.onReceive(context, intent);
		if (LocationsManager.BROADCAST_LOCATION_CHANGED.equals(intent.getAction()))
            setWidgetsState(context, (Uri) intent.getParcelableExtra(LocationsManager.PARAM_LOCATION_URI));
	}

	private void setWidgetsState(Context context,Uri locationUri)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, LocationShortcutWidget.class));
		setWidgetsState(context, appWidgetManager, appWidgetIds, locationUri);
	}

	private void setWidgetsState(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,Uri locationUri)
	{
		LocationsManager lm = LocationsManager.getLocationsManager(context);
		if(lm == null)
			setWidgetsState(context, appWidgetManager, appWidgetIds, false);
		else
		try
		{
			UserSettings settings = UserSettings.getSettings(context);
			for(int widgetId: appWidgetIds)
			{
				Settings.LocationShortcutWidgetInfo widgetInfo = settings.getLocationShortcutWidgetInfo(widgetId);
				if(widgetInfo!=null)
				{
					Location widgetLoc = lm.findExistingLocation(Uri.parse(widgetInfo.locationUriString));
					if(widgetLoc!=null)
					{
                        if(locationUri!=null)
                        {
                            Location changedLoc = lm.getLocation(locationUri);
                            if (changedLoc != null && changedLoc.getId().equals(widgetLoc.getId()))
                                setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, widgetLoc);
                        }
                        else
                            setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, widgetLoc);
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

    public void setWidgetLayout(Context context,AppWidgetManager appWidgetManager,int widgetId,Settings.LocationShortcutWidgetInfo widgetInfo, Location loc)
    {
        setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, LocationsManager.isOpen(loc));
    }

	private void setWidgetsState(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean isOpen)
	{
		UserSettings settings = UserSettings.getSettings(context);
		for(int widgetId: appWidgetIds)
		{
			Settings.LocationShortcutWidgetInfo widgetInfo = settings.getLocationShortcutWidgetInfo(widgetId);
			if(widgetInfo!=null)
				setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, isOpen);
		}
	}
}
