package com.sovworks.eds.crypto;

import android.annotation.SuppressLint;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.SecureRandom;


public class AF
{
	public static final int SECTOR_SIZE = 512;
	
	public static int calcNumRequiredSectors(int blocksize, int numBlocks)
	{
		int afSize = blocksize * numBlocks;
		return (afSize + (SECTOR_SIZE - 1)) / SECTOR_SIZE;
	}
	
	public AF(MessageDigest hash, int blockSize)
	{
		_hash = hash;
		_blockSize = blockSize;
	}
	
	@SuppressLint("TrulyRandom")
	public void split(byte[] src, int srcOffset, byte[] dest, int destOffset, int blockNumber) throws DigestException
	{
		byte[] block = new byte[_blockSize];
		byte[] tmp = new byte[_blockSize];
		SecureRandom sr = new SecureRandom();
		for(int i = 0; i < blockNumber - 1; i++)
		{
			sr.nextBytes(tmp);			
			System.arraycopy(tmp, 0, dest, destOffset + _blockSize*i, _blockSize);
			xorBlock(dest, destOffset + i*_blockSize, block, 0, block);
			diffuse(block, 0, block, 0, _blockSize);
		}
		xorBlock(src, srcOffset, dest, destOffset + _blockSize*(blockNumber - 1), block);	
	}
	
	public void merge(byte[] src, int srcOffset, byte[] dest, int destOffset, int blockNumber) throws DigestException
	{
		byte[] block = new byte[_blockSize];		
		for(int i = 0; i < blockNumber - 1; i++)
		{
			xorBlock(src, srcOffset + i*_blockSize, block, 0, block);
			diffuse(block, 0, block, 0, _blockSize);
		}
		xorBlock(src, srcOffset + _blockSize*(blockNumber - 1), dest, destOffset, block);	
	}
	
	public int calcNumRequiredSectors(int numBlocks)
	{
		return calcNumRequiredSectors(_blockSize, numBlocks);
	}
	
	
	private final MessageDigest _hash;
	private final int _blockSize;
	
	private void xorBlock(byte[] src, int srcOffset, byte[] dst, int dstOffset, byte[] xorBlock)
	{
		for(int i = 0; i < xorBlock.length; i++)
			dst[dstOffset + i] = (byte)((src[srcOffset + i] ^ xorBlock[i]) & 0xFF);
	}
	
	private void diffuse(byte[] src, int srcOffset, byte[] dst, int dstOffset, int len) throws DigestException
	{
		int ds = _hash.getDigestLength();
		int blocks = len/ds;
		int padding = len % ds;
		
		for(int i=0;i<blocks; i++)		
			hashBuf(src, srcOffset + ds*i, dst, dstOffset + ds*i, ds, i);
		if(padding > 0)
			hashBuf(src, srcOffset + ds*blocks, dst, dstOffset + ds*blocks, padding, blocks);		
	}

	private void hashBuf(byte[] src, int srcOffset, byte[] dst, int dstOffset, int len, int iv) throws DigestException
	{		
		ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(iv);
		_hash.reset();
		_hash.update(bb.array());
		_hash.update(src, srcOffset, len);
		byte[] res = _hash.digest();
		System.arraycopy(res, 0, dst, dstOffset, Math.min(res.length, dst.length - dstOffset));
	}
	
	/*
	private static int htonl(int value) 
	{
		return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? value : Integer.reverseBytes(value);
	}
	*/
}
