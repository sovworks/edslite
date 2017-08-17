package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.blockciphers.AES;
import com.sovworks.eds.crypto.modes.CTR;


public class AESCTR extends CTR
{
	public AESCTR()
	{
		this(32);
	}

	public AESCTR(final int keySize)
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
		});
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

    