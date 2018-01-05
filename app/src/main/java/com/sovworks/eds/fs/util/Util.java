package com.sovworks.eds.fs.util;

import android.os.ParcelFileDescriptor;

import com.sovworks.eds.fs.DataInput;
import com.sovworks.eds.fs.DataOutput;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File.AccessMode;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.reactivex.functions.Cancellable;

public class Util
{
	public static long copyFileToOutputStream(OutputStream output, com.sovworks.eds.fs.File file, long offset, long count, com.sovworks.eds.fs.File.ProgressInfo pi) throws IOException
	{
		InputStream input;
		if (offset>0)
		{
			RandomAccessIO f = file.getRandomAccessIO(com.sovworks.eds.fs.File.AccessMode.Read);
			try
			{
				f.seek(offset);
				input = new RandomAccessInputStream(f);
			}
			catch (Throwable e)
			{
				f.close();
				throw new IOException(e);
			}
		} else
			input = file.getInputStream();
		try
		{
			return copyStream(input, output, count, pi);
		}
		finally
		{
			input.close();
		}
	}

	public static long copyFileFromInputStream(InputStream input, com.sovworks.eds.fs.File file, long offset, long count, com.sovworks.eds.fs.File.ProgressInfo pi) throws IOException
	{
		OutputStream output;
		if (offset>0)
		{
			RandomAccessIO f = file.getRandomAccessIO(AccessMode.Write);
			try
			{
				f.seek(offset);
				output = new RandomAccessOutputStream(f);
			}
			catch (Throwable e)
			{
				f.close();
				throw new IOException(e);
			}
		} else
			output = file.getOutputStream();
		try
		{
			return copyStream(input, output, count, pi);
		}
		finally
		{
			output.close();
		}
	}

	public static long copyStream(InputStream src, OutputStream dst, long count, com.sovworks.eds.fs.File.ProgressInfo pi) throws IOException
	{
		long limit = count <= 0 ? Long.MAX_VALUE : count;
		long bytesRead = 0;
		byte[] buf = new byte[4096];
		int n;
		while (bytesRead < limit && (n = src.read(buf, 0, (int) Math.min(buf.length, limit - bytesRead))) >= 0)
		{
			dst.write(buf, 0, n);
			bytesRead += n;
			if(pi!=null)
			{
				if(pi.isCancelled())
					break;
				pi.setProcessed(bytesRead);
			}

		}
		return bytesRead;
	}

	public static int getParcelFileDescriptorModeFromAccessMode(AccessMode mode)
	{
		switch (mode)
		{
			case Read:
				return ParcelFileDescriptor.MODE_READ_ONLY;
			case Write:
				return ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_CREATE;
			case WriteAppend:
				return ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_APPEND | ParcelFileDescriptor.MODE_CREATE;
			case ReadWriteTruncate:
				return ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_TRUNCATE  | ParcelFileDescriptor.MODE_CREATE;
			case ReadWrite:
			default:
				return ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
		}
	}

	public static AccessMode getAccessModeFromString(String mode)
	{
		boolean read = mode.contains("r");
		boolean write = mode.contains("w");
		if(read && write)
			return mode.contains("t") ? AccessMode.ReadWriteTruncate : AccessMode.ReadWrite;
		if(read)
			return AccessMode.Read;
		if(write)
			return mode.contains("a") ? AccessMode.WriteAppend : AccessMode.Write;
		throw new IllegalArgumentException("Unsupported mode: " + mode);
	}

	public static String getStringModeFromAccessMode(AccessMode mode)
	{
		switch (mode)
		{
			case Read:
				return "r";
			case Write:
				return "rw";
			case ReadWrite:
				return "rw";
			case WriteAppend:
				return "wa";
			case ReadWriteTruncate:
				return "rwt";
		}
		throw new IllegalArgumentException("Unsupported mode: " + mode);
	}

