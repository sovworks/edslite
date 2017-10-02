package com.sovworks.eds.fs.fat;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.sovworks.eds.android.BuildConfig;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.errors.DirectoryIsNotEmptyException;
import com.sovworks.eds.fs.errors.FileInUseException;
import com.sovworks.eds.fs.errors.FileSystemClosedException;
import com.sovworks.eds.fs.errors.NoFreeSpaceLeftException;
import com.sovworks.eds.fs.errors.WrongImageFormatException;
import com.sovworks.eds.fs.util.PathBase;
import com.sovworks.eds.fs.util.RandomAccessInputStream;
import com.sovworks.eds.fs.util.RandomAccessOutputStream;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

//import android.util.Log;

public class FatFS implements FileSystem
{
	public static final int SECTOR_SIZE = 512;

	public static boolean isFAT(RandomAccessIO f)
	{
		byte[] cmp = new byte[] {'F', 'A', 'T'};
		byte[] buf = new byte[3];
		try
		{
			f.seek(0x036);
			if(Util.readBytes(f, buf) == buf.length)
			{
				if(!Arrays.equals(cmp, buf))
				{
					f.seek(0x052);
					if(Util.readBytes(f, buf) == buf.length)
						return Arrays.equals(cmp, buf);
				}
				else
					return true;
			}

		}
		catch (IOException ignored) {}

		return false;
	}


    public static final String TAG = "FatFS";
    public static final boolean LOG_ACQUIRE = false;
    private static int logAquiring(String tag)
    {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if(BuildConfig.DEBUG && LOG_ACQUIRE)
        {
            Object o = new Object();
            int id = o.hashCode();
            Log.v(TAG, String.format("Acquiring %s. Id = %d. Current thread id = %d...", tag, id, Thread.currentThread().getId()));
            Thread.dumpStack();
            return id;
        }
        return 0;
    }

    private static void logAquired(int id)
    {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if(BuildConfig.DEBUG && LOG_ACQUIRE)
            Log.v(TAG, id + " has been acquired.");
    }

    private static void logReleased(int id)
    {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if(BuildConfig.DEBUG && LOG_ACQUIRE)
            Log.v(TAG, id + " has been released.");
    }

	/**
	 * Get Fat instance
	 * 
	 * @param input
	 *            disk image stream.
	 * @return Fat instance
	 * @throws WrongImageFormatException
	 *             - if fs image file is wrong
	 * @throws IOException
	 *             - if io error occurs
	 */
	public static FatFS getFat(RandomAccessIO input) throws IOException
	{
		FatFS fat;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (input)
		{
			BPB bpb = new BPB();
			bpb.read(input);
			long sectorsNumber = bpb.totalSectorsNumber == 0 ? bpb.sectorsBig : bpb.totalSectorsNumber;
			int root_dir_sectors = bpb.rootDirEntries * DirEntry.RECORD_SIZE / bpb.bytesPerSector;
			long data_sectors = sectorsNumber - (bpb.reservedSectors + bpb.numberOfFATs * bpb.getSectorsPerFat() + root_dir_sectors);
			int num_clusters = 1 + (int)(data_sectors / bpb.sectorsPerCluster);
			if(root_dir_sectors == 0)
				fat = new Fat32FS(input);
			else
			{
				if (num_clusters < 4085)
					fat = new Fat12FS(input);
				else if (num_clusters < 65525)
					fat = new Fat16FS(input);
				else
					fat = new Fat32FS(input);
			}
			fat.init();			
		}
		return fat;
	}

	public static FatFS formatFat(RandomAccessIO input, long size) throws IllegalArgumentException, IOException
	{
		if (size <= 0 || size > 1000000000000L) throw new IllegalArgumentException("Wrong size: " + size);

		FatFS fat;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (input)
		{
			int clustSize = getOptimalClusterSize(size, SECTOR_SIZE);
			int numClusters = (int)(size / (clustSize*SECTOR_SIZE));
			size = (long)numClusters*clustSize*SECTOR_SIZE;
			
			if (numClusters < 4085)
				fat = new Fat12FS(input);
			else if (numClusters < 65525)
				fat = new Fat16FS(input);
			else
				fat = new Fat32FS(input);
			
			input.seek(size - 1);
			input.write(0);
			
			fat.initBPB(size);			
			fat.writeHeader();			
			fat.writeEmptyClusterTable();
			fat.writeFatBackup();
			fat.init();

			DirWriter os = fat.getDirWriter((FatPath) fat.getRootPath(), new Object());
			try
			{
				int clusterSize = fat._bpb.bytesPerSector*fat._bpb.sectorsPerCluster; 
				for(int i=0;i<clusterSize;i++)
					os.write(0);
			}
			finally
			{
				os.close();
			}
		}

		return fat;
	}
	
	public static int getOptimalClusterSize(long volumeSize,int sectorSize)
	{
		int clusterSize;
		if (volumeSize >= 2 * 1024L*1024L*1024L*1024L)
			clusterSize = 256 * 1024;
		else if (volumeSize >= 512 * 1024L*1024L*1024L)
			clusterSize = 128 * 1024;
		else if (volumeSize >= 128 * 1024L*1024L*1024L)
			clusterSize = 64 * 1024;
		else if (volumeSize >= 64 * 1024L*1024L*1024L)
			clusterSize = 32 * 1024;
		else if (volumeSize >= 32 * 1024L*1024L*1024L)
			clusterSize = 16 * 1024;
		else if (volumeSize >= 16 * 1024L*1024L*1024L)
			clusterSize = 8 * 1024;
		else if (volumeSize >= 512 * 1024L*1024L)
			clusterSize = 4 * 1024;
		else if (volumeSize >= 256 * 1024L*1024L)
			clusterSize = 2 * 1024;
		else if (volumeSize >= 1024L * 1024L)
			clusterSize = 1024;
		else
			clusterSize = 512;

		clusterSize /= sectorSize;
		
		if(clusterSize == 0)
			clusterSize = 1;
		else if(clusterSize > 128)
			clusterSize = 128;
		
		return clusterSize;		
	}

	private Path _rootPath;

