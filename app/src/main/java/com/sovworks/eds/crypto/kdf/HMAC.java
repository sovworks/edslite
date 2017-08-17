package com.sovworks.eds.crypto.kdf;

import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;

import com.sovworks.eds.crypto.EncryptionEngineException;

public class HMAC
{
	public HMAC(byte[] key,MessageDigest md,int blockSize)
    {				
		_md = md;
        _digest = new byte[getDigestLength()];
        _block = new byte[blockSize];
        _key = key.length > _block.length ? md.digest(key) : key.clone();
        
    }
	
	public int getDigestLength()
	{
		return _md.getDigestLength();
	}
	
	public void calcHMAC(byte[] data,int dataOffset,int dataLen,byte[] out) throws DigestException, EncryptionEngineException
    {
        _md.reset();
        for(int i=0;i<_key.length;i++)
            _block[i] = (byte)(_key[i] ^ 0x36);
        Arrays.fill(_block, _key.length, _block.length, (byte)0x36);        
        
        _md.update(_block);
        _md.update(data,dataOffset,dataLen);
        _md.digest(_digest,0,_digest.length);

        for(int i=0;i<_key.length;i++)
            _block[i] = (byte)(_key[i] ^ 0x5C);
        Arrays.fill(_block, _key.length, _block.length, (byte)0x5C);
        _md.update(_block);
        _md.update(_digest);
        _md.digest(_digest,0,_digest.length);
        System.arraycopy(_digest,0,out,0,_digest.length);
    }
	
	public void close()
    {
        _md.reset();
	    Arrays.fill(_key,(byte)0);
	    Arrays.fill(_digest,(byte)0);
	    Arrays.fill(_block,(byte)0);
    }	
	
	protected final MessageDigest _md;
	protected final byte[] _digest,_block,_key;   
	
}