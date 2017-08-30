package com.sovworks.eds.android.filemanager.fragments;


import android.app.Activity;
import android.app.Fragment;
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
import com.sovworks.eds.android.filemanager.comparators.FileSizesComparator;
import com.sovworks.eds.android.filemanager.comparators.ModDateComparator;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.tasks.ReadDirTask;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.android.locations.activities.OpenLocationsActivity;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_DATE_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_DATE_DESC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_FILENAME_DESC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_SIZE_ASC;
import static com.sovworks.eds.settings.SettingsCommon.FB_SORT_SIZE_DESC;

public class FileListDataFragment extends Fragment
{
    public static FileListDataFragment newInstance()
    {
        return new FileListDataFragment();
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
        _sortingComparator = initSorter();
        loadLocation(state, true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        synchronized (_fileList)
        {
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
            _resHandler.addResult(new Runnable()
            {
                @Override
                public void run()
                {
                    loadLocation(null, false);
                }
            });
        }
		else
			super.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void onDetach ()
    {
        super.onDetach();
        synchronized (_fileList)
        {
            for(BrowserRecord br: _fileList)
                br.setHostActivity(null);
        }
    }

    @Override
    public void onDestroy()
    {
        ReadDirTask f = (ReadDirTask) getFragmentManager().findFragmentByTag(ReadDirTask.TAG);
        if(f!=null && f.isAdded())
            f.cancel();
        _sortingComparator = null;
        _resHandler.clear();
        _navigHistory.clear();
        synchronized (_fileList)
        {
            _fileList.clear();
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
        FileListViewFragment fl = getFilesListViewFragment();
        if(fl != null)
            state.putInt(ReadDirTask.ARG_SCROLL_POSITION, fl.getListView().getLastVisiblePosition());
        state.putParcelableArrayList(STATE_NAVIG_HISTORY, new ArrayList<>(_navigHistory));
    }

    public ArrayList<BrowserRecord> getSelectedFiles()
    {
        ArrayList<BrowserRecord> res = new ArrayList<>();
        synchronized (_fileList)
        {
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
        synchronized (_fileList)
        {
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
        for(BrowserRecord f: _fileList)
            if(path.equals(f.getPath()))
                return f;
        return null;
    }

    public ArrayList<Path> getSelectedPaths()
    {
        return FileListViewFragment.getPathsFromRecords(getSelectedFiles());
    }

    public ArrayList<BrowserRecord> getFileList()
    {
        return _fileList;
    }

    public Location getLocation()
    {
        return _location;
    }

    public void addFileToList(BrowserRecord file)
    {
        FileManagerActivity act = (FileManagerActivity) getActivity();
        synchronized (_fileList)
        {
            file.setHostActivity(act);
            _fileList.add(file);
        }
    }

    public void sortFiles()
    {
        if(_sortingComparator!=null)
            synchronized (_fileList)
            {
                Collections.sort(_fileList, _sortingComparator);
            }
    }

    public void copyToAdapter(FileListViewAdapter adapter)
    {
        synchronized (_fileList)
        {
            adapter.clear();
            adapter.addAll(_fileList);
        }
    }

    public ArrayList<Path> getImageFilesInCurrentDir()
    {
        ArrayList<Path> al = new ArrayList<>();
        synchronized (_fileList)
        {
            int length = 0;
            for (BrowserRecord rec: _fileList)
            {
                if (rec.isFile())
                {
                    String mime = FileOpsService.getMimeTypeFromExtension(getActivity(), new StringPathUtil(rec.getName()).getFileExtension());
                    if (mime.startsWith("image/"))
                    {
                        al.add(rec.getPath());
                        length += rec.getPath().getPathString().length();
                        if (length >= 100000)
                            break;
                    }
                }
            }
        }
        return al;
    }


    public void goTo(Location location, int scrollPosition)
    {
        Location prevLocation = getLocation();
        int prevScrollPosition = 0;
        FileListViewFragment fl = getFilesListViewFragment();
        if(fl!=null)
            prevScrollPosition = fl.getListView().getLastVisiblePosition();
		readLocation(location, scrollPosition);
		if(prevLocation!=null)
        {
            Uri uri = prevLocation.getLocationUri();
            if(_navigHistory.empty() || !_navigHistory.lastElement().locationUri.equals(uri))
            {
                HistoryItem hi = new HistoryItem();
                hi.locationUri = uri;
                hi.scrollPosition = prevScrollPosition;
                hi.locationId = prevLocation.getId();
                _navigHistory.push(hi);
            }
        }
    }

    public void goToPrevLocation()
	{
		while(!_navigHistory.isEmpty())
        {
            HistoryItem hi = _navigHistory.pop();
            Uri uri  = hi.locationUri;
            try
            {
                Location loc = _locationsManager.getLocation(uri);
                if(loc!=null && LocationsManager.isOpen(loc))
                {
                    readLocation(loc, hi.scrollPosition);
                    break;
                }
            }
            catch (Exception e)
            {
                Logger.log(e);
            }
        }
	}

    public void setDirectorySettings(DirectorySettings ds)
    {
        _directorySettings = ds;
    }

    public void setCurrentPathRecord(BrowserRecord rec)
    {
        _currentPathRecord = rec;
    }

    public BrowserRecord getCurrentPathRecord()
    {
        return _currentPathRecord;
    }

    public void rereadCurrentLocation()
    {
        int scrollPosition = 0;
        FileListViewFragment fl = getFilesListViewFragment();
        if(fl!=null)
            scrollPosition = fl.getListView().getLastVisiblePosition();
        readLocation(getLocation(), scrollPosition);
    }

    public void readLocation(Location location, int scrollPosition)
    {
        cancelReadDirTask();
        clearCurrentFiles();
        _location = location;
        if(_location == null)
            return;
        FileListViewFragment f = getFilesListViewFragment();
        if(f!=null)
            f.onLocationChanged();
        FileManagerActivity act = (FileManagerActivity) getActivity();
        getFragmentManager().beginTransaction().add(
                ReadDirTask.newInstance(
                        getLocation(),
                        act.getIntent().
                                getBooleanExtra(
                                        FileManagerActivity.EXTRA_ALLOW_SELECT_ROOT_FOLDER,
                                        act.isSelectAction() && act.allowFolderSelect()
                                ),
                        scrollPosition
                ),
                ReadDirTask.TAG
        ).commitAllowingStateLoss();
    }

    public void reSortFiles()
    {
        synchronized (_fileList)
        {
            _sortingComparator = initSorter();
            sortFiles();
        }
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

    private Comparator<BrowserRecord> _sortingComparator;

    private LocationsManager _locationsManager;
    private Location _location;
    private BrowserRecord _currentPathRecord;
    private DirectorySettings _directorySettings;
    private final ArrayList<BrowserRecord> _fileList = new ArrayList<>();
    private final Stack<HistoryItem> _navigHistory = new Stack<>();
    private final ActivityResultHandler _resHandler = new ActivityResultHandler();
    private void cancelReadDirTask()
    {
        ReadDirTask task = (ReadDirTask) getFragmentManager().findFragmentByTag(ReadDirTask.TAG);
        if(task!=null)
            task.cancel();
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

    private void loadLocation(final Bundle savedState, final boolean autoOpen)
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
        {
            loc = getFallbackLocation();
        }
        if(autoOpen && !LocationsManager.isOpen(loc))
		{
            Intent i = new Intent(getActivity(), OpenLocationsActivity.class);
            LocationsManager.storeLocationsInIntent(i, Collections.singletonList(loc));
            startActivityForResult(i, REQUEST_CODE_OPEN_LOCATION);
		}
		else if(savedState == null)
            readLocation(loc, 0);
        else
            restoreState(savedState);
	}

    private  void clearCurrentFiles()
    {
        synchronized (_fileList)
        {
            if(_location!=null)
            {
                ExtendedFileInfoLoader loader = ExtendedFileInfoLoader.getInstance();
                for (BrowserRecord rec : _fileList)
                    loader.detachRecord(_location.getId(), rec);
            }
            _directorySettings = null;
            _fileList.clear();
        }
    }

    private void restoreState(Bundle state)
    {
        restoreNavigHistory(state);
        getFragmentManager().beginTransaction().add(ReadDirTask.newInstance(state), ReadDirTask.TAG).commit();
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

    private FileListViewFragment getFilesListViewFragment()
	{
		FileListViewFragment f = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
		return f!=null && f.isAdded() && f.isResumed() ? f : null;
	}

    private Comparator<BrowserRecord> initSorter()
	{
		switch (UserSettings.getSettings(getActivity()).getFilesSortMode())
		{
		case FB_SORT_FILENAME_ASC:
			return new FileNamesComparator(true);
		case FB_SORT_FILENAME_DESC:
			return new FileNamesComparator(false);
		case FB_SORT_SIZE_ASC:
			return new FileSizesComparator(true);
		case FB_SORT_SIZE_DESC:
			return new FileSizesComparator(false);
		case FB_SORT_DATE_ASC:
			return new ModDateComparator(true);
		case FB_SORT_DATE_DESC:
			return new ModDateComparator(false);
		default:
			return null;
		}
	}
}
