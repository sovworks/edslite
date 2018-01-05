package com.sovworks.eds.android.filemanager.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.FileListViewAdapter;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.filemanager.comparators.FileNamesComparator;
import com.sovworks.eds.android.filemanager.comparators.FileNamesNumericComparator;
import com.sovworks.eds.android.filemanager.comparators.FileSizesComparator;
import com.sovworks.eds.android.filemanager.comparators.ModDateComparator;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.tasks.CreateNewFile;
import com.sovworks.eds.android.filemanager.tasks.LoadDirSettingsObservable;
import com.sovworks.eds.android.filemanager.tasks.ReadDir;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CachedPathInfoBase;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.android.locations.activities.OpenLocationsActivity;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.settings.Settings;
import com.trello.rxlifecycle2.android.FragmentEvent;
import com.trello.rxlifecycle2.components.RxFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_DATE_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_DATE_DESC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_DESC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_NUM_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_NUM_DESC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_SIZE_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_SIZE_DESC;

public class FileListDataFragment extends RxFragment
{
    public static FileListDataFragment newInstance()
    {
        return new FileListDataFragment();
    }

    static
    {
        if(GlobalConfig.isTest())
            TEST_READING_OBSERVABLE = BehaviorSubject.createDefault(false);

    }

    public static Subject<Boolean> TEST_READING_OBSERVABLE;

    public static <T extends CachedPathInfo> Comparator<T> getComparator(Settings settings)
    {
        switch (settings.getFilesSortMode())
        {
            case FB_SORT_FILENAME_ASC:
                return new FileNamesComparator<>(true);
            case FB_SORT_FILENAME_DESC:
                return new FileNamesComparator<>(false);
            case FB_SORT_FILENAME_NUM_ASC:
                return new FileNamesNumericComparator<>(true);
            case FB_SORT_FILENAME_NUM_DESC:
                return new FileNamesNumericComparator<>(false);
            case FB_SORT_SIZE_ASC:
                return new FileSizesComparator<>(true);
            case FB_SORT_SIZE_DESC:
                return new FileSizesComparator<>(false);
            case FB_SORT_DATE_ASC:
                return new ModDateComparator<>(true);
            case FB_SORT_DATE_DESC:
                return new ModDateComparator<>(false);
            default:
                return null;
        }
    }

    public static Uri getLocationUri(Intent intent, Bundle state)
    {
        Uri locUri;
        if(state!=null)
            locUri = state.getParcelable(LocationsManager.PARAM_LOCATION_URI);
        else
            locUri = intent.getData();
        return locUri;
    }

