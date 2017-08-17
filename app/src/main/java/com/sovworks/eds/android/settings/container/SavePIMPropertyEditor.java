package com.sovworks.eds.android.settings.container;

import android.app.DialogFragment;
import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragment;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragmentBase;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;
import com.sovworks.eds.android.settings.dialogs.TextEditDialog;
import com.sovworks.eds.locations.EDSLocation;

public class SavePIMPropertyEditor extends SwitchPropertyEditor implements TextEditDialog.TextResultReceiver
{
    public SavePIMPropertyEditor(EDSLocationSettingsFragmentBase settingsFragment)
    {
        super(settingsFragment, R.string.remember_kdf_iterations_multiplier, 0);
    }

	@Override
	public EDSLocationSettingsFragment getHost()
	{
		return (EDSLocationSettingsFragment) super.getHost();
	}

    @Override
    protected boolean loadValue()
    {
        EDSLocation loc = getHost().getLocation();
        return !loc.requireCustomKDFIterations();
    }

    @Override
    public void setResult(String text) throws Exception
    {
        int val = text.isEmpty() ? 0 : Integer.valueOf(text);
        if(val < 0)
            val = 0;
        else if(val > 100000)
            val = 100000;
        getHost().getLocation().getExternalSettings().setCustomKDFIterations(val);
        getHost().saveExternalSettings();
    }

    @Override
    protected void saveValue(boolean value)
    {

    }

    @Override
    protected boolean onChecked(boolean isChecked)
    {
        if (isChecked)
        {
            startChangeValueDialog();
			return true;
        }
        else
        {
            getHost().getLocation().getExternalSettings().setCustomKDFIterations(-1);
            getHost().saveExternalSettings();
			return true;
        }
    }

    protected void startChangeValueDialog()
    {
        Bundle args = initDialogArgs();
        DialogFragment df = new TextEditDialog();
        df.setArguments(args);
        df.show(getHost().getFragmentManager(), TextEditDialog.TAG);
    }

    protected int getDialogViewResId()
    {
        return R.layout.settings_edit_num;
    }

    protected Bundle initDialogArgs()
    {
        Bundle b = new Bundle();
        b.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        b.putInt(TextEditDialog.ARG_MESSAGE_ID, _titleResId);
        b.putInt(TextEditDialog.ARG_EDIT_TEXT_RES_ID, getDialogViewResId());
        b.putString(PropertyEditor.ARG_HOST_FRAGMENT_TAG, getHost().getTag());
        return b;
    }
}
