package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.content.Context;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.DummyUpDirRecord;
import com.sovworks.eds.android.filemanager.records.LocRootDirRecord;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.ContainerFSWrapper;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ReadDirTaskBase extends TaskFragment
{
	public static final String TAG = "ReadDirTask";

	public static final String ARG_SHOW_ROOT_FOLDER_LINK = "com.sovworks.eds.android.SHOW_ROOT_FOLDER_LINK";
	public static final String ARG_SCROLL_POSITION = "com.sovworks.eds.android.SCROLL_POSITION";

	public static BrowserRecord getBrowserRecordFromFsRecord(Context context, Location loc, Path path, DirectorySettings directorySettings) throws IOException, ApplicationException
	{
		BrowserRecord rec = ReadDirTask.createBrowserRecordFromFile(context, loc, path ,directorySettings);
		if(rec!=null)
			rec.init(loc, path);
		return rec;
	}

	public static DirectorySettings getDirectorySettings(Path path) throws IOException
	{
		return path.getFileSystem() instanceof ContainerFSWrapper ?
				((ContainerFSWrapper) path.getFileSystem()).getDirectorySettings(path)
				:
				loadDirectorySettings(path);
	}

	public static DirectorySettings loadDirectorySettings(Path path) throws IOException
	{
		Path dsPath;
		try
		{
			dsPath = path.combine(DirectorySettings.FILE_NAME);
		}
		catch(IOException e)
		{
			return null;
		}
		try
		{
			return dsPath.isFile() ?
					new DirectorySettings(com.sovworks.eds.fs.util.Util.readFromFile(dsPath))
				:
					null;

		}
		catch (JSONException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public void initTask(Activity activity)
	{
		_context = activity.getApplicationContext();
		_locationsManager = LocationsManager.getLocationsManager(_context);
		_dataFragment = (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
	}

	@Override
	public void onDestroy()
	{
		_context = null;
		_locationsManager = null;
		super.onDestroy();
	}

	@Override
	public synchronized void cancel()
	{
		super.cancel();
		if(_dirReader!=null)
		{
			try
			{
				_dirReader.close();
			}
			catch (IOException e)
			{
				Logger.log(e);
			}
			_dirReader = null;
		}
	}

	@Override
	protected void doWork(TaskState state) throws Exception
	{
        HashSet<Path> selectedFiles = new HashSet<>();
		Location targetLocation = _locationsManager.getFromBundle(getArguments(), selectedFiles);
		BrowserRecord startRecord = null;
		Path startPath = checkStartPath(targetLocation);
		Path targetPath = targetLocation.getCurrentPath();
		ArrayList<BrowserRecord> uiUpdateList  = new ArrayList<>();
		if(targetPath == null)
			return;
        DirectorySettings directorySettings = null;
		try
		{
			directorySettings = getDirectorySettings(targetPath);
		}
		catch(IOException e)
		{
			Logger.log(e);
		}
		_dataFragment.setDirectorySettings(directorySettings);
		BrowserRecord curPathRecord = getBrowserRecordFromFsRecord(targetLocation, targetPath, null);
		_dataFragment.setCurrentPathRecord(curPathRecord);
		Directory.Contents dirReader = getDirReader(state, targetPath);
		if(dirReader == null)
			return;
		try
		{
			int count = 0;
			Path basePath = targetPath.getParentPath();
			if(basePath!=null)
			{
				BrowserRecord br = new DummyUpDirRecord(_context);
				br.init(null, basePath);
				procRecord(state, uiUpdateList, selectedFiles, br, count++);
			}

			if(targetPath.isRootDirectory() && getArguments().getBoolean(ARG_SHOW_ROOT_FOLDER_LINK, false))
			{
				BrowserRecord br = new LocRootDirRecord(_context);
				br.init(targetLocation, targetPath);
				procRecord(state, uiUpdateList, selectedFiles, br, count++);
			}

			for(Path path: dirReader)
			{
				if(state.isTaskCancelled())
					return;

				BrowserRecord record = getBrowserRecordFromFsRecord(targetLocation, path, directorySettings);
				if(path.equals(startPath))
					startRecord = record;
				procRecord(state, uiUpdateList, selectedFiles, record, count);
				if(sendUpdate(state, uiUpdateList))
					uiUpdateList = new ArrayList<>();
				count++;
			}
            _dataFragment.sortFiles();
			state.setResult(startRecord);
		}
		finally
		{
			dirReader.close();
		}					
	}		
	
	@Override
	protected TaskCallbacks getTaskCallbacks(Activity activity)
	{
        FileListViewFragment f = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
        return f == null || f.getView() == null ? null : f.getReadDirTaskCallbacks();
	}	
	
	protected boolean sendUpdate(TaskState state, List<BrowserRecord> files)
	{
		long ct = System.currentTimeMillis();
		if(ct - _prevUpdateTime > 1000)
		{
			state.updateUI(files);
			_prevUpdateTime = ct;
			return true;
		}
		return false;
	}
	
	protected BrowserRecord getBrowserRecordFromFsRecord(Location loc, Path path, DirectorySettings directorySettings) throws IOException, ApplicationException
	{
		return getBrowserRecordFromFsRecord(_context, loc, path, directorySettings);
	}

	//Must be synchronized to support cancelling
	protected synchronized void procRecord(
			TaskState state,
			Collection<BrowserRecord> nextFiles,
			Set<Path> selectedFiles,
			BrowserRecord rec,
			int count
	)
	{
        if(state.isTaskCancelled() || rec == null)
            return;

        nextFiles.add(rec);
        _dataFragment.addFileToList(rec);
        if(selectedFiles.contains(rec.getPath()))
            rec.setSelected(true);
	}
	
	protected Path checkStartPath(Location loc) throws IOException
	{
		Path startPath = loc.getCurrentPath();
		if(startPath.isFile())		
			loc.setCurrentPath(startPath.getParentPath());
		return startPath;
	}

	private long _prevUpdateTime;
	private Context _context;
	private LocationsManager _locationsManager;
	private FileListDataFragment _dataFragment;
	private Directory.Contents _dirReader;

	private synchronized Directory.Contents getDirReader(TaskState state, Path targetPath) throws IOException
	{
		_dirReader = state.isTaskCancelled() ? null : targetPath.getDirectory().list();
		return _dirReader;
	}
}