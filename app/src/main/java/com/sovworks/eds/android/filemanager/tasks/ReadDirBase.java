package com.sovworks.eds.android.filemanager.tasks;

import android.content.Context;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.DummyUpDirRecord;
import com.sovworks.eds.android.filemanager.records.LocRootDirRecord;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.ContainerFSWrapper;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.GlobalConfig;

import org.json.JSONException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public abstract class ReadDirBase
{
	public static Observable<BrowserRecord> createObservable(
			Context context,
			Location targetLocation,
			Collection<Path> selectedFiles,
			DirectorySettings dirSettings,
			boolean showRootFolderLink)
	{
		Observable<BrowserRecord> observable = Observable.create(em -> {
			ReadDir rd = new ReadDir(context, targetLocation, selectedFiles, dirSettings, showRootFolderLink);
			rd.readDir(em);
		});
		if(GlobalConfig.isTest())
				observable = observable.
						doOnSubscribe(res -> TEST_READING_OBSERVABLE.onNext(true)).
						doFinally(() -> TEST_READING_OBSERVABLE.onNext(false));
		return observable;
	}

	static
	{
		if(GlobalConfig.isTest())
			TEST_READING_OBSERVABLE = BehaviorSubject.createDefault(false);

	}

	public static Subject<Boolean> TEST_READING_OBSERVABLE;

	public static BrowserRecord getBrowserRecordFromFsRecord(Context context, Location loc, Path path, DirectorySettings directorySettings) throws IOException, ApplicationException
	{
		BrowserRecord rec = ReadDir.createBrowserRecordFromFile(context, loc, path ,directorySettings);
		if(rec!=null)
			try
			{
				rec.init(loc, path);
			}
			catch (Exception e)
			{
				Logger.log(e);
				rec = null;
			}
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
	ReadDirBase(Context context, Location targetLocation, Collection<Path> selectedFiles, DirectorySettings dirSettings, boolean showRootFolderLink)
	{
		_context = context;
		_targetLocation = targetLocation;
		_selectedFiles = selectedFiles == null ? null : new HashSet<>(selectedFiles);
		_directorySettings = dirSettings;
		_showFolderLinks = showRootFolderLink;
	}

	void readDir(ObservableEmitter<BrowserRecord> em) throws IOException
	{
		Path targetPath = _targetLocation.getCurrentPath();
		if(targetPath.isFile())
			targetPath = targetPath.getParentPath();
		if(targetPath == null)
		{
			em.onComplete();
			return;
		}

		int count = 0;
		Path basePath = targetPath.getParentPath();
		if(basePath!=null && !em.isDisposed())
		{
			BrowserRecord br = new DummyUpDirRecord(_context);
			br.init(null, basePath);
			procRecord(br, count++);
			em.onNext(br);
		}

		if(targetPath.isRootDirectory() && _showFolderLinks && !em.isDisposed())
		{
			BrowserRecord br = new LocRootDirRecord(_context);
			br.init(_targetLocation, targetPath);
			procRecord(br, count++);
			em.onNext(br);
		}

		Directory.Contents dirReader = targetPath.getDirectory().list();
		if(dirReader == null)
		{
			em.onComplete();
			return;
		}
		try
		{
			em.setCancellable(dirReader::close);
			for(Path path: dirReader)
			{
				if(em.isDisposed())
					break;

				BrowserRecord record = getBrowserRecordFromFsRecord(_targetLocation, path, _directorySettings);
				if(record == null)
					continue;
				procRecord(record, count++);
				em.onNext(record);
			}
		}
		finally
		{
			dirReader.close();
		}
		em.onComplete();
	}

	private BrowserRecord getBrowserRecordFromFsRecord(Location loc, Path path, DirectorySettings directorySettings)
	{
		try
		{
			return ReadDir.getBrowserRecordFromFsRecord(_context, loc, path, directorySettings);
		}
		catch (ApplicationException | IOException e)
		{
			Logger.log(e);
		}
		return null;
	}

	protected void procRecord(
			BrowserRecord rec,
			int count
	)
	{
		if(_selectedFiles!=null && _selectedFiles.contains(rec.getPath()))
			rec.setSelected(true);
	}

	private final Context _context;
	private final Location _targetLocation;
	private final Set<Path> _selectedFiles;
	private final DirectorySettings _directorySettings;
	private final boolean _showFolderLinks;

}