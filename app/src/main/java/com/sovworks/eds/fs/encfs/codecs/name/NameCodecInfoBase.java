package com.sovworks.eds.fs.encfs.codecs.name;

import com.sovworks.eds.fs.encfs.AlgInfo;
import com.sovworks.eds.fs.encfs.Config;
import com.sovworks.eds.fs.encfs.DataCodecInfo;
import com.sovworks.eds.fs.encfs.NameCodec;
import com.sovworks.eds.fs.encfs.NameCodecInfo;
import com.sovworks.eds.fs.encfs.ciphers.BlockNameCipher;

public abstract class NameCodecInfoBase implements NameCodecInfo
{
    @Override
    public boolean useChainedNamingIV()
    {
        return _config.useChainedNameIV();
    }

    @Override
    public AlgInfo select(Config config)
    {
        NameCodecInfoBase info = createNew();
        info._config = config;
        return info;
    }

    public Config getConfig()
    {
        return _config;
    }

    private Config _config;

    protected abstract NameCodecInfoBase createNew();
}
