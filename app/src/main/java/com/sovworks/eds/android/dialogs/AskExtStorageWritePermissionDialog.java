package com.sovworks.eds.android.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.opener.fragments.ExternalStorageOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;

public class AskExtStorageWritePermissionDialog extends DialogFragment
{
	public static void showDialog(FragmentManager fm, String openerTag)
	{
		Bundle args = new Bundle();
		args.putString(LocationOpenerBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG, openerTag);
		DialogFragment newFragment = new AskExtStorageWritePermissionDialog();
		newFragment.setArguments(args);
	    newFragment.show(fm, "AskExtStorageWritePermissionDialog");
	}
	
	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.ext_storage_write_permission_request)
                .setPositiveButton(R.string.grant,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.dismiss();
								ExternalStorageOpenerFragment f = getRecFragment();
								if(f!=null)
									f.showSystemDialog();
							}
                        })
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								ExternalStorageOpenerFragment f = getRecFragment();
								if(f!=null)
									f.setDontAskPermissionAndOpenLocation();

							}
						});
		return builder.create();		
	}

	private ExternalStorageOpenerFragment getRecFragment()
	{
		return (ExternalStorageOpenerFragment) getFragmentManager().
				findFragmentByTag(
						getArguments().getString(
								LocationOpenerBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG
						)
				);
	}

}
