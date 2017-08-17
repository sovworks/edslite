package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragment;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.container.ContainerFormatInfo;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class HashAlgHintPropertyEditor extends ChoiceDialogPropertyEditor
{
    public HashAlgHintPropertyEditor(ContainerSettingsFragmentBase containerSettingsFragment)
    {
        super(containerSettingsFragment, R.string.hash_algorithm, R.string.hash_alg_desc, containerSettingsFragment.getTag());
    }

    @Override
    public ContainerSettingsFragment getHost()
    {
        return (ContainerSettingsFragment) super.getHost();
    }

    @Override
    protected void saveValue(int value)
    {
        if (value == 0)
            getHost().getLocation().getExternalSettings().setHashFuncName(null);
        else
            getHost().getLocation().getExternalSettings().setHashFuncName(getHashFuncName(getSupportedHashFuncs().get(value - 1)));
        getHost().saveExternalSettings();
    }

    @Override
    protected int loadValue()
    {
        String name = getHost().getLocation().getExternalSettings().getHashFuncName();
        if (name != null)
        {
            int i = findEngineIndexByName(name);
            if (i >= 0)
                return i + 1;
        }
        return 0;
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> entries = new ArrayList<>();
        entries.add("-");

        for (MessageDigest hf : getSupportedHashFuncs())
            entries.add(getHashFuncName(hf));
        return entries;
    }

    private String getHashFuncName(MessageDigest hf)
    {
        return hf.getAlgorithm();
    }

    private int findEngineIndexByName(String name)
    {
        int i = 0;
        for (MessageDigest md : getSupportedHashFuncs())
        {
            if (name.equalsIgnoreCase(getHashFuncName(md)))
                return i;
            i++;
        }
        return -1;
    }

    private List<MessageDigest> getSupportedHashFuncs()
    {
        ContainerFormatInfo cfi = getHost().getCurrentContainerFormat();
        return cfi != null ?
                cfi.getVolumeLayout().getSupportedHashFuncs()
                :
                new ArrayList<MessageDigest>();
    }
}
