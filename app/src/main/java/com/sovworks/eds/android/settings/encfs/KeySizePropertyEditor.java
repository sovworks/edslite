package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;

import java.util.ArrayList;
import java.util.List;

public class KeySizePropertyEditor extends ChoiceDialogPropertyEditor
{
    public KeySizePropertyEditor(CreateEDSLocationFragment hostFragment)
    {
        super(hostFragment, R.string.key_size, R.string.key_size_descr, hostFragment.getTag());
    }

    @Override
    protected int loadValue()
    {
        return (getHostFragment().getState().getInt(CreateEncFsTaskFragment.ARG_KEY_SIZE, 16)*8 - 128)/64;
    }

    @Override
    protected void saveValue(int value)
    {
        getHostFragment().getState().putInt(CreateEncFsTaskFragment.ARG_KEY_SIZE, (128 + value*64)/8);
    }

    @Override
    protected List<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        for(int i=128;i<=256;i+=64)
            res.add(String.valueOf(i));
        return res;
    }

    protected CreateEDSLocationFragment getHostFragment()
    {
        return (CreateEDSLocationFragment) getHost();
    }
}
