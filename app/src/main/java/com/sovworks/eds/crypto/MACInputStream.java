package com.sovworks.eds.crypto;


import com.sovworks.eds.fs.encfs.macs.MACCalculator;
import com.sovworks.eds.fs.util.TransInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MACInputStream extends TransInputStream
{

	public MACInputStream(
			InputStream base,
			MACCalculator macCalc,
			int blockSize,
			int macBytes,
			int randBytes,
			boolean forceDecode)
	{
		super(base, blockSize - macBytes - randBytes);
		_macCalc = macCalc;
		_macBytes = macBytes;
		_randBytes = randBytes;
		_overhead = macBytes + randBytes;
		_forceDecode = forceDecode;
		_transBuffer = new byte[_bufferSize + _overhead];
	}

	public synchronized void close(boolean closeBase) throws IOException
	{
		Arrays.fill(_buffer, (byte) 0);
		Arrays.fill(_transBuffer, (byte) 0);
		super.close(closeBase);
	}

    public final void setAllowEmptyParts(boolean val)
    {
        _allowEmptyParts = val;
    }

	private byte[] _transBuffer;
	private final MACCalculator _macCalc;
	private final int _macBytes, _randBytes, _overhead;
	private final boolean _forceDecode;
	protected boolean _allowEmptyParts = true;


	@Override
	protected int readFromBaseAndTransformBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		int br = readFromBase(_transBuffer, offset, count + _overhead);
		if(br > 0)
			return transformBufferFromBase(_transBuffer, offset, br, bufferPosition, buf);
		else
			return 0;
	}

	@Override
	protected int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException
	{
		return MACFile.getMACCheckedBuffer(
				baseBuffer,
				offset,
				count,
				bufferPosition,
				dstBuffer,
				_macCalc,
				_macBytes,
				_randBytes,
				_allowEmptyParts,
				_forceDecode
		);
	}
}
