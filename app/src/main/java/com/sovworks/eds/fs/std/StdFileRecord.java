package com.sovworks.eds.fs.std;

import android.os.ParcelFileDescriptor;

import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StdFileRecord extends StdFsRecord implements com.sovworks.eds.fs.File
{
	public StdFileRecord(StdFsPath path) throws IOException
	{
		super(path);
		if(path.exists() && !path.getJavaFile().isFile())
			throw new IllegalArgumentException("StdFileRecord error: path must point to a file");		
	}
	
	@Override
	public long getSize() throws IOException
	{
		return _path.getJavaFile().length();		
	}

	@Override
	public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode) throws IOException
	{
		return ParcelFileDescriptor.open(_path.getJavaFile(), Util.getParcelFileDescriptorModeFromAccessMode(accessMode));
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
	public InputStream getInputStream() throws IOException
	{
		return new FileInputStream(_path.getJavaFile());
		//return new RandomAccessInputStream(getRandomAccessIO(AccessMode.Read));
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return new FileOutputStream(_path.getJavaFile());
	}

	@Override
	public RandomAccessIO getRandomAccessIO(AccessMode accessMode)
			throws IOException
	{
		return new StdFsFileIO(_path.getJavaFile(), accessMode);
	}


}