package com.sovworks.eds.android.filemanager.tasks;

import android.content.Context;

import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.ExecutableFileRecord;
import com.sovworks.eds.android.filemanager.records.FolderRecord;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.GlobalConfig;

import org.json.JSONException;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public abstract class CreateNewFileBase
{

	public static Single<BrowserRecord> createObservable(Context context, Location location, String fileName, int fileType, boolean returnExisting)
	{
		Single<BrowserRecord> observable = Single.create(em -> {
			CreateNewFileBase cnf = new CreateNewFile(context, location, fileName, fileType, returnExisting);
			if(!em.isDisposed())
				em.onSuccess(cnf.create());
		});
		if(GlobalConfig.isTest())
			observable = observable.
					doOnSubscribe(res -> TEST_OBSERVABLE.onNext(true)).
					doFinally(() -> TEST_OBSERVABLE.onNext(false));
		return observable;
	}

	static
	{
		if(GlobalConfig.isTest())
			TEST_OBSERVABLE = PublishSubject.create();

	}

	public static Subject<Boolean> TEST_OBSERVABLE;

	public static final int FILE_TYPE_FILE = 0;
	public static final int FILE_TYPE_FOLDER = 1;

	CreateNewFileBase(Context context, Location location, String fileName, int fileType, boolean returnExisting)
	{
		_context = context;
		_location = location;
		_fileName = fileName;
		_fileType = fileType;
		_returnExisting = returnExisting;
	}

	protected BrowserRecord create() throws Exception
	{
		if(_returnExisting)
		{
			Path path = PathUtil.buildPath(_location.getCurrentPath(), _fileName);
			if(path != null && path.exists())
				return ReadDir.getBrowserRecordFromFsRecord(_context, _location, path, null);
		}
		return createFile(_fileName, _fileType);
	}

	protected BrowserRecord createFile(String fileName, int ft) throws IOException, JSONException, ApplicationException
	{
		switch (ft)
		{
			case FILE_TYPE_FOLDER:
				return createNewFolder(fileName);
			case FILE_TYPE_FILE:
				return createNewFile(fileName);
			default:
				throw new IllegalArgumentException("Unsupported file type");
		}
	}

	
	protected final Context _context;
	protected final Location _location;
	protected final String _fileName;
	private final int _fileType;
	private final boolean _returnExisting;

	private FolderRecord createNewFolder(String fileName) throws IOException
	{
		Directory parentDir = _location.getCurrentPath().getDirectory();
		Directory newDir = parentDir.createDirectory(fileName);
		FolderRecord fr = new FolderRecord(_context);
		fr.init(_location, newDir.getPath());
		return fr;
	}

	private ExecutableFileRecord createNewFile(String fileName) throws IOException
	{
		Directory parentDir = _location.getCurrentPath().getDirectory();
		File newFile = parentDir.createFile(fileName);
		ExecutableFileRecord r = new ExecutableFileRecord(_context);
		r.init(_location, newFile.getPath());
		return r;
	}

}