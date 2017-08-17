package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.engines.AESCBC;

public class AESCBCFileCipher extends CipherBase implements FileEncryptionEngine
{
    public AESCBCFileCipher(int keySize, int fileBlockSize)
    {
        super(new AESCBC(keySize, fileBlockSize));
    }

    @Override
    public int getFileBlockSize()
    {
        return getBase().getFileBlockSize();
    }

    @Override
    public int getEncryptionBlockSize()
    {
        return getBase().getEncryptionBlockSize();
    }

    @Override
    public void setIncrementIV(boolean val)
    {
        getBase().setIncrementIV(val);
    }

    @Override
    protected AESCBC getBase()
    {
        return (AESCBC) super.getBase();
    }
}
