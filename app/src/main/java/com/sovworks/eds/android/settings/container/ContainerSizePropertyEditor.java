package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.settings.IntPropertyEditor;

public class ContainerSizePropertyEditor extends IntPropertyEditor
{

    public ContainerSizePropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.container_size, R.string.container_size_desc, createContainerFragment.getTag());
    }

    @Override
    protected int loadValue()
    {
        return getHostFragment().getState().getInt(CreateContainerTaskFragmentBase.ARG_SIZE, 10);
    }

    @Override
    protected void saveValue(int value)
    {
        getHostFragment().getState().putInt(CreateContainerTaskFragmentBase.ARG_SIZE, value);
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
