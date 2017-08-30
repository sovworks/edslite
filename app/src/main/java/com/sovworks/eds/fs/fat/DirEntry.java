package com.sovworks.eds.fs.fat;

import android.annotation.SuppressLint;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.DataInput;
import com.sovworks.eds.fs.DataOutput;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.RandomStorageAccess;
import com.sovworks.eds.fs.fat.FatFS.FatPath;
import com.sovworks.eds.fs.util.RandomAccessInputStream;
import com.sovworks.eds.fs.util.RandomAccessOutputStream;
import com.sovworks.eds.fs.util.Util;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

@SuppressLint("DefaultLocale")
class DirEntry
{
	public DirEntry()
	{
		this(-1);
	}

	public DirEntry(int streamOffset)
	{
		createDateTime = lastAccessDate = lastModifiedDateTime = new Date();
		offset = streamOffset;
	}

	public static final byte[] ALLOWED_SYMBOLS = { '!', '#', '$', '%', '&', '(', ')', '-', '@', '^', '_', '`', '{', '}', '~', '\'' };
	public static final byte[] RESTRICTED_SYMBOLS = { '+', ',', '.', ';', '=', '[', ']' };
	public static final byte[] SYSTEM_SYMBOLS = { '*', '?', '<', ':', '>', '/', '\\', '|' };

	public String name;
	public String dosName;
	public byte attributes;
	public Date createDateTime;
	public Date lastAccessDate;
	public Date lastModifiedDateTime;
	public int startCluster;
	public long fileSize;
	public int offset = -1;
	public int numLFNRecords;

	public void copyFrom(DirEntry src, boolean copyName)
	{
		if (copyName)
		{
			name = src.name;
			dosName = src.dosName;
			numLFNRecords = src.numLFNRecords;
		}
		attributes = src.attributes;
		createDateTime = src.createDateTime;
		lastAccessDate = src.lastAccessDate;
		lastModifiedDateTime = src.lastModifiedDateTime;
		fileSize = src.fileSize;
		startCluster = src.startCluster;
	}

	public boolean isDir()
	{
		return (attributes & Attributes.subDir) != 0;
	}

	public void setDir(boolean val)
	{
		if (val)
			attributes |= Attributes.subDir;
		else
			attributes &= ~Attributes.subDir;
	}

	public boolean isReadOnly()
	{
		return (attributes & Attributes.readOnly) != 0;
	}

	public void setReadOnly(boolean val)
	{
		if (val)
			attributes |= Attributes.readOnly;
		else
			attributes &= ~Attributes.readOnly;
	}

	public boolean isVolumeLabel()
	{
		return (attributes & Attributes.volumeLabel) != 0;
	}

	public void setVolume(boolean val)
	{
		if (val)
			attributes |= Attributes.volumeLabel;
		else
			attributes &= ~Attributes.volumeLabel;
	}

	public boolean isFile()
	{
		return !isDir() && (attributes & Attributes.volumeLabel) == 0;
	}

