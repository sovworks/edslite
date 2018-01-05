package com.sovworks.eds.android.filemanager.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.FileListViewAdapter;
import com.sovworks.eds.android.filemanager.FileManagerFragment;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.filemanager.dialogs.DeleteConfirmationDialog;
import com.sovworks.eds.android.filemanager.dialogs.NewFileDialog;
import com.sovworks.eds.android.filemanager.dialogs.RenameFileDialog;
import com.sovworks.eds.android.filemanager.dialogs.SortDialog;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.ExecutableFileRecord;
import com.sovworks.eds.android.filemanager.tasks.CopyToClipboardTask;
import com.sovworks.eds.android.filemanager.tasks.CreateNewFile;
import com.sovworks.eds.android.filemanager.tasks.OpenAsContainerTask;
import com.sovworks.eds.android.filemanager.tasks.PrepareToSendTask;
import com.sovworks.eds.android.filemanager.tasks.RenameFileTask;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.fs.ContentResolverFs;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.locations.ContentResolverLocation;
import com.sovworks.eds.android.locations.DocumentTreeLocation;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.android.providers.MainContentProvider;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstGroup;
import com.sovworks.eds.fs.util.SrcDstPlain;
import com.sovworks.eds.fs.util.SrcDstRec;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.GlobalConfig;
import com.trello.rxlifecycle2.android.FragmentEvent;
import com.trello.rxlifecycle2.components.RxFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static com.sovworks.eds.android.settings.UserSettingsCommon.FILE_BROWSER_SORT_MODE;

