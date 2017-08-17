package com.sovworks.eds.android.settings.container;

import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.settings.ButtonPropertyEditor;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.Openable;

public class ContainerPasswordPropertyEditor extends ButtonPropertyEditor implements PasswordDialog.PasswordReceiver
{
    public ContainerPasswordPropertyEditor(CreateEDSLocationFragment createEDSLocationFragment)
    {
        super(createEDSLocationFragment,
                R.string.container_password,
                0,
                R.string.change
        );
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        getHostFragment().getState().putParcelable(Openable.PARAM_PASSWORD, new SecureBuffer(dlg.getPassword()));
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg){}

    @Override
    protected void onButtonClick()
    {
        Bundle args = new Bundle();
        args.putBoolean(PasswordDialog.ARG_HAS_PASSWORD, true);
        args.putBoolean(PasswordDialog.ARG_VERIFY_PASSWORD, true);
        args.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        args.putString(PasswordDialog.ARG_RECEIVER_FRAGMENT_TAG, getHostFragment().getTag());
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(args);
        pd.show(getHost().getFragmentManager(), PasswordDialog.TAG);
    }

    CreateEDSLocationFragment getHostFragment()
    {
        return (CreateEDSLocationFragment)getHost();
    }
}
