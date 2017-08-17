package com.sovworks.eds.crypto;

import android.util.SparseArray;

import com.sovworks.eds.container.VolumeLayout;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class EncryptedFileWithCache extends EncryptedFile
{
	private static final int DEFAULT_NUM_CACHED_BUFFERS = 25;
	private static final int DEFAULT_BUFFER_SIZE_IN_BLOCKS = 40;
	
	public EncryptedFileWithCache(Path pathToFile, AccessMode mode,VolumeLayout layout) throws IOException
	{
		this(pathToFile.getFile().getRandomAccessIO(mode),layout, DEFAULT_NUM_CACHED_BUFFERS, DEFAULT_BUFFER_SIZE_IN_BLOCKS);
	}
	
	public EncryptedFileWithCache(RandomAccessIO base, VolumeLayout layout)
			throws FileNotFoundException
	{
		this(base,layout, DEFAULT_NUM_CACHED_BUFFERS, DEFAULT_BUFFER_SIZE_IN_BLOCKS);
	}

	public EncryptedFileWithCache(Path pathToFile, AccessMode mode,VolumeLayout layout, int maxNumberCachedBuffers, int bufferSizeInBlocks) throws IOException
	{
		this(pathToFile.getFile().getRandomAccessIO(mode),layout, maxNumberCachedBuffers, bufferSizeInBlocks);
	}
	
	public EncryptedFileWithCache(RandomAccessIO base, VolumeLayout layout, int maxNumberCachedBuffers, int bufferSizeInBlocks)
			throws FileNotFoundException
	{
		super(base, layout, bufferSizeInBlocks);
		_maxNumberCachedBuffers = maxNumberCachedBuffers;
		_cache = new SparseArray<>(_maxNumberCachedBuffers);
	}
	
	@Override
	public synchronized void flush() throws IOException
	{
		writeCurrentBuffer();
		flushCachedChanges();
		getBase().flush();
	}
	
	@Override
	public synchronized void close(boolean closeBase) throws IOException
	{
        try
        {
            writeCurrentBuffer();
            flushCachedChanges();
            for (int i = 0; i < _cache.size(); i++)
            {
                CachedSectorInfo ci = _cache.valueAt(i);
                Arrays.fill(ci.buffer, (byte) 0);
            }
            _cache.clear();
            Arrays.fill(_buffer, (byte) 0);
        }
        finally
        {
            if(closeBase)
				getBase().close();
        }
	}

	@Override
	protected void loadCurrentBuffer() throws IOException
	{
		if(_isBufferLoaded)
			return;
		long bp = getBufferPosition();
		int space = (int)Math.min(_length - bp, _bufferSize);
		if(space < 0)
			space = 0;
        int bufIndex = getBufferIndex();
		CachedSectorInfo ci = _cache.get(bufIndex);
		if(ci==null)
		{
			ci = reserveCacheSlot(bufIndex);
			if(space>0)
				space = readFromBaseAndTransformBuffer(ci.buffer, 0, space, bp);
			Arrays.fill(ci.buffer, space, _bufferSize, (byte)0);
		}
		else
			ci.refCount++;

		System.arraycopy(ci.buffer, 0, _buffer, 0, _bufferSize);
		_isBufferChanged = false;
		_isBufferLoaded = true;
	}

	@Override
	protected void writeCurrentBuffer() throws IOException
	{
		if(!_isBufferChanged)
			return;
		CachedSectorInfo ci = _cache.get(getBufferIndex());
		System.arraycopy(_buffer, 0, ci.buffer, 0, _bufferSize);
		ci.isChanged = true;
		_isBufferChanged = false;
	}

	private static class CachedSectorInfo
	{
		public CachedSectorInfo(int bufSize)
		{
			buffer = new byte[bufSize];
			refCount = 1;
		}
		public int refCount;
		public final byte[] buffer;
		public boolean isChanged;
	}
	
	private final int _maxNumberCachedBuffers;
	private final SparseArray<CachedSectorInfo> _cache;
	
	private int getBufferIndex()
	{
		return (int) (getBufferPosition()/_bufferSize);
	}
		
	private void flushCachedChanges() throws IOException
	{
		int cs = _cache.size();
		for(int i=0;i<cs;i++)
		{
			CachedSectorInfo ci = _cache.valueAt(i);
			if(ci.isChanged)
				writeCachedBuffer(_cache.keyAt(i), ci);
		}
	}
	
	private void writeCachedBuffer(int bufIndex, CachedSectorInfo ci) throws IOException
	{
		long bp = (long)bufIndex*_bufferSize;
		transformBufferAndWriteToBase(ci.buffer, 0, (int)Math.min(_bufferSize, _length - bp), bp);
		ci.isChanged = false;
	}
	
	private CachedSectorInfo reserveCacheSlot(int bufIndex) throws IOException
	{
		int cs = _cache.size();
		if(cs<_maxNumberCachedBuffers)
		{
			CachedSectorInfo ci = new CachedSectorInfo(_buffer.length);
			_cache.put(bufIndex, ci);
			return ci;
		}

		int minRefsBufIndex = -1;
		int minRefs = 0;
		for(int i=0;i<cs;i++)
		{
			int oldBufIndex = _cache.keyAt(i);
			CachedSectorInfo ci = _cache.valueAt(i);
			if(minRefs == 0 || ci.refCount<minRefs)
			{
				minRefs = ci.refCount;
				minRefsBufIndex = oldBufIndex;
			}
		}
		CachedSectorInfo ci = _cache.get(minRefsBufIndex);
		if(ci.isChanged)
			writeCachedBuffer(minRefsBufIndex, ci);
		_cache.remove(minRefsBufIndex);
		ci.refCount = 1;
		_cache.put(bufIndex, ci);
		return ci;
	}	
}
