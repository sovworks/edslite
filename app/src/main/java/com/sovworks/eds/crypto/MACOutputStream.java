package com.sovworks.eds.crypto;


import com.sovworks.eds.fs.encfs.macs.MACCalculator;
import com.sovworks.eds.fs.util.TransOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

public class MACOutputStream extends TransOutputStream
{
	public MACOutputStream(
			OutputStream base,
			MACCalculator macCalc,
			int blockSize,
			int macBytes,
			int randBytes) throws FileNotFoundException
	{
		super(base, blockSize - macBytes - randBytes);
		_macCalc = macCalc;
		_macBytes = macBytes;
		_randBytes = randBytes;
		_overhead = macBytes + randBytes;
		_random = _randBytes > 0 ? new SecureRandom() : null;
		_transBuffer = new byte[_bufferSize + _overhead];
	}

	public synchronized void close(boolean closeBase) throws IOException
	{
		try
		{
			super.close(closeBase);
		}
		finally
		{
			Arrays.fill(_buffer, (byte) 0);
			Arrays.fill(_transBuffer, (byte) 0);
		}
	}

	private byte[] _transBuffer;
	private final MACCalculator _macCalc;
	private final int _macBytes, _randBytes, _overhead;
	protected final SecureRandom _random;

	@Override
	protected void transformBufferAndWriteToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		transformBufferToBase(buf, offset, count, bufferPosition, _transBuffer);
		writeToBase(_transBuffer, offset, count + _overhead);
	}

	@Override
	protected void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException
	{
		MACFile.makeMACCheckedBuffer(
				buf,
				offset,
				count,
				baseBuffer,
				_macCalc,
				_macBytes,
				_randBytes,
				_random);
	}


}
