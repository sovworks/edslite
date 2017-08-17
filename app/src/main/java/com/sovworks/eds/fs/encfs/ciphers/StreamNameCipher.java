package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.fs.encfs.B64;
import com.sovworks.eds.fs.encfs.NameCodec;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class StreamNameCipher implements NameCodec
{
    public StreamNameCipher(EncryptionEngine cipher, MACCalculator mac)
    {
        _cipher = cipher;
        _hmac = mac;
    }

    @Override
    public String encodeName(String plaintextName)
    {
        byte[] plain = plaintextName.getBytes();
        int len = plain.length; //calcLengthIncBlocs(plain.length);
        byte[] res = new byte[B64.B256ToB64Bytes(len + 2)];
        System.arraycopy(plain, 0, res, 2, len);
        _hmac.setChainedIV(_iv);
        short mac = _hmac.calc16(plain, 0, len);
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
            _cipher.encrypt(res, 2, len);
        }
        catch (EncryptionEngineException e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
        B64.changeBase2Inline(res, 0, len + 2, 8, 6, true);
        return B64.B64ToString(res, 0, res.length);
    }

    @Override
    public String decodeName(String encodedName)
    {
        byte[] buf = B64.StringToB64(encodedName);
        int decodedLen = B64.B64ToB256Bytes(buf.length) - 2;
        B64.changeBase2Inline(buf, 0, buf.length, 6, 8, false);
        short mac = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).getShort();
        byte[] iv = new byte[_cipher.getIVSize()];
        ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(mac & 0xFFFFL);
        if(_iv!=null)
            for(int i=0;i<_iv.length;i++)
                iv[i] ^= _iv[i];
        _cipher.setIV(iv);
        try
        {
            _cipher.decrypt(buf, 2, decodedLen);
        }
        catch (EncryptionEngineException e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
        try
        {
            _hmac.setChainedIV(_iv);
            short mac2 = _hmac.calc16(buf, 2, decodedLen);
            _chainedIV = _hmac.getChainedIV();
            if (mac != mac2)
                throw new IllegalArgumentException("Failed decoding name. Checksum mismatch. Name=" + encodedName);
            return new String(buf, 2, decodedLen);
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
            throw new RuntimeException("Failed initilizing cipher", e);
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

    private final EncryptionEngine _cipher;
    private final MACCalculator _hmac;
    private byte[] _iv;
    private byte[] _chainedIV;

    /*private int calcLengthIncBlocs(int plainLength)
    {
        int bs = _cipher.getEncryptionBlockSize();
        return ((plainLength + bs)/bs) * bs;
       // int len = ((plainLength + bs)/bs) * bs + 2; //num blocks + 2 checksum bytes
       // return calcEncodedLength(len);
    }*/


    private byte[] calcChainedIV(String plainTextName)
    {
        byte[] plain = plainTextName.getBytes();
        try
        {
            _hmac.setChainedIV(_iv);
            _hmac.calc64(plain, 0, plain.length);
            return _hmac.getChainedIV();
        }
        finally
        {
            Arrays.fill(plain, (byte)0);
        }
    }
}
