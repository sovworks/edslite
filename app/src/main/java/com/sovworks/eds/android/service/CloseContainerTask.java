package com.sovworks.eds.android.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.sovworks.eds.android.R;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.locations.closer.fragments.OMLocationCloserFragment;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.EDSLocation;

public class CloseContainerTask extends ServiceTaskWithNotificationBase
{

	@Override
	public Object doWork(Context context, Intent i) throws Throwable
	{
		super.doWork(context, i);
		EDSLocation cont = (EDSLocation) LocationsManager.getLocationsManager(context).getFromIntent(i, null);
		if(cont!=null)
			OMLocationCloserFragment.unmountAndClose(context, cont, UserSettings.getSettings(context).alwaysForceClose());
		return null;
	}

    @Override
    protected NotificationCompat.Builder initNotification()
    {
        return super.initNotification().setContentTitle(_context.getString(R.string.closing));
    }

}
