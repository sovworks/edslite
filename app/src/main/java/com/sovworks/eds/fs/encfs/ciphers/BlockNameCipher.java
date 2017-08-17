package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.fs.encfs.B64;
import com.sovworks.eds.fs.encfs.NameCodec;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class BlockNameCipher implements NameCodec
{
    public BlockNameCipher(FileEncryptionEngine cipher, MACCalculator mac, boolean caseSensitive)
    {
        _cipher = cipher;
        _hmac = mac;
        _caseSensitive = caseSensitive;
    }

    @Override
    public String encodeName(String plaintextName)
    {
        byte[] plain = plaintextName.getBytes();
        int len = plain.length; //calcLengthIncBlocs(plain.length);
        int blockSize = _cipher.getEncryptionBlockSize();
        int padding = blockSize - len % blockSize;
        if(padding == 0)
            padding = blockSize;
        byte[] res = new byte[calcEncodedLength(len + padding + 2)];
        System.arraycopy(plain, 0, res, 2, len);
        Arrays.fill(res, len + 2, len + padding + 2, (byte) padding);
        _hmac.setChainedIV(_iv);
        short mac = _hmac.calc16(res, 2, len + padding);
        _chainedIV = _hmac.getChainedIV();
        ByteBuffer.wrap(res).order(ByteOrder.BIG_ENDIAN).putShort(mac);
        byte[] iv = new byte[_cipher.getIVSize()];
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(mac & 0xFFFFL);
        if(_iv!=null)
            for(int i=0;i<_iv.length;i++)
                iv[i] ^= _iv[i];
        _cipher.setIV(iv);
        try
        {
            _cipher.encrypt(res, 2, len + padding);
        }
        catch (EncryptionEngineException e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
        if(_caseSensitive)
        {
            B64.changeBase2Inline(res, 0, len + padding + 2, 8, 5, true);
            return B64.B32ToString(res, 0, res.length);
        }
        else
        {
            B64.changeBase2Inline(res, 0, len + padding + 2, 8, 6, true);
            return B64.B64ToString(res, 0, res.length);
        }
    }

    @Override
    public String decodeName(String encodedName)
    {
        byte[] buf;
        if(_caseSensitive)
        {
            byte[] tmp = B64.StringToB32(encodedName);
            buf = new byte[B64.B32ToB256Bytes(tmp.length)];
            B64.changeBase2Inline(tmp, 0, tmp.length, 5, 8, false, 0, 0, buf, 0);
        }
        else
        {
            byte[] tmp = B64.StringToB64(encodedName);
            buf = new byte[B64.B64ToB256Bytes(tmp.length)];
            B64.changeBase2Inline(tmp, 0, tmp.length, 6, 8, false, 0, 0, buf, 0);
        }
        if(buf.length - 2 < _cipher.getEncryptionBlockSize())
            throw new IllegalArgumentException("Encoded name is too short: " + encodedName);
        short mac = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).getShort();
        byte[] iv = new byte[_cipher.getIVSize()];
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(mac & 0xFFFFL);
        if(_iv!=null)
            for(int i=0;i<_iv.length;i++)
                iv[i] ^= _iv[i];
        _cipher.setIV(iv);
        try
        {
            _cipher.decrypt(buf, 2, buf.length - 2);
        }
        catch (EncryptionEngineException e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
        try
        {
            int padding = buf[buf.length - 1];
            int finalSize = buf.length - padding - 2;
            if (padding > _cipher.getEncryptionBlockSize() || finalSize < 0)
                throw new IllegalArgumentException("Failed decoding name. Wrong padding. Name=" + encodedName);

            _hmac.setChainedIV(_iv);
            short mac2 = _hmac.calc16(buf, 2, buf.length - 2);
            _chainedIV = _hmac.getChainedIV();
            if (mac != mac2)
                throw new IllegalArgumentException("Failed decoding name. Checksum mismatch. Name=" + encodedName);
            return new String(buf, 2, finalSize);
        }
        finally
        {
            Arrays.fill(buf, (byte)0);
        }
    }

    @Override
    public void init(byte[] key)
    {
        _cipher.setKey(key);
        try
        {
            _cipher.init();
        }
        catch (EncryptionEngineException e)
        {
            throw new RuntimeException("Failed initializing cipher", e);
        }
        _hmac.init(key);
    }

    @Override
    public void close()
    {
        _cipher.close();
        _hmac.close();
    }

    @Override
    public void setIV(byte[] iv)
    {
        _iv = iv;
    }

    @Override
    public byte[] getChainedIV(String plaintextName)
    {
        if(_chainedIV == null)
            _chainedIV = calcChainedIV(plaintextName);
        return _chainedIV;
    }

    @Override
    public byte[] getIV()
    {
        return _iv;
    }

    @Override
    public int getIVSize()
    {
        return 8;
    }

    private final FileEncryptionEngine _cipher;
    private final MACCalculator _hmac;
    private final boolean _caseSensitive;
    private byte[] _iv;
    private byte[] _chainedIV;

    /*private int calcLengthIncBlocs(int plainLength)
    {
        int bs = _cipher.getEncryptionBlockSize();
        return ((plainLength + bs)/bs) * bs;
       // int len = ((plainLength + bs)/bs) * bs + 2; //num blocks + 2 checksum bytes
       // return calcEncodedLength(len);
    }*/

    private int calcEncodedLength(int plainLength)
    {
        return _caseSensitive ? B64.B256ToB32Bytes(plainLength) : B64.B256ToB64Bytes(plainLength);
    }

    private byte[] calcChainedIV(String plainTextName)
    {
        byte[] plain = plainTextName.getBytes();
        int len = plain.length; //calcLengthIncBlocs(plain.length);
        int blockSize = _cipher.getEncryptionBlockSize();
        int padding = blockSize - len % blockSize;
        if(padding == 0)
            padding = blockSize;
        byte[] res = new byte[calcEncodedLength(len + padding + 2)];
        System.arraycopy(plain, 0, res, 2, len);
        Arrays.fill(res, len + 2, len + padding + 2, (byte) padding);
        _hmac.setChainedIV(_iv);
        _hmac.calc64(res, 2, len + padding);
        return _hmac.getChainedIV();
    }
}
