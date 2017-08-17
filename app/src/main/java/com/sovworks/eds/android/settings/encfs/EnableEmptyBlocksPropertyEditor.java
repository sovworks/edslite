package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;

public class EnableEmptyBlocksPropertyEditor extends SwitchPropertyEditor
{
    public EnableEmptyBlocksPropertyEditor(CreateEDSLocationFragment hostFragment)
    {
        super(hostFragment, R.string.allow_empty_blocks, R.string.allow_empty_blocks_descr);
    }

    @Override
    protected boolean loadValue()
    {
        return getHostFragment().getState().getBoolean(CreateEncFsTaskFragment.ARG_ALLOW_EMPTY_BLOCKS, true);
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateEncFsTaskFragment.ARG_ALLOW_EMPTY_BLOCKS, value);
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