	@Override
	public synchronized Path getRootPath()
	{
		if(_rootPath == null) try
		{
			_rootPath = getPath("");
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return _rootPath;
	}
	
	public void init() throws IOException
	{
		synchronized (_ioSyncer)
		{
			_bpb.read(_input);
			_emptyCluster = new byte[_bpb.bytesPerSector * _bpb.sectorsPerCluster];
			loadClusterTable();
		}
	}

	@Override
	public Path getPath(String pathString) throws IOException
	{
		FatPath fp = new FatPath(pathString);
		String[] components = fp.getPathUtil().getComponents();
		for(String cmp: components)
			if(!isValidFileName(cmp))
				throw new IOException("Invalid path: " + pathString);
		return fp;
	}

	@Override
	public void close(boolean force) throws IOException
	{
		_isClosing = true;
		int timeLeft = PATH_LOCK_TIMEOUT;
		while(timeLeft > 0)
		{
			synchronized (_openedFiles)
			{
				if(!_openedFiles.isEmpty())
				{
					long curTime = System.currentTimeMillis();
					try
					{
						_openedFiles.wait(timeLeft);
					}
					catch (InterruptedException e)
					{
						break;
					}
					timeLeft -= (int) (System.currentTimeMillis() - curTime);
				}
				else
					break;
			}
		}
		synchronized(_ioSyncer)
		{
			if(!force)
			{
				synchronized (_openedFiles) 
				{
					if(!_openedFiles.isEmpty())
					{
						if(GlobalConfig.isDebug())
						{
							StringBuilder sb = new StringBuilder();
							for(Path p: _openedFiles.keySet())
								sb.append(p.getPathDesc()).append(", ");
							sb.delete(sb.length() - 2, sb.length());
							throw new IOException("File system is in use. Opened files list: " + sb.toString());
						}
						else
							throw new IOException("File system is in use.");
					}
				}
			}
		}
	}

	@Override
	public boolean isClosed()
	{
		return _isClosing;
	}


	public RandomAccessIO getContainerFile()
	{
		return _input;
	}

	public boolean isValidFileName(String fileName)
	{
		return isValidFileNameImpl(fileName);
	}

	public static boolean isValidFileNameImpl(String fileName)
	{
		String tfn = fileName.trim();
		if (tfn.equals("") || tfn.equals(".") || tfn.endsWith("..")) return false;

		for (int i = 0; i < fileName.length(); i++)
		{
			char c = fileName.charAt(i);
			if (c <= 31 || RESERVED_SYMBOLS.indexOf(c) >= 0) return false;
		}

		return true;
	}
	
	public void setReadOnlyMode(boolean val)
	{
		_readOnlyMode = val;
	}
	
	public int[] getClusterTable()
	{
		return _clusterTable;
	}
	
	public long getClusterOffset(int clusterIndex)
	{
		return _bpb.getClusterOffset(clusterIndex);
	}
	
	public int getSectorsPerCluster()
	{
		return _bpb.sectorsPerCluster;
	}
	
	public int getBytesPerSector()
	{
		return _bpb.bytesPerSector;
	}

	Object lockPath(Path path,AccessMode mode) throws FileInUseException
	{
		Object tag = new Object();
		lockPath(path, mode,tag);
		return tag;
	}
	
	void lockPath(Path path,AccessMode mode,Object opTag) throws FileInUseException
	{		
		int timeLeft = PATH_LOCK_TIMEOUT;
		while(timeLeft > 0)
		{
			synchronized (_openedFiles)
			{
				if(_isClosing)
					throw new FileInUseException("File system is closing");

				OpenFileInfo ofi = _openedFiles.get(path);
				if (ofi != null)
				{
					if (ofi.opTag != opTag && (
							mode == AccessMode.ReadWrite
									|| mode == AccessMode.Write
									|| ofi.accessMode == AccessMode.ReadWrite
									|| ofi.accessMode == AccessMode.Write))
					{
						Log.i("EDS", String.format("%s is busy waiting %d", path.getPathString(), timeLeft));
						long curTime = System.currentTimeMillis();
						try
						{
							_openedFiles.wait(timeLeft);
						}
						catch (InterruptedException e)
						{
							break;
						}
						timeLeft -= (int) (System.currentTimeMillis() - curTime);
					}
					else
					{
						ofi.refCount++;
						if (ofi.accessMode != mode && ofi.accessMode == AccessMode.Read)
							ofi.accessMode = mode;
						return;
					}
				}
				else
				{
					ofi = new OpenFileInfo(mode, opTag);
					_openedFiles.put(path, ofi);
					return;
				}
			}
		}
			

		throw new FileInUseException("File is in use " + path.getPathString());
	}

	void releasePathLock(Path path)
	{
		synchronized (_openedFiles)
		{
			OpenFileInfo ofi = _openedFiles.get(path);
			if (ofi != null)
			{
				ofi.refCount--;
                if(ofi.refCount<0)
                    throw new IllegalStateException(path + " ref count < 0");
				if (ofi.refCount == 0)
				{
					_openedFiles.remove(path);
					_openedFiles.notifyAll();
				}
			}
		}
		
	}

	DirWriter getDirWriter(FatPath targetPath,Object opTag) throws IOException
	{	
		lockPath(targetPath, AccessMode.ReadWrite,opTag);
		try
		{
			return getDirWriterNoLock(targetPath,opTag);
		}
		catch(IOException e)
		{
			releasePathLock(targetPath);
			throw e;
		}
	}
	
	DirWriter getDirWriterNoLock(FatPath targetPath,Object opTag) throws IOException
	{	
		if (targetPath.getPathUtil().isEmpty())
			return getRootDirOutputStream();	
		DirEntry de = getCachedDirEntry(targetPath,opTag);
		if (de == null || de.isFile()) throw new FileNotFoundException();		
		return new DirOutputStream(new ClusterChainIO(de.startCluster, targetPath, -1, AccessMode.Write));
	}

	DirReader getDirReader(FatPath targetPath,Object opTag) throws IOException
	{
		lockPath(targetPath, AccessMode.Read,opTag);
		try
		{		
			return getDirReaderNoLock(targetPath,opTag);		
		}
		catch(IOException e)
		{
			releasePathLock(targetPath);
			throw e;
		}
	}
	
	DirReader getDirReaderNoLock(FatPath targetPath,Object opTag) throws IOException
	{
		if (targetPath.getPathUtil().isEmpty())
			return getRootDirInputStream();			
		DirEntry de = getCachedDirEntry(targetPath,opTag);
		if (de == null || de.isFile()) throw new FileNotFoundException("Path not found: " + targetPath.toString());		
		return new DirInputStream( new ClusterChainIO(de.startCluster, targetPath, -1, AccessMode.Read));		
	}
	
	
	public abstract class FatRecord implements FSRecord
	{
		@Override
		public Path getPath()
		{
			return _path;
		}

		@Override
		public String getName()
		{
			return _path.getPathUtil().getFileName();
		}

		@SuppressWarnings("unused")
        public Date getCreateDate() throws IOException
		{
			DirEntry entry = _path.getEntry();
			return entry!=null ? entry.createDateTime : new Date();
		}

		@Override
		public Date getLastModified() throws IOException
		{
			DirEntry entry = _path.getEntry();
			return entry!=null ? entry.lastModifiedDateTime : new Date();
		}

		@Override
		public void setLastModified(Date dt) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't update file %s: file system is opened in read only mode",_path.getPathString()));

			FatPath parentPath = (FatPath) _path.getParentPath();
			if(parentPath == null)
				throw new IOException("Can't update last modified time of the root directory");

			Object tag = lockPath(_path, AccessMode.Write);
			try
			{
				DirEntry entry = _path.getEntry(tag);
				if (entry == null)
					throw new IOException("setLastModified error: failed opening source path: " + _path);
				entry.lastModifiedDateTime = dt;
				entry.writeEntry(FatFS.this, parentPath, tag);
			}
			finally
			{
				releasePathLock(_path);
			}
		}

		@SuppressWarnings("unused")
        public Date getAccessDate() throws IOException
		{
			DirEntry entry = _path.getEntry();
			return entry!=null ? entry.lastAccessDate : new Date();
		}

		@Override
		public void rename(String newName) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't rename file %s: file system is opened in read only mode",_path.getPathString()));

			FatPath parentPath = (FatPath) _path.getParentPath();
			if(parentPath == null)
				throw new IOException("Can't rename root directory");

			Object tag = lockPath(parentPath,AccessMode.Write);
			try
			{
				DirEntry entry = _path.getEntry(tag);
				if (entry == null) throw new IOException("rename error: failed opening source path: " + _path);
				FatPath newPath = (FatPath) parentPath.combine(newName);
				DirEntry destEntry = newPath.getEntry(tag);
				if (destEntry != null && destEntry!=entry)
				{
					if(entry.isDir())
						throw new IOException("rename error: destination path already exists: " + newPath);
					else
						deleteEntry(destEntry,parentPath, tag);
				}
				if(entry.offset >= 0)
				{
					entry.deleteEntry(FatFS.this, parentPath,tag);
					entry.offset = -1;
					entry.dosName = null;
				}
				cacheDirEntry(_path, null);
				entry.name = newName;
				entry.writeEntry(FatFS.this, parentPath, tag);
				_path = newPath;
				cacheDirEntry(_path, entry);
			}
			finally
			{
				releasePathLock(parentPath);
			}
		}

		@Override
		public void moveTo(Directory newParent) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't rename file %s: file system is opened in read only mode",_path.getPathString()));

			FatPath parentPath = (FatPath) _path.getParentPath();
			if(parentPath == null)
				throw new IOException("Can't rename root directory");

			FatPath newParentPath = (FatPath) newParent.getPath();
			if(newParentPath.equals(parentPath))
				return;

