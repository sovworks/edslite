package com.sovworks.eds.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.CompatHelper;

public class StaticPropertyEditor extends PropertyEditorBase
{
	public StaticPropertyEditor(PropertyEditor.Host host, int titleResId)
	{
		this(host,titleResId, 0);
	}
	
	public StaticPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId)
	{
		super(host, R.layout.settings_simple_editor, titleResId, descResId);
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_descTextView = (TextView) view.findViewById(R.id.desc);
		return view;
	}
	
	@Override
	public void load()
	{		
		String txt = loadText();
		if(txt!=null)
		{
			_descTextView.setVisibility(View.VISIBLE);
			_descTextView.setText(txt);
		}
		else
			_descTextView.setVisibility(View.GONE);
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_descTextView != null)
		{
			if(isInstantSave())
				load();
			else
				_descTextView.setText(b.getString(getBundleKey()));
		}
	}
	
	@Override
	public void save()
	{		
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _descTextView!=null)
			b.putString(getBundleKey(), _descTextView.getText().toString());
	}

	@Override
	public void onClick()
	{
		super.onClick();
		CompatHelper.storeTextInClipboard(getHost().getContext(), _descTextView.getText().toString());
		Toast.makeText(getHost().getContext(), R.string.text_has_been_copied, Toast.LENGTH_SHORT).show();
	}

	protected TextView _descTextView;
	
	protected String loadText()
	{
		return null;		
	}	
}
