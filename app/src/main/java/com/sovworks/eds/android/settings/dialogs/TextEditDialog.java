package com.sovworks.eds.android.settings.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.PropertyEditor.Host;
import com.sovworks.eds.android.settings.views.PropertiesView;

public class TextEditDialog extends DialogFragment
{
	public static final String TAG = "TextEditDialog";

	public interface TextResultReceiver
	{
		void setResult(String text) throws Exception;
	}

	public static final String ARG_TEXT = "com.sovworks.eds.android.ARG_TEXT";
	public static final String ARG_MESSAGE_ID = "com.sovworks.eds.android.ARG_MESSAGE_ID";
	public static final String ARG_EDIT_TEXT_RES_ID = "com.sovworks.eds.android.EDIT_TEXT_RES_ID";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
		int mid = getArguments().getInt(ARG_MESSAGE_ID);
		if(mid!=0)
			alert.setMessage(getString(mid));
		LayoutInflater inflater = LayoutInflater.from(getActivity());

		_input = (EditText) inflater.inflate(getArguments().getInt(ARG_EDIT_TEXT_RES_ID, R.layout.settings_edit_text), null);
		_input.setText(savedInstanceState == null ? getArguments().getString(ARG_TEXT) : savedInstanceState.getString(ARG_TEXT));
		alert.setView(_input);

		alert.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
                        Host host = PropertiesView.getHost(TextEditDialog.this);
						if(host!=null)
						{
							PropertyEditor pe = host.getPropertiesView().getPropertyById(getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID));
							if(pe!=null)
								try
								{
									((TextResultReceiver)pe).setResult(_input.getText().toString());
								}
								catch (Exception e)
								{
									Logger.showAndLog(getActivity(), e);
								}
						}
					}
				});

		/*alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						// Canceled.
					}
				});*/

		return alert.create();
	}
	
	@Override
	public void onSaveInstanceState (@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(ARG_TEXT, _input.getText().toString());
	}
	
	private EditText _input;


}
