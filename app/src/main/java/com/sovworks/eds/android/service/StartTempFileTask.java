package com.sovworks.eds.android.service;

import android.content.Intent;

import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.locations.DeviceBasedLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.List;
import java.util.concurrent.CancellationException;

class StartTempFileTask extends PrepareTempFilesTask
{
	@Override
	public void onCompleted(Result result)
	{
		Location loc = LocationsManager.
				getLocationsManager(_context).
				getFromIntent(getParam().getIntent(), null);
		if(loc == null)
			loc = new DeviceBasedLocation(UserSettings.getSettings(_context));
		try
		{
			@SuppressWarnings("unchecked") List<File> tmpFilesList = (List<File>) result.getResult();
			for(File f: tmpFilesList)
			{
				loc.setCurrentPath(f.getPath());
				FileOpsService.startFileViewer(_context, loc);
			}
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
	protected FilesTaskParam initParam(Intent i)
	{
		return new FilesTaskParam(i, _context)
		{
			@Override
			public boolean forceOverwrite()
			{
				return true;
			}
		};
	}
}