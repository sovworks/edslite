package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.fs.encfs.AlgInfo;
import com.sovworks.eds.util.RefVal;

import java.util.ArrayList;

public abstract class CodecInfoPropertyEditor extends ChoiceDialogPropertyEditor
{
    public CodecInfoPropertyEditor(CreateEDSLocationFragment hostFragment, int titleId, int descrId)
    {
        super(hostFragment, titleId, descrId, hostFragment.getTag());
    }

    protected abstract Iterable<? extends AlgInfo> getCodecs();
    protected abstract String getParamName();

    protected int findCodec(String name, RefVal<AlgInfo> codec)
    {
        int i = 0;
        for(AlgInfo ci: getCodecs())
        {
            if(name.equals(ci.getName()))
            {
                if(codec!=null)
                    codec.value = ci;
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    protected int loadValue()
    {
        String encAlgName = getHostFragment().getState().getString(getParamName());
        return encAlgName != null ? findCodec(encAlgName, null) : 0;
    }

    @Override
    protected void saveValue(int value)
    {
        int i = 0;
        for(AlgInfo ci: getCodecs())
        {
            if(i == value)
            {
                getHostFragment().getState().putString(getParamName(), ci.getName());
                return;
            }
            i++;
        }
        getHostFragment().getState().remove(getParamName());
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        for(AlgInfo ci: getCodecs())
            res.add(ci.getDescr());
        return res;
    }

    protected CreateEDSLocationFragment getHostFragment()
    {
        return (CreateEDSLocationFragment) getHost();
    }

}
