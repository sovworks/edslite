package com.sovworks.eds.android.filemanager.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.PreviewFragment;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.fragments.TaskFragment.Result;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CachedPathInfoBase;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.views.GestureImageViewWithFullScreenMode;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.ArrayList;
import java.util.List;

import static com.sovworks.eds.android.settings.UserSettingsCommon.IMAGE_VIEWER_FULL_SCREEN_ENABLED;

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
		}

		@Override
		protected void doWork(TaskState state) throws Exception
		{			
			ArrayList<Path> paths = Util.restorePaths(_loc.getFS(), _pathStrings);
			ArrayList<CachedPathInfo> res = new ArrayList<>();
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
	}

	public static class ImageViewFragment extends PreviewFragment
	{
		public static ImageViewFragment newInstance(String currentImagePathString)
		{
			Bundle b = new Bundle();
			b.putString(STATE_CURRENT_PATH,currentImagePathString);
			ImageViewFragment pf = new ImageViewFragment();
			pf.setArguments(b);
			return pf;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View res = super.onCreateView(inflater, container, savedInstanceState);
			if(res!=null)
			{
				_imageView = res.findViewById(R.id.imageView);
				if(UserSettings.getSettings(getActivity()).isImageViewerFullScreenModeEnabled())
					_imageView.setFullscreenMode(true);
			}
			return res;
		}

		GestureImageViewWithFullScreenMode getImageView()
		{
			return _imageView;
		}

		private GestureImageViewWithFullScreenMode _imageView;
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
	public boolean onCreateOptionsMenu (Menu menu)
	{
		super.onCreateOptionsMenu(menu);
    	final MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.menu.image_viewer_activity_menu, menu);
    	return true; 
	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		MenuItem mi = menu.findItem(R.id.fullScreenModeMenuItem);
		mi.setChecked(_isFullScreen);
		return true;
	}
	

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		super.onOptionsItemSelected(item);
		if(item.getItemId() == R.id.fullScreenModeMenuItem)
		{
			_isFullScreen = !_isFullScreen;
            UserSettings.getSettings(this).getSharedPreferences().edit().putBoolean(IMAGE_VIEWER_FULL_SCREEN_ENABLED, _isFullScreen).commit();
			if(android.os.Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT)
				CompatHelper.restartActivity(this);
			else
			{
				GestureImageViewWithFullScreenMode imageView = getImageView();
				if(imageView!=null)
					imageView.setFullscreenMode(_isFullScreen);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public List<? extends CachedPathInfo> getCurrentFiles()
	{
		return _files;
	}

	@Override
	public Location getLocation()
	{
		return _location;
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
					_files = (List<CachedPathInfo>) result.getResult();		
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
		if (hasFocus && _isFullScreen)
		{
			GestureImageViewWithFullScreenMode img = getImageView();
			if(img!=null)
				img.setFullscreenMode(true);
		}
	}

	private boolean _isFullScreen;
	private List<CachedPathInfo> _files = new ArrayList<>();
	private Location _location;

	private ImageViewFragment getPreviewFragment()
	{
		return (ImageViewFragment) getFragmentManager().findFragmentByTag(ImageViewFragment.TAG);
	}

	private GestureImageViewWithFullScreenMode getImageView()
	{
		ImageViewFragment f = getPreviewFragment();
		return f != null ? f.getImageView() : null;
	}
		
	private void showFragment(String currentImagePathString)
	{
		PreviewFragment f = ImageViewFragment.newInstance(currentImagePathString);
		getFragmentManager().beginTransaction().add(android.R.id.content, f, ImageViewFragment.TAG).commit();
	}	
	
	private void enableFullScreen()
	{		
		if(android.os.Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	     _isFullScreen = true;
		invalidateOptionsMenu();
	}
}