			Object tag = lockPath(parentPath,AccessMode.Write);
			try
			{
				lockPath(newParentPath, AccessMode.Write,tag);
				try
				{
					DirEntry entry = _path.getEntry(tag);
					if (entry == null) throw new IOException("rename error: failed opening source path: " + _path);
					FatPath newPath = (FatPath) newParentPath.combine(getName());
					if (entry.isDir() && _path.getPathUtil().isParentDir(newParentPath.getPathUtil())) throw new IOException("rename error: can't move directory to it's subdirectory: " + _path);
					DirEntry destEntry = newPath.getEntry(tag);
					if (destEntry != null && destEntry!=entry)
					{
						if(entry.isDir())
							throw new IOException("rename error: destination path already exists: " + newPath);
						else
							deleteEntry(destEntry,parentPath, tag);
					}
					if(entry.offset >= 0)
					{
						entry.deleteEntry(FatFS.this, parentPath,tag);
						entry.offset = -1;
					}
					cacheDirEntry(_path, null);
					entry.writeEntry(FatFS.this, newParentPath, tag);
					_path = newPath;
					cacheDirEntry(_path, entry);
				}
				finally
				{
					releasePathLock(newParentPath);
				}
			}
			finally
			{
				releasePathLock(parentPath);
			}
		}

		protected FatRecord(FatPath path) throws IOException
		{
			_path = path;
		}
			
		protected FatPath _path;
	}
	
	class DirIterator implements Iterator<Path>
	{

		/**
		 * Returns <tt>true</tt> if the iteration has more elements. (In other
		 * words, returns <tt>true</tt> if <tt>next</tt> would return an element
		 * rather than throwing an exception.)
		 * 
		 * @return <tt>true</tt> if the iterator has more elements.
		 */
		@Override
		public boolean hasNext()
		{
			return _next != null;
		}

		/**
		 * Returns the next element in the iteration.
		 * 
		 * @return the next element in the iteration.
		 * @throws java.util.NoSuchElementException
		 *             iteration has no more elements.
		 */
		@Override
		public Path next()
		{
			if (_next == null) throw new NoSuchElementException();

			FatPath res;
			try
			{
				res = (FatPath) _path.combine(_next.name);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);				
			}

            int li = logAquiring("dc");
            try
            {
                synchronized (_dirEntriesCache)
                {
                    logAquired(li);
                    if (!_dirEntriesCache.containsKey(res))
                        cacheDirEntry(res, _next);
                }
            }
            finally
            {
                logReleased(li);
            }

			setNext();
			return res;
		}

		/**
		 * Removes from the underlying collection the last element returned by the
		 * iterator (optional operation). This method can be called only once per
		 * call to <tt>next</tt>. The behavior of an iterator is unspecified if the
		 * underlying collection is modified while the iteration is in progress in
		 * any way other than by calling this method.
		 * 
		 * @throws UnsupportedOperationException
		 *             if the <tt>remove</tt> operation is not supported by this
		 *             Iterator.
		 * @throws IllegalStateException
		 *             if the <tt>next</tt> method has not yet been called, or the
		 *             <tt>remove</tt> method has already been called after the last
		 *             call to the <tt>next</tt> method.
		 */
		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		void reset(FatPath path, DirReader dirStream)
		{
			_dirStream = dirStream;
			setNext();
			_path = path;
		}

		DirEntry nextDirEntry() throws IOException, NoSuchElementException
		{
			if (_next == null) throw new NoSuchElementException();
			DirEntry res = _next;
			_next = DirEntry.readEntry(_dirStream);
			return res;
		}

		private DirReader _dirStream;
		private DirEntry _next;
		private FatPath _path;
		
		private void setNext()
		{
			try
			{
				do
				{
					_next = DirEntry.readEntry(_dirStream);
				}
				while(_next!=null && (_next.name.equals(".") || _next.name.equals("..")));

			}
			catch (IOException e)
			{
				Logger.log(e);
				_next = null;
			}
		}
	}
		
	class FatDirectory extends FatRecord implements Directory
	{		

		public FatDirectory(FatPath path) throws IOException
		{
			super(path);
		}

		@Override
		public void delete() throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't delete directory %s: file system is opened in read only mode",_path.getPathString()));
			if (_path.isRootDirectory()) throw new IOException("Can't delete root directory");
			
			Object tag = lockPath(_path,AccessMode.Write);
			try
			{
				DirEntry entry = _path.getEntry(tag);
				if (entry == null) return;
				if (!entry.isDir()) throw new IOException("Specified path is not a directory: " + _path.getPathString());

				Directory.Contents dc = list(tag);
				try
				{
					for (Path rec : dc)
						if (!((FatPath)rec).getPathUtil().isSpecial()) throw new DirectoryIsNotEmptyException(_path.getPathString(),"Directory is not empty: " + _path.getPathString());
				}
				finally
				{
					dc.close();
				}
				deleteEntry(entry, (FatPath)_path.getParentPath(),tag);
				cacheDirEntry(_path, null);				
			}
			finally
			{
				releasePathLock(_path);
			}
		}

		@Override
		public Directory createDirectory(String name) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException("Can't create directory: file system is opened in read only mode");
			if(!isValidFileName(name))
				throw new IOException("Invalid file name: " + name);

			Object tag = lockPath(_path,AccessMode.Write);
			try
			{
				FatPath newPath = (FatPath) _path.combine(name);
				DirEntry entry = getCachedDirEntry(newPath, tag);
				//if (entry != null) throw new IOException("File record with the specified name already exists: " + _path.getPathString());
				if(entry == null)
					makeNewDir(_path, name,tag);
				return newPath.getDirectory();
			}
			finally
			{
				releasePathLock(_path);
			}
		}

		@Override
		public File createFile(String name) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException("Can't create directory: file system is opened in read only mode");
			if(!isValidFileName(name))
				throw new IOException("Invalid file name: " + name);

			Object tag = lockPath(_path,AccessMode.Write);
			try
			{
				FatPath newPath = (FatPath) _path.combine(name);
				DirEntry entry = newPath.getEntry(tag);
				if(entry != null && entry.isDir())
					throw new IOException("Can't create file: there is a directory with the same name.");
				else if(entry!=null && entry.isFile())
					deleteEntry(entry, _path, tag);
				makeNewFile(_path, name,tag);
				return newPath.getFile();
			}
			finally
			{
				releasePathLock(_path);
			}
		}

		@Override
		public Directory.Contents list() throws IOException
		{
			return list(new Object());
		}		
		
		@Override
		public long getTotalSpace() throws IOException
		{
			return _bpb.getTotalSectorsNumber()*_bpb.bytesPerSector;
		}

		@Override
		public long getFreeSpace() throws IOException
		{
			//long totalSpace = getTotalSpace();
			long freeSpace = 0;
			int bytesPerCluster = _bpb.sectorsPerCluster * _bpb.bytesPerSector;
			synchronized (_ioSyncer)
			{
				if (_input == null) throw new FileSystemClosedException();			
				for (int i = 2; i < _totalClusterNumber; i++)
				{
					int clusterIndex = (_clusterTable == null ? readNextClusterIndex(i) : _clusterTable[i]); 
					if (clusterIndex == 0)//clusterIndex >=0 && clusterIndex!=LAST_CLUSTER)
						freeSpace += bytesPerCluster;				
				}
			}
			return freeSpace;//totalSpace - usedSpace;
		}
		
		Directory.Contents list(Object opTag) throws IOException
		{
			final DirReader stream = getDirReader(_path,opTag);					
			return new Directory.Contents()
			{
				@Override
				public Iterator<Path> iterator()
				{
					DirIterator it = new DirIterator();
					it.reset(_path, stream);
					return it;
				}

				@Override
				public void close() throws IOException
				{
					stream.close();
				}	
			};
		}
	}

	class FatFile extends FatRecord implements File
	{

		public FatFile(FatPath path) throws IOException
		{
			super(path);

		}

		@Override
		public long getSize() throws IOException
		{
			DirEntry entry = _path.getEntry();
			if(entry == null || !entry.isFile())
				throw new FileNotFoundException("File not found: " + _path.getPathString());					
			return entry.fileSize;
		}

		@Override
		public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode)
		{
			return null;
		}

		@Override
		public void copyToOutputStream(OutputStream output, long offset, long count, ProgressInfo progressInfo) throws IOException
		{
			Util.copyFileToOutputStream(output, this, offset, count, progressInfo);
		}

		@Override
		public void copyFromInputStream(InputStream input, long offset, long count, ProgressInfo progressInfo) throws IOException
		{
			Util.copyFileFromInputStream(input, this, offset, count, progressInfo);
		}

		@Override
		public void delete() throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't delete file %s: file system is opened in read only mode",_path.getPathString()));
			Object tag = lockPath(_path,AccessMode.Write);
			try
			{
				delete(tag);
			}
			finally
			{
				releasePathLock(_path);
			}
			
		}

		@Override
		public RandomAccessInputStream getInputStream() throws IOException
		{
			return new RandomAccessInputStream(getRandomAccessIO(AccessMode.Read));
		}

		@Override
		public RandomAccessOutputStream getOutputStream() throws IOException
		{
			Object tag = lockPath(_path, AccessMode.ReadWrite);
			try
			{
				return new RandomAccessOutputStream(getRandomAccessIO(AccessMode.ReadWriteTruncate,tag));
				
			}
			catch(IOException e)
			{
				releasePathLock(_path);
				throw e;
			}		
		}

		@Override
		public FileIO getRandomAccessIO(AccessMode accessMode)
				throws IOException
		{			
			Object tag = lockPath(_path, accessMode);
			try
			{					
				return getRandomAccessIO(accessMode,tag);				
				
			}
			catch(IOException e)
			{
				releasePathLock(_path);
				throw e;
			}		
		}
		
		private FileIO getRandomAccessIO(AccessMode accessMode,Object tag)
				throws IOException
		{
			if(_readOnlyMode && accessMode!=AccessMode.Read)
				throw new IOException(String.format("Can't open file %s for writing: file system is opened in read only mode",_path.getPathString()));								
			DirEntry entry = _path.getEntry(tag);
			if(entry == null)
			{
				if(accessMode!=AccessMode.Read)
				{
					FatPath newFilePath = (FatPath) _path.getParentPath().getDirectory().createFile(getName()).getPath();
					entry = newFilePath.getEntry(tag);
				}
				else
					throw new FileNotFoundException("File not found: " + _path);
			}
			else if (entry.isDir()) 
				throw new FileNotFoundException("File name conflicts with directory name: " + _path);
			return new FileIO(FatFS.this,entry,_path,accessMode,tag);						
		}
		
		void delete(Object tag) throws IOException
		{
			if(_readOnlyMode)
				throw new IOException(String.format("Can't delete file %s: file system is opened in read only mode",_path.getPathString()));
			DirEntry entry = _path.getEntry(tag);
			if (entry == null) return;
			if (!entry.isFile()) throw new IOException("deleteFile error: specified path is not a file: " + _path.getPathString());
			deleteEntry(entry, (FatPath)_path.getParentPath(),tag);
			cacheDirEntry(_path, null);		
		}
	}
	
	class FatPath extends PathBase
	{

		public FatPath(String pathString)
		{
			super(FatFS.this);
			_pathString = pathString;
		}

		@Override
		public boolean exists() throws IOException
		{			
			return getPathUtil().isEmpty() || getEntry()!=null;
		}

		@Override
		public boolean isFile() throws IOException
		{
			DirEntry entry = getEntry();
			return entry!=null && entry.isFile();
		}

		@Override
		public boolean isDirectory() throws IOException
		{		
			if(getPathUtil().isEmpty())
				return true;
			DirEntry entry = getEntry();
			return entry!=null && entry.isDir();
		}

		@Override
		public Directory getDirectory() throws IOException
		{
			DirEntry entry = getEntry();
			if(entry!=null && !entry.isDir())
				throw new IOException(getPathString() + " is not a directory");
			return new FatDirectory(this);
		}

		@Override
		public File getFile() throws IOException
		{
			DirEntry entry = getEntry();
			if(entry!=null && !entry.isFile())
				throw new IOException(getPathString() + " is not a file");
			return new FatFile(this);
		}

		public DirEntry getEntry() throws IOException
		{
			return getCachedDirEntry(this,new Object());
		}
		
		public DirEntry getEntry(Object opTag) throws IOException
		{
			return getCachedDirEntry(this,opTag);
		}
		
		@Override
		public String getPathString()
		{
			return _pathString.length() == 0 ? "/" : _pathString;
		}
		
		private final String _pathString;					
	}

	protected static final long MAX_FILE_SIZE = 2L*Integer.MAX_VALUE - 1; 
	protected static final String RESERVED_SYMBOLS = "<>:\"/\\|?*";
	protected static final int LAST_CLUSTER = 0x0FFFFFFF;
	protected static final int MAX_DIR_ENTRIES_CACHE = 10000;
	
	protected RandomAccessIO _input;
	protected boolean _readOnlyMode, _isClosing;
	protected BPB _bpb;
	protected byte _clusterIndexSize;
	protected int _totalClusterNumber;
	protected int[] _clusterTable;
	protected final Map<Path, OpenFileInfo> _openedFiles = new HashMap<>();
	protected final Map<Path, DirEntry> _dirEntriesCache = new HashMap<>();
	protected final Object _ioSyncer = new Object();
	protected byte[] _emptyCluster;
	
	protected FatFS(RandomAccessIO input)
	{
		_input = input;
	}	
		
	protected void writeHeader() throws IOException
	{
		_input.seek(0);
		int cnt = _bpb.reservedSectors*_bpb.bytesPerSector + _bpb.rootDirEntries*32 + _bpb.sectorsPerFat*_bpb.numberOfFATs*_bpb.bytesPerSector;
		byte[] buf = new byte[512];
		for(int i=0;i<cnt;i+=512)
			_input.write(buf,0,buf.length);	
		_input.seek(0);
		_input.write(FAT_START,0,FAT_START.length);		
		_bpb.write(_input);	
	}
	
	protected void copySectors(int startSector,int destSector,int count) throws IOException
	{
		byte[] buf = new byte[_bpb.bytesPerSector];
		int startOffset = startSector*_bpb.bytesPerSector;
		int destOffset = destSector*_bpb.bytesPerSector;
		for(int i=0;i<count;i++)
		{
			_input.seek(startOffset + i*_bpb.bytesPerSector);
			Util.readBytes(_input,buf,buf.length);			
			_input.seek(destOffset + i*_bpb.bytesPerSector);
			_input.write(buf,0,buf.length);			
		}
	}
	
	protected void writeEmptyClusterTable() throws IOException
	{		
		_input.seek(getClusterIndexPosition(0));
		int bytesPerFat = _bpb.getSectorsPerFat()*_bpb.bytesPerSector;
		for (int i = 0; i < bytesPerFat; i++)
			_input.write(0);
		writeClusterIndex(0, _bpb.mediaType | 0x0FFFFF00);
		writeClusterIndex(1, LAST_CLUSTER);
	}
	
	protected void writeFatBackup() throws IOException
	{
		copySectors(_bpb.reservedSectors, _bpb.reservedSectors + _bpb.getSectorsPerFat(), _bpb.getSectorsPerFat());
	}

    protected int calcTotalClustersNumber()
	{
		//return bpb.sectorsPerFat * bpb.bytesPerSector * 8 / clusterIndexSize;
		int root_dir_sectors = _bpb.rootDirEntries * DirEntry.RECORD_SIZE / _bpb.bytesPerSector;
		long data_sectors = _bpb.getTotalSectorsNumber() - (_bpb.reservedSectors + _bpb.numberOfFATs * _bpb.getSectorsPerFat() + root_dir_sectors);
		return 2 + (int)(data_sectors / _bpb.sectorsPerCluster);
	}

    @SuppressWarnings("unused")
    protected long getDataRegionSize(long totalSize)
	{		
		int fatSize = _bpb.getSectorsPerFat()*_bpb.bytesPerSector;	
		return totalSize - fatSize * _bpb.numberOfFATs - _bpb.reservedSectors * _bpb.bytesPerSector - _bpb.rootDirEntries * DirEntry.RECORD_SIZE;
	}

	protected int calcSectorsPerCluster(long volumeSize)
	{
		return getOptimalClusterSize(volumeSize, SECTOR_SIZE);
	}

	protected short getReservedSectorsNumber()
	{
		return 2;
	}

	protected int getNumClusters(long volumeSize)
	{
		int bytesPerCluster = _bpb.sectorsPerCluster * _bpb.bytesPerSector;
		return (int) (volumeSize / bytesPerCluster);
	}

	protected void initBPB(long size)
	{
		long numSectors = size/SECTOR_SIZE;
		
		_bpb.bytesPerSector = SECTOR_SIZE;
		_bpb.sectorsPerCluster = calcSectorsPerCluster(size);
		_bpb.reservedSectors = getReservedSectorsNumber();
		_bpb.numberOfFATs = 2;
		_bpb.rootDirEntries = 512;
		
		_bpb.mediaType = 0xF8;

		//bpb.physicalDriveNumber = 0x80;
		_bpb.physicalDriveNumber = 0;
		_bpb.extendedBootSignature = 0x29;
		_bpb.volumeSerialNumber = 12345;
		_bpb.volumeLabel = new byte[] { 'E', 'D', 'S', ' ',' ', ' ', ' ', ' ', ' ', ' ', ' ' };

//		fatsecs = ft->num_sectors - (ft->size_root_dir + ft->sector_size - 1) / ft->sector_size - ft->reserved;
//		ft->cluster_count = (int) (((__int64) fatsecs * ft->sector_size) / (ft->cluster_size * ft->sector_size));
//		ft->fat_length = (((ft->cluster_count * 3 + 1) >> 1) + ft->sector_size - 1) / ft->sector_size;
		int rootDirSize = _bpb.rootDirEntries*32;
		
		long dataSectors = numSectors - (rootDirSize + _bpb.bytesPerSector - 1)/_bpb.bytesPerSector - _bpb.reservedSectors;
		int clusterCount = (int)((dataSectors * _bpb.bytesPerSector)/(_bpb.sectorsPerCluster*_bpb.bytesPerSector));
		_bpb.sectorsPerFat = (((clusterCount * 3 + 1) >> 1) + _bpb.bytesPerSector - 1)/_bpb.bytesPerSector;
		
		//clusterCount -= bpb.sectorsPerFat*bpb.numberOfFATs / bpb.sectorsPerCluster;
		if(numSectors > 65535)
		{
			_bpb.sectorsBig = numSectors;
			_bpb.totalSectorsNumber = 0;
		}
		else
		{
			_bpb.sectorsBig = 0;
			_bpb.totalSectorsNumber = (int)numSectors;
		}	
		
		Arrays.fill(_bpb.fileSystemLabel, (byte)' ');		
		byte[] label = BPB.FAT12_LABEL.getBytes();		
		System.arraycopy(label,0,_bpb.fileSystemLabel,0,label.length);		
		_bpb.calcParams();
	}

	protected void loadClusterTable() throws IOException
	{
		_totalClusterNumber = calcTotalClustersNumber();		
		_clusterTable = new int[_totalClusterNumber];
		for (int i = 0; i < _totalClusterNumber; i++)
			_clusterTable[i] = readNextClusterIndex(i);
	}
	
	protected void freeClusters(int startCluster) throws IOException
	{
		synchronized (_ioSyncer)
		{
			int ci = startCluster;
			while (ci > 0 && ci != LAST_CLUSTER)
			{
				int tci = ci;
				ci = getNextClusterIndex(ci);
				setNextClusterIndex(tci, 0,true);
			}			
		}		
	}

	protected void deleteEntry(DirEntry entry, FatPath basePath,Object opTag) throws IOException
	{
		lockPath(basePath,AccessMode.ReadWrite,opTag);
		try
		{
			freeClusters(entry.startCluster);
			entry.deleteEntry(this, basePath,opTag);
		}
		finally
		{
			releasePathLock(basePath);
		}
	}

	protected DirEntry makeNewEntry(boolean setCluster) throws IOException
	{
		DirEntry entry = new DirEntry();
		if (setCluster)
		{
			synchronized (_ioSyncer)
			{
				entry.startCluster = attachFreeCluster(0,true);
				zeroCluster(entry.startCluster);
			}
		}
		return entry;
	}

	protected DirEntry makeNewFile(FatPath parentPath, String name,Object opTag) throws IOException
	{
		DirEntry entry = makeNewEntry(false);
		entry.name = name;
		entry.setDir(false);
		entry.writeEntry(this, parentPath, opTag);
		updateModTime(parentPath, opTag);
		FatPath newPath = (FatPath) parentPath.combine(name);
		cacheDirEntry(newPath, entry);
		return entry;
	}

	protected DirEntry makeNewDir(FatPath parentPath, String name,Object opTag) throws IOException
	{
		DirEntry entry = makeNewEntry(true);
		entry.name = name;
		entry.setDir(true);		
		entry.writeEntry(this, parentPath,opTag);
		updateModTime(parentPath, opTag);
		FatPath newPath = (FatPath) parentPath.combine(name);
		cacheDirEntry(newPath, entry);

		DirWriter s = getDirWriter(newPath,opTag);
		try
		{
			DirEntry dotEntry = new DirEntry(0);
			dotEntry.name = ".";
			dotEntry.setDir(true);
			dotEntry.startCluster = entry.startCluster;
			dotEntry.writeEntry(new FileName("."),s);
	
			dotEntry = new DirEntry(DirEntry.RECORD_SIZE);
			dotEntry.name = "..";
			dotEntry.setDir(true);
			if (!parentPath.isRootDirectory())
			{
				DirEntry parentEntry = getCachedDirEntry(parentPath, opTag);
				if(parentEntry!=null)
					dotEntry.startCluster = parentEntry.startCluster;
			}
			dotEntry.writeEntry(new FileName(".."),s);
			s.write(0);
		}
		finally
		{
			s.close();
		}
		return entry;
	}

	private void updateModTime(FatPath path, Object tag) throws IOException
	{
		FatPath parentPath = (FatPath) path.getParentPath();
		if(parentPath!=null)
		{
			DirEntry entry = getDirEntry(path, tag);
			if(entry!=null)
			{
				entry.lastModifiedDateTime = new Date();
				entry.writeEntry(this, parentPath, tag);
			}
		}
	}


	protected int getClusterIndexPosition(int clusterIndex)
	{
		return _bpb.reservedSectors * _bpb.bytesPerSector + clusterIndex * _clusterIndexSize / 8;
	}

	protected int readNextClusterIndex(int clusterIndex) throws IOException
	{
		_input.seek(getClusterIndexPosition(clusterIndex));
		return 0;
	}

	protected int getNextClusterIndex(int clusterIndex) throws IOException
	{		
		return _clusterTable == null ? readNextClusterIndex(clusterIndex) : _clusterTable[clusterIndex];		
	}

	protected DirReader getRootDirInputStream() throws IOException
	{
		return null;
	}

	protected DirWriter getRootDirOutputStream() throws IOException
	{
		return null;
	}
	
	protected void cacheDirEntry(FatPath path,DirEntry entry)
	{
        int li = logAquiring("dc");
        try
        {
            synchronized (_dirEntriesCache)
            {
                logAquired(li);
                if (_dirEntriesCache.size() > MAX_DIR_ENTRIES_CACHE)
                    _dirEntriesCache.clear();
                _dirEntriesCache.put(path, entry);
            }
        }
        finally
        {
            logReleased(li);
        }
	}
	
	protected DirEntry getCachedDirEntry(FatPath path,Object opTag) throws IOException
	{
        int li = logAquiring("dc");
		try
		{
			synchronized (_dirEntriesCache)
			{
				logAquired(li);
				if (_dirEntriesCache.containsKey(path))
				{
					////DEBUG
					//DirEntry de = _dirEntriesCache.get(path);
					//Log.d("EDS", String.format("DirEntry %s found in cache for %s . ",de,path.getPathString()));
					//return de;
					return _dirEntriesCache.get(path);
				}
			}
		}
		finally
		{
			logReleased(li);
		}
		DirEntry res = getDirEntry(path, opTag);
		//Log.d("EDS", String.format("DirEntry %s not found in cache for %s and was created. ",res,path.getPathString()));
		cacheDirEntry(path, res);
		return res;
    }

	protected DirEntry getDirEntry(FatPath targetPath,Object opTag) throws IOException
	{
		String[] pathComponents = targetPath.getPathUtil().getComponents();
		if (pathComponents.length == 0) return null;

		DirEntry res = null;
		DirIterator it = new DirIterator();
		FatPath p = (FatPath) getRootPath();
		for (String dir : pathComponents)
		{
			DirReader dirStream;
			lockPath(p, AccessMode.Read,opTag);
			try
			{
				if (res == null)
					dirStream = getRootDirInputStream();
				else
				{
					if (res.isFile())
						return null;
					else if (res.name.equals("..") && res.startCluster == 0)
						dirStream = getRootDirInputStream();
					else
						dirStream = new DirInputStream(new ClusterChainIO(res.startCluster, p,-1,AccessMode.Read));
				}
			}
			catch(IOException e)
			{
				releasePathLock(p);
				throw e;
			}
			try
			{
				it.reset(p, dirStream);
				res = null;
				while (it.hasNext())
				{
					DirEntry entry = it.nextDirEntry();
					if (dir.equalsIgnoreCase(entry.name))
					{
						res = entry;
						break;
					}
				}
			}
			finally
			{
				dirStream.close();
			}

			if (res == null) return null;

			p = (FatPath) p.combine(dir);
		}
		return res;
	}

	protected ArrayList<Integer> loadClusterChain(int startClusterIndex) throws IOException
	{
		ArrayList<Integer> res = new ArrayList<>();
		int idx = startClusterIndex;
		synchronized (_ioSyncer)
		{
			if (_input == null) throw new FileSystemClosedException();

			try
			{
				while (idx > 0 && idx != LAST_CLUSTER)
				{
					res.add(idx);
					idx = getNextClusterIndex(idx);
				}
			}
			catch(ArrayIndexOutOfBoundsException ignored)
			{				
			}
		}		
		return res;
	}

	/*protected int getClusterIndexFromChainAt(int positionInChain, int startClusterIndex, boolean attachNew) throws IOException
	{
		int idx = startClusterIndex;
		for (int i = 0; i < positionInChain; i++)
		{
			int prev = idx;
			idx = getNextClusterIndex(idx);
			if (idx < 0 || idx == LAST_CLUSTER)
			{
				if (attachNew)
					idx = attachFreeCluster(prev,true);
				else
					throw new EOFException();
			}
		}
		return idx;
	}*/

	protected void writeClusterIndex(int clusterPosition, int clusterIndex) throws IOException
	{
		_input.seek(getClusterIndexPosition(clusterPosition));
	}

	protected void setNextClusterIndex(int clusterPosition, int clusterIndex,boolean commit) throws IOException
	{		
		if(commit) writeClusterIndex(clusterPosition, clusterIndex);
		if (_clusterTable != null) _clusterTable[clusterPosition] = clusterIndex;
		
	}

	protected int attachFreeCluster(int lastClusterIndex,boolean commit) throws IOException
	{
		synchronized (_ioSyncer)
		{
			if (_input == null) throw new FileSystemClosedException();
			int freeCluster = getFreeClusterIndex();
			if (lastClusterIndex > 0 && lastClusterIndex != LAST_CLUSTER) setNextClusterIndex(lastClusterIndex, freeCluster,commit);
			setNextClusterIndex(freeCluster, LAST_CLUSTER,commit);			
			return freeCluster;
		}
	}
	
	protected void zeroCluster(int clusterIndex) throws IOException
	{		
		_input.seek(_bpb.getClusterOffset(clusterIndex));
		_input.write(_emptyCluster,0,_emptyCluster.length);				
	}

	protected int getFreeClusterIndex() throws IOException
	{		
		for (int i = 2; i < _totalClusterNumber; i++)
		{
			if (getNextClusterIndex(i) == 0) return i;
		}

		throw new NoFreeSpaceLeftException();
	}	

	class RootDirReader extends InputStream implements DirReader
	{
		@Override
		public void seek(long position) throws IOException
		{
			bytesRead = (int)position;
			bufferOffset = bytesAvail = 0;
		}

		@Override
		public long getFilePointer()
		{
			return bytesRead + bufferOffset;
		}
		
		@Override
		public long length() throws IOException
		{
			return length;
		}

		@Override
		public int read() throws IOException
		{
			if (bufferOffset == bytesAvail) fillBuffer();
			if (bytesAvail <= 0) return -1;
			return (buffer[bufferOffset++] & 0xFF);
		}

		@Override
		public void close() throws IOException
		{
			releasePathLock(getRootPath());
		}

		RootDirReader(int lng,long startPosition)
		{
			length = lng;
			buffer = new byte[lng > 1024 ? 1024 : lng];
			_startPosition = startPosition;			
		}

		private int bytesRead;
		private int length;
		private byte[] buffer;
		private long _startPosition;
		private int bufferOffset;
		private int bytesAvail;

		private void fillBuffer() throws IOException
		{
			synchronized (_ioSyncer)
			{
				if (_input == null) throw new FileSystemClosedException();

				bytesRead += bytesAvail;
				_input.seek(_startPosition + bytesRead);
				bufferOffset = 0;
				bytesAvail = length - bytesRead;
				if (bytesAvail <= 0) return;
				if (bytesAvail > buffer.length) bytesAvail = buffer.length;
				Util.readBytes(_input,buffer,bytesAvail);
			}
		}

	}
	
	class ClusterChainIO implements RandomAccessIO
	{
		@Override
		public void seek(long position) throws IOException
		{
			if(position < 0) return;
			synchronized (_rwSync)
			{				
				if(_isBufferLoaded)
				{
					long dif = position - getBufferPosition();
					if(dif<0 || dif>=_bufferSize)
					{
						if(_isBufferDirty)
							writeBuffer();
						_isBufferLoaded = false;
					}
				}
				_currentStreamPosition = position;				
			}			
		}
		
		@Override
		public void setLength(long newLength) throws IOException
		{
            if(_mode == AccessMode.Read)
                throw new IOException("The file is opened in read only mode");
			if(newLength<0)
				throw new IllegalArgumentException();
			if(newLength > MAX_FILE_SIZE)
				throw new IOException("File size is too large for FAT.");
			synchronized (_rwSync)
			{			
				long curOffset = _currentStreamPosition;
				seek(newLength);						
				int clusterIndex = _currentStreamPosition == 0 ? -1 : getClusterIndexInChain();
				if(clusterIndex >= _clusterChain.size())				
					addMissingClusters(clusterIndex - _clusterChain.size() + 1);					
				else if(clusterIndex < _clusterChain.size() - 1)
					removeExcessClusters(clusterIndex);
				_lastCluster = clusterIndex < 0 ? LAST_CLUSTER : _clusterChain.get(clusterIndex);
				_maxStreamPosition =  _currentStreamPosition;
				_currentStreamPosition = curOffset > _maxStreamPosition ? _maxStreamPosition : curOffset;
			}
			
		}

		@Override
		public long getFilePointer() throws IOException
		{
			return _currentStreamPosition;
		}
		
		public void flush() throws IOException
		{
			synchronized (_rwSync)
			{
				if(_isBufferDirty)
					writeBuffer();
				commitAddedClusters();				
			}			
		}
		
		public void close() throws IOException
		{
			try
			{
				flush();
			}
			finally
			{
				releasePathLock(_path);
			}		
			
			//if(LOG_MORE)
			//	Log.d("FatFs",String.format("Closed file %s. Max size: %d.",_path.getPathString(),_maxStreamPosition));			
		}
		
		public void write(int data) throws IOException
		{			
			if(_mode == AccessMode.Read)
				throw new IOException("Writing disabled");			
			
			synchronized (_rwSync)
			{
				_oneByteBuf[0] = (byte) (data & 0xFF);
				write(_oneByteBuf, 0, 1);				
			}
		}
		
		public void write(byte[] b, int off, int len) throws IOException
		{
			if(_mode == AccessMode.Read)
				throw new IOException("Writing disabled");			
			if (len <= 0) return;							
			
			synchronized (_rwSync)
			{				
				if(_currentStreamPosition + len > MAX_FILE_SIZE)
					throw new IOException("File size is too large for FAT.");
				while(len>0)
				{
					if(!_isBufferLoaded)
						loadBuffer();
					int currentPositionInBuffer = getPositionInBuffer();
					int avail = _bufferSize - currentPositionInBuffer;					
					int written = Math.min(avail, len);
					System.arraycopy(b, off, _buffer, currentPositionInBuffer, written);
					_isBufferDirty = true;
					if(avail == written)
					{
						writeBuffer();
						_isBufferLoaded = false;
					}
					off += written;
					len -= written;
					_currentStreamPosition += written;
				}
				if(_currentStreamPosition>_maxStreamPosition)
					_maxStreamPosition = _currentStreamPosition;
			}
		}
		
		
		public int read() throws IOException
		{
            if(_mode == AccessMode.Write || _mode == AccessMode.WriteAppend)
                throw new IOException("The file is opened in write only mode");
			synchronized (_rwSync)
			{
				if (_currentStreamPosition >= _maxStreamPosition) return -1;
				return read(_oneByteBuf,0,1) == 1 ? (_oneByteBuf[0] & 0xFF) : -1;
			}			
		}
		
		public int read(byte[] b, int off, int len) throws IOException
		{
            if(_mode == AccessMode.Write || _mode == AccessMode.WriteAppend)
                throw new IOException("The file is opened in write only mode");
			if(len<=0)
				return 0;
			synchronized (_rwSync)
			{
				if(!_isBufferLoaded)
					loadBuffer();
				int currentPositionInBuffer = getPositionInBuffer();
				int avail = _bufferSize - currentPositionInBuffer;					
				int read = (int) Math.min(Math.min(avail, len), _maxStreamPosition - _currentStreamPosition );
				if(read<=0)
					return -1;
				System.arraycopy(_buffer, currentPositionInBuffer, b, off, read);				
				if(avail == read)
				{
					if(_isBufferDirty)
						writeBuffer();
					_isBufferLoaded = false;
				}
				_currentStreamPosition += read;
				//if(LOG_MORE)
				//Log.d("EDS ClusterChainIO",String.format("ClusterChainIO read: file=%s read %d bytes",_path.getPathString(),avail));
				return read;
			}
		}
		
		public long length() throws IOException
		{
			return _maxStreamPosition;
		}

		ClusterChainIO(int startClusterIndex, Path path,long currentSize, AccessMode mode) throws IOException
		{
			_mode = mode;			
			_bufferSize = _bpb.sectorsPerCluster * _bpb.bytesPerSector;
			_buffer = new byte[_bufferSize];	
			_clusterChain = loadClusterChain(startClusterIndex);
			_lastCluster = _clusterChain.isEmpty() ? LAST_CLUSTER : _clusterChain.get(_clusterChain.size() - 1);
			_maxStreamPosition = currentSize<0 ? _clusterChain.size()*_bufferSize : currentSize;
			_path = path;
			//if(LOG_MORE)
			//Log.d("FatFs",String.format("Opened file %s. Current size: %d.",_path.getPathString(),currentSize));
		}
		
		protected final ArrayList<Integer> _clusterChain;
		protected final ArrayList<Integer> _addedClusters = new ArrayList<>();
		protected final byte[] _oneByteBuf = new byte[1];
		protected long _currentStreamPosition,_maxStreamPosition;
		protected int _lastCluster;
		protected final int _bufferSize;
		protected final byte[] _buffer;
		protected boolean _isBufferLoaded, _isBufferDirty;
		protected final Path _path;
		protected final AccessMode _mode;	
		protected final Object _rwSync = new Object();

		protected void writeBuffer() throws IOException
		{			
			synchronized (_ioSyncer)
			{
				if (_input == null) throw new FileSystemClosedException();
				try
				{
					int numClusters = _clusterChain.size();
					int cluster;
					int clusterIndex = getClusterIndexInChain();
					if(clusterIndex < numClusters)
						cluster = _clusterChain.get(clusterIndex);
					else if(clusterIndex ==  numClusters)
						cluster = addCluster();
					else
					{
						addMissingClusters(clusterIndex - numClusters);
						cluster = addCluster();
					}											
					_input.seek(_bpb.getClusterOffset(cluster));
					_input.write(_buffer,0,_bufferSize);									
				}
				catch(NoFreeSpaceLeftException e)
				{
					_isBufferDirty = false;
					setLength(_clusterChain.size()*_bufferSize);
					throw e;
				}
				_isBufferDirty = false;				
			}
		}
		
		protected void loadBuffer() throws IOException
		{	
			synchronized (_ioSyncer)
			{				
				if (_input == null) throw new FileSystemClosedException();
				int cluster;
				int clusterIndex = getClusterIndexInChain();
				if(clusterIndex >=  _clusterChain.size())
					cluster = 0;
				else
					cluster = _clusterChain.get(clusterIndex);
				
				int read = 0;
				if(cluster != LAST_CLUSTER && cluster != 0)
				{					
					_input.seek(_bpb.getClusterOffset(cluster));
					read = Util.readBytes(_input,_buffer);
				}
				Arrays.fill(_buffer,read, _bufferSize, (byte)0);
				_isBufferLoaded = true;
			}

		}		
		
		private int getClusterIndexInChain()
		{
			return (int)(_currentStreamPosition/_bufferSize);
		}

		private int getPositionInBuffer()
		{
			return (int)(_currentStreamPosition % _bufferSize);
		}
		
		private long getBufferPosition()
		{
			return _currentStreamPosition - (_currentStreamPosition % _bufferSize);
		}
		
		private int addCluster() throws IOException
		{
			int prev = _clusterChain.isEmpty() ? 0 : _clusterChain.get(_clusterChain.size() - 1);
			int freeCluster = attachFreeCluster(prev, false);
			_clusterChain.add(freeCluster);
			_addedClusters.add(freeCluster);
			return freeCluster;
		}

		private void addMissingClusters(int numClusters) throws IOException
		{
			int prev = _clusterChain.isEmpty() ? 0 : _clusterChain.get(_clusterChain.size() - 1);
			for (int i = 0; i < numClusters; i++)
			{
				int freeCluster = attachFreeCluster(prev,false);
				zeroCluster(freeCluster);
				_clusterChain.add(freeCluster);
				_addedClusters.add(freeCluster);
			}
		}
		
		private void removeExcessClusters(int lastClusterIndex) throws IOException
		{
			synchronized (_ioSyncer)
			{						
				for(int i=_clusterChain.size()-1;i>lastClusterIndex;i--)
				{
					setNextClusterIndex(_clusterChain.get(i), 0,true);
					_clusterChain.remove(i);
				}					
			}
		}
		
		private void commitAddedClusters() throws IOException
		{
			if(_addedClusters.isEmpty())
				return;
			synchronized (_ioSyncer)
			{
				if(_input == null)
					throw new FileSystemClosedException();
				
				int numAddedClusters = _addedClusters.size();
				if(_lastCluster != LAST_CLUSTER)
					setNextClusterIndex(_lastCluster, _addedClusters.get(0), true);					
				for(int i=0;i<numAddedClusters - 1;i++)
					setNextClusterIndex(_addedClusters.get(i), _addedClusters.get(i + 1),true);
				setNextClusterIndex(_addedClusters.get(numAddedClusters - 1), LAST_CLUSTER,true);
				_lastCluster = _clusterChain.get(_clusterChain.size() - 1);
				_addedClusters.clear();
                _input.flush();
			}
		}
	}
	

	class RootDirWriter extends OutputStream implements DirWriter
	{

		@Override
		public void seek(long position) throws IOException
		{
			writeBuffer();
			_bytesWritten = (int)position;
			_bytesAvail = _bufferOffset = 0;

		}

		@Override
		public long getFilePointer()
		{
			return _bytesWritten + _bufferOffset;
		}
		
		@Override
		public long length() throws IOException
		{
			return _length;
		}

		@Override
		public void write(int oneByte) throws IOException
		{
			if (_bufferOffset >= _bytesAvail) writeBuffer();

			if (_bytesAvail <= 0) throw new EOFException();

			_buffer[_bufferOffset++] = (byte) oneByte;
		}

		@Override
		public void flush() throws IOException
		{
			writeBuffer();			
		}

		@Override
		public void close() throws IOException
		{
			try
			{
				flush();
			}
			finally
			{
				releasePathLock(getRootPath());
			}

		}

		RootDirWriter(int lng,long startPosition)
		{
			_length = lng;
			_buffer = new byte[lng > 1024 ? 1024 : lng];
			_startPosition = startPosition;			
		}

		private byte[] _buffer;
		private int _bufferOffset;
		private int _bytesAvail;
		private int _bytesWritten;
		private int _length;
		private long _startPosition;

		private void writeBuffer() throws IOException
		{
			synchronized (_ioSyncer)
			{
				if (_input == null) throw new FileSystemClosedException();
				_input.seek(_startPosition + _bytesWritten);
				_input.write(_buffer, 0, _bufferOffset);
			}
			_bytesWritten += _bufferOffset;
			_bufferOffset = 0;
			_bytesAvail = _length - _bytesWritten;
			if (_bytesAvail <= 0) return;
			if (_bytesAvail > _buffer.length) _bytesAvail = _buffer.length;
		}

		

	}

	
	private static byte[] FAT_START = new byte[] {(byte)0xeb,0x3c,(byte)0x90,(byte)0x4D ,(byte)0x53 ,(byte)0x44 ,(byte)0x4F ,(byte)0x53 ,(byte)0x35 ,(byte)0x2E,(byte)0x30 ,(byte)0x00 ,(byte)0x02 ,(byte)0x01};
	
	private final static int PATH_LOCK_TIMEOUT = 5000;	

}

