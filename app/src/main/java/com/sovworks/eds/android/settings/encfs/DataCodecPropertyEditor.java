package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateEDSLocationTaskFragment;
import com.sovworks.eds.android.settings.views.PropertiesView;
import com.sovworks.eds.fs.encfs.AlgInfo;
import com.sovworks.eds.fs.encfs.FS;

public class DataCodecPropertyEditor extends CodecInfoPropertyEditor
{
    public static final int ID = PropertiesView.newId();

    public DataCodecPropertyEditor(CreateEDSLocationFragment hostFragment)
    {
        super(hostFragment, R.string.encryption_algorithm, 0);
        setId(ID);
    }

    @Override
    protected Iterable<? extends AlgInfo> getCodecs()
    {
        return FS.getSupportedDataCodecs();
    }

    @Override
    protected String getParamName()
    {
        return CreateEDSLocationTaskFragment.ARG_CIPHER_NAME;
    }
}
