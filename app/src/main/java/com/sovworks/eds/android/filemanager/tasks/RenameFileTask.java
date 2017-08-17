package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

public class RenameFileTask extends TaskFragment
{
	public static final String TAG = "RenameFileTask";

	public static final String ARG_NEW_NAME = "com.sovworks.eds.android.NEW_NAME";

	public static RenameFileTask newInstance(Location target, String newName)
	{
		Bundle args = new Bundle();
		args.putString(ARG_NEW_NAME, newName);
		LocationsManager.storePathsInBundle(args, target, null);
		RenameFileTask f = new RenameFileTask();
		f.setArguments(args);
		return f;			
	}
	
	@Override
	public void initTask(Activity activity)
	{
		_context = activity.getApplicationContext();
	}
	
	@Override
	protected void doWork(TaskState state) throws Exception
	{
		String newName = getArguments().getString(ARG_NEW_NAME);
		Location target = LocationsManager.getLocationsManager(_context).getFromBundle(getArguments(), null);
		Path path = target.getCurrentPath();
		if(path.isFile())
			path.getFile().rename(newName);
		else if(path.isDirectory())
			path.getDirectory().rename(newName);
	}
	
	@Override
	protected TaskCallbacks getTaskCallbacks(final Activity activity)
	{
		return new TaskCallbacks()
        {
            @Override
            public void onUpdateUI(Object state)
            {
            }
            @Override
            public void onPrepare(Bundle args)
            {
            }

            @Override
            public void onResumeUI(Bundle args)
            {
            }

            @Override
            public void onSuspendUI(Bundle args)
            {
            }

            @Override
            public void onCompleted(Bundle args, Result result)
            {
                try
                {
                    result.getResult();
					activity.sendBroadcast(new Intent(FileOpsService.BROADCAST_FILE_OPERATION_COMPLETED));
                }
                catch(Throwable e)
                {
                    Logger.showAndLog(activity, e);
                }
            }
        };
	}
	
	private Context _context;
}