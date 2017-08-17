package com.sovworks.eds.crypto;

public interface CipherFactory
{
	BlockCipherNative createCipher(int typeIndex);
	int getNumberOfCiphers();
}