	/**
	 * Reads directory entry from the stream at the current position
	 * 
	 * @param input InputStream.
	 * @return next entry or null if there are no more entries
	 * @throws IOException
	 */
	public static DirEntry readEntry(DirReader input) throws IOException
	{
		byte[] buf = new byte[RECORD_SIZE];
		StringBuilder lfnSb = null;
		String lfn;
		byte expectedChecksum = 0;
		int seq = -1;
		int pSeq;
		int lfns = 0;

		for (;;)
		{
			if (Util.readBytes(input, buf)!=RECORD_SIZE) return null;
			int fb = Util.unsignedByteToInt(buf[0]);
			//if (fb == 0) return null;
			if (fb == 0xE5 || fb == 0) // deleted record
				continue;
			if (buf[0x0B] == 0x0F) // LFN record
			{
				if (seq < 0)
				{
					pSeq = -1;
					expectedChecksum = 0;
					lfnSb = null;
				}
				else
					pSeq = seq;

				seq = fb;
				if (lfnSb == null)
				{
					if ((seq & 0x40) == 0)
					{
						seq = -1;
						continue;
					}
					lfnSb = new StringBuilder();
					lfns = 0;
				}
				seq &= 0x3F;
				if (pSeq >= 0)
				{
					if (buf[0x0d] != expectedChecksum || pSeq != seq + 1)
					{
						seq = -1;
						continue;
					}
				}
				else
					expectedChecksum = buf[0x0d];
				lfnSb.insert(0, new String(buf, 1, 10, "UTF-16LE") + new String(buf, 0x0E, 12, "UTF-16LE") + new String(buf, 0x1C, 4, "UTF-16LE"));
				lfns++;
			}
			else
			{
				if (seq == 1)
				{
					lfn = lfnSb.toString();
					int idx = lfn.indexOf(0);
					if (idx >= 0) lfn = lfn.substring(0, idx);
				}
				else
					lfn = null;
				//if (fb == 0x05 || fb == 0xE5)
				//	seq = -1;
				//else				
				DirEntry entry = new DirEntry();
				entry.attributes = buf[0x0B];
				if (entry.isVolumeLabel())
				{
					seq = -1;
					continue;
				}

				if (lfn != null)
				{
					byte cs = calcChecksum(buf, 0);
					if (expectedChecksum != cs) lfn = null;
				}

									
				entry.dosName = new String(buf, 0, 11, "ASCII");
				if(lfn == null)
				{
					String tmp = entry.dosName.substring(0, 8).trim();
					if ((buf[0x0C] & 0x8) != 0) // lower case base name
						tmp = FileName.toLowerCase(tmp);
					entry.name = tmp;
					tmp = entry.dosName.substring(8, 11).trim();
					if(tmp.length()>0)
					{
						if ((buf[0x0C] & 0x4) != 0) // lower case extension
							tmp = FileName.toLowerCase(tmp);
						entry.name += '.' + tmp;
					}
				}
				else
				{
					entry.name = lfn;
					entry.numLFNRecords = lfns;
				}					

				entry.offset = ((int)input.getFilePointer() - RECORD_SIZE * (entry.numLFNRecords + 1));
				if(!".".equals(entry.name) && !"..".equals(entry.name) && !FatFS.isValidFileNameImpl(entry.name))
				{
					Logger.log(String.format("DirEntry.readEntry: incorrect file name: %s; dir offset: %d",entry.name,entry.offset));
					continue;
				}

				int dv = Util.unsignedShortToIntLE(buf, 0x10);
				int tv = Util.unsignedShortToIntLE(buf, 0x0E);
				int secs = Util.unsignedByteToInt(buf[0x0D]) / 100;

				entry.createDateTime = new GregorianCalendar(1980 + (dv >> 9), ((dv >> 5) & 0xF) - 1, dv & 0x1F, tv >> 11, (tv >> 5) & 0x3F, (tv & 0x1F)
						* 2 + secs).getTime();
				dv = Util.unsignedShortToIntLE(buf, 0x12);
				entry.lastAccessDate = new GregorianCalendar(1980 + (dv >> 9), ((dv >> 5) & 0xF) - 1, dv & 0x1F).getTime();
				dv = Util.unsignedShortToIntLE(buf, 0x18);
				tv = Util.unsignedShortToIntLE(buf, 0x16);
				entry.lastModifiedDateTime = new GregorianCalendar(1980 + (dv >> 9), ((dv >> 5) & 0xF) - 1, dv & 0x1F, tv >> 11, (tv >> 5) & 0x3F,
						(tv & 0x1F) * 2).getTime();
				entry.fileSize = Util.unsignedIntToLongLE(buf, 0x1C);
				entry.startCluster = (Util.unsignedShortToIntLE(buf, 0x14) << 16) | Util.unsignedShortToIntLE(buf, 0x1A);
				//DEBUG
				//Log.d("EDS", String.format("Read entry %s at %d. lfns=%d", entry.name,entry.offset,lfns));
				return entry;
				
			}
		}
	}

	public void writeEntry(FatFS fat, FatPath basePath,Object opTag) throws IOException
	{
		fat.lockPath(basePath,AccessMode.ReadWrite,opTag);		
		try
		{
			boolean isLast = false;
			FileName fn = new FileName(name);			
			if(dosName == null)
				initDosName(fn,fat, basePath, opTag);
			if(offset >= 0 && numLFNRecords == 0 && fn.isLFN)
			{
				deleteEntry(fat, basePath, opTag);
				offset = -1;
			}
			if (offset < 0)
				isLast = getFreeDirEntryOffset(fat, basePath, fn.isLFN ? (getNumLFNRecords() + 1) : 1,opTag);
			
			DirWriter os = fat.getDirWriter(basePath,opTag);
			try
			{
				//DEBUG 
				//Log.d("EDS", String.format("Writing dir entry %s at offset %d",name,offset));
				os.seek(offset);
				writeEntry(fn,os);
				if (isLast)			
					os.write(0);
					//zeroRemainingClusterSpace(fat, os, ((IFSStream) os).getPosition());
				
			}
			finally
			{
				os.close();
			}		
		}
		finally
		{
			fat.releasePathLock(basePath);
		}
		
	}

