package com.sovworks.eds.android.locations.tasks;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.errors.InputOutputException;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.errors.WrongPasswordOrBadContainerException;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.container.ContainerFormatterBase;
import com.sovworks.eds.container.EDSLocationFormatter;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.fs.errors.WrongImageFormatException;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.io.IOException;

public abstract class CreateEDSLocationTaskFragmentBase extends
        com.sovworks.eds.android.fragments.TaskFragment
{
    public static final String TAG = "com.sovworks.eds.android.locations.tasks.CreateEDSLocationTaskFragment";

    public static final String ARG_LOCATION = "com.sovworks.eds.android.LOCATION";
    public static final String ARG_CIPHER_NAME = "com.sovworks.eds.android.CIPHER_NAME";
    public static final String ARG_OVERWRITE = "com.sovworks.eds.android.OVERWRITE";

    public static final int RESULT_REQUEST_OVERWRITE = 1;

    @Override
    public void initTask(Activity activity)
    {
        _context = activity.getApplicationContext();
        _locationsManager = LocationsManager.getLocationsManager(_context);
    }

    protected Context _context;
    protected LocationsManager _locationsManager;

    @Override
    protected TaskCallbacks getTaskCallbacks(Activity activity)
    {
        CreateEDSLocationFragment f = (CreateEDSLocationFragment) getFragmentManager().findFragmentByTag(SettingsBaseActivity.SETTINGS_FRAGMENT_TAG);
        return f == null ? null : f.getCreateLocationTaskCallbacks();
    }

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        state.setResult(0);
        Location location = _locationsManager
                .getLocation(
                        (Uri) getArguments().getParcelable(ARG_LOCATION));

        if(!checkParams(state, location))
            return;
        PowerManager pm = (PowerManager) _context
                .getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                toString());
        wl.acquire();
        try
        {
            createEDSLocation(state, location);
        }
        finally
        {
            wl.release();
        }
    }

    protected void createEDSLocation(TaskState state, Location locationLocation) throws Exception
    {
        EDSLocationFormatter f = createFormatter();
        SecureBuffer password = getArguments().getParcelable(Openable.PARAM_PASSWORD);
        try
        {
            initFormatter(state, f, password);
            f.format(locationLocation);
        }
        catch (WrongImageFormatException e)
        {
            WrongPasswordOrBadContainerException e1 = new WrongPasswordOrBadContainerException(
                    _context);
            e1.initCause(e);
            throw e1;
        }
        catch (IOException e)
        {
            throw new InputOutputException(_context, e);
        }
        catch (Exception e)
        {
            throw new UserException(_context,
                    R.string.err_failed_creating_container, e);
        }
    }

    protected abstract EDSLocationFormatter createFormatter();

    protected void initFormatter(final TaskState state, final EDSLocationFormatter formatter, SecureBuffer password) throws Exception
    {
        formatter.setContext(_context);
        formatter.setPassword(password);
        formatter.setProgressReporter(new ContainerFormatterBase.ProgressReporter()
        {
            @Override
            public boolean report(byte prc)
            {
                state.updateUI(prc);
                return !state.isTaskCancelled();
            }
        });

    }

    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        if (!getArguments().getBoolean(ARG_OVERWRITE, false))
        {
            if (locationLocation.getCurrentPath().exists())
            {
                state.setResult(RESULT_REQUEST_OVERWRITE);
                return false;
            }
        }
        return true;
    }
}
