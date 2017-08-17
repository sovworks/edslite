package com.sovworks.eds.crypto.kdf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

import com.sovworks.eds.android.helpers.ProgressReporter;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.hash.RIPEMD160;
import com.sovworks.eds.crypto.hash.Whirlpool;

public abstract class PBKDF
{	
	public static Iterable<PBKDF> getAvailablePBKDFS()
	{
		return Arrays.asList(new HMACSHA512KDF(),new HMACRIPEMD160KDF(),new HMACWhirlpoolKDF());		
	}
	
	public byte[] deriveKey(byte[] srcKey,byte[] salt,int keyLen)throws EncryptionEngineException, DigestException
	{
		return deriveKey(srcKey,salt,getDefaultIterationsCount(),keyLen);
	}
	
	public byte[] deriveKey(byte[] srcKey,byte[] salt,int iterations,int keyLen)throws EncryptionEngineException, DigestException
    {
		HMAC hmac = initHMAC(srcKey);
		try
		{
			int digestLength = hmac.getDigestLength();
	        byte[] u = new byte[digestLength];
	        byte[] res = new byte[keyLen];
	        int l = keyLen % digestLength != 0 ? 1 + keyLen/digestLength : keyLen/digestLength;
	        int r = keyLen - (l - 1)*digestLength;
	        int b;
			_finishedIterations = 0;
			_totalIterations = iterations*l;
	        for(b=1;b<l;b++)
	        {
	            deriveKey(hmac, srcKey,salt,iterations,u,b);
	            System.arraycopy(u,0,res,(b-1)*digestLength,digestLength);
	        }
	
	        deriveKey(hmac, srcKey,salt,iterations,u,b);
	        System.arraycopy(u,0,res,(b-1)*digestLength,r);
		    Arrays.fill(u,(byte)0);
	        return res;
		}
		finally
		{
			hmac.close();
		}   
    }

	public void setProgressReporter(ProgressReporter r)
	{
		_progressReporter = r;
	}

	protected ProgressReporter _progressReporter;
	
	private static final int COUNTER_LENGTH = 4;
	private int _finishedIterations, _totalIterations;
	
	protected void calcHMAC(HMAC hmac, byte[] key, byte[] message, byte[] result) throws DigestException, EncryptionEngineException
	{
		hmac.calcHMAC(message, 0, message.length, result);
	}

    protected void deriveKey(HMAC hmac, byte[] key,byte[] salt,int iterations,byte[] u,int block) throws DigestException, EncryptionEngineException
    {
    	int digestLength = hmac.getDigestLength();
    	ByteBuffer bb = ByteBuffer.allocate(COUNTER_LENGTH);
    	bb.order(ByteOrder.BIG_ENDIAN);
    	bb.putInt(block);
        
        byte[] init = new byte[salt.length + COUNTER_LENGTH];
        System.arraycopy(salt,0,init,0,salt.length);
        System.arraycopy(bb.array(),0,init,salt.length,COUNTER_LENGTH);

        byte[] j = new byte[digestLength];   
        calcHMAC(hmac, key, init, j);
        System.arraycopy(j,0,u,0,digestLength);

		int prevPrc = -1;
        byte[] k = new byte[digestLength];
        for(int c = 1;c<iterations;c++)
        {
        	calcHMAC(hmac, key, j, k);
            for(int i=0;i<digestLength;i++)
            {
                u[i] ^= k[i];
                j[i] = k[i];
            }
			if(_progressReporter!=null)
			{
				int prc = (int) (((float)_finishedIterations++*100)/_totalIterations);
				if(prc!=prevPrc)
				{
					prevPrc = prc;
					_progressReporter.setProgress(prc);
				}
				if(_progressReporter.isCancelled())
					throw new CancellationException();
			}
        }
	    Arrays.fill(j,(byte)0);
	    Arrays.fill(k,(byte)0);
    }
    
    protected abstract HMAC initHMAC(byte[] srcKey) throws EncryptionEngineException;	
    
    protected int getDefaultIterationsCount()
    {
    	return 1000;
    }
}

class HMACSHA512 extends HMAC
{
    public HMACSHA512(byte[] key) throws NoSuchAlgorithmException
    {
    	super(key,MessageDigest.getInstance("SHA-512"),SHA512_BLOCK_SIZE);        
    }
    
    private static final int SHA512_BLOCK_SIZE = 128;
}

class HMACRIPEMD160 extends HMAC
{
    public HMACRIPEMD160(byte[] key) throws NoSuchAlgorithmException
    {
    	super(key,new RIPEMD160(),RIPEMD160_BLOCK_SIZE);        
    }
    
    private static final int RIPEMD160_BLOCK_SIZE = 64;
}

class HMACWhirlpool extends HMAC
{
    public HMACWhirlpool(byte[] key) throws NoSuchAlgorithmException
    {
    	super(key,new Whirlpool(),WHIRLPOOL_BLOCK_SIZE);        
    }
    
    private static final int WHIRLPOOL_BLOCK_SIZE = 64;
}