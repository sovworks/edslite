package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.crypto.engines.AESCFB;

public class AESCFBStreamCipher extends StreamCipherBase
{
    public AESCFBStreamCipher(int keySize)
    {
        super(new AESCFB(keySize));
    }
}
