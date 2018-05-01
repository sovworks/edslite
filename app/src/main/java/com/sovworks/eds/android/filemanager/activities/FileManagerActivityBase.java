package com.sovworks.eds.android.filemanager.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.sovworks.eds.android.EdsApplication;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.VersionHistory;
import com.sovworks.eds.android.dialogs.AskOverwriteDialog;
import com.sovworks.eds.android.filemanager.FileManagerFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.filemanager.fragments.FilePropertiesFragment;
import com.sovworks.eds.android.filemanager.fragments.PreviewFragment;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.tasks.CheckStartPathTask;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.AppInitHelper;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.navigdrawer.DrawerController;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.settings.GlobalConfig;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.RxActivity;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
public abstract class FileManagerActivityBase extends RxActivity implements PreviewFragment.Host
{
    public static final String TAG = "FileManagerActivity";
    public static final String ACTION_ASK_OVERWRITE = "com.sovworks.eds.android.ACTION_ASK_OVERWRITE";

    static
    {
        if(GlobalConfig.isTest())
            TEST_INIT_OBSERVABLE = BehaviorSubject.createDefault(false);

    }
    public static Subject<Boolean> TEST_INIT_OBSERVABLE;

    public static Location getStartLocation(Context context)
    {
        return LocationsManager.getLocationsManager(context, true).getDefaultDeviceLocation();
    }

    public static Intent getOverwriteRequestIntent(
            Context context,
            boolean move,
            SrcDstCollection records)
    {
        Intent i = new Intent(context, FileManagerActivity.class);
        i.setAction(ACTION_ASK_OVERWRITE);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra(AskOverwriteDialog.ARG_MOVE, move);
        i.putExtra(AskOverwriteDialog.ARG_PATHS, records);
        return i;
    }

