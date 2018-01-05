package com.sovworks.eds.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.sovworks.eds.android.R;

public abstract class SwitchPropertyEditor extends PropertyEditorBase
{

	public SwitchPropertyEditor(Host host, int titleResId, int descResId)
	{
		super(host, R.layout.settings_switch_editor, titleResId, descResId);
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_switchButton = view.findViewById(android.R.id.button1);
		_switchButton.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
            if(!_loadingValue)
            {
                if(!onChecked(isChecked))
                    buttonView.setChecked(!isChecked);
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
			_switchButton.setChecked(loadValue());
		}
		finally
		{
			_loadingValue = false;
		}
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_switchButton == null)
			return;
		if(isInstantSave())
			load();
		else
		{
			_loadingValue = true;
			try
			{
				_switchButton.setChecked(b.getBoolean(getBundleKey()));
			}
			finally
			{
				_loadingValue = false;
			}
		}
	}
	
	@Override
	public void save()
	{
		saveValue(_switchButton.isChecked());
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _switchButton!=null)
			b.putBoolean(getBundleKey(), _switchButton.isChecked());
	}	
	
	@Override
	public void onClick()
	{
		_switchButton.toggle();
	}

	protected boolean getCurrentValue()
	{
		return _switchButton.isChecked();
	}
	
	protected CompoundButton _switchButton;
	private boolean _loadingValue;
	
	protected abstract boolean loadValue();
	protected abstract void saveValue(boolean value);
	
	protected boolean onChecked(boolean isChecked)
	{
		if(_host.getPropertiesView().isInstantSave())
			save();
		return true;
	}
}
