package com.sovworks.eds.android.filemanager.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.fragments.PreviewFragment;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.fragments.TaskFragment.Result;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CachedPathInfoBase;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.Settings;

import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref", "InlinedApi"})
public class ImageViewerActivity extends Activity implements PreviewFragment.Host
{	
	public static final String INTENT_PARAM_CURRENT_PATH = "current_path";
	
	public static class RestorePathsTask extends TaskFragment
	{
		public static final String TAG = "RestorePathsTask";
		
		public static RestorePathsTask newInstance()
		{
			return new RestorePathsTask();
		}

		protected void initTask(Activity activity)
		{
			_loc = ((ImageViewerActivity)activity).getLocation();
			_pathStrings = activity.getIntent().getStringArrayListExtra(LocationsManager.PARAM_PATHS);
			_settings = UserSettings.getSettings(activity);
		}

		@Override
		protected void doWork(TaskState state) throws Exception
		{			
			ArrayList<Path> paths = Util.restorePaths(_loc.getFS(), _pathStrings);
			@SuppressWarnings("unchecked") TreeSet<CachedPathInfo> res = new TreeSet(FileListDataFragment.getComparator(_settings));
			for(Path p: paths)
			{
				CachedPathInfoBase cpi = new CachedPathInfoBase();
				cpi.init(p);
				res.add(cpi);
			}
			state.setResult(res);
		}		
		
		@Override
		protected TaskCallbacks getTaskCallbacks(Activity activity)
		{
			return ((ImageViewerActivity) activity).getRestorePathsTaskCallbacks();
		}
		private Location _loc;
		private ArrayList<String> _pathStrings;
		private Settings _settings;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		com.sovworks.eds.android.helpers.Util.setTheme(this);
		super.onCreate(savedInstanceState);
		UserSettings us = UserSettings.getSettings(this);
		if(us.isFlagSecureEnabled())
			CompatHelper.setWindowFlagSecure(this);
		if(us.isImageViewerFullScreenModeEnabled())
			enableFullScreen();
		_location = LocationsManager.getLocationsManager(this).getFromIntent(getIntent(), null);
		getFragmentManager().beginTransaction().add(RestorePathsTask.newInstance(), RestorePathsTask.TAG).commit();
	}

	@Override
	public NavigableSet<? extends CachedPathInfo> getCurrentFiles()
	{
		return _files;
	}

	@Override
	public Location getLocation()
	{
		return _location;
	}

	@Override
	public Object getFilesListSync()
	{
		return new Object();
	}

	@Override
	public void onToggleFullScreen()
	{
		if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			CompatHelper.restartActivity(this);
	}

	public TaskFragment.TaskCallbacks getRestorePathsTaskCallbacks()
	{
		return new ProgressDialogTaskFragmentCallbacks(this,R.string.loading)
		{
			@SuppressWarnings("unchecked")
			@Override
			public void onCompleted(Bundle args, Result result)
			{
				super.onCompleted(args, result);
				try			
				{
					_files = (TreeSet<CachedPathInfo>) result.getResult();
				}
				catch(Throwable e)
				{
					Logger.showAndLog(_context, result.getError());
				}
				if(getPreviewFragment()==null)
					showFragment(getIntent().getStringExtra(INTENT_PARAM_CURRENT_PATH));
			}
		};
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		PreviewFragment pf = (PreviewFragment) getFragmentManager().findFragmentByTag(PreviewFragment.TAG);
		if(pf!=null)
			pf.updateImageViewFullScreen();
	}

	private TreeSet<CachedPathInfo> _files;
	private Location _location;

	private PreviewFragment getPreviewFragment()
	{
		return (PreviewFragment) getFragmentManager().findFragmentByTag(PreviewFragment.TAG);
	}

	private void showFragment(String currentImagePathString)
	{
		PreviewFragment f = PreviewFragment.newInstance(currentImagePathString);
		getFragmentManager().beginTransaction().add(android.R.id.content, f, PreviewFragment.TAG).commit();
	}	
	
	private void enableFullScreen()
	{		
		if(android.os.Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		invalidateOptionsMenu();
	}
}
