package com.sovworks.eds.android.helpers;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.errors.ExternalStorageNotAvailableException;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class TempFilesMonitor 
{

	public static void deleteRecWithWiping(Path path,boolean wipe) throws IOException
	{
		if(!path.exists())
			return;
		if(path.isDirectory())
		{
			Directory.Contents dc = path.getDirectory().list(); 
			try
			{
				for(Path rec: dc)
					deleteRecWithWiping(rec, wipe);
			}
			finally
			{
				dc.close();
			}
			path.getDirectory().delete();
		}
		else if(path.isFile())
		{
			if(wipe)
				WipeFilesTask.wipeFileRnd(path.getFile(),null);
			else
				path.getFile().delete();
		}
	}
	
	public static Location getTmpLocation(Location location, Context context, String workDir) throws IOException
	{
		return getTmpLocation(location,location.getCurrentPath(), context, workDir);
	}

	public static Location getTmpLocation(Location rootLocation, Path path, Context context, String workDir) throws IOException
	{
		return getTmpLocation(rootLocation, path, context, workDir, true);
	}

	public static Location getTmpLocation(Location rootLocation, Path path, Context context, String workDir, boolean monitored) throws IOException
	{
		Location loc = monitored ?
				FileOpsService.getMonitoredMirrorLocation(workDir, context, rootLocation.getId()) :
				FileOpsService.getNonMonitoredMirrorLocation(workDir, context, rootLocation.getId());
		loc.setCurrentPath(
				PathUtil.getDirectory(
						loc.getCurrentPath(),
						SimpleCrypto.calcStringMD5(path.getPathString())
				).getPath()
		);
		return loc;
	}

	public static synchronized TempFilesMonitor getMonitor(Context context)
	{
		if(_instance == null)		
			_instance = new TempFilesMonitor(context);
		
		return _instance;		
	}
	
	private static TempFilesMonitor _instance;
	
	private TempFilesMonitor(Context context)
	{
		_context = context;
		_syncObject = new Object();
	}
	
	public Object getSyncObject()
	{
		return _syncObject;
	}
	
	public void startFile(Location fileLocation) throws IOException, UserException
	{
		if(!isTempDirWriteable())
			throw new ExternalStorageNotAvailableException(_context);		
		decryptAndStartFile(fileLocation);
	}

	public boolean isUpdateRequired(Location srcLocation, Location deviceLocation) throws IOException
	{
		synchronized(_syncObject)
		{
			Uri deviceUri = deviceLocation.getLocationUri();
			OpenFileInfo ofi = _openedFiles.get(deviceUri);
			if (ofi != null) {
				long srcLastModified = srcLocation.getCurrentPath().getFile().getLastModified().getTime();
				return ofi.srcLastModified < srcLastModified;
			}
			return true;
		}
	}

	public boolean addFileToMonitor(Location srcLocation, Location srcFolderLocation, Location devicePath, boolean isReadOnly) throws IOException
	{
		synchronized(_syncObject)
		{
			OpenFileInfo ofi = new OpenFileInfo();
			ofi.srcFileLocation = srcLocation;
			ofi.srcFolderLocation = srcFolderLocation;
			ofi.devicePath = devicePath;
			ofi.srcLastModified = srcLocation.getCurrentPath().getFile().getLastModified().getTime();
			ofi.isReadOnly = isReadOnly;
			File f = devicePath.getCurrentPath().getFile();
			ofi.lastModified = f.getLastModified().getTime();
			ofi.prevSize = f.getSize();
			_openedFiles.put(devicePath.getLocationUri(), ofi);
		}
		return true;
	}

	public void updateMonitoredInfo(Location deviceLocation, Date srclastModified) throws IOException
	{
		synchronized(_syncObject)
		{
			OpenFileInfo ofi = _openedFiles.get(deviceLocation.getLocationUri());
			if (ofi != null)
				ofi.srcLastModified = srclastModified.getTime();
		}
	}
	
	public void removeFileFromMonitor(Location tmpPath)
	{
		synchronized (_syncObject)
		{			
			_openedFiles.remove(tmpPath.getLocationUri());
		}
	}
	
	public synchronized void startChangesMonitor()
	{
		if(_modCheckTask == null)
		{
			_modCheckTask = new ModificationCheckingTask();
			_modCheckTask.start();
		}
	}
	
	public synchronized void stopChangesMonitor()
	{
		if(_modCheckTask == null)
			return;
		_modCheckTask.cancel();
		try 
		{
			_modCheckTask.join();
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		_modCheckTask = null;
	}
	
	private class ModificationCheckingTask extends Thread
	{
		@Override
		public void run()
		{
			while(!_stop)
			{
				synchronized (_syncObject) 
				{
					Iterator<Map.Entry<Uri,OpenFileInfo>> iter = _openedFiles.entrySet().iterator();
					while(iter.hasNext())
					{
						OpenFileInfo fileInfo = iter.next().getValue();	
						try
						{
							if(!fileInfo.devicePath.getCurrentPath().exists() ||
									( 
											fileInfo.srcFolderLocation instanceof Openable &&
											!((Openable)fileInfo.srcFolderLocation).isOpen()
									)
							)
								iter.remove();
							else if (!fileInfo.isReadOnly)
							{							
								//long lastModified = fileInfo.devicePath.getAbsoluteFile().lastModified(); //fileInfo.devicePath.lastModified();
								File f = fileInfo.devicePath.getCurrentPath().getFile();
								long lastModified = f.getLastModified().getTime();
								long prevSize = f.getSize();
								if(fileInfo.lastModified != lastModified || fileInfo.prevSize != prevSize)
								{	
									fileInfo.lastModified = lastModified;
									fileInfo.prevSize = prevSize;
									saveChangedFile(fileInfo.srcFolderLocation,fileInfo.devicePath);
								}
							}
						}
						catch(Exception e)
						{
							Logger.log(e);
						}
					}					
				}
				try 
				{
					Thread.sleep(POLLING_INTERVAL);
				} 
				catch (InterruptedException ignored)
				{				
				}				
			}			
		}
		
		public void cancel()
		{
			_stop=true;
		}
		
		private static final int POLLING_INTERVAL = 3000;
		
		private boolean _stop;
	}	
	

	private final Object _syncObject;
	private final TreeMap<Uri,OpenFileInfo> _openedFiles = new TreeMap<>();
	private final Context _context;	
	private ModificationCheckingTask _modCheckTask;

	
	private void decryptAndStartFile(Location srcLocation) throws IOException, UserException
	{
		if(_context == null)
			return;
		FileOpsService.startTempFile(_context, srcLocation);		
	}	
	
	private void saveChangedFile(Location srcLocation, Location tmpPath) throws IOException, UserException
	{		
		FileOpsService.saveChangedFile(_context, new SrcDstSingle(tmpPath, srcLocation) );
	}
	
	private boolean isTempDirWriteable()
    {
    	return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
	
}