	public static String getCStringModeFromAccessMode(com.sovworks.eds.fs.File.AccessMode mode)
	{
		switch (mode)
		{
			case Read:
				return "r";
			case Write:
				return "w";
			case WriteAppend:
				return "a";
			case ReadWrite:
				return "r+";
			case ReadWriteTruncate:
				return "w+";
		}
		throw new IllegalArgumentException("Unsupported mode: " + mode);
	}
	
	public static RandomAccessIO appendFile(Path path) throws IOException
	{	
		long pos;
		if(path.exists())
		{
			 if(!path.isFile())
				 throw new IOException("getFileWriter error: path exists and it is not a file: " + path.getPathString());
			 pos = path.getFile().getSize();
		}		
		else
			pos = 0;
		
		RandomAccessIO res = path.getFile().getRandomAccessIO(AccessMode.ReadWrite);		
		res.seek(pos);
		return res;
	}

    public static String getNewFileName(Directory baseDir, String startName) throws IOException
    {
		String res = startName;
		Path testPath = PathUtil.buildPath(baseDir.getPath(), startName);
        if(testPath == null || !testPath.exists())
            return res;
        String baseName = StringPathUtil.getFileNameWithoutExtension(startName) + " ";
        String ext = StringPathUtil.getFileExtension(startName);
        if(ext.length() > 0)
            ext = "." + ext;
        int i = 1;
        do
        {
			res = baseName + (i++) + ext;
			testPath = PathUtil.buildPath(baseDir.getPath(), res);
        }
        while(testPath != null && testPath.exists());

        return res;
    }
	
	
	public static Path makePath(FileSystem fs,Object... els) throws IOException
	{
		Path cur = null;
		for(Object o: els)
		{
			if(cur == null)
				cur = o instanceof Path ? (Path)o : fs.getPath(o.toString());
			else
				cur = cur.combine(o.toString());
		}
		return cur;
	}	
	
	public static List<Path> listDir(Directory dir) throws IOException
	{
		ArrayList<Path> res = new ArrayList<>();
		Directory.Contents dc = dir.list();
		try
		{
			for(Path p: dc)
				res.add(p);
		}
		finally
		{
			dc.close();
		}
		return res;
	}
	
	public interface DirProcessor
	{
		boolean procPath(Path path) throws IOException;
	}
	
	public static void procDir(Directory dir,DirProcessor dirProc) throws IOException
	{
		Directory.Contents dc = dir.list();
		try
		{
			for(Path p: dc)
				if(!dirProc.procPath(p))
					break;
		}
		finally
		{
			dc.close();
		}
	}
	
	public static long copyStream(DataInput src, DataOutput dst, long length) throws IOException
	{
		long res = 0;
		byte[] buf = new byte[1024];
		for(int tmp;length<0 || res < length;)
		{
			tmp = src.read(buf, 0, length < 0 ? buf.length : (int)Math.min(buf.length, length - res ) );
			if(tmp>=0)
			{
				res+=tmp;
				dst.write(buf, 0, tmp);
			}
			else
				break;
		}
		return res;
	}
	
	public static ArrayList<Path> restorePaths(FileSystem fs,String... pathStrings) throws IOException
	{
		return restorePaths(fs, Arrays.asList(pathStrings));
	}
	
	public static ArrayList<Path> restorePaths(FileSystem fs,Collection<String> pathStrings) throws IOException
	{
		ArrayList<Path> tmp = new ArrayList<>();
		restorePaths(tmp, fs, pathStrings);
		return tmp;
	}
	
	public static void restorePaths(List<Path> pathsReceiver,FileSystem fs,Collection<String> pathStrings) throws IOException
	{		
		for(String s: pathStrings)					
			pathsReceiver.add(fs.getPath(s));		
	}
	
	public static ArrayList<String> storePaths(Path... paths)
	{
		return storePaths(Arrays.asList(paths));
	}
	
	public static ArrayList<String> storePaths(Collection< ? extends Path> paths)
	{
		ArrayList<String> res = new ArrayList<>();
		for(Path path: paths)				
			res.add(path.getPathString());
		return res;			
	}

