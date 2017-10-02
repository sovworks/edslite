package com.sovworks.eds.android.dialogs;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.VersionHistory;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.providers.MainContentProvider;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.DeviceBasedLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.util.exec.ExecuteExternalProgram;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public abstract class AboutDialogBase extends DialogFragment
{
	public static void showDialog(FragmentManager fm)
	{
		DialogFragment newFragment = new AboutDialog();
	    newFragment.show(fm, "AboutDialog");
	}

	public static String getVersionName(Context context)
	{
		try
		{
			 return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Logger.log(e);
			return "";
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Util.setDialogStyle(this);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.about_dialog, container);
		String verName = getVersionName(getActivity());
    	String aboutMessage = String.format(
    			"%s v%s\n%s",
    			getResources().getString(R.string.eds),
    			verName,
    			getResources().getString(R.string.about_message)
    		);

    	((TextView)v.findViewById(R.id.about_text_view)).setText(aboutMessage);
		v.findViewById(R.id.show_version_history_button).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getActivity(), VersionHistory.class));
            }
        });
		v.findViewById(R.id.contact_support_button).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    sendSupportRequest();
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(getActivity(), e);
                }
            }
        });
		v.findViewById(R.id.get_program_log).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				try
				{
					saveDebugLog();
				}
				catch (Throwable e)
				{
					Logger.showAndLog(getActivity(), e);
				}
			}
		});
		return v;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		setWidthHeight();
	}

	protected void setWidthHeight()
	{
		Window w = getDialog().getWindow();
		if(w!=null)
			w.setLayout(calcWidth(), calcHeight());
	}

	protected int calcWidth()
	{
		return getResources().getDimensionPixelSize(R.dimen.about_dialog_width);
	}

	protected int calcHeight()
	{
		return WRAP_CONTENT;
		//return getResources().getDimensionPixelSize(R.dimen.about_dialog_heigh);
	}

	protected String getSubjectString()
	{
		return "EDS support";
	}

	private void sendSupportRequest()
	{
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{GlobalConfig.SUPPORT_EMAIL});
		String subj = getSubjectString();
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subj);
		emailIntent.putExtra(Intent.EXTRA_TEXT, Util.getSystemInfoString());
		startActivity(Intent.createChooser(emailIntent, "Send mail"));
	}

	private void saveDebugLog() throws IOException, ApplicationException
	{
		Context ctx = getActivity();
		File out = ctx.getExternalFilesDir(null);
		if(out == null || !out.canWrite())
			out = ctx.getFilesDir();
		Location loc = new DeviceBasedLocation(
				UserSettings.getSettings(ctx),
				new StringPathUtil(out.getPath()).combine(
						String.format(
								Locale.US,
								"eds-log-%1$tY%1$tm%1$td%1$tH%1$tM%1$tS.txt",
								new Date()
						)
				).toString()
		);
		dumpLog(loc);
		sendLogFile(loc);
	}

	protected void dumpLog(Location logLocation) throws IOException, ApplicationException
	{
		ExecuteExternalProgram.executeAndReadString(
				"logcat",
				"-df",
				logLocation.getCurrentPath().getPathString());
	}

	private void sendLogFile(Location logLocation)
	{
		Context ctx = getActivity();
		Uri uri = MainContentProvider.getContentUriFromLocation(logLocation);
		Intent actionIntent = new Intent(Intent.ACTION_SEND);
		actionIntent.setType("text/plain");
		actionIntent.putExtra(Intent.EXTRA_STREAM, uri);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			ClipData cp = ClipData.newUri(
					ctx.getContentResolver(),
					ctx.getString(R.string.get_program_log),
					uri
			);
			actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			actionIntent.setClipData(cp);
		}

		Intent startIntent = Intent.createChooser(
				actionIntent,
				ctx.getString(R.string.save_log_file_to)
		);
		startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(startIntent);
	}

}
