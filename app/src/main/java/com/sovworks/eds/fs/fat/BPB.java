package com.sovworks.eds.fs.fat;

import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.errors.WrongImageFormatException;
import com.sovworks.eds.fs.util.Util;

import java.io.EOFException;
import java.io.IOException;

/**
 * Fat Bios Parameter Block structure
 */
class BPB
{
	public static String FAT12_LABEL = "FAT12";
	public static String FAT16_LABEL = "FAT16";
	public static String FAT32_LABEL = "FAT32";

	int bytesPerSector;
	int sectorsPerCluster;
	int reservedSectors;
	short numberOfFATs;
	int rootDirEntries;
	int totalSectorsNumber;
	short mediaType;
	int sectorsPerFat;
	int sectorsPerTrack = 1;
	int numberOfHeads = 1;
	long hiddenSectors;
	long sectorsBig;

	short physicalDriveNumber;
	// 1 byte reserved
	short extendedBootSignature;
	long volumeSerialNumber;
	byte[] volumeLabel = new byte[12];
	byte[] fileSystemLabel = new byte[8];

	void read(RandomAccessIO input) throws IOException, WrongImageFormatException
	{
		input.seek(0xB);
		bytesPerSector = Util.readWordLE(input);
		sectorsPerCluster = Util.readUnsignedByte(input);
		reservedSectors = Util.readWordLE(input);
		numberOfFATs = Util.readUnsignedByte(input);
		rootDirEntries = Util.readWordLE(input);
		totalSectorsNumber = Util.readWordLE(input);
		mediaType = Util.readUnsignedByte(input);
		sectorsPerFat = Util.readWordLE(input);
		sectorsPerTrack = Util.readWordLE(input);
		numberOfHeads = Util.readWordLE(input);
		hiddenSectors = Util.readDoubleWordLE(input);
		sectorsBig = Util.readDoubleWordLE(input);		
	}

	void write(RandomAccessIO output) throws IOException
	{
		output.seek(0xB);
		Util.writeWordLE(output, (short) bytesPerSector);
		output.write(sectorsPerCluster);
		Util.writeWordLE(output, (short) reservedSectors);
		output.write(numberOfFATs);
		Util.writeWordLE(output, (short) rootDirEntries);
		Util.writeWordLE(output, (short) totalSectorsNumber);
		output.write(mediaType);
		Util.writeWordLE(output, (short) sectorsPerFat);
		Util.writeWordLE(output, (short) sectorsPerTrack);
		Util.writeWordLE(output, (short) numberOfHeads);
		Util.writeDoubleWordLE(output, (int) hiddenSectors);
		Util.writeDoubleWordLE(output, (int) sectorsBig);	

	}
	
	int getSectorsPerFat()
	{
		return sectorsPerFat;
	}
	
	long getTotalSectorsNumber()
	{
		return totalSectorsNumber;
	}
	
	long getClusterOffset(int cluster)
	{
		return _clusterOffsetStart + ((long)cluster - 2)*sectorsPerCluster*bytesPerSector;		
	}
	
	void calcParams()
	{
		_clusterOffsetStart = bytesPerSector * (reservedSectors + sectorsPerFat * numberOfFATs) + rootDirEntries * 32;
	}
	
	protected long _clusterOffsetStart;

	protected void readCommonPart(RandomAccessIO input) throws IOException
	{
		physicalDriveNumber = Util.readUnsignedByte(input);
		input.read();
		extendedBootSignature = Util.readUnsignedByte(input);
		volumeSerialNumber = Util.readDoubleWordLE(input);
		if(Util.readBytes(input, volumeLabel, 11)!=11)
			throw new EOFException();
		if(Util.readBytes(input, fileSystemLabel, 8)!=8)
			throw new EOFException();
	}

	protected void writeCommonPart(RandomAccessIO output) throws IOException
	{
		output.write(physicalDriveNumber);
		output.write(0);
		output.write(extendedBootSignature);
		Util.writeDoubleWordLE(output, (int) volumeSerialNumber);
		output.write(volumeLabel,0,volumeLabel.length);
		output.write(fileSystemLabel,0,fileSystemLabel.length);
	}

	protected void checkEndingSignature(RandomAccessIO input) throws WrongImageFormatException, IOException
	{
		input.seek(0x1FE);
		if (Util.readWordLE(input) != 0xAA55) throw new WrongImageFormatException("Invalid bpb sector signature");
		if(fileSystemLabel[0]!='F' || fileSystemLabel[1]!='A' || fileSystemLabel[2]!='T')
			throw new WrongImageFormatException("Looks like the file system is not FAT");			
	}

	protected void writeBPBSignature(RandomAccessIO output) throws IOException
	{
		output.seek(0x1FE);
		Util.writeWordLE(output, (short) 0xAA55);
	}
}

class BPB16 extends BPB
{

	@Override
	void read(RandomAccessIO input) throws IOException, WrongImageFormatException
	{
		super.read(input);
		readCommonPart(input);
		checkEndingSignature(input);
		
		calcParams();
	}

	@Override
	void write(RandomAccessIO output) throws IOException
	{
		super.write(output);
		writeCommonPart(output);
		writeBPBSignature(output);
	}
	
	@Override
	long getTotalSectorsNumber()
	{
		return totalSectorsNumber == 0 ? sectorsBig : totalSectorsNumber;
	}	
	
}

class BPB32 extends BPB
{

	long sectorsPerFat32;
	int updateMode;
	int versionNumber;
	int rootClusterNumber;
	int FSInfoSector;
	int bootSectorReservedCopySector;

	@Override
	void read(RandomAccessIO input) throws IOException, WrongImageFormatException
	{
		super.read(input);
		sectorsPerFat32 = Util.readDoubleWordLE(input);
		updateMode = Util.readWordLE(input);
		versionNumber = Util.readWordLE(input);
		rootClusterNumber = (int) Util.readDoubleWordLE(input);
		FSInfoSector = Util.readWordLE(input);
		bootSectorReservedCopySector = Util.readWordLE(input);
		input.seek(input.getFilePointer()+12);
		readCommonPart(input);
		checkEndingSignature(input);		
		calcParams();
	}

	@Override
	void write(RandomAccessIO output) throws IOException
	{
		super.write(output);
		Util.writeDoubleWordLE(output, (int) sectorsPerFat32);
		Util.writeWordLE(output, (short) updateMode);
		Util.writeWordLE(output, (short) versionNumber);
		Util.writeDoubleWordLE(output, (int) rootClusterNumber);
		Util.writeWordLE(output, (short) FSInfoSector);
		Util.writeWordLE(output, (short) bootSectorReservedCopySector);
		for(int i=0;i<12;i++)
			output.write(0);		
		writeCommonPart(output);
		writeBPBSignature(output);
	}
	
	@Override
	int getSectorsPerFat()
	{
		return (int)sectorsPerFat32;
	}
	
	@Override
	long getTotalSectorsNumber()
	{
		return totalSectorsNumber == 0 ? sectorsBig : totalSectorsNumber;
	}

	@Override
	void calcParams()
	{
		_clusterOffsetStart = bytesPerSector * (reservedSectors + sectorsPerFat32 * numberOfFATs);
	}
}