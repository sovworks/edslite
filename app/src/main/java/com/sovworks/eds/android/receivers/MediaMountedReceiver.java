package com.sovworks.eds.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.locations.ExternalStorageLocation;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.LocationsManagerBase;

public class MediaMountedReceiver extends BroadcastReceiver
{
	public MediaMountedReceiver(LocationsManagerBase lm)
	{
		_lm = lm;
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Logger.debug("MediaMountedReceiver");
		LocationsManagerBase lm = _lm;
		if(lm == null)
			return;
		String mountPath = intent.getDataString();
        ExternalStorageLocation loc = mountPath != null ? new ExternalStorageLocation(context, "ext storage", mountPath, null) : null;
		try
		{
			Thread.sleep(3000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		switch (intent.getAction())
		{
			case UsbManager.ACTION_USB_DEVICE_ATTACHED:
				Logger.debug("ACTION_USB_DEVICE_ATTACHED");
				lm.updateDeviceLocations();
				LocationsManager.broadcastLocationAdded(context, loc);
				break;
			case Intent.ACTION_MEDIA_MOUNTED:
				Logger.debug("ACTION_MEDIA_MOUNTED");
				lm.updateDeviceLocations();
				LocationsManager.broadcastLocationAdded(context, loc);
				break;
			case Intent.ACTION_MEDIA_UNMOUNTED:
				Logger.debug("ACTION_MEDIA_UNMOUNTED");
				lm.updateDeviceLocations();
				LocationsManager.broadcastLocationRemoved(context, loc);
				break;
			case Intent.ACTION_MEDIA_REMOVED:
				Logger.debug("ACTION_MEDIA_REMOVED");
				lm.updateDeviceLocations();
				LocationsManager.broadcastLocationRemoved(context, loc);
				break;
			case UsbManager.ACTION_USB_DEVICE_DETACHED:
				Logger.debug("ACTION_USB_DEVICE_DETACHED");
				lm.updateDeviceLocations();
				LocationsManager.broadcastLocationRemoved(context, loc);
				break;
		}
	}

	private final LocationsManagerBase _lm;
}
