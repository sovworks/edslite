package com.sovworks.eds.android.filemanager.tasks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.locations.Location;

import java.io.IOException;
import java.util.Collections;

public class CreateNewFileAndSelectTask extends CreateNewFileTask
{
	public static final String TAG = "CreateNewFileAndSelectTask";

	public static CreateNewFileAndSelectTask newInstance(String filename, int fileType)
	{
		Bundle args = new Bundle();
		args.putString(ARG_FILENAME, filename);
		args.putInt(ARG_TYPE, fileType);
		CreateNewFileAndSelectTask f = new CreateNewFileAndSelectTask();
		f.setArguments(args);
		return f;			
	}

	@Override
	protected void doWork(TaskState state) throws Exception
	{
		Path path = PathUtil.buildPath(_location.getCurrentPath(), getArguments().getString(ARG_FILENAME));
		if(path!=null && path.exists())
			state.setResult(ReadDirTask.getBrowserRecordFromFsRecord(_context, _location, path, null));
		else
			super.doWork(state);
	}

	@Override
	protected void onCompleted(FileManagerActivity activity, BrowserRecord rec) throws IOException
	{
		FileListDataFragment f = (FileListDataFragment) getFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
		if(f!=null && f.findLoadedFileByPath(rec.getPath()) == null)
			addFileToList(rec);
		Location loc = activity.getRealLocation().copy();
		loc.setCurrentPath(rec.getPath());
		Intent i = new Intent();
		LocationsManager.storePathsInIntent(i, loc, Collections.singletonList(rec.getPath()));
		activity.setResult(Activity.RESULT_OK, i);
		activity.finish();
	}

}