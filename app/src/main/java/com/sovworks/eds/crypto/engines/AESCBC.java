package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.blockciphers.AES;
import com.sovworks.eds.crypto.modes.CBC;


public class AESCBC extends CBC
{
	public AESCBC()
	{
		this(32, 512);
	}

	public AESCBC(final int keySize, final int fileBlockSize)
	{
		super(new CipherFactory()
		{
			
			@Override
			public int getNumberOfCiphers()
			{
				return 1;
			}
			
			@Override
			public BlockCipherNative createCipher(int typeIndex)
			{
				return new AES(keySize);
			}
		}, fileBlockSize);
		_keySize = keySize;
	}	
	
	@Override
	public String getCipherName()
	{
		return "aes";
	}
	

    @Override
	public int getKeySize()
	{
    	return _keySize;
	}

	private final int _keySize;
}

    