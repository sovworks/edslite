package com.sovworks.eds.veracrypt;


import com.sovworks.eds.truecrypt.StdLayout;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class VolumeLayout extends StdLayout
{
	public static int getKDFIterationsFromPIM(int pim)
	{
		return 15000 + pim*1000;
	}

	@Override
	public void setNumKDFIterations(int num)
	{
		_numIterations = num;
	}

	@Override
	public void close() throws IOException
	{
		super.close();
		_numIterations = 0;
	}

	@Override
	public List<MessageDigest> getSupportedHashFuncs()
	{
		List<MessageDigest> l = super.getSupportedHashFuncs();
		try
		{
			l.add(MessageDigest.getInstance("SHA256"));
		}
		catch (NoSuchAlgorithmException ignored)
		{
		}
		return l;
	}

	protected static final byte[] SIG = {'V','E','R','A'};
	protected static final short COMPATIBLE_PROGRAM_VERSION = 0x010b;
	
	@Override
	protected byte[] getHeaderSignature()
	{
		return SIG;
	}
	
    @Override
    protected int getMKKDFNumIterations(MessageDigest hashFunc)
    {
		return _numIterations > 0 ?
				getKDFIterationsFromPIM(_numIterations) :
				"ripemd160".equalsIgnoreCase(hashFunc.getAlgorithm()) ?
						655331 :
						500000;
    }	
    
    @Override
    protected short getMinCompatibleProgramVersion()
	{
		return COMPATIBLE_PROGRAM_VERSION;
	}

	private int _numIterations;
}
