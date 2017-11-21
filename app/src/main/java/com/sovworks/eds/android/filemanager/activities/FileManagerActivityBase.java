package com.sovworks.eds.android.filemanager.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.sovworks.eds.android.EdsApplication;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.VersionHistory;
import com.sovworks.eds.android.dialogs.AskPrimaryStoragePermissionDialog;
import com.sovworks.eds.android.dialogs.MasterPasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialogBase;
import com.sovworks.eds.android.filemanager.FileManagerFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.filemanager.fragments.FilePropertiesFragment;
import com.sovworks.eds.android.filemanager.fragments.PreviewFragment;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.tasks.CheckStartPathTask;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.navigdrawer.DrawerController;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;

import static com.sovworks.eds.android.settings.UserSettingsCommon.CURRENT_SETTINGS_VERSION;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
public abstract class FileManagerActivityBase extends Activity implements PreviewFragment.Host, PasswordDialogBase.PasswordReceiver
{
    public static Location getStartLocation(Context context)
    {
        return LocationsManager.getLocationsManager(context, true).getDefaultDeviceLocation();
    }
    public static Intent getSelectPathIntent(
            Context context,
            Uri startPath,
            boolean allowMultiSelect,
            boolean allowFileSelect,
            boolean allowDirSelect,
            boolean allowCreateNew,
            boolean allowBrowseDevice,
            boolean allowBrowseContainer) throws IOException
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

