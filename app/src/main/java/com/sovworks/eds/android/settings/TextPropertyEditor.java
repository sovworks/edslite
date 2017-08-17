package com.sovworks.eds.android.settings;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.dialogs.TextEditDialog;

public abstract class TextPropertyEditor extends PropertyEditorBase implements TextEditDialog.TextResultReceiver
{

	public TextPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId, String hostFragmentTag)
	{
		this(host, R.layout.settings_text_editor, titleResId, descResId, hostFragmentTag);
	}
	
	public TextPropertyEditor(PropertyEditor.Host host, int layoutId, int titleResId, int descResId, String hostFragmentTag)
	{
		super(host, layoutId, titleResId, descResId);
		_hostFragmentTag = hostFragmentTag;
	}	
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_selectedValueTextView = (TextView) view.findViewById(android.R.id.text1);
		Button selectButton = (Button) view.findViewById(android.R.id.button1);
		selectButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				startChangeValueDialog();
			}
		});
		return view;
	}

	@Override
	public void load()
	{
		_selectedValueTextView.setText(loadText());
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_selectedValueTextView!=null)
		{
			if(isInstantSave())
				load();
			else
				_selectedValueTextView.setText(b.getString(getBundleKey()));
		}
	}
	
	@Override
	public void save() throws Exception
	{
		saveText(_selectedValueTextView.getText().toString());
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _selectedValueTextView!=null)
			b.putString(getBundleKey(), _selectedValueTextView.getText().toString());
	}

	public void setResult(String value) throws Exception
	{
		onTextChanged(value);
	}

	protected TextView _selectedValueTextView;
	
	protected abstract String loadText();
	protected abstract void saveText(String text) throws Exception;

	protected void startChangeValueDialog()
	{
		Bundle args = initDialogArgs();
		DialogFragment df = new TextEditDialog();
		df.setArguments(args);
		df.show(getHost().getFragmentManager(), TextEditDialog.TAG);
	}

	protected int getDialogViewResId()
	{
		return R.layout.settings_edit_text;
	}

	protected Bundle initDialogArgs()
	{
		Bundle b = new Bundle();
		b.putString(TextEditDialog.ARG_TEXT, _selectedValueTextView.getText().toString());
		b.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
		b.putInt(TextEditDialog.ARG_MESSAGE_ID, _titleResId);
		b.putInt(TextEditDialog.ARG_EDIT_TEXT_RES_ID, getDialogViewResId());
		if(_hostFragmentTag!=null)
			b.putString(PropertyEditor.ARG_HOST_FRAGMENT_TAG, _hostFragmentTag);
		return b;
	}
	protected void onTextChanged(String newValue)
	{
		_selectedValueTextView.setText(newValue);
		if(!_host.getPropertiesView().isLoadingProperties() && _host.getPropertiesView().isInstantSave())
		try
		{
			save();
		}
		catch (Exception e)
		{
			Logger.showAndLog(getHost().getContext(), e);
		}
	}

	private final String _hostFragmentTag;
}
