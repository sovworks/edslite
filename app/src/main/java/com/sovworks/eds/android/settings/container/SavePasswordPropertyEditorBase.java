package com.sovworks.eds.android.settings.container;

import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragment;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragmentBase;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Openable;

public class SavePasswordPropertyEditorBase extends SwitchPropertyEditor implements PasswordDialog.PasswordReceiver
{
    public SavePasswordPropertyEditorBase(EDSLocationSettingsFragmentBase settingsFragment)
    {
        super(settingsFragment, R.string.save_password, R.string.save_password_desc);
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
        return !loc.requirePassword();
    }

    @Override
    protected void saveValue(boolean value)
    {

    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        EDSLocation.ExternalSettings settings = getHost().getLocation().getExternalSettings();
        SecureBuffer sb = new SecureBuffer(dlg.getPassword());
        byte[] data = sb.getDataArray();
        settings.setPassword(data);
        SecureBuffer.eraseData(data);
        sb.close();
        getHost().saveExternalSettings();
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        _switchButton.setChecked(false);
    }

    @Override
    protected boolean onChecked(boolean isChecked)
    {
        Openable loc = getHost().getLocation();
        if (isChecked)
        {
            Bundle args = new Bundle();
            args.putBoolean(PasswordDialog.ARG_HAS_PASSWORD, loc.hasPassword());
            args.putString(PasswordDialog.ARG_RECEIVER_FRAGMENT_TAG, getHost().getTag());
            args.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
            PasswordDialog pd = new PasswordDialog();
            pd.setArguments(args);
            pd.show(getHost().getFragmentManager(), PasswordDialog.TAG);
			return true;
        }
        else
        {
            getHost().getLocation().getExternalSettings().setPassword(null);
            getHost().saveExternalSettings();
			return true;
        }
    }
}
