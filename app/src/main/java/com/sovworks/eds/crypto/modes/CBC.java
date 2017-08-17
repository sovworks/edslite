package com.sovworks.eds.crypto.modes;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.FileEncryptionEngine;

import java.util.ArrayList;
import java.util.Arrays;


public abstract class CBC implements FileEncryptionEngine
{
	public static final String NAME = "cbc-plain";

	@Override
	public synchronized void init() throws EncryptionEngineException
	{			
		closeCiphers();
		closeContext();
		
		_cbcContextPointer = initContext();
		if(_cbcContextPointer == 0)
			throw new EncryptionEngineException("CBC context initialization failed");
		
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
				attachNativeCipher(_cbcContextPointer,p.getNativeInterfacePointer());
			}
			finally
			{
				Arrays.fill(tmp,(byte)0);
			}
			keyOffset += ks;
		}					
	}

	@Override
	public int getFileBlockSize()
	{
		return _fileBlockSize;
	}

	@Override
	public int getEncryptionBlockSize()
	{
		return 16;
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
		_key = key == null ? null :  Arrays.copyOf(key, getKeySize());
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
		if(_cbcContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len % getEncryptionBlockSize() != 0 || (offset+len) > data.length)
			throw new EncryptionEngineException("Wrong buffer length");		
        if(encrypt(data, offset, len,_iv,_cbcContextPointer, _incrementIV)!=0)
        	throw new EncryptionEngineException("Failed encrypting data");
	}	

	@Override
	public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
		if(_cbcContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len % getEncryptionBlockSize() != 0 || (offset+len) > data.length)
			throw new EncryptionEngineException("Wrong buffer length");		
		
		if(decrypt(data,offset,len,_iv,_cbcContextPointer, _incrementIV)!=0)
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
		return NAME;
	}

	@Override
	public void setIncrementIV(boolean val)
	{
		_incrementIV = val;
	}

	static
	{
		System.loadLibrary("edscbc");
	}
	
	protected byte[] _iv;
	protected byte[] _key;	
	protected final CipherFactory _cf;
	protected final ArrayList<BlockCipherNative> _blockCiphers = new ArrayList<>();

    protected CBC(CipherFactory cf)
    {
        this(cf, 512);
    }

	protected CBC(CipherFactory cf, int fileBlockSize)
	{
		_cf = cf;
        _fileBlockSize = fileBlockSize;
	}
	
	protected void closeCiphers()
	{
		for(BlockCipherNative p: _blockCiphers)
			p.close();
		_blockCiphers.clear();		
	}	

	protected void closeContext()
	{
		if(_cbcContextPointer!=0)
		{
			closeContext(_cbcContextPointer);
			_cbcContextPointer = 0;
		}
	}	
	
	private long _cbcContextPointer;
	private boolean _incrementIV = true;
	private final int _fileBlockSize;
	
	private native long initContext();
	private native void closeContext(long contextPointer);
	private native void attachNativeCipher(long contextPointer,long nativeCipherInterfacePointer);
	private native int encrypt(byte[] data,int offset, int len,byte[] iv,long contextPointer, boolean incrementIV);
	private native int decrypt(byte[] data,int offset, int len,byte[] iv,long contextPointer, boolean incrementIV);
	
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

    