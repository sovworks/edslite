package com.sovworks.eds.fs.encfs;

public interface NameCodec
{
    String encodeName(String plaintextName);
    String decodeName(String encodedName);
    byte[] getChainedIV(String plaintextName);
    void init(byte[] key);
    void close();
    void setIV(byte[] iv);
    byte[] getIV();
    int getIVSize();
}
