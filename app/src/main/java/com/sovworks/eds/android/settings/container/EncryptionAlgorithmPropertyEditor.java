package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.container.VolumeLayout;
import com.sovworks.eds.container.VolumeLayoutBase;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.truecrypt.EncryptionEnginesRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EncryptionAlgorithmPropertyEditor extends ChoiceDialogPropertyEditor
{
    public static String getEncEngineName(EncryptionEngine eng)
    {
        return EncryptionEnginesRegistry.getEncEngineName(eng);
    }

    public EncryptionAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.encryption_algorithm, 0, createContainerFragment.getTag());
    }

    @Override
    protected int loadValue()
    {
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        String encAlgName = getHostFragment().getState().getString(CreateContainerTaskFragmentBase.ARG_CIPHER_NAME);
        String encModeName = getHostFragment().getState().getString(CreateContainerTaskFragmentBase.ARG_CIPHER_MODE_NAME);
        if (encAlgName != null && encModeName != null)
        {
            EncryptionEngine ee = VolumeLayoutBase.findCipher(algs, encAlgName, encModeName);
            return algs.indexOf(ee);
        } else if (!algs.isEmpty())
            return 0;
        else
            return -1;

    }

    @Override
    protected void saveValue(int value)
    {
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        EncryptionEngine ee = algs.get(value);
        getHostFragment().getState().putString(CreateContainerTaskFragmentBase.ARG_CIPHER_NAME, ee.getCipherName());
        getHostFragment().getState().putString(CreateContainerTaskFragmentBase.ARG_CIPHER_MODE_NAME, ee.getCipherModeName());
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        List<? extends EncryptionEngine> supportedEngines = getCurrentEncAlgList();
        if (supportedEngines != null)
        {
            for (EncryptionEngine eng : supportedEngines)
                res.add(getEncEngineName(eng));
        }
        return res;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }

    private List<? extends EncryptionEngine> getCurrentEncAlgList()
    {
        VolumeLayout vl = getHostFragment().getSelectedVolumeLayout();
        return vl != null ? vl.getSupportedEncryptionEngines() : Collections.<FileEncryptionEngine>emptyList();
    }
}
