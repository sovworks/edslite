package com.sovworks.eds.crypto;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;
import com.sovworks.eds.fs.util.TransRandomAccessIO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

public class MACFile extends TransRandomAccessIO
{
    public static long calcVirtPosition(long realPos, int blockSize, int overhead)
    {
        int blockSizeWithOverhead = blockSize + overhead;
        long blockNum = (realPos + blockSizeWithOverhead - 1) / blockSizeWithOverhead;
        return realPos - blockNum * overhead;
    }

    public static int getMACCheckedBuffer(
            byte[] baseBuffer,
            int offset,
            int count,
            long bufferPosition,
            byte[] dstBuffer,
            MACCalculator macCalc,
            int macBytes,
            int randBytes,
            boolean allowSkip,
            boolean forceDecode) throws IOException
    {
        int resCount = count - macBytes - randBytes;
        System.arraycopy(baseBuffer, offset + macBytes + randBytes, dstBuffer, offset, resCount);
        if(macBytes == 0 || (allowSkip && count > macBytes && EncryptedFile.isBufferEmpty(baseBuffer, offset, count)))
            return resCount;
        byte fail = 0;
        byte[] mac = macCalc.calcChecksum(baseBuffer, offset + macBytes, count - macBytes);
        for(int i=0;i<macBytes;i++)
            fail |= mac[i] ^ baseBuffer[macBytes - i - 1];
        if(fail != 0)
        {
            String msg = "MAC comparison failure for the block at " + bufferPosition;
            if(forceDecode)
                Logger.log(msg);
            else
                throw new IOException(msg);
        }
        return resCount;
    }

    public static void makeMACCheckedBuffer(
            byte[] buf,
            int offset,
            int count,
            byte[] baseBuffer,
            MACCalculator macCalc,
            int macBytes,
            int randBytes,
            SecureRandom random) throws IOException
    {
        System.arraycopy(buf, offset, baseBuffer, offset + macBytes + randBytes, count);
        if (randBytes > 0)
        {
            byte[] rb = new byte[randBytes];
            random.nextBytes(rb);
            System.arraycopy(rb, 0, baseBuffer, offset + macBytes, randBytes);
        }
        if(macBytes > 0)
        {
            byte[] mac = macCalc.calcChecksum(baseBuffer, offset + macBytes, count + randBytes);
            for(int i=0;i<macBytes;i++)
                baseBuffer[offset + i] = mac[macBytes - i - 1];
        }
    }

    public MACFile(
            RandomAccessIO base,
            MACCalculator macCalc,
            int blockSize,
            int macBytes,
            int randBytes,
            boolean forceDecode) throws FileNotFoundException
    {
        super(base, blockSize - macBytes - randBytes);
        _macCalc = macCalc;
        _macBytes = macBytes;
        _randBytes = randBytes;
        _overhead = macBytes + randBytes;
        _forceDecode = forceDecode;
        _random = _randBytes > 0 ? new SecureRandom() : null;
        _transBuffer = new byte[_bufferSize + _overhead];
        try
        {
            _length = calcVirtPosition(base.length());
        }
        catch (IOException ignored)
        {

        }
    }

    @Override
    public synchronized void close(boolean closeBase) throws IOException
    {
        try
        {
            super.close(closeBase);
        }
        finally
        {
            _macCalc.close();
            Arrays.fill(_transBuffer, (byte) 0);
        }
    }

    @Override
    protected long calcBasePosition(long position)
    {
        long blockNum = (position + _bufferSize - 1) / _bufferSize;
        return position + blockNum * _overhead;
    }

    @Override
    protected long calcVirtPosition(long basePosition)
    {
        return calcVirtPosition(basePosition, _bufferSize, _overhead);
    }

    @Override
    protected int readFromBaseAndTransformBuffer(byte[] buf, int offset, int count, long bufferPosition) throws IOException
    {
        int bc = readFromBase(_transBuffer, offset, count + _overhead, bufferPosition);
        if(bc > 0)
            return transformBufferFromBase(_transBuffer, offset, bc, bufferPosition, buf);
        else
            return 0;
    }

    @Override
    protected int transformBufferFromBase(byte[] baseBuffer, int offset, int count, long bufferPosition, byte[] dstBuffer) throws IOException
    {
        return getMACCheckedBuffer(
                baseBuffer,
                offset,
                count,
                bufferPosition,
                dstBuffer,
                _macCalc,
                _macBytes,
                _randBytes,
                _allowSkip,
                _forceDecode
        );
    }

    @Override
    protected void transformBufferAndWriteToBase(byte[] buf, int offset, int count, long bufferPosition) throws IOException
    {
        transformBufferToBase(buf, offset, count, bufferPosition, _transBuffer);
        writeToBase(_transBuffer, offset, count + _overhead, bufferPosition);
    }

    @Override
    protected void transformBufferToBase(byte[] buf, int offset, int count, long bufferPosition, byte[] baseBuffer) throws IOException
    {
        makeMACCheckedBuffer(
                buf,
                offset,
                count,
                baseBuffer,
                _macCalc,
                _macBytes,
                _randBytes,
                _random);
    }

    private byte[] _transBuffer;
    private final MACCalculator _macCalc;
    private final int _macBytes, _randBytes, _overhead;
    private final boolean _forceDecode;
    private final SecureRandom _random;
}
