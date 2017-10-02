package com.sovworks.eds.container;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;

import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.locations.LUKSLocation;
import com.sovworks.eds.android.locations.TrueCryptLocation;
import com.sovworks.eds.android.locations.VeraCryptLocation;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.fat.FATInfo;
import com.sovworks.eds.fs.fat.FatFS;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.Settings;
import com.sovworks.eds.truecrypt.StdLayout;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;

public abstract class ContainerFormatterBase extends EDSLocationFormatter
{
	public static ContainerLocation createBaseContainerLocationFromFormatInfo(
			String formatName,
			Location containerLocation,
			EdsContainer cont,
			Context context,
			Settings settings)
	{

		switch (formatName)
		{
			case com.sovworks.eds.luks.FormatInfo.FORMAT_NAME:
				return new LUKSLocation(containerLocation, cont, context, settings);
			case com.sovworks.eds.truecrypt.FormatInfo.FORMAT_NAME:
				return new TrueCryptLocation(containerLocation, cont, context, settings);
			case com.sovworks.eds.veracrypt.FormatInfo.FORMAT_NAME:
				return new VeraCryptLocation(containerLocation, cont, context, settings);
			default:
				return new ContainerBasedLocation(containerLocation, cont, context, settings);
		}
	}

	protected ContainerFormatterBase(Parcel in)
	{
		super(in);
		String s = in.readString();
		if(s!=null)
			_containerFormat = getContainerFormatByName(s);
		_containerSize = in.readLong();
		_randFreeSpace = in.readByte() != 0;
		s = in.readString();
		String s2 = in.readString();
		if(s!=null && s2!=null)
			setEncryptionEngine(s, s2);
		s = in.readString();
		if(s!=null)
			setHashFunc(s);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		super.writeToParcel(dest, flags);
		dest.writeString(_containerFormat == null ? null : _containerFormat.getFormatName());
		dest.writeLong(_containerSize);
		dest.writeByte((byte) (_randFreeSpace ? 1 : 0));
		if(_encryptionEngine!=null)
		{
			dest.writeString(_encryptionEngine.getCipherName());
			dest.writeString(_encryptionEngine.getCipherModeName());
		}
		else
		{
			dest.writeString(null);
			dest.writeString(null);
		}
		dest.writeString(_hashFunc != null ? _hashFunc.getAlgorithm() : null);
	}

	protected ContainerFormatterBase()
	{

	}

	public void setContainerFormat(ContainerFormatInfo containerFormat)
	{
		_containerFormat = containerFormat;
	}

	public void setNumKDFIterations(int num)
	{
		_numKDFIterations = num;
	}
	
	public void setEncryptionEngine(String encAlgName, String hashFuncName)
	{
		setEncryptionEngine((FileEncryptionEngine) VolumeLayoutBase.findCipher(getLayout().getSupportedEncryptionEngines(), encAlgName, hashFuncName));
	}
	
	public void setEncryptionEngine(FileEncryptionEngine engine)
	{
		_encryptionEngine = engine;
	}
	
	public void setHashFunc(String name)
	{
		setHashFunc(VolumeLayoutBase.findHashFunc(getLayout().getSupportedHashFuncs(), name));
	}
	
	public void setHashFunc(MessageDigest md)
	{
		_hashFunc = md;
	}
	
	public void setContainerSize(long containerSize)
	{
		_containerSize = containerSize;
	}

	public void enableFreeSpaceRand(boolean val)
	{
		_randFreeSpace = val;
	}

	public void setFileSystemType(FileSystemInfo fsInfo)
	{
		_fileSystemType = fsInfo;
	}

	protected ContainerFormatInfo _containerFormat;
	protected FileSystemInfo _fileSystemType = new FATInfo();
	protected FileEncryptionEngine _encryptionEngine;
	protected MessageDigest _hashFunc;
	protected long _containerSize;
	protected boolean _randFreeSpace;
	protected int _numKDFIterations;

	@Override
	protected EDSLocation createLocation(Location location) throws IOException, ApplicationException, UserException
	{
		if(_containerFormat == null)
			throw new IllegalStateException("Container format is not specified");
		if (_containerSize < 1024L * 1024L)
			throw new IllegalStateException("Container size is too small");

		VolumeLayout layout = getLayout();
		setVolumeLayoutPassword(layout);
		if(_numKDFIterations > 0)
			layout.setNumKDFIterations(_numKDFIterations);
		if(_encryptionEngine!=null)
			layout.setEngine(_encryptionEngine);
		if(_hashFunc!=null)
			layout.setHashFunc(_hashFunc);

		/*Path contPath = location.getCurrentPath();
		if(!contPath.isFile())
		{
			Path parentPath = contPath.getParentPath();
			if(parentPath!=null)
			{
				String fn = PathUtil.getNameFromPath(contPath);
				if(fn!=null)
					parentPath.getDirectory().createFile(fn);
			}
		}*/
		RandomAccessIO io = getIO(location);
		try
		{
			format(io, layout);
		}
		finally
		{
			io.close();
		}
		return createContainerBasedLocation(location, layout);

	}

	protected RandomAccessIO getIO(Location targetLocation) throws IOException
	{
		return targetLocation.getCurrentPath().getFile().getRandomAccessIO(AccessMode.ReadWrite);
	}

