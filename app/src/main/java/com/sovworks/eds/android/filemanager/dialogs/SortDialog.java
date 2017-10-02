package com.sovworks.eds.android.filemanager.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.sovworks.eds.android.R;

public class SortDialog extends DialogFragment
{
	
	public interface SortingReceiver
	{
		void applySort(int sortMode);
	}	
	
	public static void showDialog(FragmentManager fm,int sortMode, String receiverFragmentTag)
	{
		showDialog(fm, sortMode, R.array.sort_mode, receiverFragmentTag);
	}

	public static void showDialog(FragmentManager fm,int sortMode,int sortLabelsResId, String receiverFragmentTag)
	{
		DialogFragment newFragment = new SortDialog();		
		Bundle b = new Bundle();		
		b.putInt(ARG_SORT_MODE, sortMode);
		b.putInt(ARG_SORT_LABELS_RES_ID, sortLabelsResId);
        if(receiverFragmentTag!=null)
            b.putString(ARG_RECEIVER_FRAGMENT_TAG, receiverFragmentTag);
		newFragment.setArguments(b);
	    newFragment.show(fm, "SortDialog");
	}	
	
	@NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
	{			
		@SuppressLint("InflateParams") View v = getActivity().getLayoutInflater().inflate(R.layout.sort_dialog, null);
		final ListView listView = v.findViewById(android.R.id.list);
        final RadioGroup sortDirection = v.findViewById(R.id.sort_group);
		listView.setAdapter(
            new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_single_choice,
                getActivity().getResources().getStringArray(
                        getArguments().getInt(ARG_SORT_LABELS_RES_ID, R.array.sort_mode)
                )
            )
        );
		int sortMode = getArguments().getInt(ARG_SORT_MODE);
		listView.setItemChecked(sortMode/2, true);
		boolean asc = sortMode % 2 == 0;
        sortDirection.check(asc ? R.id.sort_asc : R.id.sort_desc);

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.sort)
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            int pos = listView.getCheckedItemPosition();
                            if (pos == ListView.INVALID_POSITION)
                                pos = -1;
                            applySort(pos, sortDirection.getCheckedRadioButtonId() == R.id.sort_asc);
                            dialog.dismiss();
                        }
                    }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int whichButton)
                            {
                                // Canceled.
                            }
                        }
                );
		return alert.create();
	}	
	
	
	protected void applySort(int listPos,boolean isAscending)
	{
        int sortMode = listPos*2 + (isAscending ? 0 : 1);
        String rft = getArguments().getString(ARG_RECEIVER_FRAGMENT_TAG);
        if(rft!=null)
        {
            SortingReceiver sr = (SortingReceiver) getFragmentManager().findFragmentByTag(rft);
            if(sr!=null)
                sr.applySort(sortMode);
        }
        else if(getActivity() instanceof SortingReceiver)
			((SortingReceiver)getActivity()).applySort(sortMode);
	}
	
	private static final String ARG_SORT_MODE = "com.sovworks.eds.android.SORT_MODE";
	private static final String ARG_SORT_LABELS_RES_ID = "com.sovworks.eds.android.SORT_LABELS_RES_ID";
    private static final String ARG_RECEIVER_FRAGMENT_TAG = "com.sovworks.eds.android.RECEIVER_FRAGMENT_TAG";

}
