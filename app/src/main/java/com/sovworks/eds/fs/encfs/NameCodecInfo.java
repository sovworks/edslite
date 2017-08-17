package com.sovworks.eds.fs.encfs;

public interface NameCodecInfo extends AlgInfo
{
    NameCodec getEncDec();
    boolean useChainedNamingIV();
}
