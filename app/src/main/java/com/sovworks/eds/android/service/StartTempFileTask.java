package com.sovworks.eds.android.service;

import android.content.Intent;

import com.sovworks.eds.locations.Location;

import java.util.List;
import java.util.concurrent.CancellationException;

class StartTempFileTask extends PrepareTempFilesTask
{
	@Override
	public void onCompleted(Result result)
	{
		try
		{
			@SuppressWarnings("unchecked") List<Location> tmpFilesList = (List<Location>) result.getResult();
			for(Location f: tmpFilesList)
				FileOpsService.startFileViewer(_context, f);

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