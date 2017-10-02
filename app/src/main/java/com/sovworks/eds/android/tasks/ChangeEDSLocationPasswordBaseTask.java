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
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.locations.EDSLocation;

import java.io.IOException;

public abstract class ChangeEDSLocationPasswordBaseTask extends TaskFragment
{
    @Override
    public void initTask(Activity activity)
    {
        _context = activity.getApplicationContext();
        _location = (EDSLocation) LocationsManager.getLocationsManager(_context).getFromBundle(getArguments(), null);
    }

    protected EDSLocation _location;
    protected Context _context;

    @Override
    protected void doWork(TaskState state) throws Exception
    {
		changeLocationPassword();
	}

	@Override
    protected TaskCallbacks getTaskCallbacks(Activity activity)
    {
        final EDSLocationSettingsFragment f = (EDSLocationSettingsFragment) getFragmentManager().findFragmentByTag(SettingsBaseActivity.SETTINGS_FRAGMENT_TAG);
        if(f == null)
            return null;
        return new ProgressDialogTaskFragmentCallbacks(activity, R.string.changing_password)
        {
            @Override
            public void onCompleted(Bundle args, Result result)
            {
                super.onCompleted(args, result);
                try
                {
                    result.getResult();
                    f.getPropertiesView().loadProperties();
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(_context, result.getError());
                }
            }
        };
    }

	protected abstract void changeLocationPassword() throws IOException, ApplicationException;
}
