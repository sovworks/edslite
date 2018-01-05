package com.sovworks.eds.truecrypt;


import com.sovworks.eds.android.Logger;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.container.VolumeLayoutBase;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.engines.AESXTS;
import com.sovworks.eds.crypto.hash.RIPEMD160;
import com.sovworks.eds.crypto.hash.Whirlpool;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.exceptions.HeaderCRCException;
import com.sovworks.eds.exceptions.WrongContainerVersionException;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

public class StdLayout extends VolumeLayoutBase
{
	public static final int HEADER_SIZE = 64*1024;
	
	public StdLayout()
	{
		_encryptedAreaStart = 2*HEADER_SIZE;
	}
	
	@Override
	public void initNew()
	{
		super.initNew();
		if(_hashFunc == null) try
		{
			_hashFunc = MessageDigest.getInstance("SHA-512");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("SHA-512 is not available", e);
		}
		if(_encEngine == null)
			setEngine(new AESXTS());
	}

	protected long getBackupHeaderOffset()
	{
		return _inputSize - 2*HEADER_SIZE;
	}
	
	@Override
	public void writeHeader(RandomAccessIO output) throws IOException, ApplicationException
	{
		checkWriteHeaderPrereqs();		
		byte[] headerData = encodeHeader();
		encryptAndWriteHeaderData(output, headerData);
		prepareEncryptionEngineForPayload();
	}

	@Override
	public boolean readHeader(RandomAccessIO input) throws IOException, ApplicationException
	{   	
		checkReadHeaderPrereqs();
		_inputSize = input.length();
		input.seek(getHeaderOffset());				
		int hs = getEffectiveHeaderSize();
		byte[] encryptedHeader = new byte[hs + getEncryptedHeaderPartOffset()];
		if(Util.readBytes(input,encryptedHeader,hs) != hs)
			return false;
		if(isUnsupportedHeaderType(encryptedHeader))
			return false;
		byte[] salt = getSaltFromHeader(encryptedHeader);
		if(selectAlgosAndDecodeHeader(encryptedHeader, salt))
		{
			prepareEncryptionEngineForPayload();
			return true;
		}
		return false;
	}	
	
	public int getHeaderSize()
	{
		return HEADER_SIZE;
	}
	
	@Override
	public long getEncryptedDataOffset()
	{
		return _encryptedAreaStart;
	}
		
	@Override
	public long getEncryptedDataSize(long fileSize)
	{
		return _volumeSize;
	}
	
	@Override
	public List<FileEncryptionEngine> getSupportedEncryptionEngines()
    {
    	return EncryptionEnginesRegistry.getSupportedEncryptionEngines();
    }
	
	@Override
    public List<MessageDigest> getSupportedHashFuncs()
    {
    	ArrayList<MessageDigest> l = new ArrayList<>();
		try
		{
			l.add(MessageDigest.getInstance("SHA-512"));
		}
		catch (NoSuchAlgorithmException ignored)
		{
		}
		l.add(new RIPEMD160());
		l.add(new Whirlpool());
		return l;
    }
	
	public void setContainerSize(long containerSize)
	{
		_inputSize = containerSize;
		_volumeSize = calcVolumeSize(containerSize);
	}
	
	protected static class KeyHolder
	{
		public byte[] getKey()
		{
			return _key;
		}
		
		public void setKey(byte[] key)
		{
			if(key!=null)
				close();
			_key = key;
		}
		
		public void close()
		{
			if(_key!=null)
				Arrays.fill(_key, (byte)0);
		}
		
		private byte[] _key;
	}

	protected static final int RESERVED_HEADER_SIZE = 4*HEADER_SIZE; //header + hidden volume header + backup header + backup hidden volume header
	protected static final short MIN_ALLOWED_HEADER_VERSION = 3;
	protected static final short CURRENT_HEADER_VERSION = 5;
	protected static final short HEADER_CRC_OFFSET = 252;
	protected static final short DATA_KEY_AREA_MAX_SIZE = 256;
	protected static final short DATA_AREA_KEY_OFFSET = 256;
	protected static final int SALT_SIZE = 64;	
	protected static final int VOLUME_SIZE_OFFSET = 116;

