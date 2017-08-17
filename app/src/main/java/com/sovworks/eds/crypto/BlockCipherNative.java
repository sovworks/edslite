package com.sovworks.eds.crypto;

public interface BlockCipherNative extends BlockCipher
{	
	long getNativeInterfacePointer() throws EncryptionEngineException;
}