	public static com.sovworks.eds.fs.File  copyFile(com.sovworks.eds.fs.File src, Directory dest, String fileName) throws IOException
	{
		com.sovworks.eds.fs.File dstFile = PathUtil.getFile(dest.getPath(), fileName);
		copyFile(src, dstFile);
		return dstFile;
	}

	public static void copyFile(com.sovworks.eds.fs.File src, com.sovworks.eds.fs.File dst) throws IOException
	{
		OutputStream out = dst.getOutputStream();
		try
		{
			src.copyToOutputStream(out, 0, 0, null);
		}
		finally
		{
			out.close();
		}
	}

	public static com.sovworks.eds.fs.File copyFile(com.sovworks.eds.fs.File src, Directory dest) throws IOException
	{
		return copyFile(src, dest, src.getName());
	}

	public static void copyFiles(Iterable<Path> paths, Directory directory) throws IOException
	{
		for(Path p: paths)
			copyFiles(p, directory);
	}

	/**
	 * This function will copy files or directories from one location to
	 * another. note that the source and the destination must be mutually
	 * exclusive. This function can not be used to copy a directory to a sub
	 * directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param src
	 *            -- Source path
	 * @param dest
	 *            -- Destination path
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static Path copyFiles(Path src, Directory dest) throws IOException
	{
		if (!src.exists())
			throw new IOException("copyFiles: Can not find source: " + src.getPathString());
		
		if (src.isDirectory())
		{
			Directory newDir = dest.createDirectory(src.getDirectory().getName());
			Directory.Contents dc = src.getDirectory().list();
			try
			{
				for(Path p: dc)							
					copyFiles(p, newDir);
			}
			finally
			{
				dc.close();
			}
			return newDir.getPath();
		}
		else if(src.isFile())
			return copyFile(src.getFile(), dest).getPath();
		return null;
	}
	
	/**
	 * This function will copy files or directories from one location to
	 * another. note that the source and the destination must be mutually
	 * exclusive. This function can not be used to copy a directory to a sub
	 * directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param src
	 *            -- A File object that represents the source for the copy
	 * @param dest
	 *            -- A File object that represents the destination for the copy.
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static void copyFiles(File src, File dest) throws IOException
	{
		// Check to ensure that the source is valid...
		if (!src.exists())
			throw new IOException("copyFiles: Cannot find source: "
					+ src.getAbsolutePath() + ".");
		else if (!src.canRead())
			throw new IOException("copyFiles: No right to source: "
					+ src.getAbsolutePath() + ".");
		// is this a directory copy?
		if (src.isDirectory())
		{
			if (!dest.exists())
				// if not we need to make it exist if possible (note this is
				// mkdirs not mkdir)
				if (!dest.mkdirs())
					throw new IOException(
							"copyFiles: Could not create direcotry: "
									+ dest.getAbsolutePath() + ".");
			// get a listing of files...
			final String list[] = src.list();
			// copy all the files in the list.
			for (final String element : list)
			{
				final File dest1 = new File(dest, element);
				final File src1 = new File(src, element);
				copyFiles(src1, dest1);
			}
		}
		else
		{
			// This was not a directory, so lets just copy the file
			FileInputStream fin = null;
			FileOutputStream fout = null;
			final byte[] buffer = new byte[4096]; // Buffer 4K at a time (you
													// can change this).
			int bytesRead;
			try
			{
				// open the files for input and output
				fin = new FileInputStream(src);
				fout = new FileOutputStream(dest);
				// while bytesRead indicates a successful read, lets write...
				while ((bytesRead = fin.read(buffer)) >= 0)
					fout.write(buffer, 0, bytesRead);
			}
			catch (final IOException e)
			{ // Error copying file...
				final IOException wrapper = new IOException(
						"copyFiles: Unable to copy file: "
								+ src.getAbsolutePath() + "to"
								+ dest.getAbsolutePath() + ".");
				wrapper.initCause(e);
				wrapper.setStackTrace(e.getStackTrace());
				throw wrapper;
			}
			finally
			{ // Ensure that the files are closed (if they were open).
				if (fin != null) fin.close();
				if (fout != null) fout.close();
			}
		}
	}
	
	/**
	 * Deletes files recursively
	 * 
	 * @param src
	 *            -- A File object that represents the file or directory to delete	 *            
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static void deleteFiles(File src) throws IOException
	{
		// Check to ensure that the source is valid...
		if (!src.exists())
			return;
		
		if (!src.canRead())
			throw new IOException("deleteFiles: No right to source: "
					+ src.getAbsolutePath() + ".");
		
		// is this a directory copy?
		if (src.isDirectory())
		{			
			// get a listing of files...
			final String list[] = src.list();
			// copy all the files in the list.
			for (final String element : list)
			{				
				final File src1 = new File(src, element);
				deleteFiles(src1);
			}			
		}
        //noinspection ResultOfMethodCallIgnored
        src.delete();
	}
	
	/**
	 * Deletes files recursively
	 * 
	 * @param path
	 *            -- Path to file or directory to delete            
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static void deleteFiles(Path path) throws IOException
	{			
		if(!path.exists())
			return;
		
		if (path.isDirectory())
		{	
			Directory dir = path.getDirectory();
			for(Path p: listDir	(dir))
				deleteFiles(p);			
			dir.delete();
		}
		else
			path.getFile().delete();
	}

	public static com.sovworks.eds.fs.File writeToFile(Path path, CharSequence content) throws IOException
	{
		return writeToFile(
				path.getParentPath().getDirectory(),
				PathUtil.getNameFromPath(path),
				content
		);
	}
	
	public static com.sovworks.eds.fs.File writeToFile(Directory dir, String fileName, CharSequence content) throws IOException
	{
		Path dstPath;
		try
		{
			dstPath = dir.getPath().combine(fileName);
		}
		catch (IOException e)
		{
			dstPath = null;
		}
		com.sovworks.eds.fs.File res = dstPath != null && dstPath.isFile() ? dstPath.getFile() : dir.createFile(fileName);
		writeToFile(res, content);
		return res;
	}

	public static void writeToFile(com.sovworks.eds.fs.File dst, CharSequence content) throws IOException
	{
		OutputStreamWriter w = new OutputStreamWriter(dst.getOutputStream());
		try
		{
			w.append(content);
		}
		finally
		{
			w.close();
		}
	}

	public static void writeToFile(String path, CharSequence content) throws IOException
	{
		OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(path));
		try
		{
			w.append(content);
		}
		finally
		{
			w.close();
		}
	}
	
	public static void writeAll(OutputStream out, CharSequence content) throws IOException
	{		
		OutputStreamWriter w = new OutputStreamWriter(out);
		try
		{
			w.append(content);
		}
		finally
		{
			w.flush();
		}
	}
	
	/**
	 * Reads the whole file to the string
	 * @param path - path to file
	 * @return - result string
	 * @throws FileNotFoundException - if specified file cannot be found
	 * @throws IOException - if internal io error occured
	 */
	public static String readFromFile(Path path) throws IOException
	{		
		return readFromFile(path.getFile());
	}

