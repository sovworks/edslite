package com.sovworks.eds.android.locations;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.fs.DocumentTreeFS;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.LocationBase;

import java.io.IOException;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DocumentTreeLocation extends LocationBase
{
	public static final String URI_SCHEME = "doc-tree";

	public static String getLocationId(Uri locationUri)
	{
		return getId(getDocumentUri(locationUri));
	}

	public static String getId(Uri treeUri)
	{
		return URI_SCHEME + treeUri;
	}

	public static Uri getDocumentUri(Uri locationUri)
	{
		return DocumentsContract.buildTreeDocumentUri(
				locationUri.getAuthority(),
				DocumentsContract.getTreeDocumentId(locationUri)
		);
	}

	public static boolean isDocumentTreeUri(Context context, Uri uri)
	{
		try
		{
			//noinspection ConstantConditions
			return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
					DocumentsContract.getTreeDocumentId(uri) != null && DocumentFile.isDocumentUri(context, uri);
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}


	public static DocumentTreeLocation fromLocationUri(Context context, Uri locationUri) throws IOException
	{
		DocumentTreeLocation loc = new DocumentTreeLocation(
				context,
				getDocumentUri(locationUri)
		);
		loc.loadFromUri(locationUri);
		return loc;
	}

	public DocumentTreeLocation(Context context, Uri treeUri)
	{
		super(UserSettings.getSettings(context), new SharedData(
				getId(treeUri),
				context,
				treeUri
		));
	}

	public DocumentTreeLocation(DocumentTreeLocation sibling)
	{
		super(sibling);
	}

	@Override
	public void loadFromUri(Uri uri)
	{
		super.loadFromUri(uri);
		_currentPathString = uri.buildUpon().scheme(ContentResolver.SCHEME_CONTENT).build().toString();
	}

	@Override
	public String getTitle()
	{
		try
		{
			return getFS().getRootPath().getDirectory().getName();
		}
		catch (IOException e)
		{
			Logger.log(e);
			return getTreeUri().toString();
		}
	}

	@Override
	public synchronized DocumentTreeFS getFS() throws IOException
	{
		if(getSharedData().fs == null)
		{
			getSharedData().fs = new DocumentTreeFS(getContext(), getTreeUri());
		}

		return (DocumentTreeFS) getSharedData().fs;
	}

    @Override
    public DocumentTreeFS.DocumentPath getCurrentPath() throws IOException
    {
        return (DocumentTreeFS.DocumentPath) super.getCurrentPath();
    }

    @Override
	public Uri getLocationUri()
	{
		try
		{
			DocumentTreeFS.DocumentPath path = getCurrentPath();
			Uri.Builder ub = path.getPathUri().buildUpon();
			ub.scheme(URI_SCHEME);
			return ub.build();
		}
		catch (IOException err)
		{
			throw new IllegalStateException("Wrong path", err);
		}
	}

	@Override
	public DocumentTreeLocation copy()
	{
		return new DocumentTreeLocation(this);
	}

	@Override
	public boolean isReadOnly()
	{
		return false;
	}
	
	@Override
	public boolean isEncrypted()
	{
		return false;
	}

	@Override
	public Uri getDeviceAccessibleUri(Path path)
	{
		try
		{
			return ((DocumentTreeFS.DocumentPath)path).getDocumentUri();
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	protected ExternalSettings loadExternalSettings()
	{
		ExternalSettings res = new ExternalSettings();
		res.load(_globalSettings,getId());
		return res;
	}

	@Override
	protected SharedData getSharedData()
	{
		return (SharedData) super.getSharedData();
	}

	protected Uri getTreeUri()
	{
		return getSharedData().treeUri;
	}

	protected Context getContext()
	{
		return getSharedData().context;
	}

	protected static class SharedData extends LocationBase.SharedData
	{

		protected SharedData(String id, Context ctx, Uri treeUri)
		{
			super(id);
			context = ctx;
			this.treeUri = treeUri;
		}
		public final Context context;
		public final Uri treeUri;
	}


}