class Fat12FS extends FatFS
{
	public Fat12FS(RandomAccessIO inputStream)
	{
		super(inputStream);
		_bpb = new BPB16();
		_clusterIndexSize = 12;
	}

	@Override
	protected DirReader getRootDirInputStream() throws IOException
	{
		return new RootDirReader(_bpb.rootDirEntries * 32,_bpb.bytesPerSector * (_bpb.reservedSectors + _bpb.sectorsPerFat * _bpb.numberOfFATs));		
	}

	@Override
	protected DirWriter getRootDirOutputStream() throws IOException
	{
		return new RootDirWriter(_bpb.rootDirEntries * 32,_bpb.bytesPerSector * (_bpb.reservedSectors + _bpb.sectorsPerFat * _bpb.numberOfFATs));		
	}

	@Override
	protected int readNextClusterIndex(int clusterIndex) throws IOException
	{
		super.readNextClusterIndex(clusterIndex);
		int byte_offset = (clusterIndex * _clusterIndexSize) % 8;

		int res = ((Util.readWordLE(_input) >> byte_offset) & 0xFFF);

		return (res == 0 || (res >= 0x002 && res <= 0xFEF)) ? res : LAST_CLUSTER;
	}

	@Override
	protected void writeClusterIndex(int clusterPosition, int clusterIndex) throws IOException
	{
		super.writeClusterIndex(clusterPosition, clusterIndex);
		if (clusterIndex == LAST_CLUSTER) clusterIndex = 0xFFF;
		int val = Util.readWordLE(_input);
		int byteOffset = (clusterPosition * _clusterIndexSize) % 8;
		if (byteOffset == 0)
			// val = clusterIndex | (val & 0xF00);
			val = clusterIndex | (val & 0xF000);
		else
			val = ((clusterIndex << 4) | (val & 0xF));
		super.writeClusterIndex(clusterPosition, clusterIndex);
		Util.writeWordLE(_input, (short) val);
	}

}

