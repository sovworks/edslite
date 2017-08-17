package com.sovworks.eds.android.locations;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.fs.ContentResolverFs;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.LocationBase;

import java.io.IOException;

public class ContentResolverLocation extends LocationBase
{
	public static String getLocationId()
	{
		return URI_SCHEME;
	}

	public static final String URI_SCHEME = ContentResolver.SCHEME_CONTENT;
	
	public ContentResolverLocation(Context context)
	{
		super(UserSettings.getSettings(context), new SharedData(
				getLocationId(),
				context
		));
	}
	
	public ContentResolverLocation(Context context, Uri uri) throws IOException
	{
		this(context);
		loadFromUri(uri);
	}

	public ContentResolverLocation(ContentResolverLocation sibling)
	{
		super(sibling);
	}

	@Override
	public void loadFromUri(Uri uri)
	{
		super.loadFromUri(uri);
		if(uri.getPath()!=null && uri.getPath().length()>1)
			_currentPathString = uri.toString();
	}

	@Override
	public String getTitle()
	{
		return getContext().getString(R.string.content_provider);
	}

	@Override
	public synchronized ContentResolverFs getFS() throws IOException
	{
		if(getSharedData().fs == null)
			getSharedData().fs = new ContentResolverFs(getContext().getContentResolver());

		return (ContentResolverFs) getSharedData().fs;
	}

	@Override
	public Path getCurrentPath() throws IOException
	{
		return _currentPathString == null ? getFS().getRootPath() : getFS().getPath(_currentPathString);
	}

	@Override
	public void setCurrentPath(Path path)
	{
		_currentPathString = path == null ? null : path.getPathString();
	}

	@Override
	public Uri getLocationUri()
	{
		if(_currentPathString != null)
			return Uri.parse(_currentPathString);
		
		Uri.Builder ub = new Uri.Builder();
		ub.scheme(URI_SCHEME);
		ub.path("/");
		return ub.build();
	}

	@Override
	public ContentResolverLocation copy()
	{
		return new ContentResolverLocation(this);
	}

	@Override
	public Uri getDeviceAccessibleUri(com.sovworks.eds.fs.Path path)
	{
		try
		{
			return ((ContentResolverFs.Path)getCurrentPath()).getUri();
		}
		catch (IOException e)
		{
			Logger.log(e);
			return null;
		}
	}

	@Override
	public void saveExternalSettings()
	{

	}

	@Override
	protected SharedData getSharedData()
	{
		return (SharedData) super.getSharedData();
	}

	protected Context getContext()
	{
		return getSharedData().context;
	}

	protected static class SharedData extends LocationBase.SharedData
	{
		protected SharedData(String id, Context context)
		{
			super(id);
			this.context = context;
		}

		final Context context;
	}
	
}