    public static void selectPath(
            Activity context,
            Fragment f,
            int requestCode,
            boolean allowMultiSelect,
            boolean allowFileSelect,
            boolean allowDirSelect,
            boolean allowCreateNew,
            boolean allowBrowseDevice,
            boolean allowBrowseContainer) throws IOException
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
            boolean allowCreateNew) throws IOException
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
        closeIntegratedViewer();
        FileListDataFragment f = getFileListDataFragment();
        if(f!=null)
            f.goTo(location, scrollPosition);
    }

    public void goTo(Path path) throws IOException
	{
		Location prevLocation = getLocation();
        if(prevLocation != null)
        {
            Location newLocation = prevLocation.copy();
            newLocation.setCurrentPath(path);
            goTo(newLocation);
        }
	}

    public void rereadCurrentLocation()
    {
        FileListDataFragment f = getFileListDataFragment();
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
            hideSecondaryFragment();
        else if(_isLargeScreenLayout || !allowInplace)
            showPropertiesFragment(currentFile);
	}

	public void showPhoto(BrowserRecord currentFile, boolean allowInplace)
	{
		Path contextPath = currentFile == null ? null : currentFile.getPath();
        if(!hasSelectedFiles() && contextPath == null)
            hideSecondaryFragment();
        else if(_isLargeScreenLayout || !allowInplace)
            showPreviewFragment(contextPath);
	}

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
        Util.setTheme(this);
	    super.onCreate(savedInstanceState);
        Logger.debug("fm start intent: " + getIntent());
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
        Logger.debug("Checking master password");
        if(MasterPasswordDialog.checkMasterPasswordIsSet(this, getFragmentManager(), null))
            startAction(savedInstanceState);
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

    public void continueActionMainInit()
    {
        _drawer.init(null);
        if(Intent.ACTION_MAIN.equals(getIntent().getAction()) && getIntent().getData() == null)
            _drawer.showContainers();
        convertLegacySettings();
        showPromoDialogIfNeeded();
        _testIsInited = true;
    }

    public DrawerController getDrawerController()
    {
        return _drawer;
    }

    public void initActionMain() throws IOException
    {
        initActionCommon();
        continueActionMainInit();
    }

    //master passsword is set
    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        startAction(null);
    }

    //master passsword is not set
    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        if(MasterPasswordDialog.checkSettingsKey(this))
            startAction(null);
        else
            finish();
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
                        setIntent(new Intent(Intent.ACTION_MAIN, locToOpen.getLocationUri()));
                    else
                        setIntent(new Intent());
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(_context, e);
                    setIntent(new Intent());
                }
                try
                {
                    actionMain(null);
                }
                catch (Exception e)
                {
                    Logger.showAndLog(_context, e);
                    finish();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_EXT_STORAGE_PERMISSIONS)
        {
            if((grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) && !requestExtStoragePermissionWithRationale())
                return;

            try
            {
                initActionMain();
            }
            catch (Exception e)
            {
                Logger.showAndLog(this, e);
                finish();
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onPostCreate(Bundle state)
    {
        super.onPostCreate(state);
        _drawer.onPostCreate();
    }

    @Override
    protected void onPause()
    {
        Logger.debug("FileManagerActivity onPause");
        _resHandler.onPause();
        super.onPause();
    }

    @Override
	protected void onResume()
	{
        Logger.debug("FileManagerActivity onResume");
		super.onResume();
        _resHandler.handle();
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

    protected void convertLegacySettings()
    {
        int curSettingsVersion = _settings.getCurrentSettingsVersion();
        if(curSettingsVersion >= Settings.VERSION)
            return;

        if(curSettingsVersion == 1)
        {
            if(_settings.getLastViewedPromoVersion() > 160)
            {
                _settings.getSharedPreferences().edit().putInt(CURRENT_SETTINGS_VERSION, Settings.VERSION).commit();
                return;
            }
        }

        LocationsManager lm = LocationsManager.getLocationsManager(this);
        for(Location l: lm.getLoadedLocations(false))
            if(l instanceof ContainerBasedLocation && !l.getExternalSettings().isVisibleToUser())
            {
                l.getExternalSettings().setVisibleToUser(true);
                l.saveExternalSettings();
            }
        SharedPreferences prefs = _settings.getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(UserSettings.CURRENT_SETTINGS_VERSION, Settings.VERSION);
        editor.commit();
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
                case Intent.ACTION_SEND:
                case Intent.ACTION_SEND_MULTIPLE:
                    actionSend(savedState);
                    break;
                case Intent.ACTION_VIEW:
                    actionView(savedState);
                    break;
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
	protected void onDestroy ()
	{
        unregisterReceiver(_locationAddedOrRemovedReceiver);
        unregisterReceiver(_locationChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_exitBroadcastReceiver);
        _settings = null;
        super.onDestroy();
    }

    protected abstract DrawerController createDrawerController();

    private static final int REQUEST_EXT_STORAGE_PERMISSIONS = 2;
    protected static final String FOLDER_MIME_TYPE = "resource/folder";
    protected final DrawerController _drawer = createDrawerController();
    protected final ActivityResultHandler _resHandler = new ActivityResultHandler();
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

    private void actionMain(Bundle savedState) throws Exception
    {
        if(savedState == null)
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkExtStoragePermissions())
                initActionMain();
        }
        else
            restoreActionCommon(savedState);
    }

    private void actionSend(Bundle savedState)
    {
        if(savedState == null)
            initActionSend();
        else
            restoreActionCommon(savedState);
    }

    private void actionView(Bundle savedState) throws Exception
    {
        if(savedState == null)
            initActionView();
        else
            restoreActionCommon(savedState);
    }

    protected void initActionCommon()
    {
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.add(FileListDataFragment.newInstance(), FileListDataFragment.TAG);
        trans.add(R.id.fragment1, FileListViewFragment.newInstance(), FileListViewFragment.TAG);
        trans.commit();
    }

    private void initActionView() throws IOException
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
                return;
            }

        }
        setIntent(new Intent());
        initActionMain();
    }


    private void restoreActionCommon(Bundle state)
    {
        _drawer.init(state);
    }

    private void initActionSend()
    {
        initActionCommon();
        _drawer.init(null);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkExtStoragePermissions()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            requestExtStoragePermission();
            return false;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestExtStoragePermissionWithRationale()
    {
        if (shouldShowRequestPermissionRationale(
                Manifest.permission.READ_EXTERNAL_STORAGE)
                || shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            AskPrimaryStoragePermissionDialog.showDialog(getFragmentManager());
            return false;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestExtStoragePermission()
    {
        requestPermissions(
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_EXT_STORAGE_PERMISSIONS);
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

    public boolean isInited()
    {
        return _testIsInited;
    }


    private boolean _testIsInited;

    private FilePropertiesFragment showPropertiesFragment(BrowserRecord currentFile)
	{
		FilePropertiesFragment f = FilePropertiesFragment.newInstance(currentFile == null ? null : currentFile.getPath());
		showSecondaryFragment(f, FilePropertiesFragment.TAG);
		return f;
	}

    private PreviewFragment showPreviewFragment(Path currentImage)
    {
        PreviewFragment f = PreviewFragment.newInstance(currentImage);
        showSecondaryFragment(f, PreviewFragment.TAG);
        return f;
    }

    private void closeIntegratedViewer()
    {
        hideSecondaryFragment();
    }
}

