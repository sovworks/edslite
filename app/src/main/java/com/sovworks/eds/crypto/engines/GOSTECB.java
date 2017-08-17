package com.sovworks.eds.crypto.engines;

import com.sovworks.eds.crypto.blockciphers.GOST;
import com.sovworks.eds.crypto.modes.ECB;

public class GOSTECB extends ECB
{

	public GOSTECB()
	{
		super(new GOST());
	}

	@Override
	public String getCipherName()
	{
		return "gost";
	}

}
