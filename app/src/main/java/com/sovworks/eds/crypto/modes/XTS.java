package com.sovworks.eds.crypto.modes;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.FileEncryptionEngine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;


public abstract class XTS implements FileEncryptionEngine
{
	@Override
	public synchronized void init() throws EncryptionEngineException
	{			
		closeCiphers();
		closeContext();
		
		_xtsContextPointer = initContext();
		if(_xtsContextPointer == 0)
			throw new EncryptionEngineException("XTS context initialization failed");
		
		addBlockCiphers(_cf);
		
		if(_key == null)
			throw new EncryptionEngineException("Encryption key is not set");
		
		int keyOffset = 0;
		int eeKeySize = getKeySize()/2;
		for(CipherPair p: _blockCiphers)
		{
			int ks = p.cipherA.getKeySize();
			byte[] tmp = new byte[ks];			
			try
			{
				System.arraycopy(_key, keyOffset, tmp, 0, ks);
				p.cipherA.init(tmp);
				System.arraycopy(_key, eeKeySize + keyOffset, tmp, 0, ks);
				p.cipherB.init(tmp);
				attachNativeCipher(_xtsContextPointer,p.cipherA.getNativeInterfacePointer(),p.cipherB.getNativeInterfacePointer());
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
		return SECTOR_SIZE;
	}

    @Override
    public void setIV(byte[] iv)
    {
		_iv = ByteBuffer.wrap(iv).getLong();
	}

	@Override
	public byte[] getIV()
	{
		return ByteBuffer.allocate(getIVSize()).putLong(_iv).array();
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
	public void setIncrementIV(boolean val)
	{
		_incrementIV = val;
	}

	@Override
	public int getKeySize()
	{
    	int res = 0;
    	for(CipherPair c: _blockCiphers)
    		res += c.cipherA.getKeySize();
    	return 2*res;
	}

	@Override
	public void close()
	{
		closeCiphers();
		closeContext();
		clearAll();
	}

	@Override
	public void encrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
		if(_xtsContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len % getEncryptionBlockSize() != 0 || (offset+len) > data.length)
			throw new EncryptionEngineException("Wrong buffer length");		
        if(encrypt(data, offset, len,_iv,_xtsContextPointer)!=0)
        	throw new EncryptionEngineException("Failed encrypting data");
		if(_incrementIV)
			_iv += (len/getFileBlockSize());
	}	

	@Override
	public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
		if(_xtsContextPointer == 0)
			throw new EncryptionEngineException("Engine is closed");
		if(len % getEncryptionBlockSize() != 0 || (offset+len) > data.length)
			throw new EncryptionEngineException("Wrong buffer length");		
		
		if(decrypt(data,offset,len,_iv,_xtsContextPointer)!=0)
        	throw new EncryptionEngineException("Failed decrypting data");
		if(_incrementIV)
			_iv += (len/getFileBlockSize());
	}
	
	@Override
	public byte[] getKey()
	{
		return _key;
	}
	
	@Override
	public String getCipherModeName()
	{
		return "xts-plain64";
	}
	
	public long getXTSContextPointer()
	{
		return _xtsContextPointer;
	}

	@Override
	public int getEncryptionBlockSize()
	{
		return 16;
	}

	private static final int SECTOR_SIZE = 512;
	
	static
	{
		System.loadLibrary("edsxts");
	}
	
	protected static class CipherPair
	{		
		public CipherPair(BlockCipherNative a,BlockCipherNative b)
		{
			cipherA = a;
			cipherB = b;
		}
		public BlockCipherNative cipherA;
		public BlockCipherNative cipherB;
	}	
	protected long _iv;
	protected byte[] _key;	
	protected final CipherFactory _cf;
	protected final ArrayList<CipherPair> _blockCiphers = new ArrayList<>();
	protected  boolean _incrementIV = false;
	
	protected XTS(CipherFactory cf)
	{
		_cf = cf;
	}
	
	protected void closeCiphers()
	{
		for(CipherPair p: _blockCiphers)
		{
			p.cipherA.close();
			p.cipherB.close();
		}		
		_blockCiphers.clear();		
	}	

	protected void closeContext()
	{
		if(_xtsContextPointer!=0)
		{
			closeContext(_xtsContextPointer);
			_xtsContextPointer = 0;
		}
	}	
	
	private long _xtsContextPointer;
	
	private native long initContext();
	private native void closeContext(long contextPointer);
	private native void attachNativeCipher(long contextPointer,long nativeCipherInterfacePointer,long secNativeCipherInterfacePointer);
	private native int encrypt(byte[] data,int offset, int len,long startSectorAddress,long contextPointer);
	private native int decrypt(byte[] data,int offset, int len,long startSectorAddress,long contextPointer);
	
	private void addBlockCiphers(CipherFactory cipherFactory)
	{		
		for(int i=0;i<cipherFactory.getNumberOfCiphers();i++)		
			_blockCiphers.add(new CipherPair(cipherFactory.createCipher(i),cipherFactory.createCipher(i)));
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

    