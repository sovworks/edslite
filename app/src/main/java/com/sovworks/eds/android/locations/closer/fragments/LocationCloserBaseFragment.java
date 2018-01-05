package com.sovworks.eds.android.locations.closer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.locations.dialogs.ForceCloseDialog;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.concurrent.CancellationException;

public class LocationCloserBaseFragment extends Fragment
{
    public static final String ARG_FORCE_CLOSE = "com.sovworks.eds.android.FORCE_CLOSE";

    public static String getCloserTag(Location location)
    {
        return TAG + location.getId();
    }

    public static String getCloseLocationTaskTag(Location location)
    {
        return CloseLocationTaskFragment.TAG + location.getId();
    }

    public interface CloseLocationReceiver
    {
        void onTargetLocationClosed(Location location, Bundle closeTaskArgs);
        void onTargetLocationNotClosed(Location location, Bundle closeTaskArgs);
    }

    public static LocationCloserBaseFragment getDefaultCloserForLocation(Location location)
    {
        return ClosersRegistry.getDefaultCloserForLocation(location);
    }

    public static final String PARAM_RECEIVER_FRAGMENT_TAG = "com.sovworks.eds.android.locations.closer.fragments.LocationCloserBaseFragment.RECEIVER_FRAGMENT_TAG";

    public static class CloseLocationTaskFragment extends TaskFragment
    {
        public static final String ARG_CLOSER_TAG = "com.sovworks.eds.android.CLOSER_TAG";

        protected Context _context;
        protected LocationsManager _locationsManager;

        @Override
        protected void initTask(Activity activity)
        {
            _context = activity.getApplicationContext();
            _locationsManager = LocationsManager.getLocationsManager(_context);
        }

        @Override
        protected void doWork(TaskState taskState) throws Exception
        {
            PowerManager pm = (PowerManager)_context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm == null ? null : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, toString());
            if(wl!=null)
                wl.acquire(30000);
            try
            {
                Uri locationUri = getArguments().getParcelable(LocationsManager.PARAM_LOCATION_URI);
                Location location = _locationsManager.getLocation(locationUri);
                procLocation(taskState, location);
                taskState.setResult(location);
            }
            finally
            {
                if(wl!=null)
                    wl.release();
            }
        }

        @Override
        protected TaskCallbacks getTaskCallbacks(Activity activity)
        {
            LocationCloserBaseFragment f = (LocationCloserBaseFragment) getFragmentManager().findFragmentByTag(getArguments().getString(ARG_CLOSER_TAG));
            return f == null ? null : f.getCloseLocationTaskCallbacks();
        }

        protected void procLocation(TaskState state, Location location) throws Exception
        {
            LocationsManager.broadcastLocationChanged(_context, location);
        }

        @Override
        protected void detachTask()
        {
            FragmentManager fm = getFragmentManager();
            if(fm!=null)
            {
                FragmentTransaction trans = fm.beginTransaction();
                trans.remove(this);
                LocationCloserBaseFragment f = (LocationCloserBaseFragment) fm.findFragmentByTag(getArguments().getString(ARG_CLOSER_TAG));
                if(f!=null)
                    trans.remove(f);
                trans.commitAllowingStateLoss();
                Logger.debug(String.format("TaskFragment %s has been removed from the fragment manager", this));
                onEvent(EventType.Removed, this);
            }
        }

