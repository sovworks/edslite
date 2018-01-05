package com.sovworks.eds.android.service;

import android.content.Context;
import android.content.Intent;

import com.sovworks.eds.android.EdsApplication;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstPlain;
import com.sovworks.eds.fs.util.SrcDstRec;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.locations.Location;

import java.io.IOException;
import java.util.concurrent.CancellationException;

public class ClearTempFolderTask extends WipeFilesTask
{
	public static final String ARG_EXIT_PROGRAM = "com.sovworks.eds.android.EXIT_PROGRAM";

	public static SrcDstCollection getMirrorFiles(Context context) throws IOException
	{
		Location loc = FileOpsService.getSecTempFolderLocation(
				UserSettings.getSettings(context).getWorkDir(),
				context
		);
		if(loc.getCurrentPath()!=null && loc.getCurrentPath().exists())
		{
			SrcDstRec sdr = new SrcDstRec(new SrcDstSingle(
					FileOpsService.getSecTempFolderLocation(
							UserSettings.getSettings(context).getWorkDir(),
							context
					),
					null
			)
			);
			sdr.setIsDirLast(true);
			return sdr;
		}
		else
			return new SrcDstPlain();
	}

	public static class Param extends FileOperationTaskBase.FileOperationParam
	{
		public Param(Intent i, Context context)
		{
			super(i);
			_context = context;
		}

		public boolean shouldExitProgram()
		{
			return getIntent().getBooleanExtra(ARG_EXIT_PROGRAM, false);
		}

		@Override
		protected SrcDstCollection loadRecords(Intent i)
		{
			try
			{
				return getMirrorFiles(_context);
			}
			catch (IOException e)
			{
				Logger.log(e);
			}

			return null;
		}

		private final Context _context;
	}

	public ClearTempFolderTask()
	{
		super(true);
	}

	@Override
	protected Param getParam()
	{
		return (Param) super.getParam();
	}

	@Override
	protected FileOperationParam initParam(Intent i)
	{
		return new Param(i, _context);
	}

	@Override
	public void onCompleted(Result result)
	{
		if(getParam().shouldExitProgram())
		{
			try
			{
				removeNotification();
				result.getResult();
				EdsApplication.stopProgram(_context, true);
			}
			catch(CancellationException ignored)
			{
			}
			catch (Throwable e)
			{
				reportError(e);
				EdsApplication.stopProgram(_context, false);
			}

		}
		else
			super.onCompleted(result);

	}
}