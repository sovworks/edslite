package com.sovworks.eds.android.locations.opener.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Html;
import android.widget.Toast;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.ProgressDialog;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.android.locations.OpenersRegistry;
import com.sovworks.eds.android.service.LocationsService;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.io.IOException;
import java.util.concurrent.CancellationException;

public class LocationOpenerBaseFragment extends Fragment
{
    public static String getOpenerTag(Location location)
    {
        return TAG + location.getId();
    }

    public static String getOpenLocationTaskTag(Location location)
    {
        return OpenLocationTaskFragment.TAG + location.getId();
    }

    public interface LocationOpenerResultReceiver
    {
        void onTargetLocationOpened(Bundle openerArgs, Location location);
        void onTargetLocationNotOpened(Bundle openerArgs);
    }

    public static LocationOpenerBaseFragment getDefaultOpenerForLocation(Location location)
    {
        return OpenersRegistry.getDefaultOpenerForLocation(location);
    }

    public static final String PARAM_RECEIVER_FRAGMENT_TAG = "com.sovworks.eds.android.locations.opener.fragments.LocationOpenerFragment.RECEIVER_FRAGMENT_TAG";

    public static class OpenLocationTaskFragment extends TaskFragment
    {
        public static final String ARG_OPENER_TAG = "com.sovworks.eds.android.OPENER_TAG";

        protected Context _context;
        protected LocationsManager _locationsManager;
        protected ProgressReporter _openingProgressReporter;

        @Override
        protected void initTask(Activity activity)
        {
            _context = activity.getApplicationContext();
            _locationsManager = LocationsManager.getLocationsManager(activity);
            _openingProgressReporter = new ProgressReporter(_context);
        }

        @Override
        protected void doWork(TaskState taskState) throws Exception
        {
            PowerManager pm = (PowerManager)_context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, toString());
            wl.acquire();
            try
            {
                if(_openingProgressReporter!=null)
                    _openingProgressReporter.setTaskState(taskState);
                Location location = getTargetLocation();
                procLocation(taskState, location, getArguments());
                LocationsService.startService(_context);
                if(location.isFileSystemOpen() && !location.getCurrentPath().exists())
                    location.setCurrentPath(location.getFS().getRootPath());
                taskState.setResult(location);
            }
            finally
            {
                wl.release();
            }
        }

        protected Location getTargetLocation() throws Exception
        {
            Uri locationUri = getArguments().getParcelable(LocationsManager.PARAM_LOCATION_URI);
            return _locationsManager.getLocation(locationUri);
        }

        @Override
        protected TaskCallbacks getTaskCallbacks(Activity activity)
        {
            LocationOpenerBaseFragment f = (LocationOpenerBaseFragment) getFragmentManager().findFragmentByTag(getArguments().getString(ARG_OPENER_TAG));
            return f == null ? null : f.getOpenLocationTaskCallbacks();
        }

        protected void procLocation(TaskState state, Location location, Bundle param) throws Exception
        {
            Exception err = null;
            try
            {
                openFS(location, param);
            }
            catch (Exception e)
            {
               err = e;
            }
            LocationsManager.broadcastLocationChanged(_context, location);
            if(err!=null)
                throw err;
        }

        protected void openFS(Location location, Bundle param) throws IOException
        {
            location.getFS();
        }

        @Override
        protected void detachTask()
        {
            FragmentManager fm = getFragmentManager();
            if(fm!=null)
            {
                FragmentTransaction trans = fm.beginTransaction();
                trans.remove(this);
                LocationOpenerBaseFragment f = (LocationOpenerBaseFragment) fm.findFragmentByTag(getArguments().getString(ARG_OPENER_TAG));
                if(f!=null)
                    trans.remove(f);
                trans.commitAllowingStateLoss();
                Logger.debug(String.format("TaskFragment %s has been removed from the fragment manager", this));
                onEvent(EventType.Removed, this);
            }
        }

