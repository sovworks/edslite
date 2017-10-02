package com.sovworks.eds.container;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;

import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.DefaultSettings;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;
import java.util.Collections;

public abstract class EDSLocationFormatterBase
{
	public static final String FORMAT_ENCFS = "EncFs";

	public EDSLocationFormatterBase()
	{

	}

	protected EDSLocationFormatterBase(Parcel in)
	{
		_disableDefaultSettings = in.readByte() != 0;
		_password = in.readParcelable(ClassLoader.getSystemClassLoader());
	}

	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeByte((byte) (_disableDefaultSettings ? 1 : 0));
		dest.writeParcelable(_password, 0);
	}

	public static String makeTitle(EDSLocation cont, LocationsManager lm)
	{
		String startTitle;
		try
		{
			startTitle = new StringPathUtil(cont.getLocation().getCurrentPath().getPathDesc()).getFileNameWithoutExtension();
		}
		catch (IOException e)
		{
			startTitle = cont.getTitle();
		}
		return makeTitle(startTitle, lm, cont);
	}

	public static String makeTitle(String startTitle, LocationsManager lm, EDSLocation ignore)
	{
		String title = startTitle;
		int i = 1;
		while (checkExistingTitle(title, lm, ignore))
			title = startTitle + " " + i++;
		return title;
	}

	public interface ProgressReporter
	{
		boolean report(byte prc);
	}


	public void setContext(Context context)
	{
		_context = context;
	}

	public Context getContext()
	{
		return _context;
	}

	public Settings getSettings()
	{
		return _context == null ? new DefaultSettings() : UserSettings.getSettings(_context);
	}

	public void setPassword(SecureBuffer pass)
	{
		_password = pass;
	}

	public void disableDefaultSettings(boolean val)
	{
		_disableDefaultSettings = val;
	}
	
	public void setProgressReporter(ProgressReporter reporter)
	{
		_progressReporter = reporter;
	}
	
	public EDSLocation format(Location location) throws Exception
	{
		EDSLocation loc = createLocation(location);
		if(!_dontReg)
			addLocationToList(loc);
		loc.getFS();
		initLocationSettings(loc);
		loc.close(false);
		if(!_dontReg)
			notifyLocationCreated(loc);
		return loc;
	}

	public void setDontRegLocation(boolean dontReg)
	{
		_dontReg = dontReg;
	}

	protected boolean _disableDefaultSettings, _dontReg;
	protected SecureBuffer _password;
	protected ProgressReporter _progressReporter;
	protected Context _context;

	protected abstract EDSLocation createLocation(Location location) throws IOException, ApplicationException, UserException;

	protected void addLocationToList(EDSLocation loc) throws Exception
	{
		addLocationToList(loc, !UserSettings.getSettings(_context).neverSaveHistory());
	}

	protected void addLocationToList(EDSLocation loc, boolean store) throws Exception
	{
		LocationsManager lm = LocationsManager.getLocationsManager(_context, true);
		if(lm!=null)
		{
			Location prevLoc = lm.findExistingLocation(loc);
			if(prevLoc == null)
				lm.addNewLocation(loc, store);
			else
				lm.replaceLocation(prevLoc, loc, store);
		}

	}
	
	protected void notifyLocationCreated(EDSLocation loc)
	{
		if(_context!=null)
			LocationsManager.broadcastLocationAdded(_context, loc);
	}
	
	protected void initLocationSettings(EDSLocation loc) throws IOException, ApplicationException
	{
		writeInternalContainerSettings(loc);
		if(!_dontReg)
			setExternalContainerSettings(loc);
	}
	
	protected void setExternalContainerSettings(EDSLocation loc) throws ApplicationException, IOException
	{
		LocationsManager lm = LocationsManager.getLocationsManager(_context, true);
		String title = makeTitle(loc, lm);
		loc.getExternalSettings().setTitle(title);
		loc.getExternalSettings().setVisibleToUser(true);

		if (_context == null || !UserSettings.getSettings(_context).neverSaveHistory())
			loc.saveExternalSettings();

	}
	
	protected void writeInternalContainerSettings(EDSLocation loc) throws IOException
	{
		if(_disableDefaultSettings)
			return;
		DirectorySettings ds = new DirectorySettings();
		ds.setHiddenFilesMasks(Collections.singletonList("(?iu)\\.eds.*"));
		FileSystem fs = loc.getFS();
		ds.saveToDir(fs.getRootPath().getDirectory());
	}

	protected boolean reportProgress(byte prc)
	{
		return _progressReporter == null || _progressReporter.report(prc);
	}

	private static boolean checkExistingTitle(String title, LocationsManager lm, EDSLocation ignore)
	{
		Uri igUri = ignore.getLocation().getLocationUri();
		for(EDSLocation cnt: lm.getLoadedEDSLocations(true))
			if(cnt!=ignore && !cnt.getLocation().getLocationUri().equals(igUri) && cnt.getTitle().equals(title))
				return true;
		return false;
	}
}
