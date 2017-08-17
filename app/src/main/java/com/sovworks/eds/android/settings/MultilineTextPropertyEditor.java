package com.sovworks.eds.android.settings;

import com.sovworks.eds.android.R;

public abstract class MultilineTextPropertyEditor extends TextPropertyEditor
{

	public MultilineTextPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId, String hostFragmentTag)
	{
		super(host, titleResId, descResId, hostFragmentTag);
	}

	@Override
	protected int getDialogViewResId()
	{
		return R.layout.settings_edit_text_ml;
	}
}