class Fat16FS extends FatFS
{
	public Fat16FS(RandomAccessIO inputStream)
	{
		super(inputStream);
		_bpb = new BPB16();
		_clusterIndexSize = 16;
	}
	
	@Override
	protected int calcSectorsPerCluster(long volumeSize)
	{
		return getOptimalClusterSize(volumeSize, SECTOR_SIZE);
	}
	
	@Override
	protected int getNumClusters(long volumeSize)
	{
		int numClusters = super.getNumClusters(volumeSize);
		return numClusters <= 4085 ? 4086 : numClusters;		
	}

	@Override
	protected int readNextClusterIndex(int clusterIndex) throws IOException
	{
		super.readNextClusterIndex(clusterIndex);
		int res = (Util.readWordLE(_input) & 0xFFFF);

		return (res == 0 || (res >= 0x0002 && res <= 0xFFEF)) ? res : LAST_CLUSTER;
	}

	@Override
	protected void writeClusterIndex(int clusterPosition, int clusterIndex) throws IOException
	{
		super.writeClusterIndex(clusterPosition, clusterIndex);
		if (clusterIndex == LAST_CLUSTER) clusterIndex = 0xFFFF;
		Util.writeWordLE(_input, (short) clusterIndex);
	}

	@Override
	protected DirReader getRootDirInputStream() throws IOException
	{
		return new RootDirReader(_bpb.rootDirEntries * 32,_bpb.bytesPerSector * (_bpb.reservedSectors + _bpb.sectorsPerFat * _bpb.numberOfFATs));		
	}