    public static Intent getSelectPathIntent(
            Context context,
            Uri startPath,
            boolean allowMultiSelect,
            boolean allowFileSelect,
            boolean allowDirSelect,
            boolean allowCreateNew,
            boolean allowBrowseDevice,
            boolean allowBrowseContainer)
    {
        Intent intent = new Intent(context, FileManagerActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        if(startPath == null)
            startPath = getStartLocation(context).getLocationUri();

        intent.setData(startPath);

        intent.putExtra(
                EXTRA_ALLOW_MULTIPLE, allowMultiSelect);
        intent.putExtra(
                EXTRA_ALLOW_FILE_SELECT, allowFileSelect);
        intent.putExtra(
                EXTRA_ALLOW_FOLDER_SELECT, allowDirSelect);
        intent.putExtra(
                EXTRA_ALLOW_CREATE_NEW_FILE, allowCreateNew);
        intent.putExtra(
                EXTRA_ALLOW_CREATE_NEW_FOLDER, allowCreateNew);

        intent.putExtra(
                EXTRA_ALLOW_BROWSE_DEVICE, allowBrowseDevice);
        intent.putExtra(
                EXTRA_ALLOW_BROWSE_CONTAINERS, allowBrowseContainer);
        return intent;

    }

    @SuppressWarnings("SameParameterValue")
    public static void selectPath(
            Activity context,
            Fragment f,
            int requestCode,
            boolean allowMultiSelect,
            boolean allowFileSelect,
            boolean allowDirSelect,
            boolean allowCreateNew,
            boolean allowBrowseDevice,
            boolean allowBrowseContainer)
    {
        Intent i = getSelectPathIntent(
                context,
                null,
                allowMultiSelect,
                allowFileSelect,
                allowDirSelect,
                allowCreateNew,
                allowBrowseDevice,
                allowBrowseContainer);
        f.startActivityForResult(i, requestCode);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void selectPath(
            Activity context,
            Fragment f,
            int requestCode,
            boolean allowMultiSelect,
            boolean allowFileSelect,
            boolean allowDirSelect,
            boolean allowCreateNew)
    {
        selectPath(
                context,
                f,
                requestCode,
                allowMultiSelect,
                allowFileSelect,
                allowDirSelect,
                allowCreateNew,
                true,
                true
        );
    }

    @SuppressLint("InlinedApi")
    public static final String EXTRA_ALLOW_MULTIPLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? Intent.EXTRA_ALLOW_MULTIPLE : "com.sovworks.eds.android.ALLOW_MULTIPLE";
    public static final String EXTRA_ALLOW_FILE_SELECT = "com.sovworks.eds.android.ALLOW_FILE_SELECT";
    public static final String EXTRA_ALLOW_FOLDER_SELECT = "com.sovworks.eds.android.ALLOW_FOLDER_SELECT";
    public static final String EXTRA_ALLOW_CREATE_NEW_FILE = "com.sovworks.eds.android.ALLOW_CREATE_NEW_FILE";
    public static final String EXTRA_ALLOW_CREATE_NEW_FOLDER = "com.sovworks.eds.android.ALLOW_CREATE_NEW_FOLDER";

    public static final String EXTRA_ALLOW_BROWSE_CONTAINERS = "com.sovworks.eds.android.ALLOW_BROWSE_CONTAINERS";
    public static final String EXTRA_ALLOW_BROWSE_DEVICE = "com.sovworks.eds.android.ALLOW_BROWSE_DEVICE";
    public static final String EXTRA_ALLOW_BROWSE_DOCUMENT_PROVIDERS = "com.sovworks.eds.android.ALLOW_BROWSE_DOCUMENT_PROVIDERS";

    public static final String EXTRA_ALLOW_SELECT_FROM_CONTENT_PROVIDERS = "com.sovworks.eds.android.ALLOW_SELECT_FROM_CONTENT_PROVIDERS";
    public static final String EXTRA_ALLOW_SELECT_ROOT_FOLDER = "com.sovworks.eds.android.ALLOW_SELECT_ROOT_FOLDER";

    public static Location getRealLocation(Location loc)
    {
        return loc;
    }
    public boolean isSelectAction()
    {
        String action = getIntent().getAction();
        return Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action);
    }

    public boolean isSingleSelectionMode()
    {
        return !getIntent().getBooleanExtra(EXTRA_ALLOW_MULTIPLE, false);
    }

    public boolean allowFileSelect()
    {
        return getIntent().getBooleanExtra(EXTRA_ALLOW_FILE_SELECT, true);
    }

    public boolean allowFolderSelect()
    {
        return getIntent().getBooleanExtra(EXTRA_ALLOW_FOLDER_SELECT, true);
    }

    public void goTo(Location location)
    {
        goTo(location, 0);
    }

    public void goTo(Location location, int scrollPosition)
    {
        Logger.debug(TAG + ": goTo");
        closeIntegratedViewer();
        FileListViewFragment f = getFileListViewFragment();
        if(f!=null)
            f.goTo(location, scrollPosition, true);
    }

    public void goTo(Path path)
    {
		Location prevLocation = getLocation();
        if(prevLocation != null)
        {
            Location newLocation = prevLocation.copy();
            newLocation.setCurrentPath(path);
            goTo(newLocation, 0);
        }
	}

    public void rereadCurrentLocation()
    {
        FileListViewFragment f = getFileListViewFragment();
        if(f!=null)
            f.rereadCurrentLocation();
    }

	public boolean isWideScreenLayout()
	{
		return _isLargeScreenLayout;
	}

    @Override
    public NavigableSet<? extends CachedPathInfo> getCurrentFiles()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f!=null ? f.getFileList() : new TreeSet<>();
    }

