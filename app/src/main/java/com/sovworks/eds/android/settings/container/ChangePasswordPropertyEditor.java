package com.sovworks.eds.android.settings.container;

import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragment;
import com.sovworks.eds.android.locations.fragments.EDSLocationSettingsFragmentBase;
import com.sovworks.eds.android.settings.ButtonPropertyEditor;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.tasks.ChangeContainerPasswordTask;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

public class ChangePasswordPropertyEditor extends ButtonPropertyEditor implements PasswordDialog.PasswordReceiver
{
    public ChangePasswordPropertyEditor(EDSLocationSettingsFragmentBase settingsFragment)
    {
        super(settingsFragment, R.string.change_container_password, 0, R.string.enter_new_password);
    }

	@Override
	public EDSLocationSettingsFragment getHost()
	{
		return (EDSLocationSettingsFragment) super.getHost();
	}

    @Override
    protected void onButtonClick()
    {
        Bundle args = new Bundle();
        args.putBoolean(PasswordDialog.ARG_HAS_PASSWORD, true);
        args.putBoolean(PasswordDialog.ARG_VERIFY_PASSWORD, true);
        args.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        Location loc = getHost().getLocation();
        LocationsManager.storePathsInBundle(args, loc, null);
        args.putString(PasswordDialog.ARG_RECEIVER_FRAGMENT_TAG, getHost().getTag());
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(args);
        pd.show(getHost().getFragmentManager(), PasswordDialog.TAG);
    }

    @Override
    public void onPasswordEntered(final PasswordDialog dlg)
    {
        getHost().getResHandler().addResult(new Runnable()
        {
            @Override
            public void run()
            {
                getHost().getFragmentManager().
                        beginTransaction().
                        add(
                                getHost().getChangePasswordTask(dlg),
                                ChangeContainerPasswordTask.TAG).
                        commit();
            }
        });

    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg){}

}
