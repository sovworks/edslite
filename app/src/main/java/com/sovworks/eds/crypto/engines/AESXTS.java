package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.blockciphers.AES;
import com.sovworks.eds.crypto.modes.XTS;


public class AESXTS extends XTS
{
	public static final String NAME = "aes";

	public AESXTS()
	{
		this(64);		
	}
	
	public AESXTS(final int keySize)
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
				return new AES(keySize/2);
			}
		});
		_keySize = keySize;
	}
	
	@Override
	public int getKeySize()
	{		
		return _keySize;
	}
	
	@Override
	public String getCipherName()
	{
		return NAME;
	}
	
	private final int _keySize;
}

    