	public void writeEntry(FileName fn, DataOutput output) throws IOException
	{
		byte[] record = new byte[32];
		if(dosName == null)
			dosName = fn.getDosName(0);
		putDosName(record);
		if (fn.isLowerCaseName) record[0x0C] |= 0x8;
		if (fn.isLowerCaseExtension) record[0x0C] |= 0x4;
		if (fn.isLFN)
		{
			//DEBUG
			//Log.d("EDS", "Entry is lfn");
			writeLFNRecords(output, calcChecksum(record, 0));
		}
		record[0x0b] = attributes;
		Util.shortToBytesLE((short) startCluster, record, 0x1A);
		Util.shortToBytesLE((short) (startCluster >> 16), record, 0x14);
		Util.intToBytesLE((int) fileSize, record, 0x1C);
		record[0x0D] = getGreaterTimeRes(createDateTime);
		Util.shortToBytesLE(encodeTime(createDateTime), record, 0x0E);
		Util.shortToBytesLE(encodeDate(createDateTime), record, 0x10);
		Util.shortToBytesLE(encodeDate(lastAccessDate), record, 0x12);
		Util.shortToBytesLE(encodeTime(lastModifiedDateTime), record, 0x16);
		Util.shortToBytesLE(encodeDate(lastModifiedDateTime), record, 0x18);
		output.write(record,0,record.length);
	}

	public void deleteEntry(FatFS fat, FatPath basePath,Object opTag) throws IOException
	{
		DirWriter s = fat.getDirWriter(basePath,opTag);
		try
		{
			deleteEntry(s);
		}
		finally
		{
			s.close();
		}
	}

	public synchronized void deleteEntry(DirWriter output) throws IOException
	{
		if (offset < 0) throw new IOException("deleteEntry error: can't delete new entry");

		for (int i = 0; i <= numLFNRecords; i++)
		{
			output.seek(offset + i * 32);
			output.write(0xE5);
		}
	}

	static final int RECORD_SIZE = 32;
	
	private void initDosName(FileName fn,FatFS fat, FatPath basePath,Object opTag) throws IOException
	{
		if(fn.isLFN)	
		{
			ArrayList<String> dosNames = readDosNames(fat, basePath, opTag);
			Collections.sort(dosNames);			
			int counter = 0;
			do
			{				
				 dosName = fn.getDosName(counter++);
			}
			while(
					Collections.binarySearch(dosNames, dosName,new Comparator<String>()
					{
						@Override
						public int compare(String lhs, String rhs)
						{
							return lhs.compareTo(rhs);
						}						
					}
					)>=0
			);
		}
		else
			dosName = fn.getDosName(0);
	}


	private boolean getFreeDirEntryOffset(FatFS fat, FatPath parentDirPath, int numEntries,Object opTag) throws IOException
	{
		boolean r = false;
		int numDeletedEntries = 0;
		int res = 0;
		byte[] buf = new byte[DirEntry.RECORD_SIZE];
		DirReader dirStream = fat.getDirReader(parentDirPath,opTag);
		try
		{
			while (numDeletedEntries < numEntries && (r = (Util.readBytes(dirStream, buf)==DirEntry.RECORD_SIZE)) && buf[0] != 0)
			{
				numDeletedEntries = Util.unsignedByteToInt(buf[0]) == 0xE5 ? numDeletedEntries + 1 : 0;
				res += DirEntry.RECORD_SIZE;
			}
		}
		finally
		{
			dirStream.close();
		}
		if (!r) throw new EOFException("getFreeDirEntryOffset error: no more free space");
		offset = res - numDeletedEntries * DirEntry.RECORD_SIZE;
		return buf[0] == 0;
	}

	private static byte calcChecksum(byte[] fn, int offset)
	{
		int sum = 0;

		for (int i = 0; i < 11; i++)
			sum = Util.unsignedByteToInt((byte) (((sum & 1) << 7) + (sum >> 1) + fn[i + offset]));
		return (byte) sum;
	}