	public static String readFromFile(com.sovworks.eds.fs.File file) throws IOException
	{
		return readFromFile(file.getInputStream());
	}
	
	/**
	 * Reads the whole stream to the string
	 * @param input - input stream 
	 * @return - result string
	 * @throws IOException - if internal io error occured
	 */
	public static String readFromFile(InputStream input) throws IOException
	{
		StringBuilder sb = new StringBuilder();		
		InputStreamReader r = new InputStreamReader(input);
		try
		{		
			char[] buf = new char[1024];
			int b;		
			while((b = r.read(buf))>=0)
				sb.append(buf, 0, b);
			return sb.toString();		
		}
		finally
		{
			r.close();
		}
	}
	
	
	public static int unsignedByteToInt(byte b)
	{
		return b & 0xFF;
	}

	/**
	 * Convert the byte array to a long.
	 * 
	 * @param b
	 *            The byte array at least 4 bytes long
	 * @return The integer
	 */
	public static long bytesToLong(byte[] b)
	{
		return bytesToLong(b, 0);
	}

	public static long bytesToLong(byte[] b, int offset)
	{
		return (long) unsignedByteToInt(b[offset]) << 56
				| (long) unsignedByteToInt(b[offset + 1]) << 48
				| (long) unsignedByteToInt(b[offset + 2]) << 40
				| (long) unsignedByteToInt(b[offset + 3]) << 32
				| (long) unsignedByteToInt(b[offset + 4]) << 24
				| (long) unsignedByteToInt(b[offset + 5]) << 16
				| unsignedByteToInt(b[offset + 6]) << 8
				| unsignedByteToInt(b[offset + 7]);
	}

