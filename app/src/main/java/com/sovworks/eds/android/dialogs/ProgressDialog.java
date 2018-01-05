package com.sovworks.eds.android.dialogs;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;

public class ProgressDialog extends DialogFragment
{
	public static final String TAG = "ProgressDialog";
	public static final String ARG_TITLE = "com.sovworks.eds.android.TITLE";

	public static ProgressDialog showDialog(FragmentManager fm, String title)
	{
		Bundle args = new Bundle();
		args.putString(ARG_TITLE, title);
		ProgressDialog d = new ProgressDialog();
		d.setArguments(args);
		d.show(fm, TAG);
		return d;
	}

	public void setProgress(int progress)
	{
		if(_progressBar!=null)
			_progressBar.setProgress(progress);
	}

	public void setTitle(CharSequence title)
	{
		if(_titleTextView!=null)
			_titleTextView.setText(title);
	}

	public void setText(CharSequence text)
	{
		if(_statusTextView!=null)
			_statusTextView.setText(text);
	}

	public void setOnCancelListener(DialogInterface.OnCancelListener listener)
	{
		_cancelListener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Util.setDialogStyle(this);
	}

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.progress_dialog, container);
		_titleTextView = v.findViewById(android.R.id.text1);
		_statusTextView = v.findViewById(android.R.id.text2);
		_progressBar = v.findViewById(android.R.id.progress);
		setTitle(getArguments().getString(ARG_TITLE));
        return v;
    }

	@Override
	public void onCancel(DialogInterface dialog)
	{
		super.onCancel(dialog);
		if(_cancelListener!=null)
			_cancelListener.onCancel(dialog);
	}

	private DialogInterface.OnCancelListener _cancelListener;
	private TextView _statusTextView, _titleTextView;
	private ProgressBar _progressBar;


}
