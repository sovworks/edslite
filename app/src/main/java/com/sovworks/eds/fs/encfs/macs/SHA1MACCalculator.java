package com.sovworks.eds.fs.encfs.macs;

import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.kdf.HMACSHA1;
import com.sovworks.eds.fs.encfs.ciphers.CipherBase;

import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SHA1MACCalculator extends MACCalculator
{
    public SHA1MACCalculator(int keySize)
    {
        _keySize = keySize;
    }

    @Override
    public void init(byte[] key)
    {
        byte[] k = CipherBase.getKeyFromBuf(key, _keySize);
        try
        {
            _hmac = new HMACSHA1(k);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            Arrays.fill(k, (byte)0);
        }
    }

    @Override
    public void close()
    {
        _hmac.close();
    }

    @Override
    public byte[] calcChecksum(byte[] buf, int offset, int count)
    {
        byte[] data;
        if(isChainedIVEnabled())
        {
            byte[] iv = getChainedIV();
            data = new byte[count + 8];
            System.arraycopy(buf, offset, data, 0, count);
            for(int i=0;i<8;i++)
                data[count + i] = iv[7-i];
        }
        else
            data = Arrays.copyOfRange(buf, offset, offset + count);
        try
        {
            byte[] mac = new byte[_hmac.getDigestLength()];
            _hmac.calcHMAC(data, 0, data.length, mac);
            byte[] cut = new byte[8];
            for(int i=0; i<mac.length - 1;i++)
                cut[i % cut.length] ^= mac[i];
            if(isChainedIVEnabled())
                setChainedIV(cut.clone());
            return cut;
        }
        catch (DigestException | EncryptionEngineException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            Arrays.fill(data, (byte)0);
        }
    }

    private final int _keySize;
    private HMACSHA1 _hmac;
}
