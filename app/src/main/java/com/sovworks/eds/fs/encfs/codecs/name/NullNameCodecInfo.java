package com.sovworks.eds.fs.encfs.codecs.name;

import com.sovworks.eds.fs.encfs.NameCodec;
import com.sovworks.eds.fs.encfs.ciphers.NullNameCipher;

public class NullNameCodecInfo extends NameCodecInfoBase
{
    public static final String NAME = "nameio/null";

    @Override
    public NameCodec getEncDec()
    {
        return new NullNameCipher();
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDescr()
    {
        return "Null: No encryption of filenames";
    }

    @Override
    public int getVersion1()
    {
        return 1;
    }

    @Override
    public int getVersion2()
    {
        return 0;
    }

    @Override
    protected NameCodecInfoBase createNew()
    {
        return new NullNameCodecInfo();
    }
}
