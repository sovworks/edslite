package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.BlockCipherNative;
import com.sovworks.eds.crypto.CipherFactory;
import com.sovworks.eds.crypto.blockciphers.GOST;
import com.sovworks.eds.crypto.modes.XTS;


public class GOSTXTS extends XTS
{	
	public GOSTXTS()
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
				return new GOST();
			}
		});
	}
	
	@Override
	public int getKeySize()
	{
		return 2*32;
	}
	
	@Override
	public String getCipherName()
	{
		return "gost";
	}
}

    