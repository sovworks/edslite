package com.sovworks.eds.crypto;


import com.sovworks.eds.container.EncryptedFileLayout;
import com.sovworks.eds.fs.util.TransOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class EncryptedOutputStream extends TransOutputStream
{
	public EncryptedOutputStream(OutputStream base, EncryptedFileLayout layout) throws FileNotFoundException
	{
		super(base, layout.getEngine().getFileBlockSize());
		_layout = layout;
		_engine = layout.getEngine();
	}

	public synchronized void close(boolean closeBase) throws IOException
	{
		try
		{
			super.close(closeBase);
		}
		finally
		{
			_layout.close();
			Arrays.fill(_buffer, (byte) 0);
		}
	}

	public final void setAllowEmptyParts(boolean val)
	{
		_allowEmptyParts = val;
	}

	protected final FileEncryptionEngine _engine;
	protected final EncryptedFileLayout _layout;
	protected boolean _allowEmptyParts;

	@Override
	protected void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException
	{
		if(_allowEmptyParts && count == _bufferSize && EncryptedFile.isBufferEmpty(buf, offset, count))
			return;
		FileEncryptionEngine ee = _layout.getEngine();
		_layout.setEncryptionEngineIV(ee, bufferPosition);
		try
		{
			ee.encrypt(buf, offset, count);
		}
		catch (EncryptionEngineException e)
		{
			throw new IOException(e);
		}
	}
}