    @Override
    public Object getFilesListSync()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f!=null ? f.getFilesListSync() : new Object();
    }

    public Location getLocation()
    {
        FileListDataFragment f = (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
        return f == null ? null : f.getLocation();
    }

    public Location getRealLocation()
    {
        return getRealLocation(getLocation());
    }

    public boolean hasSelectedFiles()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f != null && f.hasSelectedFiles();
    }

    public void showProperties(BrowserRecord currentFile, boolean allowInplace)
	{
        if(!hasSelectedFiles() && currentFile == null)
        {
            Logger.debug(TAG + ": showProperties (hide)");
            if(getFragmentManager().findFragmentByTag(FilePropertiesFragment.TAG)!=null)
                hideSecondaryFragment();
        }
        else if(_isLargeScreenLayout || !allowInplace)
        {
            Logger.debug(TAG + ": showProperties");
            showPropertiesFragment(currentFile);
        }
	}

	public void showPhoto(BrowserRecord currentFile, boolean allowInplace)
	{
	    Logger.debug(TAG + ": showPhoto");
		Path contextPath = currentFile == null ? null : currentFile.getPath();
        if(!hasSelectedFiles() && contextPath == null)
            hideSecondaryFragment();
        else if(_isLargeScreenLayout || !allowInplace)
            showPreviewFragment(contextPath);
	}

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
	    if(GlobalConfig.isTest())
	        TEST_INIT_OBSERVABLE.onNext(false);
        Util.setTheme(this);
	    super.onCreate(savedInstanceState);
        Logger.debug("fm start intent: " + getIntent());
        _settings = UserSettings.getSettings(this);
        if(_settings.isFlagSecureEnabled())
            CompatHelper.setWindowFlagSecure(this);
	    _isLargeScreenLayout = UserSettings.isWideScreenLayout(_settings, this);
	    setContentView(R.layout.main_activity);
	    Fragment f = getFragmentManager().findFragmentById(R.id.fragment2);
	    if(f!=null)
        {
            View panel = findViewById(R.id.fragment2);
            if (panel != null)
                panel.setVisibility(View.VISIBLE);
            panel = findViewById(R.id.fragment1);
            if (panel != null)
                panel.setVisibility(View.GONE);
        }
	    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(_exitBroadcastReceiver, new IntentFilter(EdsApplication.BROADCAST_EXIT));
        registerReceiver(_locationAddedOrRemovedReceiver, LocationsManager.getLocationAddedIntentFilter());
        registerReceiver(_locationAddedOrRemovedReceiver, LocationsManager.getLocationRemovedIntentFilter());
        registerReceiver(_locationChangedReceiver, new IntentFilter(LocationsManager.BROADCAST_LOCATION_CHANGED));
        registerReceiver(_locationAddedOrRemovedReceiver, new IntentFilter(LocationsManager.BROADCAST_LOCATION_CHANGED));

        _drawer.init(savedInstanceState);
        AppInitHelper.
                createObservable(this).
                compose(bindToLifecycle()).
                subscribe(() -> {
                    startAction(savedInstanceState);
                    addFileListFragments();
                }, err -> {
                        if(!(err instanceof CancellationException))
                            Logger.showAndLog(getApplicationContext(), err);
                });

	}

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);
        lifecycle().
                filter(event -> event == ActivityEvent.RESUME).
                firstElement().
                subscribe(res -> startAction(null), err ->
                {
                    if(!(err instanceof CancellationException))
                        Logger.log(err);
                });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            PreviewFragment pf = (PreviewFragment) getFragmentManager().findFragmentByTag(PreviewFragment.TAG);
            if(pf!=null)
                pf.updateImageViewFullScreen();
        }
    }

    @Override
    public void onToggleFullScreen()
    {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        Logger.debug(TAG + ": dispatchKeyEvent");
        //Prevent selection clearing when back button is pressed while properties fragment is active
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
        {
            if (!_isLargeScreenLayout && hasSelectedFiles())
            {
                Fragment f = getFragmentManager().findFragmentByTag(FilePropertiesFragment.TAG);
                if (f != null)
                {
                    hideSecondaryFragment();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        return _drawer.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed()
    {
        Logger.debug(TAG + ": onBackPressed");

        if(_drawer.onBackPressed())
            return;

        Fragment f = getFragmentManager().findFragmentById(R.id.fragment2);
        if(f!=null && ((FileManagerFragment) f).onBackPressed())
            return;

        if(hideSecondaryFragment())
            return;

        f = getFragmentManager().findFragmentById(R.id.fragment1);
        if(f!=null && ((FileManagerFragment) f).onBackPressed())
            return;

        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        _drawer.onConfigurationChanged(newConfig);
    }

    public FileListDataFragment getFileListDataFragment()
    {
        FileListDataFragment f = (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
        return f != null && f.isAdded() ? f : null;
    }

    public FileListViewFragment getFileListViewFragment()
    {
        FileListViewFragment f = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
        return f != null && f.isAdded() ? f : null;
    }

    public DrawerController getDrawerController()
    {
        return _drawer;
    }


    public TaskFragment.TaskCallbacks getCheckStartPathCallbacks()
    {
        return new ProgressDialogTaskFragmentCallbacks(this, R.string.loading)
        {
            @Override
            public void onCompleted(Bundle args, TaskFragment.Result result)
            {
                try
                {
                    Location locToOpen = (Location) result.getResult();
                    if(locToOpen != null)
                    {
                        setIntent(new Intent(Intent.ACTION_MAIN, locToOpen.getLocationUri()));
                        FileListDataFragment df = getFileListDataFragment();
                        if(df!=null)
                            df.loadLocation(null, true);
                    }
                    else
                        setIntent(new Intent());
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(_context, e);
                    setIntent(new Intent());
                }
            }
        };
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        _drawer.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(Bundle state)
    {
        super.onPostCreate(state);
        _drawer.onPostCreate();
    }



    @Override
    protected void onStart()
    {
        super.onStart();
        checkIfCurrentLocationIsStillOpen();
        getDrawerController().updateMenuItemViews();
        registerReceiver(_updatePathReceiver, new IntentFilter(
                FileOpsService.BROADCAST_FILE_OPERATION_COMPLETED));
        registerReceiver(_closeAllReceiver, new IntentFilter(LocationsManager.BROADCAST_CLOSE_ALL));
        Logger.debug("FileManagerActivity has started");
    }

    @Override
    protected void onStop()
    {
        unregisterReceiver(_closeAllReceiver);
        unregisterReceiver(_updatePathReceiver);
        super.onStop();
        Logger.debug("FileManagerActivity has stopped");
    }

    protected void startAction(Bundle savedState)
    {
        String action = getIntent().getAction();
        if(action == null)
            action = "";
        Logger.log("FileManagerActivity action is " + action);
        try
        {
            switch (action)
            {
                case Intent.ACTION_VIEW:
                    actionView(savedState);
                    break;
                case ACTION_ASK_OVERWRITE:
                    actionAskOverwrite();
                    break;
                case Intent.ACTION_SEND:
                case Intent.ACTION_SEND_MULTIPLE:
                case Intent.ACTION_MAIN:
                default:
                    actionMain(savedState);
                    break;
            }
        }
        catch(Exception e)
        {
            Logger.showAndLog(this, e);
            finish();
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(GlobalConfig.isTest())
            TEST_INIT_OBSERVABLE.onNext(true);
    }

    @Override
	protected void onDestroy ()
	{
        unregisterReceiver(_locationAddedOrRemovedReceiver);
        unregisterReceiver(_locationChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_exitBroadcastReceiver);
        _settings = null;
        super.onDestroy();
    }

    protected abstract DrawerController createDrawerController();

    protected static final String FOLDER_MIME_TYPE = "resource/folder";
    protected final DrawerController _drawer = createDrawerController();
    protected boolean _isLargeScreenLayout;
    protected UserSettings _settings;

    private final BroadcastReceiver _updatePathReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
            rereadCurrentLocation();
		}
	};

    private final BroadcastReceiver _closeAllReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
            checkIfCurrentLocationIsStillOpen();
            getDrawerController().updateMenuItemViews();
        }
	};


    private final BroadcastReceiver _locationChangedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(isFinishing())
                return;

            try
            {
                Uri locUri = intent.getParcelableExtra(LocationsManager.PARAM_LOCATION_URI);
                if(locUri!=null)
                {
                    Location changedLocation = LocationsManager.getLocationsManager(getApplicationContext()).getLocation(locUri);
                    if(changedLocation!=null)
                    {
                        Location loc = getRealLocation();
                        if(loc!=null && changedLocation.getId().equals(loc.getId()))
                            checkIfCurrentLocationIsStillOpen();

                        FileListDataFragment f = getFileListDataFragment();
                        if(f!=null && !LocationsManager.isOpen(changedLocation))
                            f.removeLocationFromHistory(changedLocation);
                    }
                }
            }
            catch(Exception e)
            {
                Logger.showAndLog(context, e);
                finish();
            }
        }
    };

    private final BroadcastReceiver _locationAddedOrRemovedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(isFinishing())
                return;
            if(LocationsManager.BROADCAST_LOCATION_REMOVED.equals(intent.getAction()))
            {
                try
                {
                    Uri locUri = intent.getParcelableExtra(LocationsManager.PARAM_LOCATION_URI);
                    if(locUri!=null)
                    {
                        Location changedLocation = LocationsManager.getLocationsManager(getApplicationContext()).getLocation(locUri);
                        if(changedLocation!=null)
                        {
                            FileListDataFragment f = getFileListDataFragment();
                            if(f!=null)
                                f.removeLocationFromHistory(changedLocation);
                        }
                    }
                }
                catch(Exception e)
                {
                    Logger.showAndLog(context, e);
                }
            }
            getDrawerController().reloadItems();
        }
    };

    private final BroadcastReceiver _exitBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            finish();
        }
    };

    protected void actionMain(Bundle savedState) throws Exception
    {
        if(savedState == null)
        {
            if(getIntent().getData() == null)
                _drawer.showContainers();
            showPromoDialogIfNeeded();
        }
    }

    private void actionView(Bundle savedState)
    {
        if(savedState == null)
        {
            Uri dataUri = getIntent().getData();
            if(dataUri!=null)
            {
                String mime = getIntent().getType();
                if(!FOLDER_MIME_TYPE.equalsIgnoreCase(mime))
                {
                    getFragmentManager().
                            beginTransaction().
                            add(
                                    CheckStartPathTask.newInstance(dataUri, false),
                                    CheckStartPathTask.TAG
                            ).
                            commit();
                    setIntent(new Intent());
                }
            }
        }
    }

    private void actionAskOverwrite()
    {
        AskOverwriteDialog.showDialog(
                getFragmentManager(),
                getIntent().getExtras()
        );
        setIntent(new Intent());
    }

    protected void addFileListFragments()
    {
        FragmentManager fm = getFragmentManager();
        if(fm.findFragmentByTag(FileListDataFragment.TAG) == null)
        {
            FragmentTransaction trans = getFragmentManager().beginTransaction();
            trans.add(FileListDataFragment.newInstance(), FileListDataFragment.TAG);
            trans.add(R.id.fragment1, FileListViewFragment.newInstance(), FileListViewFragment.TAG);
            trans.commit();
        }
    }

    protected void showSecondaryFragment(Fragment f, String tag)
	{
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.fragment2, f, tag);
        View panel = findViewById(R.id.fragment2);
        if(panel!=null)
            panel.setVisibility(View.VISIBLE);
        if(!_isLargeScreenLayout)
        {
            panel = findViewById(R.id.fragment1);
            if(panel!=null)
                panel.setVisibility(View.GONE);
        }
        trans.disallowAddToBackStack();
        trans.commit();
    }

    protected boolean hideSecondaryFragment()
    {
        Logger.debug(TAG + ": hideSecondaryFragment");
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment2);
        if(f!=null)
        {
            FragmentTransaction trans = fm.beginTransaction();
            trans.remove(f);
            trans.commit();
            View panel = findViewById(R.id.fragment1);
            if(panel!=null)
                panel.setVisibility(View.VISIBLE);
            if(!_isLargeScreenLayout)
            {
                panel = findViewById(R.id.fragment2);
                if(panel!=null)
                    panel.setVisibility(View.GONE);
            }
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    protected void checkIfCurrentLocationIsStillOpen()
    {
        Location loc = getRealLocation();
        if (!isFinishing() &&
                loc instanceof Openable && !LocationsManager.isOpen(loc) &&
                (getIntent().getData() == null || !getIntent().getData().equals(loc.getLocationUri()))
                )
        {
            //closeIntegratedViewer();
            goTo(getStartLocation(this));
        }
    }

    protected void showPromoDialogIfNeeded()
    {
        if(!GlobalConfig.isDebug())
            startActivity(new Intent(this, VersionHistory.class));
    }

    private void showPropertiesFragment(BrowserRecord currentFile)
	{
		FilePropertiesFragment f = FilePropertiesFragment.newInstance(currentFile == null ? null : currentFile.getPath());
		showSecondaryFragment(f, FilePropertiesFragment.TAG);
    }

    private void showPreviewFragment(Path currentImage)
    {
        PreviewFragment f = PreviewFragment.newInstance(currentImage);
        showSecondaryFragment(f, PreviewFragment.TAG);
    }

    private void closeIntegratedViewer()
    {
        Logger.debug(TAG + ": closeIntegratedViewer");
        hideSecondaryFragment();
    }
}

