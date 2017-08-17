package com.sovworks.eds.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.dialogs.ChoiceDialog;

import java.util.List;

public abstract class ChoiceDialogPropertyEditor extends PropertyEditorBase
{

	public ChoiceDialogPropertyEditor(Host host, int titleResId, int descResId, String hostFragmentTag)
	{
		super(host, R.layout.settings_choice_dialog_editor, titleResId, descResId);
		_entries = getEntries();
		_hostFragmentTag = hostFragmentTag;
	}

	public ChoiceDialogPropertyEditor(Host host, int propertyId, String title, String desc, String hostFragmentTag)
	{
		super(host, propertyId, R.layout.settings_choice_dialog_editor, title, desc);
		_entries = getEntries();
		_hostFragmentTag = hostFragmentTag;
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		_selectedItems = (TextView) view.findViewById(android.R.id.text1);
		_selectButton = (Button) view.findViewById(android.R.id.button1);
        _selectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startChoiceDialog();
            }
        });
		return view;
	}

	public void setSelectedEntry(int val)
	{
		_selectedEntry = val;
		updateSelectionText();
		if(_host.getPropertiesView().isInstantSave())
			save();
	}

	public int getSelectedEntry()
	{
		return _selectedEntry;
	}
	
	@Override
	public void load()
	{
        _entries = getEntries();
        _selectButton.setVisibility(_entries.size()<2 ? View.GONE : View.VISIBLE);
		_selectedEntry = loadValue();
        updateSelectionText();
	}
	
	@Override
	public void load(Bundle b)
	{
		if(_selectButton != null)
		{
			if(isInstantSave())
				load();
			else
			{
				_entries = getEntries();
				_selectButton.setVisibility(_entries.size() < 2 ? View.GONE : View.VISIBLE);
				_selectedEntry = b.getInt(getBundleKey());
				updateSelectionText();
			}
		}
	}
	
	@Override
	public void save()
	{
        saveValue(_selectedEntry);
	}
	
	@Override
	public void save(Bundle b)
	{
		if(!isInstantSave() && _selectButton!=null)
			b.putInt(getBundleKey(), _selectedEntry);
	}	
	
	protected int _selectedEntry = -1;
	
	protected abstract int loadValue();
	protected abstract void saveValue(int value);
	protected abstract List<String> getEntries();

	protected TextView _selectedItems;

	private List<String> _entries;
    private Button _selectButton;

    private void updateSelectionText()
    {
        if(_selectedEntry >= 0 && _selectedEntry<_entries.size())
			_selectedItems.setText(_entries.get(_selectedEntry));
        else
            _selectedItems.setText("");
    }

    private void startChoiceDialog()
    {
        ChoiceDialog.showDialog(_host.getFragmentManager(), getId(), _title != null ? _title : _host.getContext().getString(_titleResId), _entries, _hostFragmentTag );
    }

	private final String _hostFragmentTag;
}
