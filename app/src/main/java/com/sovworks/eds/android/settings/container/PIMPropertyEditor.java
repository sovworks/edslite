package com.sovworks.eds.android.settings.container;

import android.app.Fragment;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.IntPropertyEditor;
import com.sovworks.eds.android.settings.PropertiesHostWithStateBundle;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.veracrypt.VolumeLayout;

public class PIMPropertyEditor extends IntPropertyEditor
{
    public PIMPropertyEditor(PropertiesHostWithStateBundle hostFragment)
    {
        super(hostFragment,
                R.string.kdf_iterations_multiplier,
                R.string.number_of_kdf_iterations_veracrypt_descr,
                ((Fragment)hostFragment).getTag());
    }

    @Override
    public PropertiesHostWithStateBundle getHost()
    {
        return (PropertiesHostWithStateBundle) super.getHost();
    }

    @Override
    protected int loadValue()
    {
        int val = getHost().getState().getInt(Openable.PARAM_KDF_ITERATIONS, 0);
        return val < 0 ? 0 : val;
    }


    @Override
    protected void saveValue(int value)
    {
        if(value < 0)
            value = 0;
        else if(value > 100000)
            value = 100000;
        getHost().getState().putInt(Openable.PARAM_KDF_ITERATIONS, value);
    }
}
