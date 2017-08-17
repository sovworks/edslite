package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.blockciphers.AES;
import com.sovworks.eds.crypto.modes.ECB;


public class AESECB extends ECB
{
	public AESECB()
	{
		this(32);
	}

	public AESECB(final int keySize)
	{
		super(new AES(keySize));
	}	
	
	@Override
	public String getCipherName()
	{
		return "aes";
	}
}

    