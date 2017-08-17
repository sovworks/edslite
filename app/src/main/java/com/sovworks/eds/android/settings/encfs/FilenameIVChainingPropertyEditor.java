package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragment;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;

public class FilenameIVChainingPropertyEditor extends SwitchPropertyEditor
{
    public FilenameIVChainingPropertyEditor(CreateContainerFragmentBase hostFragment)
    {
        super(hostFragment, R.string.enable_filename_iv_chain, R.string.enable_filename_iv_chain_descr);
    }

    @Override
    protected boolean loadValue()
    {
        getHostFragment().changeUniqueIVDependentOptions();
        return getHostFragment().getState().getBoolean(CreateEncFsTaskFragment.ARG_CHAINED_NAME_IV, true);
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateEncFsTaskFragment.ARG_CHAINED_NAME_IV, value);
        getHostFragment().changeUniqueIVDependentOptions();
    }

    protected CreateContainerFragment getHostFragment()
    {
        return (CreateContainerFragment) getHost();
    }
}
