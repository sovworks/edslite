package com.sovworks.eds.android.settings.encfs;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.fs.encfs.AlgInfo;
import com.sovworks.eds.fs.encfs.FS;

public class NameCodecPropertyEditor extends CodecInfoPropertyEditor
{
    public NameCodecPropertyEditor(CreateEDSLocationFragment hostFragment)
    {
        super(hostFragment, R.string.filename_encryption_algorithm, 0);
    }

    @Override
    protected Iterable<? extends AlgInfo> getCodecs()
    {
        return FS.getSupportedNameCodecs();
    }

    @Override
    protected String getParamName()
    {
        return CreateEncFsTaskFragment.ARG_NAME_CIPHER_NAME;
    }
}