	protected static final byte[] TC_SIG = {'T','R','U','E'};
	
	protected long _encryptedAreaStart, _volumeSize,_inputSize; 
	
	protected void prepareEncryptionEngineForPayload() throws EncryptionEngineException
	{
		_encEngine.setKey(_masterKey);
		_encEngine.init();
	}
	
	@SuppressWarnings("UnusedParameters")
	protected boolean isUnsupportedHeaderType(byte[] encryptedHeader)
	{
		return false;
	}
	
	protected byte[] getSaltFromHeader(byte[] headerData)
	{
		byte[] salt = new byte[SALT_SIZE];
		System.arraycopy(headerData, 0, salt, 0, SALT_SIZE);
		return salt;
	}
	
	protected boolean isValidSign(byte[] headerData)
	{
		byte[] sig = getHeaderSignature();
		int offset = getEncryptedHeaderPartOffset();
		for(int i=0;i<sig.length;i++)
        	if(headerData[offset + i] != sig[i])
        		return false;
		return true;
	}
	
	protected boolean selectAlgosAndDecodeHeader(byte[] encryptedHeaderData, byte[] salt) throws ApplicationException
	{
		if(_hashFunc == null)	
		{
			for(MessageDigest md: getSupportedHashFuncs())
			{
				FileEncryptionEngine ee = tryHashFunc(encryptedHeaderData, salt, md);
				if(ee != null)
				{
					setEngine(ee);
					_hashFunc = md;
					return true;
				}
			}
		}
		else
		{
			FileEncryptionEngine ee = tryHashFunc(encryptedHeaderData, salt, _hashFunc);
			if(ee != null)
			{
				setEngine(ee);
				return true;
			}
		}			
		return false;
	}
	
	protected FileEncryptionEngine tryHashFunc(byte[] encryptedHeaderData, byte[] salt, MessageDigest hashFunc) throws ApplicationException
	{
		Logger.debug(String.format("Using %s hash function to derive the key", hashFunc.getAlgorithm()));
		KeyHolder prevKey = new KeyHolder();
		try
		{
			if(_encEngine!=null)
			{
				if(tryEncryptionEngine(encryptedHeaderData, salt, hashFunc, _encEngine, prevKey))
					return _encEngine;				
			}
			else
			{
				for(FileEncryptionEngine ee: getSupportedEncryptionEngines())
	            { 
					if(tryEncryptionEngine(encryptedHeaderData, salt, hashFunc, ee, prevKey))
						return ee;					
	            }
			}
		}
		finally
		{
			prevKey.close();
		}
		return null;
		
	}
	
	protected boolean tryEncryptionEngine(byte[] encryptedHeaderData, byte[] salt, MessageDigest hashFunc, EncryptionEngine ee, KeyHolder prevKey) throws ApplicationException
	{
		Logger.debug(String.format("Trying to decrypt the header using %s encryption engine", VolumeLayoutBase.getEncEngineName(ee)));
		if(_openingProgressReporter!=null)
		{
			_openingProgressReporter.setCurrentKDFName(hashFunc.getAlgorithm());
			_openingProgressReporter.setCurrentEncryptionAlgName(ee.getCipherName());
		}
		byte[] key = prevKey.getKey();
		if(key == null || key.length < ee.getKeySize())
		{
    		key = deriveHeaderKey(ee, hashFunc, salt);
    		prevKey.setKey(key);
		}	
    	if(decryptAndDecodeHeader(encryptedHeaderData, ee, key))
    		return true;    	
    	else
    		ee.close();
    	return false;
	}
    
