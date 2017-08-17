package com.sovworks.eds.fs.util;

import android.os.ParcelFileDescriptor;

import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class FileWrapper extends FSRecordWrapper implements File
{
	public FileWrapper(Path path, File base)
	{
		super(path, base);
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return getBase().getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return getBase().getOutputStream();
	}

	@Override
	public RandomAccessIO getRandomAccessIO(AccessMode accessMode) throws IOException
	{
		return getBase().getRandomAccessIO(accessMode);
	}

	@Override
	public long getSize() throws IOException
	{
		return getBase().getSize();
	}

	@Override
	public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode) throws IOException
	{
		return getBase().getFileDescriptor(accessMode);
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
	public File getBase()
	{
		return (File) super.getBase();
	}
	
}
