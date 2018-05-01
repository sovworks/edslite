package com.sovworks.eds.android.service;

import android.content.Intent;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection;

import java.io.IOException;

class SaveTempFileChangesTask extends CopyFilesTask
{
	@Override
	protected int getNotificationMainTextId()
	{
		return R.string.saving_changes;
	}

	@Override
	protected boolean copyFile(SrcDstCollection.SrcDst record) throws IOException
	{
		if(super.copyFile(record))
		{
			Path dstPath = calcDstPath(record.getSrcLocation().getCurrentPath().getFile(), record.getDstLocation().getCurrentPath().getDirectory());
			if (dstPath != null && dstPath.isFile())
				TempFilesMonitor.getMonitor(_context).updateMonitoredInfo(record.getSrcLocation(), dstPath.getFile().getLastModified());
			return true;
		}
		else
			return false;
	}

	@Override
	protected boolean copyFile(File srcFile, Directory targetFolder) throws IOException
	{
		try
		{
			File tmpFile = copyToTempFile(srcFile, targetFolder);
			Path dstPath = calcDstPath(srcFile, targetFolder);
			if (dstPath != null && dstPath.exists())
				prepareBackupCopy(dstPath.getFile(), targetFolder);
			tmpFile.rename(srcFile.getName());
			return true;
		}
		catch(IOException e)
		{
			throw new IOException(_context.getText(R.string.err_failed_saving_changes).toString(), e);
		}
	}

	protected File copyToTempFile(File srcFile, Directory targetFolder) throws IOException
	{
		String tmpName = srcFile.getName() + TMP_EXTENSION;
		Path tmpPath = calcPath(targetFolder, tmpName);
		if(tmpPath != null && tmpPath.isFile())
			tmpPath.getFile().delete();
		File dstFile = targetFolder.createFile(tmpName);
		if(!super.copyFile(srcFile, dstFile))
			throw new IOException("Failed copying to temp file");
		return dstFile;
	}

	protected void prepareBackupCopy(File dstFile, Directory targetFolder) throws IOException
	{
		if(!UserSettings.getSettings(_context).disableModifiedFilesBackup() && dstFile.getSize() > 0)
		{
			String bakName = dstFile.getName() + BAK_EXTENSION;
			Path bakPath = calcPath(targetFolder, bakName);
			if(bakPath!=null && bakPath.isFile())
				bakPath.getFile().delete();
			dstFile.rename(bakName);
		}
		else
			dstFile.delete();
	}

	@Override
	protected CopyFilesTaskParam initParam(Intent i)
	{
		return new CopyFilesTaskParam(i)
		{
			@Override
			public boolean forceOverwrite()
			{
				return true;
			}
		};
	}
	
	private static final String BAK_EXTENSION = ".edsbak";
	private static final String TMP_EXTENSION = ".edstmp";

}