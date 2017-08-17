package com.sovworks.eds.veracrypt;

import com.sovworks.eds.container.VolumeLayout;


public class FormatInfo extends com.sovworks.eds.truecrypt.FormatInfo 
{

	public static final String FORMAT_NAME = "VeraCrypt";

	@Override
	public String getFormatName()
	{
		return FORMAT_NAME;
	}

	@Override
	public VolumeLayout getVolumeLayout()
	{
		return new com.sovworks.eds.veracrypt.VolumeLayout();
	}

	@Override
	public int getOpeningPriority()
	{		
		return 3;
	}

	@Override
	public boolean hasCustomKDFIterationsSupport()
	{
		return true;
	}

}
