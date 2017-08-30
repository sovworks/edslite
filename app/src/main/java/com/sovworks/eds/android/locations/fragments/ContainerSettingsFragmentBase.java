package com.sovworks.eds.android.locations.fragments;

import android.os.Bundle;

import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.locations.opener.fragments.ContainerOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.android.settings.container.ContainerFormatHintPropertyEditor;
import com.sovworks.eds.android.settings.container.EncEngineHintPropertyEditor;
import com.sovworks.eds.android.settings.container.HashAlgHintPropertyEditor;
import com.sovworks.eds.android.tasks.ChangeContainerPasswordTask;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.util.Collection;
import java.util.List;

public class ContainerSettingsFragmentBase extends EDSLocationSettingsFragment
{
    @Override
    public ContainerLocation getLocation()
    {
        return (ContainerLocation) super.getLocation();
    }

    public ContainerFormatInfo getCurrentContainerFormat()
    {
        List<ContainerFormatInfo> supportedFormats = getLocation().getSupportedFormats();
        return supportedFormats.size() == 1 ?
                supportedFormats.get(0) :
                EdsContainer.findFormatByName(
                        supportedFormats,
                        getLocation().getExternalSettings().getContainerFormatName()
                );
    }

    @Override
    protected TaskFragment createChangePasswordTaskInstance()
    {
        return new ChangeContainerPasswordTask();
    }

    @Override
    protected LocationOpenerBaseFragment getLocationOpener()
    {
        return new ContainerOpenerFragment();
    }

    @Override
    protected void createStdProperties(Collection<Integer> ids)
    {
        super.createStdProperties(ids);
        createHintProperties(ids);
    }

    @Override
    protected Bundle getChangePasswordTaskArgs(PasswordDialog dlg)
    {
        final Bundle args = new Bundle();
        args.putAll(dlg.getOptions());
        args.putParcelable(Openable.PARAM_PASSWORD, new SecureBuffer(dlg.getPassword()));
        LocationsManager.storePathsInBundle(args, getLocation(), null);
        return args;
    }

    protected void createHintProperties(Collection<Integer> ids)
    {
        ids.add(_propertiesView.addProperty(new ContainerFormatHintPropertyEditor(this)));
        ids.add(_propertiesView.addProperty(new EncEngineHintPropertyEditor(this)));
        ids.add(_propertiesView.addProperty(new HashAlgHintPropertyEditor(this)));
    }
}
