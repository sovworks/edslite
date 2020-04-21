package com.sovworks.eds.locations;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.StorageOptions;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.locations.ContentResolverLocation;
import com.sovworks.eds.android.locations.DeviceRootNPLocation;
import com.sovworks.eds.android.locations.DocumentTreeLocation;
import com.sovworks.eds.android.locations.EncFsLocation;
import com.sovworks.eds.android.locations.EncFsLocationBase;
import com.sovworks.eds.android.locations.ExternalStorageLocation;
import com.sovworks.eds.android.locations.InternalSDLocation;
import com.sovworks.eds.android.locations.LUKSLocation;
import com.sovworks.eds.android.locations.TrueCryptLocation;
import com.sovworks.eds.android.locations.VeraCryptLocation;
import com.sovworks.eds.android.locations.closer.fragments.OpenableLocationCloserFragment;
import com.sovworks.eds.android.receivers.MediaMountedReceiver;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.std.StdFs;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.settings.Settings;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public abstract class LocationsManagerBase
{
	public static final String PARAM_LOCATION_URIS = "com.sovworks.eds.android.LOCATION_URIS";
	public static final String PARAM_PATHS = "com.sovworks.eds.android.PATHS";
	public static final String PARAM_LOCATION_URI = "com.sovworks.eds.android.LOCATION_URI";

	public static final String BROADCAST_LOCATION_CREATED = "com.sovworks.eds.BROADCAST_LOCATION_CREATED";
	public static final String BROADCAST_LOCATION_REMOVED = "com.sovworks.eds.BROADCAST_LOCATION_REMOVED";
	@SuppressWarnings("WeakerAccess")
	public static final String BROADCAST_ALL_CONTAINERS_CLOSED = "com.sovworks.eds.android.BROADCAST_ALL_CONTAINERS_CLOSED";
	public static final String BROADCAST_CLOSE_ALL = "com.sovworks.eds.CLOSE_ALL";
	public static final String BROADCAST_LOCATION_CHANGED = "com.sovworks.eds.android.BROADCAST_LOCATION_CHANGED";

	/*public static synchronized LocationsManager getLocationsManager()
	{
		return getLocationsManager(null, false);
	}*/

	public static synchronized LocationsManager getLocationsManager(Context context)
	{
		return getLocationsManager(context, true);
	}

	public static synchronized LocationsManager getLocationsManager(Context context, boolean create)
	{
		if(create && _instance == null)
		{
			_instance = new LocationsManager(context.getApplicationContext(), UserSettings.getSettings(context));
			_instance.loadStoredLocations();
			_instance.startMountsMonitor();
		}
		return (LocationsManager) _instance;
	}

	private static synchronized void closeLocationsManager()
	{
		if(_instance != null)
		{
			_instance.close();
			_instance = null;
		}
	}

	public static synchronized void setGlobalLocationsManager(LocationsManagerBase lm)
	{
		if(_instance!=null)
			closeLocationsManager();
		_instance = lm;
	}

	@SuppressLint("StaticFieldLeak")
	private static LocationsManagerBase _instance;

	public static void storePathsInBundle(Bundle b, Location loc, Collection<? extends Path> paths)
	{
		b.putParcelable(PARAM_LOCATION_URI, loc.getLocationUri());
		if(paths != null)
			b.putStringArrayList(PARAM_PATHS, Util.storePaths(paths));
	}

	public static void storePathsInIntent(Intent i, Location loc, Collection<? extends Path> paths)
	{
		i.setData(loc.getLocationUri());
		i.putExtra(PARAM_LOCATION_URI, loc.getLocationUri());
		if(paths != null)
			i.putStringArrayListExtra(PARAM_PATHS, Util.storePaths(paths));
	}

	public static void storeLocationsInBundle(Bundle b, Iterable<? extends Location> locations)
	{
		ArrayList<Uri> uris = new ArrayList<>();
		for(Location loc: locations)
			uris.add(loc.getLocationUri());
		b.putParcelableArrayList(PARAM_LOCATION_URIS, uris);
	}

	public static void storeLocationsInIntent(Intent i, Iterable<? extends Location> locations)
	{
		Bundle b = new Bundle();
		storeLocationsInBundle(b, locations);
		i.putExtras(b);
	}

	@SuppressWarnings("WeakerAccess")
	public static ArrayList<Location> getLocationsFromBundle(LocationsManagerBase lm, Bundle b) throws Exception
	{
		ArrayList<Location> res = new ArrayList<>();
		if(b!=null)
		{
			ArrayList<Uri> uris = b.getParcelableArrayList(PARAM_LOCATION_URIS);
			if (uris != null)
				for (Uri uri : uris)
					res.add(lm.getLocation(uri));
		}
		return res;
	}

	@SuppressWarnings("WeakerAccess")
	public static ArrayList<Location> getLocationsFromIntent(LocationsManagerBase lm, Intent i) throws Exception
	{
		ArrayList<Location> res = getLocationsFromBundle(lm,i.getExtras());
		if(res.isEmpty() && i.getData()!=null)
			res.add(lm.getLocation(i.getData()));
		return res;
	}

	public static Location getFromIntent(Intent i, LocationsManagerBase lm, Collection<Path> pathsHolder)
	{
		try
		{
			if(i.getData() == null)
				return null;
			Location loc = lm.getLocation(i.getData());
			if(pathsHolder!=null)
			{
				ArrayList<String> pathStrings = i.getStringArrayListExtra(PARAM_PATHS);
				if(pathStrings!=null)
					pathsHolder.addAll(Util.restorePaths(loc.getFS(), pathStrings));
			}
			return loc;
		}
		catch (Exception e)
		{
			Logger.log(e);
			return null;
		}
	}

	public static Location getFromBundle(Bundle b, LocationsManagerBase lm, Collection<Path> pathsHolder)
	{
		if(b == null || !b.containsKey(PARAM_LOCATION_URI))
			return null;
		try
		{
			Location loc = lm.getLocation(b.getParcelable(PARAM_LOCATION_URI));
			if(pathsHolder!=null)
			{
				ArrayList<String> pathStrings = b.getStringArrayList(PARAM_PATHS);
				if(pathStrings!=null)
					pathsHolder.addAll(Util.restorePaths(loc.getFS(), pathStrings));
			}
			return loc;
		}
		catch (Exception e)
		{
			Logger.log(e);
			return null;
		}
	}

	public static IntentFilter getLocationRemovedIntentFilter()
	{
		return new IntentFilter(LocationsManager.BROADCAST_LOCATION_REMOVED);
	}

	public static IntentFilter getLocationAddedIntentFilter()
	{
		return new IntentFilter(LocationsManager.BROADCAST_LOCATION_CREATED);
	}

	public static void broadcastLocationChanged(Context context,Location location)
	{
		Intent i = new Intent(BROADCAST_LOCATION_CHANGED);
		//i.setData(location.getLocationUri());
		i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
		context.sendBroadcast(i);
	}

	public static void broadcastLocationAdded(Context context,Location location)
	{
		Intent i = new Intent(BROADCAST_LOCATION_CREATED);
		//i.setData(location.getLocationUri());
		if(location!=null)
			i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
		context.sendBroadcast(i);
	}

	public static void broadcastLocationRemoved(Context context,Location location)
	{
		Intent i = new Intent(BROADCAST_LOCATION_REMOVED);
		//i.setData(location.getLocationUri());
		if(location!=null)
			i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
		context.sendBroadcast(i);
	}

	@SuppressWarnings("WeakerAccess")
	public static void broadcastAllContainersClosed(Context context)
	{
		context.sendBroadcast(new Intent(BROADCAST_ALL_CONTAINERS_CLOSED));
	}

	public static ArrayList<String> makeUriStrings(Iterable<? extends Location> locations)
	{
		ArrayList<String> list = new ArrayList<>();
		for(Location l: locations)
			list.add(l.getLocationUri().toString());
		return list;
	}

	public static ArrayList<Path> getPathsFromLocations(Iterable<? extends Location> locations) throws IOException
	{
		ArrayList<Path> res = new ArrayList<>();
		for(Location loc: locations)
			res.add(loc.getCurrentPath());
		return res;
	}

	protected LocationsManagerBase(Context context, Settings settings)
	{
		_settings = settings;
		_context = context.getApplicationContext();
	}

	private void startMountsMonitor()
	{
		if(_mediaChangedReceiver != null)
			return;
		_mediaChangedReceiver = new MediaMountedReceiver(this);
		_context.registerReceiver(_mediaChangedReceiver, new IntentFilter(Intent.ACTION_MEDIA_MOUNTED));
		_context.registerReceiver(_mediaChangedReceiver, new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED));
		_context.registerReceiver(_mediaChangedReceiver, new IntentFilter(Intent.ACTION_MEDIA_REMOVED));
		_context.registerReceiver(_mediaChangedReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
		_context.registerReceiver(_mediaChangedReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
	}

	public void close()
	{
		if(_mediaChangedReceiver!=null)
		{
			_context.unregisterReceiver(_mediaChangedReceiver);
			_mediaChangedReceiver = null;
		}
		closeAllLocations(true, false);
		clearLocations();
	}

	public Location getFromIntent(Intent i,Collection<Path> pathsHolder)
	{
		return getFromIntent(i, this, pathsHolder);
	}

	public Location getFromBundle(Bundle b,Collection<Path> pathsHolder)
	{
		return getFromBundle(b, this, pathsHolder);
	}

	public ArrayList<Location> getLocationsFromIntent(Intent i) throws Exception
	{
		return getLocationsFromIntent(this, i);
	}

	public ArrayList<Location> getLocationsFromBundle(Bundle b) throws Exception
	{
		return getLocationsFromBundle(this, b);
	}

	public void broadcastAllContainersClosed()
	{
		broadcastAllContainersClosed(_context);
	}

	public Location getDefaultDeviceLocation()
	{
		Location res = null;
		Location dba = null;
		synchronized (_currentLocations)
		{
			for (LocationInfo li : _currentLocations)
			{
				Location loc = li.location;
				if (loc instanceof InternalSDLocation)
				{
					res = loc;
					break;
				}
				if (res == null && loc instanceof ExternalStorageLocation)
					res = loc;
				else if (dba == null && loc instanceof DeviceBasedLocation)
					dba = loc;
			}
		}
		if(res == null)
			res = dba == null ? new DeviceBasedLocation(_settings) : dba;
		return res.copy();
	}

	public void closeAllLocations(Iterable<Location> locations, boolean forceClose, boolean sendBroadcasts)
	{
		//boolean forceClose = _settings.alwaysForceClose();
		for(Location l: locations)
		{
			try
			{
				if(l instanceof Openable)
				{
					Openable ol = (Openable)l;
					if(ol.isOpen())
					{
						closeLocation(l, forceClose);
						if(sendBroadcasts)
							broadcastLocationChanged(_context, l);
					}
				}
			}
			catch (Exception e)
			{
				Logger.showAndLog(_context, e);
			}
		}
	}

	public Location getLocation(Uri locationUri) throws Exception
	{
		String locId = getLocationIdFromUri(locationUri);
		if(locId != null)
		{
			Location prevLoc = findExistingLocation(locId);
			if(prevLoc!=null)
			{
				Location loc = prevLoc.copy();
				loc.loadFromUri(locationUri);
				return loc;
			}
		}
		Location loc = createLocationFromUri(locationUri);
		if(loc == null)
			throw new IllegalArgumentException("Unsupported location uri: " + locationUri);
		if(findExistingLocation(loc.getId()) == null)
			addNewLocation(loc,false);
		
		return loc;
	}
	
	public ArrayList<Location> getLocations(Collection<String> uriStrings)
	{
		ArrayList<Location> res = new ArrayList<>();
		if(uriStrings == null)
			return res;
		for(String uriString: uriStrings)
		{
			Uri uri = Uri.parse(uriString);
			try
			{				
				res.add(getLocation(uri));
			}
			catch (Exception ignored)
			{
			}
		}
		return res;
	}

	public void addNewLocation(Location loc, boolean store)
	{
		synchronized (_currentLocations)
		{
			_currentLocations.add(new LocationInfo(loc, store));
			if(store)
				saveCurrentLocationLinks();
		}
	}

	private void removeLocation(String locationId)
	{
		synchronized (_currentLocations)
		{
			LocationInfo li = findExistingLocationInfo(locationId);
			if(li!=null)
			{
				_currentLocations.remove(li);
				if(li.store)
					saveCurrentLocationLinks();
			}
		}
	}
	
	public void removeLocation(Location loc)
	{
		removeLocation(loc.getId());
	}
	
	public void replaceLocation(Location oldLoc, Location newLoc, boolean store)
	{
		synchronized (_currentLocations)
		{
			removeLocation(oldLoc);
			addNewLocation(newLoc, store);
		}
	}

    private void clearLocations()
    {
        synchronized (_currentLocations)
        {
            _currentLocations.clear();
        }
    }

	public static List<Uri> getStoredLocationUris(Settings settings)
	{
		ArrayList<Uri> res = new ArrayList<>();
		try
		{
			List<String> locationStrings = com.sovworks.eds.android.helpers.Util.loadStringArrayFromString(settings.getStoredLocations());
			for(String p: locationStrings)
			{
				try
				{
					res.add(Uri.parse(p));
				}
				catch (Exception e)
				{
					Logger.log(e);
				}
			}
		}
		catch (JSONException e)
		{
			Logger.log(e);
			return res;
		}
		return res;
	}
	
	public void loadStoredLocations()
	{
		synchronized (_currentLocations)
		{
			loadStaticLocations();
			for(Uri u: getStoredLocationUris(_settings))
			{
				try
				{
					Location loc = createLocationFromUri(u);
					if(loc == null)
						throw new IllegalArgumentException("Unsupported location uri: " + u);
					_currentLocations.add(new LocationInfo(loc, true));
				}
				catch (Exception e)
				{
					Logger.log(e);
				}
			}
        }
	}
	
	public Location findExistingLocation(Location loc) throws Exception
	{
		return findExistingLocation(loc.getLocationUri());
	}
	
	public Location findExistingLocation(String locationId)
	{
		synchronized (_currentLocations)
		{
			LocationInfo li = findExistingLocationInfo(locationId);
			return li == null ? null : li.location;
		}		
	}

	public boolean isStoredLocation(String locationId)
	{
		synchronized (_currentLocations)
		{
			LocationInfo li = findExistingLocationInfo(locationId);
			return li != null && li.store;
		}
	}
	
	public Location findExistingLocation(Uri locationUri) throws Exception
	{
		Location t = createLocationFromUri(locationUri);
		if(t == null)
			throw new IllegalArgumentException("Unsupported location uri: " + locationUri);
		return findExistingLocation(t.getId());
	}
	
	public List<Location> getLoadedLocations(final boolean onlyVisible)
	{
		synchronized (_currentLocations)
		{
			return new ArrayList<>(
					new FilteredList<Location>()
					{
						@Override
						protected boolean isValid(Location l)
						{
							return !onlyVisible || l.getExternalSettings().isVisibleToUser();
						}
					}
			);


		}
	}

	public List<EDSLocation> getLoadedEDSLocations(final boolean onlyVisible)
	{
		synchronized (_currentLocations)
		{
			return new ArrayList<>(
				new FilteredList<EDSLocation>()
				{
					@Override
					protected boolean isValid(Location l)
					{
						return l instanceof EDSLocation && (!onlyVisible || l.getExternalSettings().isVisibleToUser());
					}
				}
			);
		}
	}

	public boolean hasOpenLocations()
	{
		synchronized (_currentLocations)
		{
			for(LocationInfo loc: _currentLocations)
				if(	(loc.location instanceof Openable && ((Openable)loc.location).isOpen()))
					return true;
			return false;
		}
	}

	public Location getDefaultLocationFromPath(String path) throws Exception
	{
		Uri u = Uri.parse(path);		
		if(u.getScheme() == null && !path.startsWith("/"))		
			return new DeviceBasedLocation(_settings, StdFs.getStdFs().getPath(Environment.getExternalStorageDirectory().getPath()).combine(path));
		
		return u.getScheme() == null ?
				createDeviceLocation(u)			
			:
				getLocation(u);
	}
	
	public ArrayList<Location> getLocationsFromPaths(Location loc,List<? extends Path> paths)
	{
		ArrayList<Location> res = new ArrayList<>();
		for(Path p: paths)
		{
			Location l = loc.copy();
			l.setCurrentPath(p);
			res.add(l);
		}
		return res;
	}

	public void updateDeviceLocations()
	{
		synchronized (_currentLocations)
		{
			List<LocationInfo> prev = new ArrayList<>();
			for(LocationInfo li: _currentLocations)
				if(li.isDevice)
					prev.add(li);
			List<Location> cur = loadDeviceLocations();
			for(LocationInfo li: prev)
			{
				boolean remove = true;
				for(Location loc: cur)
				{
					if(loc.getId().equals(li.location.getId()))
						remove = false;
				}
				if(remove)
					_currentLocations.remove(li);
			}

			for(Location loc: cur)
			{
				boolean add = true;
				for(LocationInfo li: prev)
				{
					if(loc.getId().equals(li.location.getId()))
						add = false;
				}
				if(add)
				{
					LocationInfo li = new LocationInfo(loc, false);
					li.isDevice = true;
					_currentLocations.add(li);
				}
			}
		}

	}
	
	public void saveCurrentLocationLinks()
	{
		ArrayList<String> links = new ArrayList<>();
		synchronized (_currentLocations)
		{
			for (LocationInfo li: _currentLocations)
				if(li.store)
					links.add(li.location.getLocationUri().toString());
		}
		_settings.setStoredLocations(com.sovworks.eds.android.helpers.Util.storeElementsToString(links));
	}
	
	public String genNewLocationId()
	{		
		while(true)
		{
            String locId = SimpleCrypto.calcStringMD5(String.valueOf(new Date().getTime()) + String.valueOf(new Random().nextLong()));
			if(findExistingLocation(locId) == null)
				return locId;
		}
	}
	
	public Iterable<Location> getLocationsClosingOrder()
	{
		ArrayList<Location> locs = new ArrayList<>();
		for(int i=_openedLocationsStack.size()-1;i>=0;i--)
		{
			Location loc = findExistingLocation(_openedLocationsStack.get(i));
			if(loc!=null)
				locs.add(loc);
		}
		return locs;		
	}
	
	public void regOpenedLocation(Location loc)
	{
		_openedLocationsStack.push(loc.getId());
	}
	
	public void unregOpenedLocation(Location loc)
	{
		String id = loc.getId();
		while(_openedLocationsStack.contains(id))
			_openedLocationsStack.remove(id);
	}
	
	public void closeAllLocations(boolean forceClose, boolean sendBroadcasts)
	{
		closeAllLocations(getLocationsClosingOrder(), forceClose, sendBroadcasts);
		closeAllLocations(new ArrayList<>(getLoadedLocations(false)), forceClose, sendBroadcasts);
	}
	
	public void unmountAndCloseLocation(Location location, boolean forceClose) throws Exception
	{
		if(location instanceof Openable)
			closeLocation(location,forceClose);
	}
	
	public void closeLocation( Location loc,boolean forceClose) throws Exception
	{
		loc.closeFileSystem(forceClose);
		if(loc instanceof Openable)
			OpenableLocationCloserFragment.closeLocation(_context, (Openable) loc, forceClose);

	}

	public Location createLocationFromUri(Uri locationUri) throws Exception
	{
		String scheme = locationUri.getScheme();
		if(scheme == null)
			return findOrCreateDeviceLocation(locationUri);
		switch (locationUri.getScheme())
		{
			case ContainerBasedLocation.URI_SCHEME:
				return createContainerLocation(locationUri);
			case DeviceRootNPLocation.URI_SCHEME:
				return createDeviceRootNPLocation(locationUri);
			case DeviceBasedLocation.URI_SCHEME:
				return createDeviceLocation(locationUri);
			case InternalSDLocation.URI_SCHEME:
				return createBuiltInMemLocation(locationUri);
			case ExternalStorageLocation.URI_SCHEME:
				return createExtStorageLocation(locationUri);
			case TrueCryptLocation.URI_SCHEME:
				return createTrueCryptLocation(locationUri);
			case VeraCryptLocation.URI_SCHEME:
				return createVeraCryptLocation(locationUri);
			case EncFsLocationBase.URI_SCHEME:
				return createEncFsLocation(locationUri);
			case LUKSLocation.URI_SCHEME:
				return createLUKSLocation(locationUri);
			case ContentResolver.SCHEME_CONTENT:
				if(DocumentTreeLocation.isDocumentTreeUri(_context, locationUri))
					return createDocumentTreeLocation(locationUri);
				else
					return createContentResolverLocation(locationUri);
			case DocumentTreeLocation.URI_SCHEME:
				return createDocumentTreeLocation(locationUri);
			default:
				return null;
		}
	}

	protected final Settings _settings;
	final List<LocationInfo> _currentLocations = new ArrayList<>();

	private final Stack<String> _openedLocationsStack = new Stack<>();
	class FilteredList<E> extends AbstractList<E>
	{

		@Override
		public int size()
		{
			synchronized (_currentLocations)
			{
				int res = 0;
				for (LocationInfo li : _currentLocations)
					if (isValid(li.location))
						res++;
				return res;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public E get(int location)
		{
			int res = 0;
			synchronized (_currentLocations)
			{
				for(LocationInfo li: _currentLocations)
				{
					if(isValid(li.location))
					{
						if(res == location)
							return (E)li.location;
						res++;
					}
				}
			}
			throw new IndexOutOfBoundsException();
		}

		protected boolean isValid(Location l)
		{
			return true;
		}
	
	}

	protected static class LocationInfo
	{
		LocationInfo(Location location, boolean store) { this.location = location; this.store = store;}
		Location location;
		boolean store;
		boolean isDevice;
	}

	protected Context getContext()
	{
		return _context;
	}

	private LocationInfo findExistingLocationInfo(String locationId)
	{
		for(LocationInfo li: _currentLocations)
			if(li.location.getId().equals(locationId))
				return li;
		return null;
	}

	protected String getLocationIdFromUri(Uri locationUri) throws Exception
	{
		String scheme = locationUri.getScheme();
		if(scheme == null)
			return null;
		switch (locationUri.getScheme())
		{
			case ContainerBasedLocation.URI_SCHEME:
				return ContainerBasedLocation.getLocationId(this, locationUri);
			case DeviceBasedLocation.URI_SCHEME:
				return DeviceBasedLocation.getLocationId(locationUri);
			case InternalSDLocation.URI_SCHEME:
				return InternalSDLocation.getLocationId(locationUri);
			case ExternalStorageLocation.URI_SCHEME:
				return ExternalStorageLocation.getLocationId(locationUri);
			case TrueCryptLocation.URI_SCHEME:
				return TrueCryptLocation.getLocationId(this, locationUri);
			case VeraCryptLocation.URI_SCHEME:
				return VeraCryptLocation.getLocationId(this, locationUri);
			case EncFsLocationBase.URI_SCHEME:
				return EncFsLocationBase.getLocationId(this, locationUri);
			case LUKSLocation.URI_SCHEME:
				return LUKSLocation.getLocationId(this, locationUri);
			case ContentResolver.SCHEME_CONTENT:
				if(DocumentTreeLocation.isDocumentTreeUri(_context, locationUri))
					return DocumentTreeLocation.getLocationId(locationUri);
				else
					return ContentResolverLocation.getLocationId();
			case DocumentTreeLocation.URI_SCHEME:
				return DocumentTreeLocation.getLocationId(locationUri);
			default:
				return null;
		}
	}

	private Location findOrCreateDeviceLocation(Uri locationUri) throws IOException
	{
		StringPathUtil path = new StringPathUtil(locationUri.getPath()),
				chroot = null,
				sdcardPath = new StringPathUtil("sdcard");
		int maxComp = 0;
		Location res = null;
		for(Location loc: getLoadedLocations(true))
		{
			if(loc instanceof InternalSDLocation || loc instanceof ExternalStorageLocation)
			{
				StringPathUtil tmpChroot = new StringPathUtil(((DeviceBasedLocation)loc).getRootPath());
				if((loc instanceof InternalSDLocation && sdcardPath.equals(path)))
				{
					res = loc;
					chroot = sdcardPath;
					break;
				}
				else if(tmpChroot.equals(path))
				{
					res = loc;
					chroot = tmpChroot;
					break;
				}
				if(tmpChroot.getComponents().length > maxComp)
				{
					if(loc instanceof InternalSDLocation && sdcardPath.isParentDir(path))
					{
						res = loc;
						maxComp = tmpChroot.getComponents().length;
						chroot = sdcardPath;
					}
					else if(tmpChroot.isParentDir(path))
					{
						res = loc;
						maxComp = tmpChroot.getComponents().length;
						chroot = tmpChroot;
					}
				}
			}
		}
		if(res!=null)
		{
			res = res.copy();
			res.setCurrentPath(res.getFS().getPath(path.getSubPath(chroot).toString()));
			return res;
		}
		return new DeviceBasedLocation(_settings, locationUri);
	}

	private Location createContainerLocation(Uri locationUri) throws Exception
	{
		return new ContainerBasedLocation(locationUri, this, getContext(), _settings);
	}

	private Location createTrueCryptLocation(Uri locationUri) throws Exception
	{
		return new TrueCryptLocation(locationUri, this, getContext(), _settings);
	}

	private Location createVeraCryptLocation(Uri locationUri) throws Exception
	{
		return new VeraCryptLocation(locationUri, this, getContext(), _settings);
	}

	private Location createEncFsLocation(Uri locationUri) throws Exception
	{
		return new EncFsLocation(locationUri, this, getContext(), _settings);
	}

	private Location createLUKSLocation(Uri locationUri) throws Exception
	{
		return new LUKSLocation(locationUri, this, getContext(), _settings);
	}

	private Location createDeviceLocation(Uri locationUri) throws IOException
	{
		/*String pathString = locationUri.getPath();
		if(pathString == null)
			pathString = "/";
		PathUtil pu = new PathUtil(pathString);
		java.io.File extStore = Environment.getExternalStorageDirectory();
		if(
				(extStore!=null && new PathUtil(extStore.getPath()).isParentDir(pu)) ||
				pathString.startsWith("/sdcard/")
		)
			return new InternalSDLocation(_context, pathString);
		*/

		return new DeviceBasedLocation(_settings, locationUri);
	}

	private Location createDeviceRootNPLocation(Uri locationUri) throws IOException
	{
		return new DeviceRootNPLocation(_context, _settings, locationUri);
	}

	private Location createBuiltInMemLocation(Uri locationUri) throws IOException
	{
		return new InternalSDLocation(_context, locationUri);
	}

	private Location createExtStorageLocation(Uri locationUri) throws IOException
	{
		return new ExternalStorageLocation(_context, locationUri);
	}

	private Location createDocumentTreeLocation(Uri uri) throws Exception
	{
		if(DocumentTreeLocation.URI_SCHEME.equals(uri.getScheme()))
			return DocumentTreeLocation.fromLocationUri(_context, uri);
		else
			return new DocumentTreeLocation(_context, uri);
	}

	private Location createContentResolverLocation(Uri locationUri) throws Exception
	{
		return new ContentResolverLocation(_context, locationUri);
	}

	private void loadStaticLocations()
	{
		for(Location l: loadDeviceLocations())
		{
			LocationInfo li = new LocationInfo(l, false);
			li.isDevice = true;
			_currentLocations.add(li);
		}
	}

	protected ArrayList<Location> loadDeviceLocations()
	{
		ArrayList<Location> res = new ArrayList<>();
		try
		{
			Location location = new DeviceRootNPLocation(_context);
			location.getExternalSettings().setVisibleToUser(true);
			res.add(location);
			StorageOptions.reloadStorageList(_context);
			for(StorageOptions.StorageInfo si: StorageOptions.getStoragesList(getContext()))
			{
				if(si.isExternal)
				{
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP || new File(si.path).isDirectory())
					{
						Location extLoc = new ExternalStorageLocation(_context, si.label, si.path, null);
						extLoc.getFS(); //pre-create fs to use the same fs instance everywhere
						extLoc.getExternalSettings().setVisibleToUser(true);
						res.add(extLoc);
					}
				}
				else
				{
					location = new InternalSDLocation(_context, si.label, si.path, null);
					location.getFS(); //pre-create fs to use the same fs instance everywhere
					location.getExternalSettings().setVisibleToUser(true);
					res.add(location);
				}
			}
		}
		catch (IOException e)
		{
			Logger.showAndLog(_context, e);
		}
		return res;
	}

	private final Context _context;
	private MediaMountedReceiver _mediaChangedReceiver;

}
