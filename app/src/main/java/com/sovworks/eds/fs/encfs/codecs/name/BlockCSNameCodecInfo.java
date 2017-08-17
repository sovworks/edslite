package com.sovworks.eds.fs.encfs.codecs.name;

import com.sovworks.eds.fs.encfs.DataCodecInfo;
import com.sovworks.eds.fs.encfs.NameCodec;
import com.sovworks.eds.fs.encfs.ciphers.BlockNameCipher;

public class BlockCSNameCodecInfo extends NameCodecInfoBase
{
    public static final String NAME = "nameio/block32";

    @Override
    public NameCodec getEncDec()
    {
        DataCodecInfo dci = getConfig().getDataCodecInfo();
        return new BlockNameCipher(dci.getFileEncDec(), dci.getChecksumCalculator(), true);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDescr()
    {
        return "Block32: Block encoding with base32 output for case-sensitive systems";
    }

    @Override
    public int getVersion1()
    {
        return 4;
    }

    @Override
    public int getVersion2()
    {
        return 0;
    }

    @Override
    protected NameCodecInfoBase createNew()
    {
        return new BlockCSNameCodecInfo();
    }
}
