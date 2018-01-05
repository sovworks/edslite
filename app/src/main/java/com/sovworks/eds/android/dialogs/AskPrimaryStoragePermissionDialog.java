package com.sovworks.eds.android.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.ExtStorageWritePermisisonCheckFragment;

public class AskPrimaryStoragePermissionDialog extends DialogFragment
{
	public static void showDialog(FragmentManager fm)
	{
		DialogFragment newFragment = new AskPrimaryStoragePermissionDialog();
	    newFragment.show(fm, "AskPrimaryStoragePermissionDialog");
	}
	
	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.storage_permission_desc)
                .setPositiveButton(R.string.grant,
						(dialog, id) ->
						{
                            dialog.dismiss();
							ExtStorageWritePermisisonCheckFragment stateFragment = (ExtStorageWritePermisisonCheckFragment) getFragmentManager().findFragmentByTag(ExtStorageWritePermisisonCheckFragment.TAG);
							if(stateFragment!=null)
								stateFragment.requestExtStoragePermission();
                        })
				.setNegativeButton(android.R.string.cancel,
						(dialog, id) ->
						{
							ExtStorageWritePermisisonCheckFragment stateFragment = (ExtStorageWritePermisisonCheckFragment) getFragmentManager().findFragmentByTag(ExtStorageWritePermisisonCheckFragment.TAG);
							if(stateFragment!=null)
								stateFragment.cancelExtStoragePermissionRequest();
                        });
		return builder.create();		
	}

}
