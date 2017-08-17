package com.sovworks.eds.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.sovworks.eds.android.R;

public abstract class SpinnerPropertyEditor extends PropertyEditorBase
{

	public SpinnerPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId)
	{
		super(host, R.layout.settings_spinner_editor, titleResId, descResId);
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_spinner = (Spinner) view.findViewById(R.id.spinner);
		reloadElements();
		_spinner.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				if(_host.getPropertiesView().isInstantSave())
					save();
			}
		});
		return view;
	}
	
	public void reloadElements()
	{
		_spinner.setAdapter(getEntries());
	}
	
	@Override
	public void load()
	{
		_spinner.setSelection(loadValue());	
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_spinner != null)
		{
			if(isInstantSave())
				load();
			else
				_spinner.setSelection(b.getInt(getBundleKey()));
		}
	}
	
	@Override
	public void save()
	{
		saveValue(_spinner.getSelectedItemPosition());
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _spinner!=null)
			b.putInt(getBundleKey(), _spinner.getSelectedItemPosition());
	}	
	
	protected Spinner _spinner;
	
	protected abstract int loadValue();
	protected abstract void saveValue(int value);
	protected abstract ArrayAdapter<?> getEntries();
}
