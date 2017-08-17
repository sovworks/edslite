package com.sovworks.eds.crypto;


import com.sovworks.eds.container.EncryptedFileLayout;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.TransRandomAccessIO;

import java.io.FileNotFoundException;
import java.io.IOException;

public class EncryptedFile extends TransRandomAccessIO
{
	public static boolean isBufferEmpty(byte[] buf, int offset, int count)
	{
		for(int i=0;i<count;i++)
			if(buf[offset + i] != 0)
				return false;
		return true;
	}

	private static final int DEFAULT_BUFFER_SIZE_IN_BLOCKS = 16;

	public EncryptedFile(RandomAccessIO base,EncryptedFileLayout layout) throws FileNotFoundException
	{
		this(base, layout, DEFAULT_BUFFER_SIZE_IN_BLOCKS);
	}

	public EncryptedFile(RandomAccessIO base, EncryptedFileLayout layout, int bufferSizeInBlocks) throws FileNotFoundException
	{
		super(base, bufferSizeInBlocks*layout.getEngine().getFileBlockSize());
		_layout = layout;
		_dataOffset = layout.getEncryptedDataOffset();
		_fileBlockSize = layout.getEngine().getFileBlockSize();
		_transBuffer = new byte[_bufferSize];
		try
		{
			_length = calcVirtPosition(base.length());
		}
		catch (IOException ignored)
		{

		}
	}

	public EncryptedFile(Path pathToFile, AccessMode mode,EncryptedFileLayout layout) throws IOException
	{
		this(pathToFile, mode, layout, DEFAULT_BUFFER_SIZE_IN_BLOCKS);
	}

	public EncryptedFile(Path pathToFile, AccessMode mode,EncryptedFileLayout layout, int bufferSizeInBlocks) throws IOException
	{
		this(pathToFile.getFile().getRandomAccessIO(mode),layout, bufferSizeInBlocks);
	}

	protected final long _dataOffset;
	protected final int _fileBlockSize;
	protected final EncryptedFileLayout _layout;
	protected byte[] _transBuffer;

	@Override
	protected long calcBasePosition(long position)
	{
		return position + _dataOffset;
	}

	@Override
	protected long calcVirtPosition(long basePosition)
	{
		return basePosition - _dataOffset;
	}

	@Override
	protected int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException
	{
		if(baseBuffer != dstBuffer)
			System.arraycopy(baseBuffer, offset, dstBuffer, offset, count);

		if(!_allowSkip)
			decryptBuffer(dstBuffer, offset, count, bufferPosition);
		else
		{
			for(int i = 0;i<count;)
			{
				int curSize = Math.min(count - i, _fileBlockSize);
				if(curSize != _fileBlockSize || !isBufferEmpty(dstBuffer, offset + i, curSize))
					decryptBuffer(dstBuffer, offset + i, curSize, bufferPosition + i);
				i += curSize;
			}
		}
		return count;
	}

	protected void decryptBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		FileEncryptionEngine ee = _layout.getEngine();
		_layout.setEncryptionEngineIV(ee, bufferPosition);
		try
		{
			ee.decrypt(buf, offset, count);
		}
		catch (EncryptionEngineException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	protected void transformBufferAndWriteToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
		transformBufferToBase(buf, offset, count, bufferPosition, _transBuffer);
		writeToBase(_transBuffer, offset, count, bufferPosition);
	}

	@Override
	protected void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException
	{
        System.arraycopy(buf, offset, baseBuffer, offset, count);
		if(!_allowSkip)
			encryptBuffer(baseBuffer, offset, count, bufferPosition);
		else
		{
			for(int i = 0;i<count;)
			{
				int curSize = Math.min(count - i, _fileBlockSize);
				if(curSize != _fileBlockSize || !isBufferEmpty(baseBuffer, offset + i, curSize))
					encryptBuffer(baseBuffer, offset + i, curSize, bufferPosition + i);
				i += curSize;
			}
		}
	}

	protected void encryptBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
	{
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
