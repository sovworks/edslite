package com.sovworks.eds.android.service;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.providers.MainContentProvider;
import com.sovworks.eds.locations.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

public class ActionSendTask extends PrepareTempFilesTask
{
	public static final String ARG_MIME_TYPE = "com.sovworks.eds.android.ARG_MIME_TYPE";

	public static class Param extends FilesTaskParam
	{
		public Param(Intent i, Context context)
		{
			super(i, context);
			_mimeType = i.getStringExtra(ARG_MIME_TYPE);
		}

		public String getMimeType()
		{
			return _mimeType;
		}

		@Override
		public boolean forceOverwrite()
		{
			return true;
		}

		private final String _mimeType;
	}

    public static void sendFiles(Context context, List<Location> files, String mimeType)
	{
		if(files == null || files.isEmpty())
			return;

		ArrayList<Uri> uris = new ArrayList<>();
		for (Location l : files)
			try
			{
				Uri uri = l.getDeviceAccessibleUri(l.getCurrentPath());
				if(uri == null)
					uri = MainContentProvider.getContentUriFromLocation(l);
				uris.add(uri);
			}
			catch (IOException e)
			{
				Logger.log(e);
			}
		sendFiles(context, uris, mimeType, null);
	}

	public static void sendFiles(Context context, ArrayList<Uri> uris, String mime, ClipData clipData)
	{
		if(uris == null || uris.isEmpty())
			return;

		Intent actionIntent = new Intent(uris.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
		actionIntent.setType(mime == null ? "*/*" : mime);
		if (uris.size() > 1)
			actionIntent.putExtra(Intent.EXTRA_STREAM, uris);
		else
			actionIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && clipData!=null)
		{
			actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			actionIntent.setClipData(clipData);
		}

		Intent startIntent = Intent.createChooser(actionIntent, context.getString(R.string.send_files_to));
		startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(startIntent);
	}

	@Override
	protected Param getParam()
	{
		return (Param) super.getParam();
	}

	@Override
	public void onCompleted(Result result)
	{
		try
		{
			@SuppressWarnings("unchecked") List<Location> tmpFilesList = (List<Location>) result.getResult();
			sendFiles(_context, tmpFilesList, getParam().getMimeType());
		}
		catch(CancellationException ignored)
		{

		}
		catch (Throwable e)
		{
			reportError(e);
		}
		finally
		{
			super.onCompleted(result);
		}
	}
	
	@Override
	protected FilesTaskParam initParam(Intent i)
	{
		return new Param(i, _context);
	}
}