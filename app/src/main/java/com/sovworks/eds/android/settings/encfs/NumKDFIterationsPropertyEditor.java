package com.sovworks.eds.android.settings.encfs;

import android.app.Fragment;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.IntPropertyEditor;
import com.sovworks.eds.android.settings.PropertiesHostWithStateBundle;
import com.sovworks.eds.locations.Openable;

public class NumKDFIterationsPropertyEditor extends IntPropertyEditor
{
    public NumKDFIterationsPropertyEditor(PropertiesHostWithStateBundle hostFragment)
    {
        super(
                hostFragment,
                R.string.number_of_kdf_iterations,
                R.string.number_of_kdf_iterations_descr,
                ((Fragment)hostFragment).getTag()
        );
    }

    @Override
    public PropertiesHostWithStateBundle getHost()
    {
        return (PropertiesHostWithStateBundle) super.getHost();
    }

    @Override
    protected int loadValue()
    {
        return getHost().getState().getInt(Openable.PARAM_KDF_ITERATIONS, 100000);
    }


    @Override
    protected void saveValue(int value)
    {
        if(value < 1000)
            value = 1000;
        getHost().getState().putInt(Openable.PARAM_KDF_ITERATIONS, value);
    }
}
