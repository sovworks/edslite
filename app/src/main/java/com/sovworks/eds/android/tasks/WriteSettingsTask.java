package com.sovworks.eds.android.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragment;
import com.sovworks.eds.locations.EDSLocation;

public class WriteSettingsTask extends TaskFragment
{
    public static final String TAG = "com.sovworks.eds.android.tasks.WriteSettingsTask";
    public static final String ARG_FIN_ACTIVITY = "com.sovworks.eds.android.FIN_ACTIVITY";

	public static WriteSettingsTask newInstance(EDSLocation cont, boolean finActivity)
    {
        Bundle args = new Bundle();
        args.putBoolean(ARG_FIN_ACTIVITY, finActivity);
        LocationsManager.storePathsInBundle(args, cont, null);
		WriteSettingsTask f = new WriteSettingsTask();
        f.setArguments(args);
        return f;
	}

    @Override
    protected void initTask(Activity activity)
    {
        super.initTask(activity);
        _context = activity.getApplicationContext();
    }

    private Context _context;

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        EDSLocation cont = (EDSLocation) LocationsManager.getLocationsManager(_context).getFromBundle(getArguments(), null);
		cont.applyInternalSettings();
        cont.writeInternalSettings();
	}

    @Override
    protected TaskCallbacks getTaskCallbacks(Activity activity)
    {
        EDSLocationSettingsFragment f = (EDSLocationSettingsFragment)
                getFragmentManager().findFragmentByTag(SettingsBaseActivity.SETTINGS_FRAGMENT_TAG);
        if(f == null)
            return null;
        return new ProgressDialogTaskFragmentCallbacks(activity, R.string.saving_changes)
        {
            @Override
            public void onCompleted(Bundle args, TaskFragment.Result result)
            {
                super.onCompleted(args, result);
                try
                {
                    result.getResult();
                    if(args.getBoolean(WriteSettingsTask.ARG_FIN_ACTIVITY,false))
                        getActivity().finish();
                }
                catch(Throwable e)
                {
                    Logger.showAndLog(_context, result.getError());
                }
            }
        };
    }
}