        private static final String TAG = "com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment.OpenLocationTaskFragment";
    }

    public static class ProgressReporter implements ContainerOpeningProgressReporter
    {
        public ProgressReporter(Context context)
        {
            _context = context;
        }

        public void setTaskState(TaskFragment.TaskState taskState)
        {
            _taskState = taskState;
        }

        @Override
        public void setCurrentKDFName(String name)
        {
            _hashName = name;
            updateText();
        }

        @Override
        public void setCurrentEncryptionAlgName(String name)
        {
            _encAlgName = name;
            updateText();

        }

        @Override
        public void setContainerFormatName(String name)
        {
            _formatName = name;
            updateText();
        }

        @Override
        public void setIsHidden(boolean val)
        {
            _isHidden = val;
            updateText();
        }

        @Override
        public void setText(CharSequence text)
        {
            _statusText = text;
            updateUI();
        }

        @Override
        public void setProgress(int progress)
        {
            _progress = progress;
            updateUI();
        }

        @Override
        public boolean isCancelled()
        {
            return _taskState!=null && _taskState.isTaskCancelled();
        }

        public CharSequence getStatusText()
        {
            return _statusText;
        }

        public int getProgress()
        {
            return _progress;
        }

        private final Context _context;
        private TaskFragment.TaskState _taskState;
        private String _formatName, _hashName, _encAlgName;
        private CharSequence _statusText;
        private boolean _isHidden;
        private int _progress;
        private long _prevUIUpdateTime;

        private void updateText()
        {
            setText(makeStatusText());
        }

        private void updateUI()
        {
            if(_taskState!=null)
            {
                long cur = SystemClock.elapsedRealtime();
                if (cur - _prevUIUpdateTime > 500)
                {
                    _prevUIUpdateTime = cur;
                    _taskState.updateUI(this);
                }
            }
        }

        private CharSequence makeStatusText()
        {
            StringBuilder statusString = new StringBuilder();
            if(_formatName != null)
            {
                String fn = _formatName;
                if(_isHidden)
                    fn += " (" + _context.getString(R.string.hidden) + ')';
                statusString.append(_context.getString(R.string.container_format_is, fn));
            }
            if(_encAlgName!=null)
                statusString.append(_context.getString(R.string.encryption_alg_is, _encAlgName));
            if(_hashName!=null)
                statusString.append(_context.getString(R.string.kdf_base_hash_func_is, _hashName));
            return Html.fromHtml(statusString.toString());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(getFragmentManager().findFragmentByTag(getOpenLocationTaskTag()) == null)
            openLocation();
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

    public TaskFragment.TaskCallbacks getOpenLocationTaskCallbacks()
	{
		return new OpenLocationTaskCallbacks();
	}

	protected class OpenLocationTaskCallbacks implements TaskFragment.TaskCallbacks
	{
		@Override
		public void onPrepare(Bundle args)
		{

		}

		@Override
		public void onResumeUI(Bundle args)
		{
			_dialog = ProgressDialog.showDialog(getFragmentManager(), getString(R.string.opening_container));
			_dialog.setCancelable(true);
			_dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					OpenLocationTaskFragment f = (OpenLocationTaskFragment) getFragmentManager().findFragmentByTag(getOpenLocationTaskTag());
					if(f!=null)
						f.cancel();
				}
			});
		}

		@Override
		public void onSuspendUI(Bundle args)
		{
			_dialog.dismiss();

		}

		@Override
		public void onCompleted(Bundle args,TaskFragment.Result result)
		{
			procOpenLocationTaskResult(args, result);
		}

		@Override
		public void onUpdateUI(Object state)
		{
            ProgressReporter r = (ProgressReporter)state;
            if(r!=null)
            {
                _dialog.setText(r.getStatusText());
                _dialog.setProgress(r.getProgress());
            }

		}

		private com.sovworks.eds.android.dialogs.ProgressDialog _dialog;
	}

	protected final ActivityResultHandler _resHandler = new ActivityResultHandler();

	protected TaskFragment getOpenLocationTask()
	{
		return new OpenLocationTaskFragment();
	}

    protected String getOpenLocationTaskTag()
    {
        return getOpenLocationTaskTag(getTargetLocation());
    }

	protected Bundle initOpenLocationTaskParams(Location location)
	{
        Bundle b = new Bundle();
        b.putString(OpenLocationTaskFragment.ARG_OPENER_TAG, getTag());
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

    protected void openLocation()
    {
        finishOpener(true, getTargetLocation());
    }

    protected void onLocationOpened(Location location)
    {
        LocationOpenerResultReceiver rec = getResultReceiver();
        if(rec!=null)
            rec.onTargetLocationOpened(getArguments(), location);
    }

    LocationOpenerResultReceiver getResultReceiver()
    {
        String recTag = getArguments()!=null ? getArguments().getString(PARAM_RECEIVER_FRAGMENT_TAG) : null;
        return recTag != null ? (LocationOpenerResultReceiver) getFragmentManager().findFragmentByTag(recTag) : null;
    }

    protected void onLocationNotOpened()
    {
        LocationOpenerResultReceiver rec = getResultReceiver();
        if(rec!=null)
            rec.onTargetLocationNotOpened(getArguments());
    }

    protected void finishOpener(boolean opened, Location location)
    {
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        if (opened)
            onLocationOpened(location);
        else
            onLocationNotOpened();
    }

    protected void procOpenLocationTaskResult(Bundle args, final TaskFragment.Result result)
    {
        try
        {
            Location location = (Location) result.getResult();
            if(location.isReadOnly())
                Toast.makeText(getActivity(), R.string.container_opened_read_only, Toast.LENGTH_LONG).show();
            finishOpener(true, location);
            return;
        }
        catch (CancellationException ignored)
        {

        }
        catch (Throwable e)
        {
            Logger.showAndLog(getActivity(), e);
        }
        finishOpener(false, null);
    }

	protected void startOpeningTask(Bundle args)
	{
        TaskFragment f = getOpenLocationTask();
        f.setArguments(args);
        getFragmentManager().beginTransaction().add(f, getOpenLocationTaskTag()).commit();
	}

    private static final String TAG = "com.sovworks.eds.android.locations.opener.fragments.LocationOpenerFragment";
}
