package com.sovworks.eds.fs.encfs.ciphers;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.kdf.HMAC;
import com.sovworks.eds.crypto.kdf.HMACSHA1;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CipherBase implements EncryptionEngine
{
    private static byte[] getIVFromBuf(byte[] buf, int keySize)
    {
        byte[] res = new byte[buf.length - keySize];
        System.arraycopy(buf, keySize, res, 0, res.length );
        return res;
    }

    public static byte[] getKeyFromBuf(byte[] buf, int keySize)
    {
        byte[] res = new byte[keySize];
        System.arraycopy(buf, 0, res, 0, res.length);
        return res;
    }

    public CipherBase(EncryptionEngine base)
    {
        _base = base;
    }

    @Override
    public void init() throws EncryptionEngineException
    {
        clearHMAC();
        try
        {
            _hmac = new HMACSHA1(_keyPart);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new EncryptionEngineException("Failed initializing cipher", e);
        }
        _base.init();
    }

    @Override
    public void decrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        _base.decrypt(data, offset, len);
    }

    @Override
    public void encrypt(byte[] data, int offset, int len) throws EncryptionEngineException
    {
        _base.encrypt(data, offset, len);
    }

    @Override
    public void setIV(byte[] iv)
    {
        byte[] buf = Arrays.copyOf(_ivPart, _ivPart.length + 8);
        for(int i=0;i<8;i++)
            buf[_ivPart.length + i] = iv[7 - i];
        byte[] hmac = new byte[_hmac.getDigestLength()];
        try
        {
            _hmac.calcHMAC(buf, 0, buf.length, hmac);
            _base.setIV(Arrays.copyOfRange(hmac, 0, _base.getIVSize()));
        }
        catch (Exception e)
        {
            Logger.log(e);
        }
        finally
        {
            Arrays.fill(buf, (byte)0);
        }
    }

    @Override
    public byte[] getIV()
    {
        return getIVFromBuf();
    }

    @Override
    public int getIVSize()
    {
        return _base.getIVSize();
    }

    @Override
    public void setKey(byte[] key)
    {
        clearKey();
        if(key!=null)
        {
            _key = Arrays.copyOf(key, getKeySize());
            _keyPart = getKeyFromBuf();
            _ivPart = getIVFromBuf();
            _base.setKey(_keyPart);
        }
    }

    @Override
    public byte[] getKey()
    {
        return _key;
    }

    @Override
    public int getKeySize()
    {
        return _base.getKeySize() + getIVSize();
    }

    @Override
    public void close()
    {
        clearAll();
        _base.close();
    }

    @Override
    public String getCipherName()
    {
        return _base.getCipherName();
    }

    @Override
    public String getCipherModeName()
    {
        return _base.getCipherModeName();
    }

    protected EncryptionEngine getBase()
    {
        return _base;
    }

    private final EncryptionEngine _base;
    private byte[] _key, _keyPart, _ivPart;
    private HMAC _hmac;

    private void clearAll()
    {
        clearKey();
        clearHMAC();
    }

    private void clearHMAC()
    {
        if(_hmac!=null)
        {
            _hmac.close();
            _hmac = null;
        }
    }

    private void clearKey()
    {
        if(_key!=null)
        {
            Arrays.fill(_key, (byte) 0);
            Arrays.fill(_ivPart, (byte)0);
            Arrays.fill(_keyPart, (byte)0);
            _key = _ivPart = _keyPart = null;
        }
    }

    private byte[] getIVFromBuf()
    {
        return getIVFromBuf(_key, _base.getKeySize());
    }

    private byte[] getKeyFromBuf()
    {
        return getKeyFromBuf(_key, _base.getKeySize());
    }

}
