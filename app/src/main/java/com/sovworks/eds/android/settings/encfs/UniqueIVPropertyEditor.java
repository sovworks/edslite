package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragment;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;

public class UniqueIVPropertyEditor extends SwitchPropertyEditor
{
    public UniqueIVPropertyEditor(CreateContainerFragmentBase hostFragment)
    {
        super(hostFragment, R.string.enable_per_file_iv, R.string.enable_per_file_iv_descr);
    }

    @Override
    protected boolean loadValue()
    {
        getHostFragment().changeUniqueIVDependentOptions();
        return getHostFragment().getState().getBoolean(CreateEncFsTaskFragment.ARG_UNIQUE_IV, true);
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateEncFsTaskFragment.ARG_UNIQUE_IV, value);
        getHostFragment().changeUniqueIVDependentOptions();
    }

    protected CreateContainerFragment getHostFragment()
    {
        return (CreateContainerFragment) getHost();
    }
}
