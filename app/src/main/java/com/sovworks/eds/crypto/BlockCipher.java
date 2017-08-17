package com.sovworks.eds.crypto;

public interface BlockCipher
{	
	void init(byte[] key) throws EncryptionEngineException;
	void encryptBlock(byte[] data) throws EncryptionEngineException;
	void decryptBlock(byte[] data) throws EncryptionEngineException;
	void close();
	int getKeySize();
	int getBlockSize();
}
