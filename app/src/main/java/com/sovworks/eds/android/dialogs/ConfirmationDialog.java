package com.sovworks.eds.android.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;


public abstract class ConfirmationDialog extends DialogFragment
{
    public static final String ARG_RECEIVER_TAG = "com.sovworks.eds.android.RECEIVER_TAG";

    public interface Receiver
    {
        void onYes();
        void onNo();
    }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        Util.setDialogStyle(this);
	}

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getTitle())
                .setCancelable(false)
                .setPositiveButton(R.string.yes,
                        (dialog, id) ->
                        {
                            onYes();
                            dismiss();
                        })
                .setNegativeButton(R.string.no,
                        (dialog, id) ->
                        {
                            onNo();
                            dismiss();
                        });

        return builder.create();
    }

    protected void onNo()
    {
        Receiver rec = getReceiver();
        if(rec != null)
            rec.onNo();
    }

    protected void onYes()
    {
        Receiver rec = getReceiver();
        if(rec != null)
            rec.onYes();
    }

    protected abstract String getTitle();

    protected Receiver getReceiver()
    {
        Bundle args = getArguments();
        String tag = args == null ? null : args.getString(ARG_RECEIVER_TAG);
        if(tag != null)
        {
            Fragment f = getFragmentManager().findFragmentByTag(tag);
            if(f instanceof Receiver)
                return (Receiver) f;
        }
        else
        {
            Activity act = getActivity();
            if(act instanceof Receiver)
                return (Receiver) act;
        }
        return null;
    }
}
