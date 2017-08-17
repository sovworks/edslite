package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;

public class ExternalFileIVPropertyEditor extends SwitchPropertyEditor
{
    public ExternalFileIVPropertyEditor(CreateEDSLocationFragment hostFragment)
    {
        super(hostFragment, R.string.enable_filename_to_file_iv_chain, R.string.enable_filename_to_file_iv_chain_descr);
    }

    @Override
    protected boolean loadValue()
    {
        return getHostFragment().getState().getBoolean(CreateEncFsTaskFragment.ARG_EXTERNAL_IV, false);
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateEncFsTaskFragment.ARG_EXTERNAL_IV, value);
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
