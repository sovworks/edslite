package com.sovworks.eds.android.filemanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.EditText;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.tasks.CreateNewFileTask;

public class NewFileDialog extends DialogFragment
{
	public static void showDialog(FragmentManager fm, int type)
	{
		DialogFragment newFragment = new NewFileDialog();
		Bundle b = new Bundle();
		b.putInt(CreateNewFileTask.ARG_TYPE, type);
		newFragment.setArguments(b);
	    newFragment.show(fm, "NewFileDialog");
	}

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		int ft = getArguments().getInt(CreateNewFileTask.ARG_TYPE);

		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
		//alert.setMessage(getString(R.string.enter_new_file_name));

		// Set an EditText view to get user input
		final EditText input = new EditText(getActivity());
		input.setSingleLine();
		input.setHint(getString(ft == CreateNewFileTask.FILE_TYPE_FOLDER ?
                R.string.enter_new_folder_name
                :
                R.string.enter_new_file_name));
		alert.setView(input);

		alert.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{						
						makeNewFile(input.getText().toString());
                        dialog.dismiss();
					}
				});

		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						// Canceled.
					}
				});

		return alert.create();
	}

	private void makeNewFile(String newName)
	{
        getFragmentManager()
		.beginTransaction()
		.add(
				CreateNewFileTask.newInstance(
                        newName,
						getArguments().getInt(CreateNewFileTask.ARG_TYPE)
                ),
				CreateNewFileTask.TAG
		)
		.commit();
	}
}
