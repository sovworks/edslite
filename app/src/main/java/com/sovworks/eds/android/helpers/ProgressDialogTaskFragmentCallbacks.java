package com.sovworks.eds.android.helpers;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.fragments.TaskFragment.Result;
import com.sovworks.eds.android.fragments.TaskFragment.TaskCallbacks;

public class ProgressDialogTaskFragmentCallbacks implements TaskCallbacks
{
    public static class Dialog extends DialogFragment
    {
        public static final String TAG = "ProgressDialog";
        public static final String ARG_DIALOG_TEXT = "dialog_text";

        public static Dialog newInstance(String dialogText)
        {
            Bundle args = new Bundle();
            args.putString(ARG_DIALOG_TEXT, dialogText);
            Dialog d =  new Dialog();
            d.setArguments(args);
            return d;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState)
        {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getArguments().getString(ARG_DIALOG_TEXT));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        }
    }

	public ProgressDialogTaskFragmentCallbacks(Activity context, int dialogTextResId)
	{
		_context = context;
		_dialogTextResId = dialogTextResId;
	}

	@Override
	public void onPrepare(Bundle args)
	{
		
	}
	
	@Override
	public void onResumeUI(Bundle args)
	{
		_dialog = initDialog(args);
		if(_dialog!=null)		
			_dialog.show(_context.getFragmentManager(), Dialog.TAG);
	}
	
	@Override
	public void onSuspendUI(Bundle args)
	{
		if(_dialog!=null)
            _dialog.dismissAllowingStateLoss();
	}

	@Override
	public void onUpdateUI(Object state)
	{		

	}

	@Override
	public void onCompleted(Bundle args, Result result)
	{
		
	}
	
	protected final Activity _context;
	
	protected DialogFragment initDialog(Bundle args)
	{
        return Dialog.newInstance(_context.getText(_dialogTextResId).toString());
	}

	private DialogFragment _dialog;
	private final int _dialogTextResId;
	
}