    public static final String TAG = "com.sovworks.eds.android.filemanager.fragments.FileListDataFragment";

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        setRetainInstance(true);
        _location = getFallbackLocation();
        _locationsManager = LocationsManager.getLocationsManager(getActivity());
        _fileList = new TreeSet<>(initSorter());
        loadLocation(state, true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        //TODO remove dependency
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
                for(BrowserRecord br: _fileList)
                    br.setHostActivity((FileManagerActivity) getActivity());
        }
    }


    @Override
	public void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if(requestCode == REQUEST_CODE_OPEN_LOCATION)
        {
            if(resultCode != Activity.RESULT_OK)
                getActivity().setIntent(new Intent());
            lifecycle().
                    filter(event -> event == FragmentEvent.RESUME).
                    firstElement().
                    subscribe(isResumed ->
                            loadLocation(null, false),
                            err ->
                    {
                        if(!(err instanceof CancellationException))
                            Logger.log(err);
                    });

        }
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

    @Override
    public void onDetach ()
    {
        super.onDetach();
        //TODO remove dependency
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
                for(BrowserRecord br: _fileList)
                    br.setHostActivity(null);
        }
    }

    @Override
    public void onDestroy()
    {
        cancelReadDirTask();
        _navigHistory.clear();
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
            {
                _fileList.clear();
                _fileList = null;
            }
        }
        _locationsManager = null;
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state)
    {
        super.onSaveInstanceState(state);
        if(_location!=null)
        {
            ArrayList<Path> selectedPaths = getSelectedPaths();
            LocationsManager.storePathsInBundle(state, _location, selectedPaths);
        }
        state.putParcelableArrayList(STATE_NAVIG_HISTORY, new ArrayList<>(_navigHistory));
    }

    public ArrayList<BrowserRecord> getSelectedFiles()
    {
        ArrayList<BrowserRecord> res = new ArrayList<>();
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
                for (BrowserRecord rec: _fileList)
                {
                    if (rec.isSelected())
                        res.add(rec);
                }
        }
        return res;
    }

    public boolean hasSelectedFiles()
    {
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
                for (BrowserRecord rec: _fileList)
                {
                    if (rec.isSelected())
                        return true;
                }
        }
        return false;
    }

    public BrowserRecord findLoadedFileByPath(Path path)
    {
        synchronized (_filesListSync)
        {
        	if(_fileList!=null)
	            for(BrowserRecord f: _fileList)
		            if(path.equals(f.getPath()))
		                return f;        
        }
        return null;
    }

    public ArrayList<Path> getSelectedPaths()
    {
        return FileListViewFragment.getPathsFromRecords(getSelectedFiles());
    }

    public NavigableSet<BrowserRecord> getFileList()
    {
        return _fileList;
    }

    public Object getFilesListSync()
    {
        return _filesListSync;
    }

    public Location getLocation()
    {
        return _location;
    }

    public void copyToAdapter(FileListViewAdapter adapter)
    {
        synchronized (_filesListSync)
        {
            adapter.clear();
            if(_fileList!=null)
                adapter.addAll(_fileList);
        }
    }

    public Observable<LoadLocationInfo> getLocationLoadingObservable()
    {
        return _locationLoading;
    }

    public Observable<BrowserRecord> getLoadRecordObservable()
    {
        return _recordLoadedSubject;
    }

    static class LoadLocationInfo implements Cloneable
    {
        enum Stage
        {
            StartedLoading,
            Loading,
            FinishedLoading
        }
        Stage stage;
        CachedPathInfo folder;
        BrowserRecord file;
        DirectorySettings folderSettings;
        Location location;

        @Override
        public LoadLocationInfo clone()
        {
            try
            {
                return (LoadLocationInfo) super.clone();
            }
            catch (CloneNotSupportedException ignore)
            {
                return null;
            }
        }
    }

    public synchronized void readLocation(Location location, Collection<Path> selectedFiles)
    {
        Logger.debug(TAG + " readCurrentLocation");
        cancelReadDirTask();
        clearCurrentFiles();
        _location = location;
        if (_location == null)
            return;

        FileManagerActivity activity = (FileManagerActivity) getActivity();
        if(activity == null)
            return;
        Context context = activity.getApplicationContext();
        boolean showRootFolder = activity.getIntent().
                getBooleanExtra(
                        FileManagerActivity.EXTRA_ALLOW_SELECT_ROOT_FOLDER,
                        activity.isSelectAction() && activity.allowFolderSelect()
                );
        LoadLocationInfo startInfo = new LoadLocationInfo();
        startInfo.stage = LoadLocationInfo.Stage.StartedLoading;
        startInfo.location = location;
        Logger.debug(TAG + ": _locationLoading.onNext started loading");
        _locationLoading.onNext(startInfo);
        Observable<BrowserRecord> observable = LoadDirSettingsObservable.
                create(location).
                toSingle(new DirectorySettings()).
                onErrorReturn(err -> {
                    Logger.log(err);
                    return new DirectorySettings();
                }).
                map(dirSettings -> {
                    LoadLocationInfo loadLocationInfo = new LoadLocationInfo();
                    loadLocationInfo.stage = LoadLocationInfo.Stage.Loading;
                    loadLocationInfo.folderSettings = dirSettings;
                    if(location.getCurrentPath().isFile())
                    {
                        loadLocationInfo.file = ReadDir.getBrowserRecordFromFsRecord(
                                context,
                                location,
                                location.getCurrentPath(),
                                dirSettings
                        );
                        Location parentLocation = location.copy();
                        parentLocation.setCurrentPath(location.getCurrentPath().getParentPath());
                        loadLocationInfo.location = parentLocation;
                    }
                    else
                        loadLocationInfo.location = location;
                    CachedPathInfo cpi = new CachedPathInfoBase();
                    cpi.init(loadLocationInfo.location.getCurrentPath());
                    loadLocationInfo.folder = cpi;
                    return loadLocationInfo;
                }).
                observeOn(AndroidSchedulers.mainThread()).
                doOnSuccess(loadLocationInfo -> {
                    _directorySettings = loadLocationInfo.folderSettings;
                    Logger.debug(TAG + ": _locationLoading.onNext loading");
                    _locationLoading.onNext(loadLocationInfo);
                }).
                observeOn(Schedulers.io()).
                flatMapObservable(loadLocationInfo -> ReadDir.createObservable(
                    context,
                    loadLocationInfo.location,
                    selectedFiles,
                    loadLocationInfo.folderSettings,
                    showRootFolder

                ));
        if(TEST_READING_OBSERVABLE != null)
        {
            observable = observable.
                    doOnSubscribe(res -> TEST_READING_OBSERVABLE.onNext(true)).
                    doFinally(() -> TEST_READING_OBSERVABLE.onNext(false));
        }

        _readLocationObserver = observable.
                compose(bindUntilEvent(FragmentEvent.DESTROY)).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                doFinally(() -> sendFinishedLoading(location)).
                subscribe(loadedRecord -> {
                            addRecordToList(loadedRecord);
                            _recordLoadedSubject.onNext(loadedRecord);
                        },
                        err ->
                        {
                            if(!(err instanceof CancellationException))
                                Logger.log(err);
                        }
                );

    }

    private void sendFinishedLoading(Location location)
    {
        Logger.debug(TAG + ": _locationLoading.onNext isLoading = false");
        LoadLocationInfo loadLocationInfo = new LoadLocationInfo();
        loadLocationInfo.location = location;
        loadLocationInfo.stage = LoadLocationInfo.Stage.FinishedLoading;
        _locationLoading.onNext(loadLocationInfo);
    }

    public Single<BrowserRecord> makeNewFile(String name, int type)
    {
        return Single.create(emitter -> CreateNewFile.createObservable(
                getActivity().getApplicationContext(),
                getLocation(),
                name,
                type,
                false
        ).compose(bindToLifecycle()).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(rec -> {
                            addRecordToList(rec);
                            if(!emitter.isDisposed())
                                emitter.onSuccess(rec);
                        },
                        err -> Logger.showAndLog(getActivity(), err)));
    }

    public Single<BrowserRecord> createOrFindFile(String name, int type)
    {
        return Single.create(emitter -> CreateNewFile.createObservable(
                getActivity().getApplicationContext(),
                getLocation(),
                name,
                type,
                true
        ).compose(bindToLifecycle()).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(rec -> {
                            if(findLoadedFileByPath(rec.getPath()) == null)
                                addRecordToList(rec);
                            if(!emitter.isDisposed())
                                emitter.onSuccess(rec);
                        },
                        err -> Logger.showAndLog(getActivity(), err)));

    }

    private void addRecordToList(BrowserRecord rec)
    {
        FileManagerActivity fm = (FileManagerActivity) getActivity();
        rec.setHostActivity(fm);
        synchronized (_filesListSync)
        {
            if(_fileList!=null)
                _fileList.add(rec);
        }
    }

    public void reSortFiles()
    {
        LoadLocationInfo loadInfo = new LoadLocationInfo();
        loadInfo.stage = LoadLocationInfo.Stage.StartedLoading;
        loadInfo.location = _location;
        Logger.debug(TAG + ": _locationLoading.onNext started loading (sorting)");
        _locationLoading.onNext(loadInfo);
        synchronized (_filesListSync)
        {
            TreeSet<BrowserRecord> n = new TreeSet<>(initSorter());
            if (_fileList != null)
                n.addAll(_fileList);
            _fileList = n;
        }
        loadInfo = loadInfo.clone();
        loadInfo.stage = LoadLocationInfo.Stage.FinishedLoading;
        Logger.debug(TAG + ": _locationLoading.onNext finished loading (sorting)");
        _locationLoading.onNext(loadInfo);
    }

    public Stack<HistoryItem> getNavigHistory()
    {
        return _navigHistory;
    }

    public void removeLocationFromHistory(Location loc)
    {
        String id = loc.getId();
        if(id!=null)
        {
            List<HistoryItem> cur = new ArrayList<>(_navigHistory);
            for(HistoryItem hi: cur)
                if(id.equals(hi.locationId))
                    _navigHistory.remove(hi);
        }
    }

    public DirectorySettings getDirectorySettings()
    {
        return _directorySettings;
    }

    public static class HistoryItem implements Parcelable
    {
        public static final Creator<HistoryItem> CREATOR = new Creator<HistoryItem>()
        {
            @Override
            public HistoryItem createFromParcel(Parcel in)
            {
                return new HistoryItem(in);
            }

            @Override
            public HistoryItem[] newArray(int size)
            {
                return new HistoryItem[size];
            }
        };

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeParcelable(locationUri, flags);
            parcel.writeInt(scrollPosition);
            parcel.writeString(locationId);
        }

        HistoryItem(){}

        public Uri locationUri;
        public int scrollPosition;
        public String locationId;

        HistoryItem(Parcel p)
        {
            locationUri = p.readParcelable(ClassLoader.getSystemClassLoader());
            scrollPosition = p.readInt();
            locationId = p.readString();
        }
    }

    private static final String STATE_NAVIG_HISTORY = "com.sovworks.eds.android.PATH_HISTORY";

    private static final int REQUEST_CODE_OPEN_LOCATION = 1;

    private LocationsManager _locationsManager;
    private Location _location;
    private DirectorySettings _directorySettings;
    private NavigableSet<BrowserRecord> _fileList;
    private final Object _filesListSync = new Object();
    private final Stack<HistoryItem> _navigHistory = new Stack<>();
    private final Subject<LoadLocationInfo> _locationLoading = BehaviorSubject.create();
    private final Subject<BrowserRecord> _recordLoadedSubject = PublishSubject.create();
    private Disposable _readLocationObserver;

    private synchronized void cancelReadDirTask()
    {
        if(_readLocationObserver != null)
        {
            _readLocationObserver.dispose();
            _readLocationObserver = null;
        }
    }

    private void restoreNavigHistory(Bundle state)
	{
        if (state.containsKey(STATE_NAVIG_HISTORY))
        {
            ArrayList<HistoryItem> l = state.getParcelableArrayList(STATE_NAVIG_HISTORY);
            if(l!=null)
                _navigHistory.addAll(l);
        }
	}

    public void loadLocation(final Bundle savedState, final boolean autoOpen)
	{
		final Uri uri = getLocationUri(getActivity().getIntent(), savedState);
        Location loc = null;
		try
		{
			loc = initLocationFromUri(uri);
		}
		catch(Exception e)
		{
			Logger.showAndLog(getActivity(), e);
		}
		if(loc == null)
            loc = getFallbackLocation();

        if(autoOpen && !LocationsManager.isOpen(loc))
		{
            Intent i = new Intent(getActivity(), OpenLocationsActivity.class);
            LocationsManager.storeLocationsInIntent(i, Collections.singletonList(loc));
            startActivityForResult(i, REQUEST_CODE_OPEN_LOCATION);
		}
		else if(savedState == null)
        {
            resetIntent();
            readLocation(loc, null);
        }
        else
            restoreState(savedState);
	}

    private void resetIntent()
    {
        Intent i = getActivity().getIntent();
        if(i.getAction() == null || Intent.ACTION_MAIN.equals(i.getAction()))
        {
            i.setData(null);
            getActivity().setIntent(i);
        }
    }

    private void clearCurrentFiles()
    {
        synchronized (_filesListSync)
        {
            if(_location!=null)
            {
                ExtendedFileInfoLoader loader = ExtendedFileInfoLoader.getInstance();
                for (BrowserRecord br: _fileList)
                    loader.detachRecord(_location.getId(), br);
            }
            _fileList.clear();
        }
        _directorySettings = null;
        _location = null;
    }

    private void restoreState(Bundle state)
    {
        restoreNavigHistory(state);
        ArrayList<Path> selectedFiles = new ArrayList<>();
        Location loc = _locationsManager.getFromBundle(state, selectedFiles);
        if(loc!=null)
            readLocation(loc, selectedFiles);
    }

    private Location initLocationFromUri(Uri locationUri) throws Exception
	{
		return locationUri != null ?
				_locationsManager.getLocation(locationUri)
			:
				null;
	}

	private Location getFallbackLocation()
	{
		return FileManagerActivity.getStartLocation(getActivity());
	}

    private Comparator<BrowserRecord> initSorter()
	{
        return getComparator(UserSettings.getSettings(getActivity()));
	}
}
