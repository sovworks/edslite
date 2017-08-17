package com.sovworks.eds.android.settings;

import com.sovworks.eds.android.R;

public abstract class IntPropertyEditor extends TextPropertyEditor
{
	public IntPropertyEditor( PropertyEditor.Host host, int titleResId, int descResId, String hostFragmentTag)
	{
		super(host,titleResId, descResId, hostFragmentTag);
	}

	public void setCurrentValue(int value)
	{
		onTextChanged(String.valueOf(value));
	}

	public int getCurrentValue()
	{
		String s = _selectedValueTextView.getText().toString();
		return s.length() > 0 ? Integer.valueOf(s) : 0;
	}
	
	protected abstract int loadValue();
	protected abstract void saveValue(int value);

	@Override
	protected String loadText()
	{
		int v = loadValue();
		return v != 0 ? String.valueOf(v) : "";
	}

	@Override
	protected void saveText(String text) throws Exception
	{
		saveValue(text.length() > 0 ? Integer.valueOf(text) : 0);
	}

	@Override
	protected int getDialogViewResId()
	{
		return R.layout.settings_edit_num;
	}
}
