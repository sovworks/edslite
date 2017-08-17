package com.sovworks.eds.crypto;


import com.sovworks.eds.container.EncryptedFileLayout;
import com.sovworks.eds.fs.util.TransInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class EncryptedInputStream extends TransInputStream
{

	public EncryptedInputStream(InputStream base, EncryptedFileLayout layout)
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

	protected final EncryptedFileLayout _layout;
	protected final FileEncryptionEngine _engine;
	protected boolean _allowEmptyParts = true;


	@Override
	protected int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException
	{
		if(_allowEmptyParts && count == _bufferSize && EncryptedFile.isBufferEmpty(baseBuffer, offset, count))
			return count;
		FileEncryptionEngine ee = _layout.getEngine();
		_layout.setEncryptionEngineIV(ee, bufferPosition);
		try
		{
			ee.decrypt(baseBuffer, offset, count);
		}
		catch (EncryptionEngineException e)
		{
			throw new IOException(e);
		}
		return count;
	}
}