	@Override
	protected DirWriter getRootDirOutputStream() throws IOException
	{
		return new RootDirWriter(_bpb.rootDirEntries * 32,_bpb.bytesPerSector * (_bpb.reservedSectors + _bpb.sectorsPerFat * _bpb.numberOfFATs));		
	}

    @Override
	protected void initBPB(long size)
	{
		super.initBPB(size);
		
		int rootDirSize = 32*_bpb.rootDirEntries;
		long numSectors = size/SECTOR_SIZE;
		long dataSectors = numSectors - (rootDirSize + _bpb.bytesPerSector - 1)/_bpb.bytesPerSector - _bpb.reservedSectors;
		int clusterCount = (int)((dataSectors * _bpb.bytesPerSector)/(_bpb.sectorsPerCluster*_bpb.bytesPerSector));
		_bpb.sectorsPerFat = (clusterCount * 2  + _bpb.bytesPerSector - 1)/_bpb.bytesPerSector;
		
		byte[] label = BPB.FAT16_LABEL.getBytes();		
		System.arraycopy(label,0,_bpb.fileSystemLabel,0,label.length);
		_bpb.calcParams();
	}
}

class Fat32FS extends FatFS
{
	public Fat32FS(RandomAccessIO inputStream)
	{
		super(inputStream);
		_bpb = new BPB32();
		_clusterIndexSize = 32;
		fsInfo = new FSInfo((BPB32)_bpb);
	}
	
	
	
