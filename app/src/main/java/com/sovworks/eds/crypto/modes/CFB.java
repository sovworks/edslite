package com.sovworks.eds.crypto.modes;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class CFB implements EncryptionEngine
{

	@Override
	public synchronized void init() throws EncryptionEngineException
	{
		closeCiphers();
		closeContext();

		_cfbContextPointer = initContext();
		if(_cfbContextPointer == 0)
			throw new EncryptionEngineException("CFB context initialization failed");

		addBlockCiphers(_cf);

		if(_key == null)
			throw new EncryptionEngineException("Encryption key is not set");

		int keyOffset = 0;
		for(BlockCipherNative p: _blockCiphers)
		{
			int ks = p.getKeySize();
			byte[] tmp = new byte[ks];
			try
			{
				System.arraycopy(_key, keyOffset, tmp, 0, ks);
				p.init(tmp);
				attachNativeCipher(_cfbContextPointer,p.getNativeInterfacePointer());
			}
			finally
			{
				Arrays.fill(tmp,(byte)0);
			}
			keyOffset += ks;
		}
	}

    @Override
    public void setIV(byte[] iv)
    {
        _iv = iv;
    }

	@Override
	public byte[] getIV()
	{
		return _iv;
	}

	@Override
	public int getIVSize()
	{
		return 16;
	}

	@Override
	public void setKey(byte[] key)
	{
	    clearKey();
		_key = key == null ? null : Arrays.copyOf(key, getKeySize());
	}

    @Override
	public int getKeySize()
	{
    	int res = 0;
    	for(BlockCipherNative c: _blockCiphers)
    		res += c.getKeySize();
    	return res;
	}

	public void close()
	{
		closeCiphers();
		closeContext();
		clearAll();
	}

	@Override
	public void encrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
		if(_cfbContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len == 0)
			return;
		if((offset+len) > data.length)
			throw new IllegalArgumentException("Wrong length or offset");
        if(encrypt(data, offset, len,_iv,_cfbContextPointer)!=0)
        	throw new EncryptionEngineException("Failed encrypting data");
	}

	@Override
	public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
		if(_cfbContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len == 0)
			return;
		if((offset+len) > data.length)
			throw new IllegalArgumentException("Wrong length or offset");

		if(decrypt(data,offset,len,_iv,_cfbContextPointer)!=0)
        	throw new EncryptionEngineException("Failed decrypting data");
	}

	@Override
	public byte[] getKey()
	{
		return _key;
	}

	@Override
	public String getCipherModeName()
	{
		return "cfb-plain";
	}

	static
	{
		System.loadLibrary("edscfb");
	}

	protected byte[] _iv;
	protected byte[] _key;
	protected final CipherFactory _cf;
	protected final ArrayList<BlockCipherNative> _blockCiphers = new ArrayList<>();

	protected CFB(CipherFactory cf)
	{
		_cf = cf;
	}
	
	protected void closeCiphers()
	{
		for(BlockCipherNative p: _blockCiphers)
			p.close();
		_blockCiphers.clear();		
	}	

	protected void closeContext()
	{
		if(_cfbContextPointer!=0)
		{
			closeContext(_cfbContextPointer);
			_cfbContextPointer = 0;
		}
	}	
	
	private long _cfbContextPointer;

	private native long initContext();
	private native void closeContext(long contextPointer);
	private native void attachNativeCipher(long contextPointer,long nativeCipherInterfacePointer);
	private native int encrypt(byte[] data,int offset, int len,byte[] iv,long contextPointer);
	private native int decrypt(byte[] data,int offset, int len,byte[] iv,long contextPointer);
	
	private void addBlockCiphers(CipherFactory cipherFactory)
	{		
		for(int i=0;i<cipherFactory.getNumberOfCiphers();i++)		
			_blockCiphers.add(cipherFactory.createCipher(i));
	}
	
	private void clearAll()
	{
		clearKey();
	}

	private void clearKey()
	{
		if(_key!=null)
		{
			Arrays.fill(_key, (byte)0);
			_key = null;
		}
	}
}

    