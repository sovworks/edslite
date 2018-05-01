package com.sovworks.eds.android.service;

import android.content.Context;
import android.content.Intent;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.fs.util.SrcDstGroup;
import com.sovworks.eds.fs.util.SrcDstRec;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PrepareTempFilesTask extends CopyFilesTask
{
	public static class FilesTaskParam extends CopyFilesTaskParam
	{
		FilesTaskParam(Intent i, Context context)
		{
			super(i);
			_context = context;
		}
		
		@Override
		protected SrcDstCollection loadRecords(Intent i)
		{
			ArrayList<Path> paths = new ArrayList<>();
			Location loc = LocationsManager.getLocationsManager(_context).getFromIntent(i, paths);
			String wd = UserSettings.getSettings(_context).getWorkDir();
			ArrayList<SrcDstCollection> cols = new ArrayList<>();
			try
			{
				for(Path srcPath: paths)
				{
					Location srcLoc = loc.copy();
					srcLoc.setCurrentPath(srcPath);
					Path parentPath = null;
					try
					{
						parentPath = srcPath.getParentPath();
					}
					catch (IOException ignored){}
					if(parentPath == null)
						parentPath = srcPath;
					SrcDstSingle sds = new SrcDstSingle(srcLoc, TempFilesMonitor.getTmpLocation(loc, parentPath, _context, wd));
					SrcDstCollection sd = srcPath.isFile() ? sds : new SrcDstRec(sds);
					cols.add(sd);							
				}		
				return new SrcDstGroup(cols);
			}
			catch(IOException e)
			{
				Logger.showAndLog(_context, e);
			}
			return null;
		}
		
		private final Context _context;
	}
	
	@Override
	public Object doWork(Context context, Intent i) throws Throwable
	{
		Settings settings = UserSettings.getSettings(context);
		_fileSizeLimit = 1024 * 1024 * settings.getMaxTempFileSize();
		_wipe = settings.wipeTempFiles();
		super.doWork(context, i);
		return _tempFilesList;
	}

	private long _fileSizeLimit;
	private boolean _wipe;
	private final List<Location> _tempFilesList = new ArrayList<>();
	
	@Override
	protected FilesTaskParam initParam(Intent i)
	{		
		return new FilesTaskParam(i, _context); 
	}

	@Override
	protected int getNotificationMainTextId()
	{
		return R.string.preparing_file;
	}

	@Override
	protected boolean copyFile(SrcDst record) throws IOException
	{
		Location srcLoc = record.getSrcLocation().copy();
		Location dstLoc = record.getDstLocation();
		if(dstLoc == null)
			throw new IOException("Failed to calc destination folder");
		dstLoc = dstLoc.copy();
        Path tmpPath = calcDstPath(srcLoc.getCurrentPath().getFile(), dstLoc.getCurrentPath().getDirectory());
        Location srcFolderLocation = srcLoc.copy();
        srcFolderLocation.setCurrentPath(srcLoc.getCurrentPath().getParentPath());
        if(tmpPath!=null)
        {
            dstLoc.setCurrentPath(tmpPath);
            if (dstLoc.getCurrentPath().exists())
            {
            	TempFilesMonitor monitor = TempFilesMonitor.getMonitor(_context);
				if(!monitor.isUpdateRequired(srcLoc, dstLoc))
				{
					incProcessedSize((int) srcLoc.getCurrentPath().getFile().getSize());
					_tempFilesList.add(dstLoc);
					return true;
				}
                TempFilesMonitor.getMonitor(_context).removeFileFromMonitor(dstLoc);
                TempFilesMonitor.deleteRecWithWiping(dstLoc.getCurrentPath(), _wipe);
            }
        }
        else
        {
            tmpPath = dstLoc.getCurrentPath().getDirectory().createFile(srcLoc.getCurrentPath().getFile().getName()).getPath();
            dstLoc.setCurrentPath(tmpPath);
        }
		if(super.copyFile(record))
		{
			addFileToMonitor(srcLoc, srcFolderLocation, dstLoc);
			_tempFilesList.add(dstLoc);
			return true;
		}
		return false;
	}

	@Override
	protected Path calcDstPath(FSRecord src, Directory dstFolder) throws IOException
	{
		Path res = super.calcDstPath(src, dstFolder);
		if(res == null)
			res = (src instanceof File) ?
					dstFolder.createFile(src.getName()).getPath() :
					(src instanceof Directory) ?
							dstFolder.createDirectory(src.getName()).getPath() :
							null;

		return res;
	}

	@Override
	protected boolean copyFile(File srcFile, Directory targetFolder) throws IOException
	{
		Path dstPath = calcDstPath(srcFile, targetFolder);
		if(dstPath!=null && dstPath.isFile())
		{
			File dstFile = dstPath.getFile();
			if (dstFile.getSize() == srcFile.getSize() &&
					dstFile.getLastModified().getTime() >= srcFile.getLastModified().getTime())
			{
				incProcessedSize((int) srcFile.getSize());
				return true;
			}
		}
		if (srcFile.getSize() > _fileSizeLimit)
			throw new IOException(_context.getText(R.string.err_temp_file_is_too_big).toString());
		return super.copyFile(srcFile, targetFolder);
	}

	private void addFileToMonitor(Location srcLocation, Location srcFodlerLocation, Location dstFilePath) throws IOException
	{
		TempFilesMonitor.getMonitor(_context).addFileToMonitor(srcLocation, srcFodlerLocation, dstFilePath, false);
	}
}