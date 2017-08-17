package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.android.providers.MainContentProvider;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.FS;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;
import java.util.Arrays;

public abstract class EncFsLocationBase extends EDSLocationBase
{
    public static String getLocationId(LocationsManagerBase lm, Uri locationUri) throws Exception
    {
        return getId(getContainerLocationFromUri(locationUri, lm));
    }
    public static String getId(Location containerLocation)
    {
        return SimpleCrypto.calcStringMD5(containerLocation.getLocationUri().toString());
    }

	public static final String URI_SCHEME = "encfs";

    public EncFsLocationBase(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        this(getContainerLocationFromUri(uri, lm), null, context, settings);
        loadFromUri(uri);
    }

	public EncFsLocationBase(Location containerLocation, FS encFs, Context context, Settings settings)
	{
		super(settings, new SharedData(
                getId(containerLocation),
                createInternalSettings(),
                containerLocation,
                context
                )
        );
		getSharedData().encFs = encFs;
	}

    public EncFsLocationBase(EncFsLocationBase sibling)
    {
        super(sibling);
    }

    @Override
    public void loadFromUri(Uri uri)
    {
        super.loadFromUri(uri);
        String p = uri.getPath();
        if(p != null && p.startsWith("/"))
            p = p.substring(1);
        _currentPathString = p == null || "/".equals(p) || p.isEmpty() ? null : p;
    }

    @Override
	public void open() throws Exception
	{
		if(isOpenOrMounted())
			return;
		byte[] pass = getFinalPassword();
		try
		{
			Location encfsLocation = getSharedData().containerLocation;//Mounter.getNonEmulatedDeviceLocationIfNeeded(_globalSettings, _context, _location);
			//if(encfsLocation == null)
			//	encfsLocation = _location;

			getSharedData().encFs = new FS(encfsLocation.getCurrentPath(), pass, (ContainerOpeningProgressReporter) _openingProgressReporter);
		}
		finally
		{
			Arrays.fill(pass, (byte) 0);
		}
	}

	@Override
	public void close(boolean force) throws IOException
	{
		super.close(force);
		getSharedData().encFs = null;
	}

	@Override
	public Uri getLocationUri()
	{
		return makeUri(URI_SCHEME).build();
	}

	@Override
	public boolean isOpen()
	{
		return getSharedData().encFs != null;
	}

	public FS getEncFs()
	{
		return getSharedData().encFs;
	}

	@Override
	public Uri getDeviceAccessibleUri(Path path)
	{
		return !_globalSettings.dontUseContentProvider() ? MainContentProvider.getContentUriFromLocation(this, path) : null;

	}
	protected static class SharedData extends EDSLocationBase.SharedData
	{

        public SharedData(String id, InternalSettings settings, Location location, Context ctx)
		{
            super(id, settings, location, ctx);
        }

		FS encFs;

	}

	@Override
	protected SharedData getSharedData()
	{
		return (SharedData)super.getSharedData();
	}

	@Override
	protected FileSystem createBaseFS(boolean readOnly) throws IOException
	{
		if(getSharedData().encFs == null)
			throw new RuntimeException("File system is closed");
		return getSharedData().encFs;
	}
}