	@Override
	public void close(boolean force) throws IOException
	{
		synchronized(_ioSyncer)
		{
			try
			{
				if(_input!=null && !_readOnlyMode)
					fsInfo.write(_input);
			}
			catch(IOException e)
			{
				if(!force)
					throw e;
			}				
			super.close(force);
			
		}
	}
	

	protected FSInfo fsInfo;
	
	@Override
	protected void writeHeader() throws IOException
	{
		super.writeHeader();
		fsInfo.freeCount = calcTotalClustersNumber();
		fsInfo.write(_input);
		copySectors(0, 6, 3);
	}
	
	@Override
	protected void writeEmptyClusterTable() throws IOException
	{
		super.writeEmptyClusterTable();
		writeClusterIndex(2, LAST_CLUSTER);
	}
	
	@Override
	protected int getFreeClusterIndex() throws IOException
	{
		int start = fsInfo.lastAllocatedCluster >= 2 && fsInfo.lastAllocatedCluster<_totalClusterNumber ? fsInfo.lastAllocatedCluster : 2;
		for (int i = start; i < _totalClusterNumber; i++)		
			if (getNextClusterIndex(i) == 0)
			{
				fsInfo.lastAllocatedCluster = i;
				return i;		
			}
		
		for(int i = 2; i<start;i++)
			if (getNextClusterIndex(i) == 0)
			{
				fsInfo.lastAllocatedCluster = i;
				return i;
			}

		throw new NoFreeSpaceLeftException();
	}
	
