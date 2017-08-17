package com.sovworks.eds.android.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;

import java.io.IOException;

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
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.dismiss();
								FileManagerActivity act = (FileManagerActivity) getActivity();
								if(act!=null)
									act.requestExtStoragePermission();
							}
                        })
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								FileManagerActivity act = (FileManagerActivity) getActivity();
								if(act!=null)
									try
									{
										act.initActionMain();
									}
									catch (IOException e)
									{
										Logger.showAndLog(getActivity(), e);
									}
							}
						});
		return builder.create();		
	}

}