	/**
	 * Convert the byte array to a long.
	 * 
	 * @param b
	 *            The byte array at least 4 bytes long
	 * @return The integer
	 */
	public static long unsignedIntToLongLE(byte[] b)
	{
		return unsignedIntToLongLE(b, 0);
	}

	public static long unsignedIntToLongLE(byte[] b, int offset)
	{
		return unsignedByteToInt(b[offset])
				| (long) unsignedByteToInt(b[offset + 1]) << 8
				| (long) unsignedByteToInt(b[offset + 2]) << 16
				| (long) unsignedByteToInt(b[offset + 3]) << 24;
	}

	/**
	 * Convert the byte array to a int
	 * 
	 * @param b
	 *            byte array at least 2 bytes long
	 * @return The integer
	 */
	public static int unsignedShortToIntLE(byte[] b)
	{
		return unsignedShortToIntLE(b, 0);
	}

	public static int unsignedShortToIntLE(byte[] b, int offset)
	{
		return unsignedByteToInt(b[offset])
				| unsignedByteToInt(b[offset + 1]) << 8;
	}

	/**
	 * Reads specified number of bytes from the input stream
	 * 
	 * @param input
	 *            The input stream
	 * @param b
	 *            target array
	 * @param len
	 *            number of bytes to read
	 * @throws IOException
	 *             exception propagated from the input stream
	 * @return true if all the requested bytes were read
	 */
	public static int readBytes(InputStream input, byte[] b, int len)
			throws IOException
	{
		return readBytes(input, b, 0, len);
	}

	/**
	 * Reads specified number of bytes from the input stream
	 *
	 * @param input
	 *            The input stream
	 * @param b
	 *            target array
	 * @param count
	 *            number of bytes to read
	 * @throws IOException
	 *             exception propagated from the input stream
	 * @return true if all the requested bytes were read
	 */
	public static int readBytes(InputStream input, byte[] b, int offset, int count)
			throws IOException
	{
		int res = 0;
		for(int tmp;res < count;)
		{
			tmp = input.read(b, offset + res, count - res);
			if(tmp>=0)
				res+=tmp;
			else
				break;
		}
		return res;
	}
	
	public static int readBytes(InputStream input, byte[] b)
			throws IOException
	{
		return readBytes(input, b,b.length);
	}
	

	/**
	 * Skips specified number of bytes
	 * 
	 * @param input
	 *            The input stream
	 * @param num
	 *            number of bytes to skip
	 * @throws IOException
	 *             error in the input stream
	 */
	public static void skip(InputStream input, long num) throws IOException
	{
		int res = 0;
		while (res < num)
		{
			final long tmp = input.skip(num - res);
			if (tmp < 0) throw new IOException("Unexpected end of stream");
			res += tmp;
		}
	}

	/**
	 * Reads specified number of bytes from the input stream
	 * 
	 * @param input
	 *            The input stream
	 * @param b
	 *            target array
	 * @param len
	 *            number of bytes to read
	 * @throws IOException
	 *             exception propagated from the input stream
	 * @return true if all the requested bytes were read
	 */
	public static int readBytes(DataInput input, byte[] b, int len)
			throws IOException
	{
		int res = 0;
		for(int tmp;res < len;)
		{
			tmp = input.read(b, res, len - res);
			if(tmp>=0)
				res+=tmp;
			else
				break;
		}
		return res;
	}
	