        private static final String TAG = "com.sovworks.eds.android.locations.closer.fragments.LocationCloserBaseFragment.CloseLocationTaskFragment";
    }

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        if(getFragmentManager().findFragmentByTag(getCloseLocationTaskTag()) == null)
            closeLocation();
    }

    @Override
    public void onPause()
    {
        _resHandler.onPause();
        super.onPause();
    }

    @Override
    public void onResume()
    {
    	super.onResume();
        _resHandler.handle();
    }


    public TaskFragment.TaskCallbacks getCloseLocationTaskCallbacks()
	{
		return new CloseLocationTaskCallbacks();
	}

	protected class CloseLocationTaskCallbacks implements TaskFragment.TaskCallbacks
	{
		@Override
		public void onPrepare(Bundle args)
		{

		}

		@Override
		public void onResumeUI(Bundle args)
		{
            Activity activity = getActivity();
            _dialog = new ProgressDialog(activity);
            _dialog.setMessage (activity.getText(R.string.closing));
            _dialog.setIndeterminate(true);
            _dialog.setCancelable(false);
            _dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    CloseLocationTaskFragment f = (CloseLocationTaskFragment) getFragmentManager().findFragmentByTag(CloseLocationTaskFragment.TAG);
                    if(f!=null)
                        f.cancel();
                }
            });
            _dialog.show();
		}

		@Override
		public void onSuspendUI(Bundle args)
		{
			_dialog.dismiss();

		}

		@Override
		public void onCompleted(Bundle args,TaskFragment.Result result)
		{
			procCloseLocationTaskResult(args, result);
		}

		@Override
		public void onUpdateUI(Object state)
		{

		}

		private ProgressDialog _dialog;
	}

	protected final ActivityResultHandler _resHandler = new ActivityResultHandler();

	protected TaskFragment getCloseLocationTask()
	{
		return new CloseLocationTaskFragment();
	}

    protected String getCloseLocationTaskTag()
    {
        return getCloseLocationTaskTag(getTargetLocation());
    }

    protected void closeLocation()
    {
        startClosingTask(initCloseLocationTaskParams(getTargetLocation()));
    }

    protected Bundle initCloseLocationTaskParams(Location location)
	{
        Bundle b = new Bundle();
        b.putString(CloseLocationTaskFragment.ARG_CLOSER_TAG, getTag());
        if(getArguments().containsKey(ARG_FORCE_CLOSE))
            b.putBoolean(ARG_FORCE_CLOSE, getArguments().getBoolean(ARG_FORCE_CLOSE, false));
        LocationsManager.storePathsInBundle(b, location, null);
		return b;
	}

    protected Location getTargetLocation()
    {
        return LocationsManager.getFromBundle(
                getArguments(),
                LocationsManager.getLocationsManager(getActivity()),
                null);
    }

    protected void finishCloser(boolean closed, Location location, Bundle closeTaskArgs)
    {
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        if (closed)
            onLocationClosed(location, closeTaskArgs);
        else
            onLocationNotClosed(location, closeTaskArgs);
    }

	protected void onLocationClosed(Location location, Bundle closeTaskArgs)
	{
        String recTag = getArguments()!=null ? getArguments().getString(PARAM_RECEIVER_FRAGMENT_TAG) : null;
        if(recTag != null)
        {
            CloseLocationReceiver rec = (CloseLocationReceiver) getFragmentManager().findFragmentByTag(recTag);
            if(rec!=null)
                rec.onTargetLocationClosed(location, closeTaskArgs);
        }
	}

    protected void onLocationNotClosed(Location location, Bundle closeTaskArgs)
    {
        String recTag = getArguments()!=null ? getArguments().getString(PARAM_RECEIVER_FRAGMENT_TAG) : null;
        if(recTag != null)
        {
            CloseLocationReceiver rec = (CloseLocationReceiver) getFragmentManager().findFragmentByTag(recTag);
            if(rec!=null)
                rec.onTargetLocationNotClosed(location, closeTaskArgs);
        }
    }

    protected void procCloseLocationTaskResult(Bundle args, TaskFragment.Result result)
    {
        try
        {
            finishCloser(true, (Location) result.getResult(), args);
        }
        catch(CancellationException ignored)
        {

        }
        catch(Throwable e)
        {
            if(!args.getBoolean(ARG_FORCE_CLOSE,false))
            {
                Logger.log(e);
                ForceCloseDialog.showDialog(
                        getFragmentManager(),
                        getTag(),
                        getTargetLocation().getTitle(),
                        getClass().getName(),
                        getArguments()
                );
                finishCloser(false, getTargetLocation(), args);
            }
            else
            {
                Logger.showAndLog(getActivity(), e);
                finishCloser(false, getTargetLocation(), args);
            }
        }

    }

	protected void startClosingTask(Bundle args)
	{
        TaskFragment f = getCloseLocationTask();
        f.setArguments(args);
        getFragmentManager().beginTransaction().add(f, getCloseLocationTaskTag()).commit();
	}

    private static final String TAG = "com.sovworks.eds.android.locations.closer.fragments.LocationCloserBaseFragment";
}
