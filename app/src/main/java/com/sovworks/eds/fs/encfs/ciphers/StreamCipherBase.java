package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class StreamCipherBase extends CipherBase
{
    public StreamCipherBase(EncryptionEngine base)
    {
        super(base);
    }

    @Override
    public void setIV(byte[] iv)
    {
        _iv = iv == null ? 0 : ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    @Override
    public void encrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        shuffleBytes(data, offset, len);
        byte[] iv = new byte[getIVSize()];
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(_iv);
        super.setIV(iv);
        super.encrypt(data, offset, len);
        flipBytes(data, offset, len);
        shuffleBytes(data, offset, len);
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(_iv + 1);
        super.setIV(iv);
        super.encrypt(data, offset, len);
    }

    @Override
    public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        byte[] iv = new byte[getIVSize()];
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(_iv + 1);
        super.setIV(iv);
        super.decrypt(data, offset, len);
        unshuffleBytes(data, offset, len);
        flipBytes(data, offset, len);
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(_iv);
        super.setIV(iv);
        super.decrypt(data, offset, len);
        unshuffleBytes(data, offset, len);
    }

    private static void shuffleBytes(byte[] buf, int offset, int count)
    {
        for (int i = 0; i < count - 1; ++i) buf[i + offset + 1] ^= buf[i + offset];
    }

    private static void unshuffleBytes(byte[] buf, int offset, int count)
    {
        for (int i = count - 1; i > 0; --i) buf[i + offset] ^= buf[i + offset - 1];
    }

    private static void flipBytes(byte[] buf, int offset, int count)
    {
        byte[] revBuf = new byte[64];

        int bytesLeft = count;
        while (bytesLeft > 0)
        {
            int toFlip = Math.min(revBuf.length, bytesLeft);

            for (int i = 0; i < toFlip; ++i) revBuf[i] = buf[toFlip + offset - (i + 1)];
            System.arraycopy(revBuf, 0, buf, offset, toFlip);
            bytesLeft -= toFlip;
            offset += toFlip;
        }
        Arrays.fill(revBuf, (byte)0);
    }

    private long _iv;
}
