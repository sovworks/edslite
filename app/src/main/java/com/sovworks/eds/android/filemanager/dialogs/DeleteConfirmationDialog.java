package com.sovworks.eds.android.filemanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragmentBase;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.ArrayList;

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
		Bundle args = getArguments();
		ArrayList<Path> paths = new ArrayList<>();
		Location loc = LocationsManager.getLocationsManager(getActivity()).getFromBundle(args, paths);
		boolean wipe = args.getBoolean(FileListViewFragmentBase.ARG_WIPE_FILES, true);
		String fn = "";
		if(loc!=null)
		{
			if(paths.size() > 1)
				fn = String.valueOf(paths.size());
			else if(paths.isEmpty())
				fn = loc.getLocationUri().toString();
			else
				fn = paths.get(0).getPathString();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getActivity().getString(R.string.do_you_really_want_to_delete_selected_files, fn))
				.setCancelable(true)
				.setPositiveButton(R.string.yes,
						(dialog, id) ->
						{
							FileListViewFragment frag = (FileListViewFragment) getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
							if(frag!=null)
								frag.deleteFiles(loc, paths, wipe);
                            dialog.dismiss();
                        })
				.setNegativeButton(R.string.no,
						(dialog, id) -> dialog.cancel());
		return builder.create();
	}

}