    protected boolean decryptAndDecodeHeader(byte[] encryptedHeader, EncryptionEngine ee, byte[] key) throws ApplicationException
	{
    	byte[] decryptedHeader = null;
    	try
    	{    
    		decryptedHeader = decryptHeader(encryptedHeader, ee, key);
    		if(decryptedHeader == null)
    			return false;    		
    		
    		if(_masterKey!=null)		
    			Arrays.fill(_masterKey, (byte)0);	
    		_masterKey = new byte[ee.getKeySize()];
			decodeHeader(decryptedHeader);			
			return true;
    	}
    	finally
    	{
    		if(decryptedHeader!=null)
    			Arrays.fill(decryptedHeader, (byte)0);    		
    	}
    	
	}
    
    protected int getEncryptedHeaderPartOffset()
    {
    	return SALT_SIZE;
    }
    
    protected int getMKKDFNumIterations(MessageDigest hashFunc)
    {
    	String an = hashFunc.getAlgorithm();
    	if("ripemd160".equalsIgnoreCase(an))
    		return 2000;
    	return 1000;
    }

	protected byte[] decryptHeader(byte[] encryptedData, EncryptionEngine ee, byte[] key) throws EncryptionEngineException
	{		
		ee.setIV(new byte[ee.getIVSize()]);
    	ee.setKey(key);
    	ee.init();
        byte[] header = encryptedData.clone();
        int ofs = getEncryptedHeaderPartOffset();
        try
        {
             ee.decrypt(header, ofs, header.length - ofs);
        }
        catch (EncryptionEngineException e)
        {
            return null;
        }
        return isValidSign(header) ? header : null;
    }
	
	protected void encryptAndWriteHeaderData(RandomAccessIO output, byte[] headerData) throws ApplicationException, IOException
	{
		byte[] salt = getSaltFromHeader(headerData);	
		byte[] key = deriveHeaderKey(_encEngine, _hashFunc, salt);
		encryptHeader(headerData, key);
		Arrays.fill(key, (byte)0);
		writeHeaderData(output, headerData);
	}

	protected void writeHeaderData(RandomAccessIO output, byte[] encryptedHeaderData) throws ApplicationException, IOException
	{
		writeHeaderData(output, encryptedHeaderData, getHeaderOffset());
		writeHeaderData(output, encryptedHeaderData, getBackupHeaderOffset());
	}

	protected void writeHeaderData(RandomAccessIO output, byte[] encryptedHeaderData, long offset) throws ApplicationException, IOException
	{
		output.seek(offset);
		output.write(encryptedHeaderData,0,encryptedHeaderData.length - getEncryptedHeaderPartOffset());
	}
	
	protected byte[] deriveHeaderKey(EncryptionEngine ee, MessageDigest md, byte[] salt) throws ApplicationException
	{
		int keySize = ee.getKeySize();
		if(_encEngine == null)
		{
			for(EncryptionEngine eng: getSupportedEncryptionEngines())
				if(eng.getKeySize() > keySize)
					keySize = eng.getKeySize();
		}
		return deriveKey(keySize, md, _password, salt, getMKKDFNumIterations(md));
	}
	
	protected void encryptHeader(byte[] headerData, byte[] key) throws ApplicationException
	{
		_encEngine.setKey(key);
		_encEngine.init();
		_encEngine.setIV(new byte[_encEngine.getIVSize()]);
		int encOffs = getEncryptedHeaderPartOffset();
		_encEngine.encrypt(headerData, encOffs , headerData.length - encOffs);
	}
	
	protected byte[] getHeaderSignature()
	{
		return TC_SIG;
	}
	
	protected short getMinCompatibleProgramVersion()
	{
		return EdsContainer.COMPATIBLE_TC_VERSION;
	}
	
