package com.sovworks.eds.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.sovworks.eds.android.R;

public abstract class CheckBoxPropertyEditor extends PropertyEditorBase
{

	public CheckBoxPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId)
	{
		super(host, R.layout.settings_checkbox_editor, titleResId, descResId);
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_checkBox = (CheckBox) view.findViewById(android.R.id.checkbox);
		_checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if(!_loadingValue)
					onChecked(isChecked);
			}
		});
		return view;
	}
	
	@Override
	public void load()
	{
		_loadingValue = true;
		try
		{
			_checkBox.setChecked(loadValue());
		}
		finally
		{
			_loadingValue = false;
		}
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_checkBox != null)
		{
			if(isInstantSave())
				load();
			else
			{
				_loadingValue = true;
				try
				{
					_checkBox.setChecked(b.getBoolean(getBundleKey()));
				}
				finally
				{
					_loadingValue = false;
				}
			}
		}
	}
	
	@Override
	public void save()
	{
		saveValue(_checkBox.isChecked());
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _checkBox!=null)
			b.putBoolean(getBundleKey(), _checkBox.isChecked());
	}	
	
	@Override
	public void onClick()
	{
		_checkBox.setChecked(!_checkBox.isChecked());
	}
	
	protected CheckBox _checkBox;
	protected boolean _loadingValue;
	
	protected abstract boolean loadValue();
	protected abstract void saveValue(boolean value);
	
	protected void onChecked(boolean isChecked)
	{
		if(_host.getPropertiesView().isInstantSave())
			save();
	}
}
