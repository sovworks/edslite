package com.sovworks.eds.crypto.blockciphers;

import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.BlockCipherNative;


public class AES implements BlockCipherNative
{	
	public AES()
	{
		this(32);		
	}
	
	public AES(int keySize)
	{
		_keySize = keySize;
	}
	
	@Override
	public void init(byte[] key) throws EncryptionEngineException
	{
		if(key.length != getKeySize())
			throw new IllegalArgumentException(String.format("Wrong key length. Required: %d. Provided: %d", _keySize, key.length));
		_contextPtr = initContext(key);
		if(_contextPtr == 0)
			throw new EncryptionEngineException("AES context initialization failed");
		
	}
	@Override
	public void encryptBlock(byte[] data) throws EncryptionEngineException
	{
		if(_contextPtr==0)
			throw new EncryptionEngineException("Cipher is closed");
		encrypt(data,_contextPtr);
		
	}
	@Override
	public void decryptBlock(byte[] data) throws EncryptionEngineException
	{
		if(_contextPtr==0)
			throw new EncryptionEngineException("Cipher is closed");
		decrypt(data,_contextPtr);		
	}
	
	@Override
	public void close()
	{
		if(_contextPtr!=0)
		{
			closeContext(_contextPtr);
			_contextPtr = 0;
		}		
	}
	
	@Override
	public long getNativeInterfacePointer() throws EncryptionEngineException
	{
		return _contextPtr;
	}
	
	@Override
	public int getKeySize()
	{
		return _keySize;
	}
	
	@Override
	public int getBlockSize()
	{
		return 16;
	}
	
	static
	{
		System.loadLibrary("edsaes");
	}
	
	private long _contextPtr;
	private int _keySize;
	
	private native long initContext(byte[] key);	
	private native void closeContext(long contextPtr);
	private native void encrypt(byte[] data,long contextPtr);
	private native void decrypt(byte[] data,long contextPtr);	
}

    