	protected byte[] encodeHeader() throws EncryptionEngineException
	{
		int encPartOffset = getEncryptedHeaderPartOffset();
		ByteBuffer bb = ByteBuffer.allocate(getEffectiveHeaderSize() + encPartOffset);
		bb.order(ByteOrder.BIG_ENDIAN);
		byte[] salt = new byte[SALT_SIZE];
		getRandom().nextBytes(salt);
		bb.put(salt);
		bb.put(getHeaderSignature());
		bb.putShort(CURRENT_HEADER_VERSION);
		bb.putShort(getMinCompatibleProgramVersion());
		
		byte[] mk = new byte[DATA_KEY_AREA_MAX_SIZE];
		System.arraycopy(_masterKey, 0, mk, 0, _masterKey.length);
		CRC32 crc = new CRC32();
		crc.update(mk);
		bb.putInt((int)crc.getValue());
		
		bb.position(bb.position() + 16);
		bb.putLong(calcHiddenVolumeSize(_volumeSize));
		bb.putLong(_volumeSize);
		bb.putLong(_encryptedAreaStart);
		bb.putLong(_volumeSize);
		//write flags
		bb.putInt(0);
		bb.putInt(SECTOR_SIZE);
		
		crc.reset();
		crc.update(bb.array(),encPartOffset,HEADER_CRC_OFFSET - encPartOffset);
		bb.position(HEADER_CRC_OFFSET);
		bb.putInt((int)crc.getValue());
		bb.position(DATA_AREA_KEY_OFFSET);
		bb.put(mk);
		Arrays.fill(mk, (byte)0);
		
		return bb.array();
	}
	
	protected long calcHiddenVolumeSize(long volumeSize)
	{
		return 0;
	}
	
	protected long calcVolumeSize(long containerSize)
	{		
		long numSectors = containerSize/SECTOR_SIZE;
		return (containerSize % SECTOR_SIZE == 0 ? numSectors : numSectors + 1)*SECTOR_SIZE - RESERVED_HEADER_SIZE;
	}
	
	protected long loadVolumeSize(ByteBuffer headerData)
	{
		long vs = headerData.getLong(VOLUME_SIZE_OFFSET);		
		return vs == 0 ? _inputSize - _encryptedAreaStart : vs;
	}

	protected void decodeHeader(byte[] data) throws ApplicationException
	{
		int encPartOffset = getEncryptedHeaderPartOffset();
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.position(encPartOffset + getHeaderSignature().length);
		//offset 68
		short headerVersion = bb.getShort();		
		if(headerVersion < MIN_ALLOWED_HEADER_VERSION || headerVersion > CURRENT_HEADER_VERSION)
			throw new WrongContainerVersionException();
		
		CRC32 crc = new CRC32();
		crc.update(data,encPartOffset,HEADER_CRC_OFFSET - encPartOffset);
		if((int)crc.getValue() != bb.getInt(HEADER_CRC_OFFSET))
			throw new HeaderCRCException();

		//offset 70
		int programVer = bb.getShort();
		if(programVer > EdsContainer.COMPATIBLE_TC_VERSION)
			throw new WrongContainerVersionException();
		
		//offset 72
		int volumeKeyAreaCRC32 = bb.getInt();
		//offset+=4;
		//skip volume creation time
		//offset+=8;
		//skip header creation time
		//offset+=8;
		//offset 92
		//hidden volume size
		//Util.bytesToLong(data,offset);
		//if(size!=0)
		//	throw new UnsupportedContainerTypeException();
		//offset+=8;
		//offset 100
		//Volume data size
		//Util.bytesToLong(data,offset);
		//offset+=8;
		//offset 108		
		_encryptedAreaStart = bb.getLong(108);
		//offset+=8;
		//offset 116
		//Encrypted area length
		//Util.bytesToLong(data,offset);
		//offset+=8;
		//offset 124
		//offset = 124;
		//Flags
		//Util.unsignedIntToLong(data,offset);
		//offset+=4;
		//Sector size
		//Util.unsignedIntToLong(data,offset);
		//_sectorSize = 512;
		_volumeSize = loadVolumeSize(bb);
		crc.reset();
		crc.update(bb.array(),DATA_AREA_KEY_OFFSET,DATA_KEY_AREA_MAX_SIZE);
		if((int)crc.getValue() != volumeKeyAreaCRC32)
			throw new HeaderCRCException();
		bb.position(DATA_AREA_KEY_OFFSET);	
		bb.get(_masterKey);		
	}

	protected long getHeaderOffset()
	{
		return 0;
	}
	
	protected int getEffectiveHeaderSize()
	{
		return 512;
	}
	
}
