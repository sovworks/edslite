package com.sovworks.eds.fs.util;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.util.IteratorConverter;

import java.io.IOException;
import java.util.Iterator;

public abstract class DirectoryWrapper extends FSRecordWrapper implements Directory
{
	public DirectoryWrapper(Path path,Directory base)
	{
		super(path, base);
	}

	@Override
	public long getTotalSpace() throws IOException
	{
		return getBase().getTotalSpace();
	}

	@Override
	public long getFreeSpace() throws IOException
	{
		return getBase().getFreeSpace();
	}

	@Override
	public Directory createDirectory(String name) throws IOException
	{
		Path basePath = getBase().createDirectory(name).getPath();
		return getPathFromBasePath(basePath).getDirectory();
	}

	@Override
	public File createFile(String name) throws IOException
	{
		Path basePath = getBase().createFile(name).getPath();
		return getPathFromBasePath(basePath).getFile();
	}

	@Override
	public Directory.Contents list() throws IOException
	{
		return new ContentsWrapper(getBase().list());
	}

	@Override
	public Directory getBase()
	{
		return (Directory) super.getBase();
	}

	protected class ContentsWrapper implements Directory.Contents
	{
		public ContentsWrapper(Directory.Contents base)
		{
			_base = base;
		}

		@Override
		public void close() throws IOException
		{
			_base.close();
		}

		@Override
		public Iterator<Path> iterator()
		{
			return new IteratorConverter<Path, Path>(_base.iterator())
			{
				@Override
				protected Path convert(Path src)
				{
					try
					{
						return getPathFromBasePath(src);
					}
					catch (IOException e)
					{
						Logger.log(e);
						return null;
					}
				}
			};

		}

		private final Directory.Contents _base;
	}
}
