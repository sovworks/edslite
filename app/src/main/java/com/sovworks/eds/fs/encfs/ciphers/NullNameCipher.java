package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.fs.encfs.NameCodec;

public class NullNameCipher implements NameCodec
{
    @Override
    public String encodeName(String plaintextName)
    {
        return plaintextName;
    }

    @Override
    public String decodeName(String encodedName)
    {
        return encodedName;
    }

    @Override
    public void init(byte[] key)
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public void setIV(byte[] iv)
    {

    }

    @Override
    public byte[] getChainedIV(String plaintextName)
    {
        return null;
    }

    @Override
    public byte[] getIV()
    {
        return null;
    }

    @Override
    public int getIVSize()
    {
        return 0;
    }

}