	protected ContainerLocation createContainerBasedLocation(Location containerLocation, VolumeLayout layout) throws IOException
	{
		EdsContainer cont = getEdsContainer(containerLocation.getCurrentPath(), layout);
		return createBaseContainerLocationFromFormatInfo(
				containerLocation,
				cont,
				getContext(),
				getSettings()
		);
	}

	protected ContainerLocation createBaseContainerLocationFromFormatInfo(Location containerLocation,
																		  EdsContainer cont,
																		  Context context,
																		  Settings settings)
	{
		return createBaseContainerLocationFromFormatInfo(
				_containerFormat.getFormatName(),
				containerLocation,
				cont,
				context,
				settings
		);
	}

	protected EdsContainer getEdsContainer(Path pathToContainer, VolumeLayout layout)
	{
		return new EdsContainer(pathToContainer, _containerFormat, layout);
	}
	
	protected void format(RandomAccessIO io, VolumeLayout layout) throws IOException, ApplicationException, UserException
    {
		if(layout instanceof StdLayout)
			formatTCBasedContainer(
					io,
					(com.sovworks.eds.truecrypt.FormatInfo)_containerFormat,
					(StdLayout)layout,
					_containerSize,
					_randFreeSpace);
		else if(layout instanceof com.sovworks.eds.luks.VolumeLayout)
			formatLUKSContainer(
				io,
				(com.sovworks.eds.luks.FormatInfo)_containerFormat,
				(com.sovworks.eds.luks.VolumeLayout)layout,
				_containerSize,
				_randFreeSpace);
	}

	protected void formatLUKSContainer(RandomAccessIO io, com.sovworks.eds.luks.FormatInfo containerFormat, com.sovworks.eds.luks.VolumeLayout layout, long containerSize, boolean randFreeSpace) throws IOException, ApplicationException
	{
		if(randFreeSpace)
		{
			io.seek(0);
			fillFreeSpace(io, containerSize);
		}
		else
		{
			io.seek(containerSize - 1);
			io.write(0);
		}
		layout.initNew();
		//setContainerSize should be called after initNew
		layout.setContainerSize(containerSize);
		containerFormat.formatContainer(io, layout, _fileSystemType);
	}

	protected VolumeLayout getLayout()
	{
		return _containerFormat.getVolumeLayout();
	}
	
	protected void setVolumeLayoutPassword(VolumeLayout layout) throws IOException
	{
		if(_password != null)
		{
			byte[] pass = _password.getDataArray();
			layout.setPassword(EdsContainerBase.cutPassword(
					pass, _containerFormat.getMaxPasswordLength()
			));
			SecureBuffer.eraseData(pass);
		}
		else
			layout.setPassword(new byte[0]);
	}

	protected void setHints(ContainerLocation cont)
	{
		cont.getExternalSettings().setContainerFormatName(_containerFormat.getFormatName());
		if(_encryptionEngine!=null)
			cont.getExternalSettings().setEncEngineName(VolumeLayoutBase.getEncEngineName(_encryptionEngine));
		if(_hashFunc!=null)
			cont.getExternalSettings().setHashFuncName(_hashFunc.getAlgorithm());
	}

	protected void setExternalContainerSettings(EDSLocation loc) throws ApplicationException, IOException
	{
		setHints((ContainerLocation)loc);
		super.setExternalContainerSettings(loc);
	}

	protected void formatTCBasedContainer(RandomAccessIO io, com.sovworks.eds.truecrypt.FormatInfo containerFormat, StdLayout layout, long containerSize, boolean randFreeSpace) throws IOException, ApplicationException
	{
		if(randFreeSpace)
		{
			io.seek(0);
			fillFreeSpace(io, containerSize);
		}
		else
		{
			io.setLength(containerSize);
			//io.seek(containerSize - 1);
			//io.write(0);
		}			
		layout.setContainerSize(containerSize);
		layout.initNew();
		containerFormat.formatContainer(io, layout, _fileSystemType);
	}
	

	@SuppressLint("TrulyRandom")
	protected void fillFreeClustersWithRandomData(FatFS fat) throws IOException
	{			
		RandomAccessIO f = fat.getContainerFile();
		int[] clusterTable = fat.getClusterTable();			
		byte[] buf = new byte[fat.getSectorsPerCluster()*fat.getBytesPerSector()];
		SecureRandom rand = new SecureRandom();
		for (int i = 2; i < clusterTable.length; i++)
		{				
			if (clusterTable[i] == 0)
			{
				rand.nextBytes(buf);
				f.seek(fat.getClusterOffset(i));
				f.write(buf,0,buf.length);
			}	
			if(!reportProgress((byte)(i*100/clusterTable.length)))
				break;
		}
	}
	
	@SuppressLint("TrulyRandom")
	protected void fillFreeSpace(RandomAccessIO f, long size) throws IOException
	{
		SecureRandom r = new SecureRandom();
		byte[] buf=new byte[512];
		for(long i=0;i<size;i+=buf.length)
		{				
			r.nextBytes(buf);
			f.write(buf,0,buf.length);
			if(!reportProgress((byte)(i*100/size)))
				break;
		}		
	}

	private ContainerFormatInfo getContainerFormatByName(String name)
	{
		for (ContainerFormatInfo ci : EdsContainer.getSupportedFormats())
			if (ci.getFormatName().equals(name))
				return ci;
		return null;
	}
}
