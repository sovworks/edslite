package com.sovworks.eds.fs.encfs.macs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class MACCalculator
{
    public void setChainedIV(byte[] iv)
    {
        _chainedIV = iv;
        _useChainedIV = iv != null;
    }

    public boolean isChainedIVEnabled()
    {
        return _useChainedIV;
    }

    public void init(byte[] key)
    {

    }

    public byte[] getChainedIV()
    {
        return _chainedIV;
    }

    public long calc64(byte[] buf, int offset, int count)
    {
        return ByteBuffer.wrap(calcChecksum(buf, offset, count)).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    public int calc32(byte[] buf, int offset, int count)
    {
        byte[] cs = calcChecksum(buf, offset, count);
        for(int i=0;i<4;i++)
            cs[i] ^= cs[i+4];
        return ByteBuffer.wrap(cs).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public short calc16(byte[] buf, int offset, int count)
    {
        byte[] cs = calcChecksum(buf, offset, count);
        for(int i=0;i<4;i++)
            cs[i] ^= cs[i+4];
        for(int i=0;i<2;i++)
            cs[i] ^= cs[i+2];
        return ByteBuffer.wrap(cs).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    public void close()
    {

    }

    public abstract byte[] calcChecksum(byte[] buf, int offset, int count);

    private byte[] _chainedIV;
    private boolean _useChainedIV;
}
