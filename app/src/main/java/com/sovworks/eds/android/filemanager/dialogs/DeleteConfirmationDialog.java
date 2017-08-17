package com.sovworks.eds.android.filemanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;

public class DeleteConfirmationDialog extends DialogFragment
{

	public static final String TAG = "DeleteConfirmationDialog";
	public static void showDialog(FragmentManager fm,Bundle args)
	{		
		DialogFragment newFragment = new DeleteConfirmationDialog();
		newFragment.setArguments(args);
	    newFragment.show(fm, TAG);
	}
	
	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.do_you_really_want_to_delete_selected_files)
				.setCancelable(true)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
                                FileListViewFragment frag = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
                                if(frag!=null)
                                    frag.deleteFiles(getArguments());
								dialog.dismiss();
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

}
