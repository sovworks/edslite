package com.sovworks.eds.android.locations.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.activities.SettingsBaseActivity;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;

public class OverwriteContainerDialog extends DialogFragment
{

	public static void showDialog(FragmentManager fm)
	{
		showDialog(fm, 0);
	}	
	
	public static void showDialog(FragmentManager fm, int requestResId)
	{		
		DialogFragment newFragment = new OverwriteContainerDialog();
		if(requestResId > 0)
		{
			Bundle args = new Bundle();
			args.putInt(ARG_REQUEST_RES_ID, requestResId);
			newFragment.setArguments(args);
		}
	    newFragment.show(fm, "com.sovworks.eds.android.locations.dialogs.OverwriteContainerDialog");
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		Bundle args = getArguments();
		int resId = args!=null ? args.getInt(ARG_REQUEST_RES_ID, R.string.do_you_want_to_overwrite_existing_file) : R.string.do_you_want_to_overwrite_existing_file;
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(resId)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.dismiss();
								doOverwrite();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.cancel();
							}
						});
		return builder.create();		
	}
	
	protected void doOverwrite()
    {
        CreateEDSLocationFragment f = (CreateEDSLocationFragment) getFragmentManager().findFragmentByTag(SettingsBaseActivity.SETTINGS_FRAGMENT_TAG);
        if (f != null)
        {
            f.setOverwrite(true);
            f.startCreateLocationTask();
        }
    }
	
	private static final String ARG_REQUEST_RES_ID = "com.sovworks.eds.android.TEXT_ID";

}
