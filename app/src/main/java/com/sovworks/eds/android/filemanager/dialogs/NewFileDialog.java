package com.sovworks.eds.android.filemanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.EditText;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.tasks.CreateNewFile;

public class NewFileDialog extends DialogFragment
{
	public interface Receiver
	{
		void makeNewFile(String name, int type);
	}

	private static final String ARG_TYPE = "com.sovworks.eds.android.TYPE";
	private static final String ARG_RECEIVER_TAG = "com.sovworks.eds.android.RECEIVER_TAG";

	public static void showDialog(FragmentManager fm, int type, String receiverTag)
	{
		DialogFragment newFragment = new NewFileDialog();
		Bundle b = new Bundle();
		b.putInt(ARG_TYPE, type);
		b.putString(ARG_RECEIVER_TAG, receiverTag);
		newFragment.setArguments(b);
	    newFragment.show(fm, "NewFileDialog");
	}

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		int ft = getArguments().getInt(ARG_TYPE);

		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
		//alert.setMessage(getString(R.string.enter_new_file_name));

		// Set an EditText view to get user input
		final EditText input = new EditText(getActivity());
		input.setSingleLine();
		input.setHint(getString(ft == CreateNewFile.FILE_TYPE_FOLDER ?
                R.string.enter_new_folder_name
                :
                R.string.enter_new_file_name));
		alert.setView(input);

		alert.setPositiveButton(getString(android.R.string.ok),
				(dialog, whichButton) ->
				{
					Receiver r = (Receiver) getFragmentManager().findFragmentByTag(
							getArguments().getString(ARG_RECEIVER_TAG)
					);
					if(r != null)
						r.makeNewFile(
								input.getText().toString(),
								getArguments().getInt(ARG_TYPE)
						);
                    dialog.dismiss();
                });

		alert.setNegativeButton(android.R.string.cancel,
				(dialog, whichButton) ->
				{
                    // Canceled.
                });

		return alert.create();
	}
}