	private byte getGreaterTimeRes(Date date)
	{
		Calendar calend = new GregorianCalendar();
		calend.setTime(date);
		int val = calend.get(Calendar.SECOND);
		if (val % 2 != 0) val = 1000;
		val += calend.get(Calendar.MILLISECOND);
		return (byte) (val / 10);
	}

	private short encodeDate(Date date)
	{
		Calendar calend = new GregorianCalendar();
		calend.setTime(date);
		int val = (calend.get(Calendar.YEAR) - 1980) << 9;
		val |= (calend.get(Calendar.MONTH) + 1) << 5;
		val |= calend.get(Calendar.DAY_OF_MONTH);
		return (short) val;
	}

	private short encodeTime(Date date)
	{
		Calendar calend = new GregorianCalendar();
		calend.setTime(date);
		int val = (calend.get(Calendar.HOUR_OF_DAY) << 11);
		val |= (calend.get(Calendar.MINUTE) << 5);
		val |= calend.get(Calendar.SECOND) / 2;
		return (short) val;
	}

	private int getNumLFNRecords()
	{
		int numChars = name.length();
		return numChars % 13 == 0 ? numChars / 13 : (numChars / 13) + 1;
	}
	
	private ArrayList<String> readDosNames(FatFS fs, FatPath path, Object opTag) throws IOException
	{
		ArrayList<String> res = new ArrayList<>();
		DirReader stream = fs.getDirReader(path,opTag);
		try
		{
			DirEntry entry;
			while(true)
			{
				entry = DirEntry.readEntry(stream);
				if(entry!=null)
					res.add(entry.dosName.toUpperCase());
				else
					break;
			}			
		}
		finally
		{
			stream.close();
		}
		return res;
		
	}
	
	private void putDosName( byte[] recordData)
	{
		System.arraycopy(dosName.getBytes(),0, recordData, 0, 11);
		if (Util.unsignedByteToInt(recordData[0]) == 0xE5) recordData[0] = 0x05;
	}
	
	private void writeLFNRecords(DataOutput output, int checkSum) throws IOException
	{
		final int[] nameCharPos = new int[] { 1, 3, 5, 7, 9, 14, 16, 18, 20, 22, 24, 28, 30 };
		byte[] bytes;
		try
		{
			bytes = name.getBytes("UTF-16LE");
		}
		catch (UnsupportedEncodingException e)
		{
			return;
		}
		int numChars = name.length();
		int numRecords = numChars / 13;
		int lastChar = numChars % 13;
		if (lastChar != 0) numRecords++;

		byte[] recData = new byte[RECORD_SIZE];
		recData[0x0b] = 0x0f;
		recData[0x0d] = (byte) checkSum;

		for (int seq = numRecords; seq > 0; seq--)
		{
			int charIdx = (seq - 1) * 13;
			recData[0] = (byte) (seq | (seq == numRecords ? 0x40 : 0));
			for (int idx : nameCharPos)
			{
				if (charIdx < numChars)
				{
					recData[idx] = bytes[charIdx * 2];
					recData[idx + 1] = bytes[charIdx * 2 + 1];
				}
				else
					Util.shortToBytesLE(charIdx == numChars ? 0 : (short) 0xFFFF, recData, idx);
				charIdx++;
			}
			//DEBUG
			//Log.d("EDS", String.format("Writing lfn seq=%d",seq));
			output.write(recData,0,recData.length);
		}
		numLFNRecords = numRecords;
	}

	final class Attributes
	{
		final static byte readOnly = 0x1;
		final static byte hidden = 0x2;
		final static byte system = 0x4;
		final static byte volumeLabel = 0x8;
		final static byte subDir = 0x10;
		final static byte archive = 0x20;
		final static byte device = 0x40;
	}

}

interface DirReader extends DataInput, RandomStorageAccess, Closeable
{
	
}

interface DirWriter extends DataOutput, RandomStorageAccess, Closeable
{
	
}

class DirOutputStream extends RandomAccessOutputStream implements DirWriter
{

	public DirOutputStream(RandomAccessIO io) throws IOException
	{
		super(io);
	}	
}

class DirInputStream extends RandomAccessInputStream implements DirReader
{

	public DirInputStream(RandomAccessIO io) throws IOException
	{
		super(io);
	}	
}