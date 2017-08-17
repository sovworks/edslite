package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragment;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.VolumeLayoutBase;
import com.sovworks.eds.crypto.EncryptionEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EncEngineHintPropertyEditor extends ChoiceDialogPropertyEditor
{
    public EncEngineHintPropertyEditor(ContainerSettingsFragmentBase containerSettingsFragment)
    {
        super(containerSettingsFragment, R.string.encryption_algorithm, R.string.encryption_alg_desc, containerSettingsFragment.getTag());
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
            getHost().getLocation().getExternalSettings().setEncEngineName(null);
        else
            getHost().getLocation().getExternalSettings().setEncEngineName(getEncEngineName(getSupportedEncEngines().get(value - 1)));
        getHost().saveExternalSettings();
    }

    @Override
    protected int loadValue()
    {
        String name = getHost().getLocation().getExternalSettings().getEncEngineName();
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
        for (EncryptionEngine ee : getSupportedEncEngines())
            entries.add(getEncEngineName(ee));
        return entries;
    }

    private String getEncEngineName(EncryptionEngine ee)
    {
        return VolumeLayoutBase.getEncEngineName(ee);
    }

    private int findEngineIndexByName(String name)
    {
        int i = 0;
        for (EncryptionEngine ee : getSupportedEncEngines())
        {
            if (name.equalsIgnoreCase(getEncEngineName(ee)))
                return i;
            i++;
        }
        return -1;
    }

    private List<? extends EncryptionEngine> getSupportedEncEngines()
    {
        ContainerFormatInfo cfi = getHost().getCurrentContainerFormat();
        return cfi != null ?
                cfi.getVolumeLayout().getSupportedEncryptionEngines()
                :
                Collections.<EncryptionEngine>emptyList();
    }
}