	public static int readBytes(DataInput input, byte[] b)
			throws IOException
	{
		return readBytes(input, b,b.length);
	}

	public static short readUnsignedByte(RandomAccessIO input)
			throws IOException
	{
		return (short) input.read();
	}

	public static int readWordLE(RandomAccessIO input) throws IOException
	{
		final byte[] buf = new byte[2];
		if(readBytes(input, buf)!=buf.length)
			throw new EOFException();		
		return unsignedShortToIntLE(buf);
	}

	public static long readDoubleWordLE(RandomAccessIO input)
			throws IOException
	{
		final byte[] buf = new byte[4];
		if(readBytes(input, buf)!=buf.length)
			throw new EOFException();
		return unsignedIntToLongLE(buf);
	}
	
	public static void writeWordLE(RandomAccessIO input,short data) throws IOException
	{
		input.write(data & 0xFF);
		input.write(data >> 8);
	}

	public static void writeDoubleWordLE(RandomAccessIO input,int data) throws IOException
	{
		writeWordLE(input,(short)(data & 0xFFFF));
		writeWordLE(input,(short)(data >> 16));
	}

	public static String convertStreamToString(InputStream is,
			String encodingName) throws IOException
	{
		if (is != null)
		{
			final Writer writer = new StringWriter();

			final char[] buffer = new char[1024];
			try
			{
				final Reader reader = new BufferedReader(new InputStreamReader(
						is, encodingName));
				int n;
				while ((n = reader.read(buffer)) != -1)
					writer.write(buffer, 0, n);
			}
			finally
			{
				is.close();
			}
			return writer.toString();
		}
		else
			return "";
	}

	public static void shortToBytesLE(short val, byte[] res, int offset)
	{
		res[offset] = (byte) (val & 0xFF);
		res[offset + 1] = (byte) ((val >> 8) & 0xFF);
	}

	public static void intToBytesLE(int val, byte[] res, int offset)
	{
		shortToBytesLE((short) (val & 0xFFFF), res, offset);
		shortToBytesLE((short) ((val >> 16) & 0xFFFF), res, offset + 2);
	}

	//exfat_pread expects that the pread function reads all the requested bytes at once
	public static int pread(RandomAccessIO io, byte[] buf, int bufOffset, int count, long position) throws IOException
	{
		long cur = io.getFilePointer();
		io.seek(position);
		try
		{
			int res = 0;
			for (int tmp; res < count; )
			{
				tmp = io.read(buf, res, count - res);
				if (tmp > 0)
					res += tmp;
				else
					break;
			}
			//int res = io.read(buf, bufOffset, count);
			return res;
		}
		finally
		{
			io.seek(cur);
		}

	}

	public static int pwrite(RandomAccessIO io, byte[] buf, int bufOffset, int count, long position) throws IOException
	{
		long cur = io.getFilePointer();
		io.seek(position);
		try
		{
			io.write(buf, bufOffset, count);
			return count;
		}
		finally
		{
			io.seek(cur);
		}
	}

	public static class CancellableProgressInfo implements com.sovworks.eds.fs.File.ProgressInfo, Cancellable
	{

		@Override
		public void setProcessed(long num)
		{
		}

		@Override
		public boolean isCancelled()
		{
			return _isCancelled;
		}

		@Override
		public void cancel() throws Exception
		{
			_isCancelled = true;
		}

		private boolean _isCancelled;
	}

	public static long countFolderSize(Directory dir) throws IOException
	{
		long res = 0;
		Directory.Contents dc = dir.list();
		for(Path p: dc)
		{
			if(p.isFile())
				res += p.getFile().getSize();
			else
				res += countFolderSize(p.getDirectory());
		}
		return res;
	}
}