	@Override
	protected int getNumClusters(long volumeSize)
	{
		int numClusters = super.getNumClusters(volumeSize);
		return numClusters <= 65525 ? 65526 : numClusters;		
	}

	@Override
	protected long getDataRegionSize(long totalSize)
	{		
		int fatSize = _bpb.getSectorsPerFat()*_bpb.bytesPerSector;	
		return totalSize - fatSize * _bpb.numberOfFATs - _bpb.reservedSectors * _bpb.bytesPerSector;
	}

	@Override
	protected int readNextClusterIndex(int clusterIndex) throws IOException
	{
		super.readNextClusterIndex(clusterIndex);
		int res = (int) (Util.readDoubleWordLE(_input) & 0x0FFFFFFF);
		return (res == 0 || (res >= 0x2 && res <= 0xFFFFFEF)) ? res : LAST_CLUSTER;
	}

	@Override
	protected void writeClusterIndex(int clusterPosition, int clusterIndex) throws IOException
	{
		super.writeClusterIndex(clusterPosition, clusterIndex);
		if (clusterIndex == LAST_CLUSTER) clusterIndex = 0x0FFFFFFF;
		Util.writeDoubleWordLE(_input, clusterIndex);
	}

	@Override
	protected DirReader getRootDirInputStream() throws IOException
	{		
		return new DirInputStream(new ClusterChainIO(((BPB32) _bpb).rootClusterNumber,getRootPath(),-1,AccessMode.Read));		
	}

	@Override
	protected DirWriter getRootDirOutputStream() throws IOException
	{
		return new DirOutputStream(new ClusterChainIO(((BPB32) _bpb).rootClusterNumber,getRootPath(),-1,AccessMode.Write));		
	}

    @Override
	protected int calcSectorsPerCluster(long volumeSize)
	{		
		return getOptimalClusterSize(volumeSize, SECTOR_SIZE);
//		for(short i=4;i<=8;i*=2)
//		{
//			long ts = bpb.bytesPerSector*i*2000000L;
//			if(ts>=volumeSize)
//				return i;
//		}		
//		for(short i=8;i<=32;i*=2)
//		{
//			long ts = (long)bpb.bytesPerSector*i*getMaxClustersNumbers();
//			if(ts>=volumeSize)
//				return i;
//		}
//		throw new IllegalArgumentException(String.format("Wrong volume size for fat16: %d", volumeSize));
	}

	@Override
	protected short getReservedSectorsNumber()
	{
		return 31;
	}

	@Override
	protected void initBPB(long size)
	{
		super.initBPB(size);	
		
		BPB32 b = (BPB32) _bpb;
		b.bytesPerSector = SECTOR_SIZE;
		b.sectorsPerCluster = calcSectorsPerCluster(size);			
		b.rootDirEntries = 0;
		b.sectorsPerFat = 0;
		
		long numSectors = size/SECTOR_SIZE;
		//Align data area for TrueCrypt
		b.reservedSectors = 32 - 1;			
		do
		{
			b.reservedSectors++;
			long dataSectors = numSectors - b.reservedSectors;
			int clusterCount = (int)((dataSectors * _bpb.bytesPerSector)/(_bpb.sectorsPerCluster*_bpb.bytesPerSector));
			b.sectorsPerFat32 = (clusterCount * 4  + _bpb.bytesPerSector - 1)/_bpb.bytesPerSector;
		}
		while(b.bytesPerSector == SECTOR_SIZE && (b.reservedSectors*b.bytesPerSector + b.sectorsPerFat32*b.numberOfFATs*b.bytesPerSector) % 4096 !=0);
		
		b.rootClusterNumber = 2;
		b.FSInfoSector = 1;
		b.bootSectorReservedCopySector = 6;
		
		byte[] label = BPB.FAT32_LABEL.getBytes();		
		System.arraycopy(label,0,_bpb.fileSystemLabel,0,label.length);		
		b.calcParams();
	}

}