public abstract class FileListViewFragmentBase extends RxFragment implements
        SortDialog.SortingReceiver,
        FileManagerFragment,
        LocationOpenerBaseFragment.LocationOpenerResultReceiver,
        NewFileDialog.Receiver
{
    public static final String TAG = "com.sovworks.eds.android.filemanager.fragments.FileListViewFragment";

    public static final int REQUEST_CODE_SELECT_FROM_CONTENT_PROVIDER = Activity.RESULT_FIRST_USER;
    public static final String ARG_SCROLL_POSITION = "com.sovworks.eds.android.SCROLL_POSITION";

    public static ArrayList<Path> getPathsFromRecords(List<? extends BrowserRecord> records)
	{
		ArrayList<Path> res = new ArrayList<>();
		for(BrowserRecord rec: records)
			res.add(rec.getPath());
		return res;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Logger.debug(TAG + " onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        _locationsManager = LocationsManager.getLocationsManager(getActivity());
        initListView();
        if(savedInstanceState!=null)
            _scrollPosition = savedInstanceState.getInt(ARG_SCROLL_POSITION, 0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
        Logger.debug(TAG + " onCreateView");
        View view = inflater.inflate(R.layout.file_list_view_fragment, container, false);
        _selectedFileEditText = view.findViewById(R.id.selected_file_edit_text);
        _listView = view.findViewById(android.R.id.list);
        if(showSelectedFilenameEditText())
        {
            _selectedFileEditText.setVisibility(View.VISIBLE);
            _selectedFileEditText.addTextChangedListener(new TextWatcher()
            {
                public void afterTextChanged(Editable arg0)
                {
                    if(arg0 == null || _changingSelectedFileText)
                        return;
                    String s = arg0.toString();
                    if(s.isEmpty())
                    {
                        if(isInSelectionMode())
                            stopSelectionMode();
                    }
                    else
                    {
                        if(isInSelectionMode())
                        {
                            clearSelectedFlag();
                            _actionMode.invalidate();
                        }
                        else
                            startSelectionMode();
                    }
                }

                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
                {
                }

                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
                {
                }
            });
        }
        else
            _selectedFileEditText.setVisibility(View.GONE);
        _currentPathTextView = view.findViewById(R.id.current_path_text);
        return view;
    }

    @Override
    public void onStart()
    {
        Logger.debug(TAG + " onStart");
        super.onStart();
        ListView lv = getListView();
        FileListViewAdapter adapter = new FileListViewAdapter(getActivity());
        lv.setAdapter(adapter);
        _isReadingLocation = true;
        _locationLoadingObserver = getFileListDataFragment().
                getLocationLoadingObservable().
                observeOn(AndroidSchedulers.mainThread()).
                compose(bindToLifecycle()).
                subscribe(loadInfo -> {
                    switch (loadInfo.stage)
                    {
                        case StartedLoading:
                            setStartedLoading(loadInfo);
                            break;
                        case Loading:
                            if(!_isReadingLocation)
                                setStartedLoading(loadInfo);
                            setLocationLoading(loadInfo);
                            break;
                        case FinishedLoading:
                            if(_isReadingLocation)
                                setLocationNotLoading();
                            break;
                    }
                }, err ->
                {
                    if(!(err instanceof CancellationException))
                        Logger.log(err);
                });
    }

    @Override
    public void onStop()
    {
        Logger.debug(TAG + " onStop");

        getListView().setAdapter(null);
        if(_locationLoadingObserver !=null)
        {
            _locationLoadingObserver.dispose();
            _locationLoadingObserver = null;
        }
        _isReadingLocation = false;
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_SCROLL_POSITION, getListView().getFirstVisiblePosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
    {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.file_list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        boolean isReading = _isReadingLocation;
        boolean isSendAction = isSendAction();
        boolean hasInClipboard = hasSelectionInClipboard();
        boolean isSelectAction = isSelectAction();
        Logger.debug(String.format("onPrepareOptionsMenu: isReading=%b isSendAction=%b hasInClipboard=%b isSelectAction=%b",
                isReading, isSendAction, hasInClipboard, isSelectAction));

        menu.findItem(R.id.progressbar).setVisible(isReading);
        menu.findItem(R.id.copy).setVisible(!isReading && !isSelectAction && (isSendAction || hasInClipboard));
        menu.findItem(R.id.move).setVisible(!isReading && !isSelectAction && hasInClipboard);

        menu.findItem(R.id.new_file).setVisible(
                !isReading
                        && !isSendAction
                        && allowCreateNewFile()
                        && (!isSelectAction || getFileManagerActivity().allowFileSelect())
        );
        menu.findItem(R.id.new_dir).setVisible(
                !isReading
                        && allowCreateNewFolder()
        );
        menu.findItem(R.id.select_all).setVisible((!isSelectAction || !isSingleSelectionMode()) && !getSelectableFiles().isEmpty());
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{
        MenuHandlerInfo mhi = new MenuHandlerInfo();
        mhi.menuItemId = menuItem.getItemId();
        boolean res = handleMenu(mhi);
        if(res && mhi.clearSelection)
        {
            clearSelectedFlag();
            onSelectionChanged();
        }
        return res || super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_CODE_SELECT_FROM_CONTENT_PROVIDER)
        {
            if(resultCode == Activity.RESULT_OK && data!=null)
            {
                returnSelectionFromContentProvider(data);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && data.getData()!=null)
                {
                    getActivity().getContentResolver().takePersistableUriPermission(
                            data.getData(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(haveSelectedFiles())
        {
            startSelectionMode();
            if(_actionMode!=null) //sometimes it is null
                _actionMode.invalidate();
        }
        _cleanSelectionOnModeFinish = true;
        ExtendedFileInfoLoader.getInstance().resumeViewUpdate();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        ExtendedFileInfoLoader.getInstance().pauseViewUpdate();
        _cleanSelectionOnModeFinish = false;
        if(isInSelectionMode())
            stopSelectionMode();
    }


    @Override
    public void onDestroyView ()
    {
        _selectedFileEditText = null;
        _currentPathTextView = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy()
    {
        _locationsManager = null;
        super.onDestroy();
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    public void applySort(int sortMode)
    {
        SharedPreferences.Editor e = UserSettings.getSettings(getActivity())
                .getSharedPreferences()
                .edit();
        if(sortMode == 0)
            e.remove(FILE_BROWSER_SORT_MODE);
        else
            e.putInt(UserSettings.FILE_BROWSER_SORT_MODE, sortMode);
        e.commit();
        getFileListDataFragment().reSortFiles();
        updateSelectionMode();
    }

    public void renameFile(String path, String newName)
    {
        try
		{
			final Location loc = getRealLocation();
			Path curPath = loc.getFS().getPath(path);
			Location srcLoc = loc.copy();
			srcLoc.setCurrentPath(curPath);
            getFragmentManager().
                    beginTransaction().
                    add(
                            RenameFileTask.newInstance(srcLoc, newName),
                            RenameFileTask.TAG
                    ).
                    commit();
		}
		catch (IOException e)
		{
			Logger.showAndLog(getActivity(), e);
		}
    }

    @Override
    public void makeNewFile(String name, int type)
    {
        getFileListDataFragment().makeNewFile(name, type).
                compose(bindToLifecycle()).
                subscribe(res -> {
                    FileListViewAdapter adapter = getAdapter();
                    if(adapter!=null)
                        adapter.add(res);
                    newRecordCreated(res);
                }, err ->
                {
                    if(!(err instanceof CancellationException))
                        Logger.log(err);
                });
    }

    public void deleteFiles(Location loc, List<Path> paths, boolean wipe)
    {
        if (wipe)
        {
            SrcDstCollection targets = SrcDstRec.fromPathsNoDest(loc, true, paths);
            FileOpsService.wipeFiles(getActivity(), targets);
        }
        else
        {
            SrcDstCollection targets = isLocSupportsRecFolderDelete(loc) ?
                    SrcDstPlain.fromPaths(loc, null, paths) :
                    SrcDstRec.fromPathsNoDest(loc, true, paths);
            FileOpsService.deleteFiles(getActivity(), targets);
        }
        Toast.makeText(getActivity(), R.string.file_operation_started, Toast.LENGTH_SHORT).show();
    }


    public void selectFile(BrowserRecord file)
    {
        if(isSelectAction() && isSingleSelectionMode())
            clearSelectedFlag();
        file.setSelected(true);
        if(_actionMode == null)
            startSelectionMode();
        file.updateView();
        onSelectionChanged();
    }

    public void unselectFile(BrowserRecord file)
    {
        file.setSelected(false);
        if(!haveSelectedFiles() && !isSelectAction())
            stopSelectionMode();
        else
            file.updateView();
        onSelectionChanged();
    }

    public TaskFragment.TaskCallbacks getOpenAsContainerTaskCallbacks()
    {
        return new ProgressDialogTaskFragmentCallbacks(getActivity(), R.string.loading)
        {
            @Override
            public void onCompleted(Bundle args, TaskFragment.Result result)
            {
                try
                {
                    final Location locToOpen = (Location) result.getResult();
                    if(locToOpen != null)
                        lifecycle().
                                filter(event -> event == FragmentEvent.RESUME).
                                firstElement().
                                subscribe(res -> openLocation(locToOpen), err ->
                                {
                                    if(!(err instanceof CancellationException))
                                        Logger.log(err);
                                });
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(getActivity(), e);
                }
            }
        };
    }

    public boolean isInSelectionMode()
    {
        return _actionMode!=null;
    }

    public void updateOptionsMenu()
    {
        //getFileManagerActivity().updateOptionsMenu();
        //if(_optionsMenu!=null)
        //    onPrepareOptionsMenu(_optionsMenu);
        getFileManagerActivity().invalidateOptionsMenu();
    }

    public static final String ARG_WIPE_FILES = "com.sovworks.eds.android.WIPE_FILES";

    protected EditText _selectedFileEditText;
    protected TextView _currentPathTextView;
    protected LocationsManager _locationsManager;
    protected ActionMode _actionMode;
    private ListView _listView;
    private Disposable _locationLoadingObserver, _loadingRecordObserver;
    private int _scrollPosition;

    protected boolean _isReadingLocation, _changingSelectedFileText, _cleanSelectionOnModeFinish;

    private boolean isLocSupportsRecFolderDelete(Location loc)
    {
        return loc instanceof DocumentTreeLocation;
    }

    @Override
    public boolean onBackPressed()
    {
        return goToPrevLocation();
    }

    private boolean goToPrevLocation()
    {
        Stack<FileListDataFragment.HistoryItem> hs = getFileListDataFragment().getNavigHistory();
        while(!hs.isEmpty())
        {
            FileListDataFragment.HistoryItem hi = hs.pop();
            Uri uri  = hi.locationUri;
            try
            {
                Location loc = _locationsManager.getLocation(uri);
                if(loc!=null && LocationsManager.isOpen(loc))
                {
                    readLocation(getFileListDataFragment(), loc, hi.scrollPosition);
                    return true;
                }
            }
            catch (Exception e)
            {
                Logger.log(e);
            }
        }
        return false;
    }

    public void goTo(Location location, int scrollPosition, boolean addToHistory)
    {
        Location prevLocation = addToHistory ? getLocation() : null;
        int prevScrollPosition = getListView().getLastVisiblePosition();
        FileListDataFragment df = getFileListDataFragment();
        readLocation(df, location, scrollPosition);
        if (prevLocation != null)
        {
            Uri uri = prevLocation.getLocationUri();
            Stack<FileListDataFragment.HistoryItem> nh = df.getNavigHistory();
            if (nh.empty() || !nh.lastElement().locationUri.equals(uri))
            {
                FileListDataFragment.HistoryItem hi = new FileListDataFragment.HistoryItem();
                hi.locationUri = uri;
                hi.scrollPosition = prevScrollPosition;
                hi.locationId = prevLocation.getId();
                nh.push(hi);
            }
        }
    }

    //call from main thread
    public void rereadCurrentLocation()
    {        Logger.debug(TAG + "rereadCurrentLocation");
        int scrollPosition = getListView().getLastVisiblePosition();
        goTo(getLocation(), scrollPosition, false);

    }

    static
    {
        if(GlobalConfig.isTest())
            TEST_READING_OBSERVABLE = BehaviorSubject.createDefault(false);
    }

    public static Subject<Boolean> TEST_READING_OBSERVABLE;

    private void setStartedLoading(FileListDataFragment.LoadLocationInfo loadInfo)
    {
        if(TEST_READING_OBSERVABLE != null)
            TEST_READING_OBSERVABLE.onNext(true);
        Logger.debug(TAG + ": Started loading " + loadInfo.location.getLocationUri());
        _isReadingLocation = true;
        FileListViewAdapter adapter = getAdapter();
        adapter.clear();
        adapter.setCurrentLocationId(loadInfo.location.getId());
        _currentPathTextView.setText("");
        _selectedFileEditText.setVisibility(View.GONE);
        if(_actionMode!=null)
            _actionMode.invalidate();
        else
            updateOptionsMenu();
    }

    private void setLocationLoading(FileListDataFragment.LoadLocationInfo loadInfo)
    {
        Logger.debug(TAG + ": Loading " + loadInfo.location.getLocationUri());
        FileListViewAdapter adapter = getAdapter();
        adapter.clear();
        if(_loadingRecordObserver!=null && !_loadingRecordObserver.isDisposed())
        {
            if(GlobalConfig.isDebug())
                throw new RuntimeException("Loading record observer was not disposed!");
            else
                _loadingRecordObserver.dispose();
        }
        _loadingRecordObserver = getFileListDataFragment().getLoadRecordObservable().
                    compose(bindToLifecycle()).
                    buffer(200, TimeUnit.MILLISECONDS).
                    filter(records -> !records.isEmpty()).
                    compose(bindToLifecycle()).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(adapter::addAll, err ->
                    {
                        if(!(err instanceof CancellationException))
                            Logger.log(err);
                    });
        if(loadInfo.folder!=null)
            updateCurrentFolderLabel(loadInfo.folder);
        showFileIfNeeded(loadInfo.file);
    }

    private void setLocationNotLoading()
    {
        Logger.debug(TAG + ": Finished loading");
        if(_loadingRecordObserver!=null)
            _loadingRecordObserver.dispose();
        FileListViewAdapter adapter = getAdapter();
        getFileListDataFragment().copyToAdapter(adapter);
        _isReadingLocation = false;
        _selectedFileEditText.setVisibility(showSelectedFilenameEditText() ? View.VISIBLE : View.GONE);
        if(_actionMode!=null)
            _actionMode.invalidate();
        else
            updateSelectionMode();
        if(_scrollPosition > 0)
        {
            int sp = _scrollPosition;
            _scrollPosition = 0;
            Completable.
                    timer(50, TimeUnit.MILLISECONDS, Schedulers.computation()).
                    observeOn(AndroidSchedulers.mainThread()).
                    compose(bindToLifecycle()).
                    subscribe(() -> scrollList(sp), err -> {});
        }
        if(TEST_READING_OBSERVABLE != null)
            TEST_READING_OBSERVABLE.onNext(false);
    }

    private void readLocation(FileListDataFragment df, Location loc, int scrollPosition)
    {
        readLocationAndScroll(df, loc, scrollPosition);
    }

    private void updateCurrentFolderLabel(CachedPathInfo currentFolder)
    {
        _currentPathTextView.setText(currentFolder.getPathDesc());
    }

    private void showFileIfNeeded(BrowserRecord file)
    {
        if(file!=null)
        {
            FileManagerActivity activity = getFileManagerActivity();
            if (activity != null)
            {
                file.setHostActivity(activity);
                try
                {
                    Logger.debug(TAG + ": Opening file " + file.getPathDesc());
                    file.open();
                }
                catch (Exception e)
                {
                    Logger.showAndLog(activity, e);
                }
            }
        }
    }

    private void readLocationAndScroll(FileListDataFragment df, Location loc, int scrollPosition)
    {
        _scrollPosition = scrollPosition;
        df.readLocation(loc, null);
    }



    private void scrollList(int scrollPosition)
    {
        if(scrollPosition > 0)
        {
            ListView lv = getListView();
            if(lv.getFirstVisiblePosition() == 0)
            {
                int num = lv.getCount();
                int sp = scrollPosition;
                if(scrollPosition >= num)
                    sp = num - 1;
                if(sp >= 0)
                    //lv.setSelection(sp);
                    lv.smoothScrollToPosition(sp);
            }
        }
    }

    private void updateSelectionMode()
    {
        if(haveSelectedFiles())
            startSelectionMode();
        else
        {
            getFileManagerActivity().showProperties(null, true);
            updateOptionsMenu();
        }
    }

    @Override
    public void onTargetLocationOpened(Bundle openerArgs, Location location)
    {
        FileManagerActivity.openFileManager((FileManagerActivity)getActivity(), location, 0);
    }

    @Override
    public void onTargetLocationNotOpened(Bundle openerArgs)
    {

    }

    static class MenuHandlerInfo
    {
        int menuItemId;
        boolean clearSelection;
    }

    protected FileListViewAdapter getAdapter()
    {
        return (FileListViewAdapter) getListView().getAdapter();
    }

    public @NonNull ListView getListView()
    {
        return _listView == null ? new ListView(getActivity()) : _listView;
    }

    protected void initListView()
    {
        ListView lv = getListView();
        //noinspection ConstantConditions
        lv.setEmptyView(getView().findViewById(android.R.id.empty));
        lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
        lv.setItemsCanFocus(true);

        lv.setOnItemLongClickListener((adapterView, view, pos, itemId) ->
        {
            BrowserRecord rec = (BrowserRecord) adapterView.getItemAtPosition(pos);
            if (rec != null && rec.allowSelect())
                selectFile(rec);
            return true;
        });
        lv.setOnItemClickListener((adapterView, view, pos, l) ->
        {
            BrowserRecord rec = (BrowserRecord) adapterView.getItemAtPosition(pos);
            if(rec!=null)
            {
                if(rec.isSelected())
                {
                    if(!isSelectAction() || !isSingleSelectionMode())
                        unselectFile(rec);
                }
                else if(rec.allowSelect() && (_actionMode!=null || (isSelectAction() && rec.isFile())))
                     selectFile(rec);
                else
                     onFileClicked(rec);
            }
        });
    }

    protected void onFileClicked(BrowserRecord file)
    {
        try
        {
            if(getFileManagerActivity().isWideScreenLayout())
                file.openInplace();
            else
                file.open();
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    @NonNull
    protected FileListDataFragment getFileListDataFragment()
    {
        return (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
    }

    protected ArrayList<BrowserRecord> getSelectedFiles()
	{
        ArrayList<BrowserRecord> selectedRecordsList = new ArrayList<>();
        ListView lv = getListView();
        int count = lv.getCount();
        for(int i=0; i<count;i++)
        {
            BrowserRecord file = (BrowserRecord) lv.getItemAtPosition(i);
            if (file.isSelected())
                selectedRecordsList.add(file);
        }
        return selectedRecordsList;
	}

    protected Collection<BrowserRecord> getSelectableFiles()
    {
        ArrayList<BrowserRecord> selectableFilesList = new ArrayList<>();
        ListView lv = getListView();
        int count = lv.getCount();
        for(int i=0; i<count;i++)
        {
            BrowserRecord file = (BrowserRecord) lv.getItemAtPosition(i);
            if (file.allowSelect())
                selectableFilesList.add(file);
        }
        return selectableFilesList;
    }

    protected ArrayList<Path> getSelectedPaths()
	{
		return getPathsFromRecords(getSelectedFiles());
	}

    protected boolean haveSelectedFiles()
    {
        ListView lv = getListView();
        int count = lv.getCount();
        for(int i=0; i<count;i++)
        {
            BrowserRecord file = (BrowserRecord) lv.getItemAtPosition(i);
            if (file.isSelected())
                return true;
        }
        return false;
    }

    protected void startSelectionMode()
    {
        _actionMode = getListView().startActionMode(new ActionMode.Callback()
        {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                if(isSendAction() || _isReadingLocation)
                    return false;
                mode.getMenuInflater().inflate(R.menu.file_list_context_menu, menu);
                ((FileListViewAdapter)getListView().getAdapter()).notifyDataSetChanged();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu)
            {
                ArrayList<BrowserRecord> selectedFiles = getSelectedFiles();
                boolean hasSelectedFiles = (
                        isSelectAction() &&
                        isSingleSelectionMode() &&
                        (allowCreateNewFile() || allowCreateNewFolder()) &&
                        !_selectedFileEditText.getText().toString().isEmpty()
                ) || !selectedFiles.isEmpty();

                boolean isSelectAction = isSelectAction();
                menu.findItem(R.id.select).setVisible(
                                isSelectAction &&
                                hasSelectedFiles &&
                                (!showSelectedFilenameEditText() || allowSelectedFileName())
                );
                menu.findItem(R.id.rename).setVisible(!isSelectAction && selectedFiles.size() == 1);
                menu.findItem(R.id.open_as_container).setVisible(!isSelectAction &&
                        selectedFiles.size() == 1 &&
                        selectedFiles.get(0) instanceof ExecutableFileRecord
                );
                menu.findItem(R.id.copy_to_temp).setVisible(!isSelectAction && getLocation().isEncrypted());
                menu.findItem(R.id.choose_for_operation).setVisible(!isSelectAction);
                menu.findItem(R.id.delete).setVisible(!isSelectAction);
                Location loc = getRealLocation();
                menu.findItem(R.id.wipe).setVisible(
                        !isSelectAction &&
                                loc!=null &&
                                !loc.isEncrypted() &&
                                !loc.isReadOnly()
                );
                menu.findItem(R.id.properties).setVisible(!selectedFiles.isEmpty());
                menu.findItem(R.id.send).setVisible(!isSelectAction);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item)
            {
                MenuHandlerInfo mhi = new MenuHandlerInfo();
                mhi.menuItemId = item.getItemId();
                boolean res = handleMenu(mhi);
                if(res && mhi.clearSelection)
                    mode.finish();

                return res;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                if(_cleanSelectionOnModeFinish)
                {
                    clearSelectedFlag();
                    _actionMode = null;
                    onSelectionChanged();
                }
                else
                    _actionMode = null;
            }

        });
    }

    protected void stopSelectionMode()
    {
        if(_actionMode!=null)
            _actionMode.finish();
    }

    protected void clearSelectedFlag()
    {
        ListView lv = getListView();
        int count = lv.getCount();
        for(int i=0; i<count;i++)
        {
            BrowserRecord file = (BrowserRecord) lv.getItemAtPosition(i);
            if (file.isSelected())
                file.setSelected(false);
        }
        ((FileListViewAdapter)lv.getAdapter()).notifyDataSetChanged();
    }

    protected boolean handleMenu(MenuHandlerInfo mhi)
    {
        switch (mhi.menuItemId)
        {
            case R.id.select:
                returnSelectedFiles();
                mhi.clearSelection = true;
                return true;
            case R.id.new_file:
                showNewFileDialog(false);
                return true;
            case R.id.new_dir:
                showNewFileDialog(true);
                return true;
            case R.id.rename:
                showRenameDialog();
                mhi.clearSelection = true;
                return true;
            case R.id.open_as_container:
                openFileAsContainer();
                mhi.clearSelection = true;
                return true;
            case R.id.choose_for_operation:
                chooseFilesForOperation();
                mhi.clearSelection = true;
                return true;
            case R.id.delete:
                confirmDelete(false);
                mhi.clearSelection = true;
                return true;
            case R.id.wipe:
                confirmDelete(true);
                mhi.clearSelection = true;
                return true;
            case R.id.copy:
                if(isSendAction())
                    pasteSentFiles();
                else
                    pasteFiles(false);
                return true;
            case R.id.move:
                pasteFiles(true);
                return true;
            case R.id.properties:
                showProperties();
                return true;
            case R.id.copy_to_temp:
                copyToTemp();
                mhi.clearSelection = true;
                return true;
            case R.id.sort:
                changeSortMode();
                return true;
            case R.id.select_all:
                selectAllFiles();
                return true;
            case R.id.send:
                sendFiles();
                mhi.clearSelection = true;
                return true;
            default:
                return false;
        }
    }

    protected void showNewFileDialog(boolean isDir)
    {
        NewFileDialog.showDialog(getFragmentManager(), isDir ?
                CreateNewFile.FILE_TYPE_FOLDER :
                CreateNewFile.FILE_TYPE_FILE,
                getTag()
        );
    }

    //full version compat
    protected void newRecordCreated(BrowserRecord rec)
    {

    }

    protected FileManagerActivity getFileManagerActivity()
    {
        return (FileManagerActivity) getActivity();
    }

    protected Location getLocation()
    {
        return getFileManagerActivity().getLocation();
    }

    protected Location getRealLocation()
	{
		return getFileManagerActivity().getRealLocation();
	}

    protected boolean isSelectAction()
    {
        return getFileManagerActivity().isSelectAction();
    }

    protected boolean isSingleSelectionMode()
    {
        return getFileManagerActivity().isSingleSelectionMode();
    }

    protected boolean allowCreateNewFile()
    {
        return getActivity().getIntent().getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_CREATE_NEW_FILE, true);
    }

    protected boolean allowCreateNewFolder()
    {
        return getActivity().getIntent().getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_CREATE_NEW_FOLDER, true);
    }

    protected boolean isSendAction()
    {
        String action = getActivity().getIntent().getAction();
        return Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action);
    }

    /*
    private void initContentValuesFromPath(ContentValues values, Path path)
    {
        values.put(MainContentProvider.COLUMN_LOCATION, getRealLocation().getLocationUri().toString());
        values.put(MainContentProvider.COLUMN_PATH, path.getPathString());
    }*/

    protected void chooseFilesForOperation()
	{
        /*ContentResolver cr = getActivity().getContentResolver();
        cr.delete(MainContentProvider.getCurrentSelectionUri(), null, null);
        ArrayList<Path> recs = getSelectedPaths();
        ContentValues cv = new ContentValues();
        for(Path path: recs)
        {
            initContentValuesFromPath(cv, path);
            cr.update(MainContentProvider.getCurrentSelectionUri(), cv, null, null);
        }
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newUri(cr, recs.size() + " files are in clipboard", MainContentProvider.getCurrentSelectionUri()));
        getActivity().invalidateOptionsMenu();*/
		getFragmentManager().
                beginTransaction().
                add(
                        CopyToClipboardTask.newInstance(
                                getRealLocation(),
                                getSelectedPaths()
                        ),
                        CopyToClipboardTask.TAG
                ).
                commit();
	}

    protected boolean allowSelectedFileName()
    {
        return true;
        //The following check doesn't work with locations in which a path can point only to an existing file.
        //So if the filename is not valid, we'll return a error from "createObservable new file" task.
        /*try
        {
            String filename = _selectedFileEditText.getText().toString();
            if(filename.length() == 0)
                return false;
            Location loc = getFileManagerActivity().getTargetLocation();
            if (loc != null)
            {
                //check if the filename is valid
                loc.getCurrentPath().combine(filename);
                return true;
            }
        }
        catch (IOException ignored)
        {

        }
        return false;*/
    }

    protected void confirmDelete(boolean wipe)
	{
		Bundle b = new Bundle();
		LocationsManager.storePathsInBundle(b, getRealLocation(), getSelectedPaths());
		b.putBoolean(ARG_WIPE_FILES, wipe);
		DeleteConfirmationDialog.showDialog(getFragmentManager(), b);
	}

    protected void onSelectionChanged()
    {
        ArrayList<BrowserRecord> sr = getSelectedFiles();
        if(showSelectedFilenameEditText())
        {
            String name;
            if(sr.isEmpty())
                name = "";
            else
                name = sr.get(0).getName();

            _changingSelectedFileText = true;
            try
            {
                _selectedFileEditText.setText(name);
            }
            finally
            {
                _changingSelectedFileText = false;
            }
        }
        if(_actionMode!=null)
           _actionMode.invalidate();
        updateOptionsMenu();
        getFileManagerActivity().showProperties(null, true);
    }

    protected void returnSelectionFromContentProvider(Intent data)
    {
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    protected void returnSelectedFiles()
    {
        List<BrowserRecord> selectedRecs = getSelectedFiles();
        if(showSelectedFilenameEditText())
        {
            String fn = _selectedFileEditText.getText().toString();
            if(fn.length() > 0)
            {
                if(selectedRecs.isEmpty() || !fn.equals(selectedRecs.get(0).getName()))
                {
                    getFileListDataFragment().createOrFindFile(fn,
                            allowCreateNewFile() ?
                                    CreateNewFile.FILE_TYPE_FILE :
                                    CreateNewFile.FILE_TYPE_FOLDER
                    ).
                            compose(bindToLifecycle()).
                            subscribe(rec -> {
                                FileListViewAdapter adapter = getAdapter();
                                if(adapter!=null)
                                    adapter.add(rec);
                                FileManagerActivity act = getFileManagerActivity();
                                if(act != null)
                                {
                                    Location loc = act.getRealLocation().copy();
                                    loc.setCurrentPath(rec.getPath());
                                    Intent i = new Intent();
                                    LocationsManager.storePathsInIntent(
                                            i,
                                            loc,
                                            Collections.singletonList(rec.getPath())
                                    );
                                    act.setResult(Activity.RESULT_OK, i);
                                    act.finish();
                                }

                            }, err ->
                            {
                                if(!(err instanceof CancellationException))
                                    Logger.log(err);
                            });
                    return;
                }
            }
        }
        List<Path> paths = getPathsFromRecords(selectedRecs);
        if (paths.size() > 0)
        {
            getActivity().setResult(Activity.RESULT_OK, getSelectResult(paths));
            getActivity().finish();
        }
    }

    protected Intent getSelectResult(List<Path> paths)
    {
        Location loc = getRealLocation();
        Intent intent = new Intent();
        if(!isSingleSelectionMode())
            intent.setData(loc.getLocationUri());
        else if(paths.size()>0)
        {
            Location res = loc.copy();
            res.setCurrentPath(paths.get(0));
            intent.setData(res.getLocationUri());
        }
        Bundle b = new Bundle();
        LocationsManager.storePathsInBundle(b, loc, paths);
        intent.putExtras(b);
        return intent;
    }

    protected void showRenameDialog()
    {
        BrowserRecord br = getSelectedFiles().get(0);
        String name = br.getName();
        RenameFileDialog.showDialog(getFragmentManager(), br.getPath().getPathString(), name);
    }

    protected void sendFiles()
    {
        getFragmentManager().
                beginTransaction().
                add(
                        PrepareToSendTask.newInstance(
                                getRealLocation(),
                                getSelectedPaths()
                        ),
                        PrepareToSendTask.TAG
                ).
                commit();
    }

    private void pasteSentFiles()
    {
        try
        {
            SrcDstCollection recs = getSrcDsts(
                    new ContentResolverLocation(getActivity()),
                    false,
                    ContentResolverFs.fromSendIntent(
                            getActivity().getIntent(),
                            getActivity().getContentResolver()
                    )
            );
            FileOpsService.copyFiles(getActivity(), recs, false);
            Toast.makeText(getActivity(), R.string.file_operation_started, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        catch (IOException e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    private boolean hasSelectionInClipboard()
    {
        return MainContentProvider.hasSelectionInClipboard((ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
    }

    private void pasteFiles(boolean move)
    {
        ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboard!=null)
        {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null)
            {
                try
                {
                    SrcDstCollection recs = getSrcDstsFromClip(
                            _locationsManager,
                            clip,
                            getRealLocation(),
                            move
                    );
                    if (recs != null)
                    {
                        if (move)
                            FileOpsService.moveFiles(getActivity(), recs, false);
                        else
                            FileOpsService.copyFiles(getActivity(), recs, false);
                        Toast.makeText(getActivity(), R.string.file_operation_started, Toast.LENGTH_SHORT).show();
                    }
                    //cr.delete(MainContentProvider.getCurrentSelectionUri(), null, null);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Empty", ""));
                    updateOptionsMenu();
                }
                catch (Exception e)
                {
                    Logger.showAndLog(getActivity(), e);
                }
            }
        }
    }

    private static SrcDstCollection getSrcDstsFromClip(
            LocationsManager lm,
            ClipData clip,
            Location dstLocation,
            boolean move) throws Exception
    {
        ArrayList<SrcDstCollection> cols = new ArrayList<>();
        for(int i=0;i<clip.getItemCount();i++)
            addSrcDstsFromClipItem(lm, clip.getItemAt(i), dstLocation, cols, move);

        return cols.isEmpty() ? null : new SrcDstGroup(cols);
    }

    private static void addSrcDstsFromClipItem(
            LocationsManager lm,
            ClipData.Item item,
            Location dstLocation,
            Collection<SrcDstCollection> cols,
            boolean move) throws Exception
    {
        Uri uri = item.getUri();
        if(uri == null || !MainContentProvider.isClipboardUri(uri))
            return;
        Location srcLoc = lm.getLocation(MainContentProvider.getLocationUriFromProviderUri(uri));
        if(move && srcLoc.getFS() == dstLocation.getFS())
            cols.add(new SrcDstSingle(srcLoc, dstLocation));
        else
        {
            SrcDstRec sdr = new SrcDstRec(new SrcDstSingle(srcLoc, dstLocation));
            sdr.setIsDirLast(false);//move);
            cols.add(sdr);
        }
        /*Cursor cur = cr.query(uri, null, null, null, null);
        if(cur!=null)
        {
            try
            {
                int ci = cur.getColumnIndex(MainContentProvider.COLUMN_LOCATION);
                while (cur.moveToNext())
                {
                    Location srcLoc = lm.getTargetLocation(Uri.parse(cur.getString(ci)));
                    SrcDstRec sdr = new SrcDstRec(srcLoc, dstLocation);
                    sdr.setIsDirLast(isDirLast);
                    cols.add(sdr);
                }
            }
            finally
            {
                cur.close();
            }
        }*/
    }

    @SuppressWarnings("SameParameterValue")
    private SrcDstCollection getSrcDsts(Location srcLocation, boolean isDirLast, Collection<? extends Path> paths) throws IOException
    {
        return SrcDstRec.fromPaths(srcLocation, getRealLocation(), isDirLast, paths);
    }

    private void showProperties()
    {
        getFileManagerActivity().showProperties(null, false);
    }

    private void copyToTemp()
    {
        ArrayList<Path> filesToCopy = getSelectedPaths();
        if (filesToCopy.size() > 0)
            FileOpsService.prepareTempFile(getActivity(), getRealLocation(), filesToCopy);
    }

    private void changeSortMode()
    {
        int mode = UserSettings.getSettings(getActivity()).getFilesSortMode();
        SortDialog.showDialog(getFragmentManager(), mode, getTag());
    }

    private void openFileAsContainer()
    {
        BrowserRecord br = getSelectedFiles().get(0);
        Location loc = getRealLocation();
        if(loc == null)
            return;
        loc = loc.copy();
        loc.setCurrentPath(br.getPath());
        getFragmentManager().
                beginTransaction().
                add(OpenAsContainerTask.newInstance(loc, false), OpenAsContainerTask.TAG).
                commit();

    }

    private void selectAllFiles()
	{
        ListView lv = getListView();
        BrowserRecord lr = null;
        for(int i=0;i<lv.getCount();i++)
        {
            BrowserRecord rec = (BrowserRecord) lv.getItemAtPosition(i);
            if(rec.allowSelect())
            {
                rec.setSelected(true);
                lr = rec;
            }
        }
		if(lr!=null)
        {
            FileListViewAdapter adapter = (FileListViewAdapter) lv.getAdapter();
            adapter.notifyDataSetInvalidated();
            startSelectionMode();
            onSelectionChanged();
        }
	}

    private boolean showSelectedFilenameEditText()
    {
        return isSelectAction()
                && isSingleSelectionMode()
                && !getRealLocation().isReadOnly()
                && (
                        allowCreateNewFile()
                        || allowCreateNewFolder()
                );
    }

    private void openLocation(Location locToOpen)
    {
        FragmentManager fm = getFragmentManager();
        String openerTag = LocationOpenerBaseFragment.getOpenerTag(locToOpen);
        if(fm.findFragmentByTag(openerTag)==null)
        {
            LocationOpenerBaseFragment opener = LocationOpenerBaseFragment.getDefaultOpenerForLocation(locToOpen);
            Bundle openerArgs = new Bundle();
            LocationsManager.storePathsInBundle(openerArgs, locToOpen, null);
            openerArgs.putString(LocationOpenerBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG, getTag());
            opener.setArguments(openerArgs);
            fm.beginTransaction().add(opener, openerTag).commit();
        }
    }
}
