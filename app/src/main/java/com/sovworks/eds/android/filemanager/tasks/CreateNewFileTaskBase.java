package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.filemanager.records.ExecutableFileRecord;
import com.sovworks.eds.android.filemanager.records.FolderRecord;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import org.json.JSONException;

import java.io.IOException;

public abstract class CreateNewFileTaskBase extends TaskFragment
{
	public static final String TAG = "CreateNewFileTask";

	public static final String ARG_FILENAME = "com.sovworks.eds.android.FILENAME";
	public static final String ARG_TYPE = "com.sovworks.eds.android.TYPE";

	public static final int FILE_TYPE_FILE = 0;
	public static final int FILE_TYPE_FOLDER = 1;

	@Override
	public void initTask(Activity activity)
	{					
		_location = ((FileManagerActivity)activity).getLocation();
		_context = activity.getApplicationContext();
	}
	
	@Override
	protected void doWork(TaskState state) throws Exception
	{
		String fileName = getArguments().getString(ARG_FILENAME);
		int ft = getArguments().getInt(ARG_TYPE);
		state.setResult(createFile(fileName, ft));
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
	
	@Override
	protected TaskCallbacks getTaskCallbacks(final Activity activity)
	{
		return new TaskCallbacks()
        {
            @Override
            public void onUpdateUI(Object state)
            {
            }
            @Override
            public void onPrepare(Bundle args)
            {
            }

            @Override
            public void onResumeUI(Bundle args)
            {
            }

            @Override
            public void onSuspendUI(Bundle args)
            {
            }

            @Override
            public void onCompleted(Bundle args, Result result)
            {
                try
                {
                    CreateNewFileTaskBase.this.onCompleted((FileManagerActivity) activity, (BrowserRecord) result.getResult());
                }
                catch(Throwable e)
                {
                    Logger.showAndLog(activity, e);
                }
            }
        };
	}

	protected void onCompleted(FileManagerActivity activity, BrowserRecord rec) throws IOException
	{
		addFileToList(rec);
	}

	protected void addFileToList(BrowserRecord rec) throws IOException
	{
		FileListDataFragment f = (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
		if(f!=null)
		{
			Path currentPath = f.getLocation().getCurrentPath();
			if(currentPath!=null && currentPath.equals(_location.getCurrentPath()))
			{
				f.addFileToList(rec);
				f.sortFiles();
			}
			FileListViewFragment fl = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
			if(fl!=null)
				fl.onReadingCompleted();

		}
	}
	
	protected Context _context;
	protected Location _location;

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