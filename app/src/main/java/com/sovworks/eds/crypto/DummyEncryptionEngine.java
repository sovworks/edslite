package com.sovworks.eds.crypto;

public class DummyEncryptionEngine implements FileEncryptionEngine
{

	@Override
	public void close()
	{		

	}

	@Override
	public void init() throws EncryptionEngineException
	{

	}

	public void decrypt(byte[] data, int offset, int len)
			throws EncryptionEngineException
	{		

	}

	@Override
	public void encrypt(byte[] data,int offset, int len)
			throws EncryptionEngineException
	{

	}

	@Override
	public int getFileBlockSize()
	{
		return 512;
	}

	@Override
	public int getEncryptionBlockSize()
	{
		return 0;
	}

	@Override
	public void setIV(byte[] iv)
	{

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

	@Override
	public void setKey(byte[] key)
	{

	}

	@Override
	public void setIncrementIV(boolean val)
	{

	}

	@Override
	public int getKeySize()
	{		
		return 0;
	}

	@Override
	public byte[] getKey()
	{
		return null;
	}

	@Override
	public String getCipherName()
	{
		return "plain";
	}

	@Override
	public String getCipherModeName()
	{
		return "plain";
	}

}
