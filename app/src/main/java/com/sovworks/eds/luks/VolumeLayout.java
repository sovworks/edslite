package com.sovworks.eds.luks;

import android.annotation.SuppressLint;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.container.VolumeLayoutBase;
import com.sovworks.eds.crypto.AF;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.engines.AESCBC;
import com.sovworks.eds.crypto.engines.AESXTS;
import com.sovworks.eds.crypto.engines.GOSTCBC;
import com.sovworks.eds.crypto.engines.GOSTXTS;
import com.sovworks.eds.crypto.engines.SerpentCBC;
import com.sovworks.eds.crypto.engines.SerpentXTS;
import com.sovworks.eds.crypto.engines.TwofishCBC;
import com.sovworks.eds.crypto.engines.TwofishXTS;
import com.sovworks.eds.crypto.hash.RIPEMD160;
import com.sovworks.eds.crypto.hash.Whirlpool;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.exceptions.UnsupportedContainerTypeException;
import com.sovworks.eds.exceptions.WrongPasswordException;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.Util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class VolumeLayout extends VolumeLayoutBase
{
	@Override
	public void initNew()
	{
		if(_encEngine == null)
			setEngine(new AESXTS());
		super.initNew();				
		if (_hashFunc == null) try
		{
			_hashFunc = MessageDigest.getInstance("SHA1");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("Failed getting sha1 instance");
		}		
		_activeKeyslotIndex = 0;
		if (_uuid == null) _uuid = UUID.randomUUID();
		_keySlots.clear();
		for(int i=0;i<NUM_KEY_SLOTS;i++)
		{
			KeySlot ks = new KeySlot();
			ks.init(i);
			_keySlots.add(ks);
		}
		
		if(_payloadOffsetSector == 0 && !_isDetachedHeader)
		{
			KeySlot ks = new KeySlot();
			ks.init(NUM_KEY_SLOTS);
			_payloadOffsetSector = sizeRoundUp(ks.keyMaterialOffsetSector, DEFAULT_DISK_ALIGNMENT / SECTOR_SIZE);
		}		
	}

	@Override
	public boolean readHeader(RandomAccessIO input) throws IOException,
			ApplicationException
	{
		checkReadHeaderPrereqs();
		
		byte[] header = new byte[HEADER_SIZE];
		input.seek(getHeaderOffset());
		if(Util.readBytes(input,header,HEADER_SIZE) != HEADER_SIZE)
			return false;
		for (int i = 0; i < MAGIC.length; i++)
			if (header[i] != MAGIC[i]) return false;

		MKInfo mki = deserializeHeaderData(header);
		int actSlot = 0;
		for(int i=0;i<_keySlots.size();i++)
		{
			KeySlot ks = _keySlots.get(i);
			if(ks.isActive)
			{
				if(_openingProgressReporter!=null)
					((ProgressReporter)_openingProgressReporter).setCurrentSlot(actSlot++);
				if(tryPassword(input, ks, mki, _password))
				{
					_activeKeyslotIndex = i;
					_volumeSize = calcVolumeSize(input.length());
					return true;
				}
			}			
		}
		throw new WrongPasswordException();
	}

	@Override
	public void writeHeader(RandomAccessIO output) throws IOException,
			ApplicationException
	{
		checkWriteHeaderPrereqs();
		for(KeySlot ks: _keySlots)
			ks.isActive = false;
		writeKey(output, _keySlots.get(_activeKeyslotIndex), _password);
		writeHeaderData(output);
	}

	@Override
    public List<MessageDigest> getSupportedHashFuncs()
    {
    	ArrayList<MessageDigest> l = new ArrayList<>();
    	try
		{
			l.add(MessageDigest.getInstance("SHA1"));
		}
		catch (NoSuchAlgorithmException ignored)
		{
		}
		try
		{
			l.add(MessageDigest.getInstance("SHA-512"));
		}
		catch (NoSuchAlgorithmException ignored)
		{
		}
		try
		{
			l.add(MessageDigest.getInstance("SHA-256"));
		}
		catch (NoSuchAlgorithmException ignored)
		{
		}
		l.add(new RIPEMD160());
		l.add(new Whirlpool());
		return l;
    }
	
	@Override
	public List<FileEncryptionEngine> getSupportedEncryptionEngines()
    {
    	return Arrays.asList(
				new AESXTS(),
				new SerpentXTS(),
				new TwofishXTS(),
				new GOSTXTS(),
				new AESCBC(),
				new SerpentCBC(),
				new TwofishCBC(),
				new GOSTCBC()
		);
    }
	
	@Override
	public void setEncryptionEngineIV(FileEncryptionEngine eng, long decryptedVolumeOffset)
	{
		long block = decryptedVolumeOffset / eng.getFileBlockSize();
		eng.setIV(getIVFromBlockIndex(block));
	}

	public void writeKey(RandomAccessIO output, int keyIndex, byte[] password)
			throws IOException, ApplicationException
	{
		checkWriteHeaderPrereqs();
		writeKey(output, _keySlots.get(keyIndex), password);
	}

	@Override
	public long getEncryptedDataOffset()
	{
		return _payloadOffsetSector*SECTOR_SIZE;
	}

	@Override
	public long getEncryptedDataSize(long fileSize)
	{
		return _volumeSize;
	}
	
	public void setContainerSize(long containerSize)
	{
		_volumeSize = calcVolumeSize(containerSize);
	}
	
	public void setActiveKeyslot(int keyslotIndex)
	{
		_activeKeyslotIndex = keyslotIndex;
	}
	
	public FileEncryptionEngine findCipher(String cipherName, String modeName, int keySize)
	{
		if(cipherName.equalsIgnoreCase("aes") && modeName.equalsIgnoreCase("xts-plain64") && keySize == 32)
			return new AESXTS(keySize);
				
		return findCipher(cipherName, modeName);
	}
	
	@Override
	public MessageDigest findHashFunc(String name)
	{
		if(name.equalsIgnoreCase("sha512"))
			name = "SHA-512";
		else if(name.equalsIgnoreCase("sha256"))
			name = "SHA-256";
		return super.findHashFunc(name);		
	}

	@Override
	public void setOpeningProgressReporter(final ContainerOpeningProgressReporter reporter)
	{
		if(reporter!=null)
			_openingProgressReporter = new ProgressReporter(reporter);
		else
			super.setOpeningProgressReporter(reporter);
	}

	protected class KeySlot
	{		
		public void init(int slotIndex)
		{
			isActive = false;
			passwordIterations = SLOT_ITERATIONS_MIN;
			salt = new byte[MK_SALT_SIZE];
			getRandom().nextBytes(salt);
			numStripes = NUM_AF_STRIPES;
			AF af = new AF(_hashFunc, _masterKey.length);			
			int blocksPerStripeSet = af.calcNumRequiredSectors(numStripes);
			int sector = KEY_MATERIAL_OFFSET / SECTOR_SIZE;
			for(int i=0; i< slotIndex;i++)			
				sector = sizeRoundUp(sector + blocksPerStripeSet, KEY_MATERIAL_OFFSET / SECTOR_SIZE);
			keyMaterialOffsetSector = sector;			
		}
		
		public void serialize(ByteBuffer bb)
		{
			bb.putInt(isActive ? KEY_ENABLED_SIG : KEY_DISABLED_SIG);
			bb.putInt(passwordIterations);
			bb.put(salt);
			bb.putInt(keyMaterialOffsetSector);
			bb.putInt(numStripes);
		}
		
		public void deserialize(ByteBuffer bb)
		{
			int act = bb.getInt();
			isActive = act == KEY_ENABLED_SIG;
			passwordIterations = bb.getInt();
			salt = new byte[MK_SALT_SIZE];
			bb.get(salt);
			keyMaterialOffsetSector = bb.getInt();
			numStripes = bb.getInt();
		}

		boolean isActive;
		int passwordIterations;
		byte[] salt;
		int keyMaterialOffsetSector;
		int numStripes;
		
	}
	
	protected class MKInfo
	{		
		public void init() throws ApplicationException
		{
			iterations = MK_ITERATIONS_MIN;
			keyLength = _masterKey.length;
			salt = new byte[MK_SALT_SIZE];
			getRandom().nextBytes(salt);
			digest = deriveKey(MK_DIGEST_SIZE, _hashFunc, _masterKey, salt, iterations);
		}
		
		public boolean isValidKey(byte[] key) throws ApplicationException
		{
			byte[] keyDigest = deriveKey(MK_DIGEST_SIZE, _hashFunc, key, salt, iterations);
			return Arrays.equals(keyDigest, digest);
		}
		
		public void serialize(ByteBuffer bb)
		{
			bb.putInt(keyLength);
			bb.put(digest);
			bb.put(salt);
			bb.putInt(iterations);
		}
		
		public void deserialize(ByteBuffer bb)
		{
			keyLength = bb.getInt();
			digest = new byte[MK_DIGEST_SIZE];
			bb.get(digest);
			salt = new byte[MK_SALT_SIZE];
			bb.get(salt);
			iterations = bb.getInt();
		}
		
		int iterations;
		int keyLength;
		byte[] salt;
		byte[] digest;
	}
	
	protected static int sizeRoundUp(int size, int block)
	{
		int s = (size + (block - 1)) / block;
		return s * block;
	}

	protected class ProgressReporter implements ContainerOpeningProgressReporter
	{
		public ProgressReporter(ContainerOpeningProgressReporter base)
		{
			_base = base;
		}

		@Override
		public void setCurrentKDFName(String name)
		{
			_base.setCurrentKDFName(name);
		}

		@Override
		public void setCurrentEncryptionAlgName(String name)
		{
			_base.setCurrentEncryptionAlgName(name);
		}

		@Override
		public void setContainerFormatName(String name)
		{
			_base.setContainerFormatName(name);
		}

		@Override
		public void setIsHidden(boolean val)
		{
			_base.setIsHidden(val);
		}

		@Override
		public void setText(CharSequence text)
		{
			_base.setText(text);
		}

		@Override
		public void setProgress(int progress)
		{
			if(_numberActiveSlots > 0)
				progress = (int)(((float)_currentSlot/_numberActiveSlots + ((_ksProcessed ? 80f : 0 ) + (float)progress*(_ksProcessed ? 0.2f : 0.8f))/(100*_numberActiveSlots))*100);
			_base.setProgress(progress);

		}

		@Override
		public boolean isCancelled()
		{
			return _base.isCancelled();
		}

		void setCurrentSlot(int i)
		{
			_currentSlot = i;
			if(_currentSlot == 0)
			{
				_numberActiveSlots = 0;
				for (KeySlot ks : _keySlots)
					if (ks.isActive)
						_numberActiveSlots++;
			}
		}

		void setKSProcessed(boolean val)
		{
			_ksProcessed = val;
		}

		private final ContainerOpeningProgressReporter _base;
		private int _currentSlot, _numberActiveSlots;
		private boolean _ksProcessed;
	}

	protected UUID _uuid;
	protected int _payloadOffsetSector, _activeKeyslotIndex;
	protected boolean _isDetachedHeader;
	protected final List<KeySlot> _keySlots = new ArrayList<>();
	protected long _volumeSize; 
	
	protected long calcVolumeSize(long containerSize)
	{
		return containerSize - _payloadOffsetSector*SECTOR_SIZE;
	}
	
	protected void writeHeaderData(RandomAccessIO output) throws IOException, ApplicationException
	{
		byte[] headerData = serializeHeaderData();
		output.seek(getHeaderOffset());
		output.write(headerData, 0, headerData.length);		
	}
	
	protected void writeKey(RandomAccessIO output, KeySlot ks, byte[] password)
			throws IOException, ApplicationException
	{
		try
		{
			byte[] derivedKey = deriveKey(_encEngine.getKeySize(), _hashFunc, password, ks.salt, ks.passwordIterations);
			AF af = new AF(_hashFunc, derivedKey.length);
			int afSize = af.calcNumRequiredSectors(ks.numStripes) * AF.SECTOR_SIZE;
			byte[] afKey = new byte[afSize];
			af.split(_masterKey, 0, afKey, 0, ks.numStripes);
			_encEngine.setKey(derivedKey);
			_encEngine.init();
			_encEngine.setIV(new byte[_encEngine.getIVSize()]);
			_encEngine.encrypt(afKey, 0, afKey.length);

			output.seek(ks.keyMaterialOffsetSector * SECTOR_SIZE);
			output.write(afKey, 0, afKey.length);
			ks.isActive = true;
		}
		catch (DigestException e)
		{
			throw new ApplicationException("Key setup failed", e);
		}
		finally
		{
			_encEngine.setKey(_masterKey);
			_encEngine.init();
		}
	}

	protected byte[] serializeHeaderData() throws ApplicationException
	{
		ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put(MAGIC);
		bb.putShort((short) 1);
		bb.put(getCipherName());
		bb.put(getCipherModeName());
		bb.put(getHashspecName());
		bb.putInt(_payloadOffsetSector);
		
		MKInfo mki = new MKInfo();
		mki.init();
		mki.serialize(bb);
		
		bb.put(getUUIDBytes());
		for (KeySlot ks : _keySlots)
			ks.serialize(bb);
		return bb.array();
	}

	protected MKInfo deserializeHeaderData(byte[] headerData) throws ApplicationException
	{
		ByteBuffer bb = ByteBuffer.wrap(headerData);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.position(MAGIC.length);
		short ver = bb.getShort();
		if(ver > 1)
			throw new UnsupportedContainerTypeException("Unsupported container format version " + ver);
		byte[] buf = new byte[MAX_CIPHERNAME_LEN];
		bb.get(buf);
		String cipherName = new String(buf).trim();
		buf = new byte[MAX_CIPHERMODENAME_LEN];
		bb.get(buf);
		String modeName = new String(buf).trim();
		
		buf = new byte[MAX_HASHSPEC_LEN];
		bb.get(buf);
		String hfName = new String(buf).trim();
		
		_hashFunc = findHashFunc(hfName);
		if(_hashFunc == null)
			throw new ApplicationException(String.format("Unsupported hash algorithm: %s", hfName));
		_payloadOffsetSector = bb.getInt();
		
		MKInfo mki = new MKInfo();
		mki.deserialize(bb);
		
		setEngine(findCipher(cipherName, modeName, mki.keyLength));
		if(_encEngine == null)
			throw new ApplicationException(String.format("Unsupported cipher/mode: %s-%s", cipherName, modeName));
		
		byte[] uuidBytes = new byte[UUID_LENGTH];
		bb.get(uuidBytes);
		String uuidStr = new String(uuidBytes).trim();
		_uuid = UUID.fromString(uuidStr);
		
		_keySlots.clear();
		for(int i=0;i<NUM_KEY_SLOTS;i++)
		{
			KeySlot ks = new KeySlot();
			ks.deserialize(bb);
			_keySlots.add(ks);
		}		
		return mki;
	}
	
	protected boolean tryPassword(RandomAccessIO io, KeySlot ks, MKInfo mki, byte[] password) throws IOException, ApplicationException
	{
		io.seek(ks.keyMaterialOffsetSector * SECTOR_SIZE);
		AF af = new AF(_hashFunc, mki.keyLength);
		int afSize = af.calcNumRequiredSectors(ks.numStripes) * SECTOR_SIZE;
		byte[] afKey = new byte[afSize];
		if(Util.readBytes(io,afKey,afKey.length) != afKey.length)
			throw new EOFException();

		if(_openingProgressReporter!=null)
		{
			_openingProgressReporter.setCurrentKDFName(_hashFunc.getAlgorithm());
			_openingProgressReporter.setCurrentEncryptionAlgName(VolumeLayoutBase.getEncEngineName(_encEngine));
			((ProgressReporter)_openingProgressReporter).setKSProcessed(false);
		}
		
		Logger.debug(String.format("Using %s hash function to derive the key", _hashFunc.getAlgorithm()));
		byte[] key = deriveKey(_encEngine.getKeySize(), _hashFunc, password, ks.salt, ks.passwordIterations);
		
		Logger.debug(String.format("Using %s encryption engine", VolumeLayoutBase.getEncEngineName(_encEngine)));
		_encEngine.setKey(key);
		_encEngine.init();
		//_encEngine.setIV(ks.keyMaterialOffsetSector);
		_encEngine.setIV(new byte[_encEngine.getIVSize()]);
		_encEngine.decrypt(afKey, 0, afKey.length);
		
		
		byte[] mk = new byte[mki.keyLength];
		try
		{
			af.merge(afKey, 0, mk, 0, ks.numStripes);
		}
		catch (DigestException e)
		{
			throw new ApplicationException("AF merge failed", e);
		}
		if(_openingProgressReporter!=null)
			((ProgressReporter)_openingProgressReporter).setKSProcessed(true);
		if(mki.isValidKey(mk))
		{
			_masterKey = mk;
			_encEngine.setKey(_masterKey);			
			_encEngine.init();
			return true;
		}
		Arrays.fill(mk, (byte)0);
		return false;

	}

	protected byte[] getCipherName()
	{
		String cn = _encEngine.getCipherName();
		byte[] res = new byte[MAX_CIPHERNAME_LEN];
		System.arraycopy(cn.getBytes(), 0, res, 0,
				Math.min(cn.length(), MAX_CIPHERNAME_LEN));
		return res;
	}

	protected byte[] getCipherModeName()
	{
		String cn = _encEngine.getCipherModeName();
		byte[] res = new byte[MAX_CIPHERMODENAME_LEN];
		System.arraycopy(cn.getBytes(), 0, res, 0,
				Math.min(cn.length(), MAX_CIPHERMODENAME_LEN));
		return res;
	}

	protected byte[] getUUIDBytes()
	{
		String uuidStr = _uuid.toString();
		byte[] res = new byte[UUID_LENGTH];
		System.arraycopy(uuidStr.getBytes(), 0, res, 0,
				Math.min(uuidStr.length(), UUID_LENGTH));
		return res;
	}

	@SuppressLint("DefaultLocale")
	protected byte[] getHashspecName()
	{
		String cn = _hashFunc.getAlgorithm().toLowerCase();
		switch (cn)
		{
			case "sha-512":
				cn = "sha512";
				break;
			case "sha-256":
				cn = "sha256";
				break;
			case "sha-1":
				cn = "sha1";
				break;
		}
		byte[] res = new byte[MAX_HASHSPEC_LEN];
		System.arraycopy(cn.getBytes(), 0, res, 0,
				Math.min(cn.length(), MAX_HASHSPEC_LEN));
		return res;
	}
	
	protected long getHeaderOffset()
	{
		return 0;
	}
	

	private static final int NUM_KEY_SLOTS = 8;
	private static final int KEY_DISABLED_SIG = 0x0000DEAD;
	private static final int KEY_ENABLED_SIG = 0x00AC71F3;
	private static final int SECTOR_SIZE = 512;
	private static final int MAX_CIPHERNAME_LEN = 32;
	private static final int MAX_CIPHERMODENAME_LEN = 32;
	private static final int MAX_HASHSPEC_LEN = 32;
	private static final int MK_SALT_SIZE = 32;
	private static final int MK_ITERATIONS_MIN = 1000;
	private static final int SLOT_ITERATIONS_MIN = 5000;
	private static final int MK_DIGEST_SIZE = 20;
	private static final int UUID_LENGTH = 40;
	private static final int HEADER_SIZE = 1024;
	private static final int KEY_MATERIAL_OFFSET = 4096;
	private static final int NUM_AF_STRIPES = 4000;
	private static final int DEFAULT_DISK_ALIGNMENT = 1024*1024;
	
	private static final byte[] MAGIC = new byte[] { 'L', 'U', 'K', 'S',
			(byte) 0xba, (byte) 0xbe };

	

}
