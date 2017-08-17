package com.sovworks.eds.container;


import android.annotation.SuppressLint;

import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.kdf.HashBasedPBKDF2;
import com.sovworks.eds.crypto.modes.CBC;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

public abstract class VolumeLayoutBase implements VolumeLayout
{
	
	public static EncryptionEngine findCipher(Iterable<? extends EncryptionEngine> engines, String cipherName, String modeName)
	{
		for(EncryptionEngine eng: engines)
			if(cipherName.equalsIgnoreCase(eng.getCipherName()) && modeName.equalsIgnoreCase(eng.getCipherModeName()))
				return eng;
		return null;		
	}
	
	public static MessageDigest findHashFunc(Iterable<MessageDigest> engines, String name)
	{
		name = name.toLowerCase();
		for(MessageDigest eng: engines)
		{
			String algName = eng.getAlgorithm().toLowerCase();
			if (algName.contains(name))
				return eng;
		}
		return null;		
	}
	
	public static String getEncEngineName(EncryptionEngine ee)
	{
		return String.format("%s-%s", ee.getCipherName(), ee.getCipherModeName());
	}
	
	public static EncryptionEngine findEncEngineByName(Iterable<? extends EncryptionEngine> engines, String name)
	{
		for(EncryptionEngine eng: engines)
			if(getEncEngineName(eng).equalsIgnoreCase(name))
				return eng;
		return null;		
	}
	
	@Override
	public void initNew()
	{
		if(_encEngine == null)
			throw new IllegalStateException("Encryption engine is not set");
		if(_masterKey!=null)		
			Arrays.fill(_masterKey, (byte)0);	
		_masterKey = new byte[_encEngine.getKeySize()];
		getRandom().nextBytes(_masterKey);
	}

	@Override
	public boolean readHeader(RandomAccessIO input) throws IOException, ApplicationException
	{   	
		if(_password == null)
			throw new IllegalStateException("Password is not set");
		return false;
	}
	
	
	@Override
	public void formatFS(RandomAccessIO output, FileSystemInfo fsInfo) throws ApplicationException, IOException
	{
		fsInfo.makeNewFileSystem(output);
	}
	
	
	@Override
	public void setHashFunc(MessageDigest hf)
	{
		_hashFunc = hf;		
	}

	@Override
	public void setPassword(byte[] password)
	{
		if(_password!=null)
			Arrays.fill(_password, (byte)0);
		_password = password;
	}

	@Override
	public void setNumKDFIterations(int num)
	{

	}

	@Override
    public FileEncryptionEngine getEngine()
    {
        return _encEngine;
    }
	
	@Override
	public MessageDigest getHashFunc()
	{
		return _hashFunc;
	}
    
	@Override
    public void setEngine(FileEncryptionEngine engine)
    {
		if(_encEngine!=null) 	
			_encEngine.close();		
        _encEngine = engine;
		_invertIV = _encEngine!=null && CBC.NAME.equalsIgnoreCase(_encEngine.getCipherModeName());
    }
	
	@Override
	public void close() throws IOException
	{
		if(_masterKey!=null)
		{
			Arrays.fill(_masterKey, (byte)0);
			_masterKey = null;
		}
		if(_password!=null)
		{
			Arrays.fill(_password, (byte)0);
			_password = null;
		}
		setEngine(null);
	}
	
	@Override
	public void setEncryptionEngineIV(FileEncryptionEngine eng, long decryptedVolumeOffset)
	{
		long block = (decryptedVolumeOffset + getEncryptedDataOffset()) / eng.getFileBlockSize();
		eng.setIV(getIVFromBlockIndex(block));
	}
	
	@Override
	public List<FileEncryptionEngine> getSupportedEncryptionEngines()
    {
    	return Collections.emptyList();
    }
    
	@Override
    public List<MessageDigest> getSupportedHashFuncs()
    {
    	return Collections.emptyList();
    }

	@Override
	public void setOpeningProgressReporter(ContainerOpeningProgressReporter reporter)
	{
		_openingProgressReporter = reporter;
	}

	public FileEncryptionEngine findCipher(String cipherName, String modeName)
	{
		return (FileEncryptionEngine) findCipher(getSupportedEncryptionEngines(), cipherName, modeName);
	}
	
	public MessageDigest findHashFunc(String name)
	{
		return findHashFunc(getSupportedHashFuncs(), name);		
	}
		
	protected static final int SECTOR_SIZE = 512;
	
    protected FileEncryptionEngine _encEngine;
    protected MessageDigest _hashFunc;
    protected byte[] _masterKey;
    protected byte[] _password;
	protected ContainerOpeningProgressReporter _openingProgressReporter;

	@SuppressLint("TrulyRandom")
	protected synchronized Random getRandom()
	{
		if(_sr == null)
			_sr = new SecureRandom();
		return _sr;
	}

	private Random _sr;
	private boolean _invertIV;
    
    protected byte[] deriveKey(int keySize, MessageDigest hashFunc, byte[] password, byte[] salt, int numIterations) throws ApplicationException
    {
    	HashBasedPBKDF2 kdf = new HashBasedPBKDF2(hashFunc);
		kdf.setProgressReporter(_openingProgressReporter);
    	try
		{
			return kdf.deriveKey(password,salt, numIterations,keySize);
		}
		catch (CancellationException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ApplicationException("Failed deriving key", e);
		}
    }	
    
    protected void checkWriteHeaderPrereqs()
    {
    	if(_encEngine == null || _hashFunc == null || _password == null || _masterKey == null)
			throw new IllegalStateException("Header data is not initialized");
    }
    
    protected void checkReadHeaderPrereqs()
    {
    	if(_password == null)
			throw new IllegalStateException("The password is not set");
    }

	protected byte[] getIVFromBlockIndex(long blockIndex)
	{
		return ByteBuffer.
				allocate(_encEngine.getIVSize()).
				order(_invertIV ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).
				putLong(blockIndex).
				array();
	}
}
