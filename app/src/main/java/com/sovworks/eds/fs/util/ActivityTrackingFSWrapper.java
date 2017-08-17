package com.sovworks.eds.fs.util;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ActivityTrackingFSWrapper extends FileSystemWrapper
{
	public interface ChangeListener
	{
		void beforeRemoval(com.sovworks.eds.fs.Path p) throws IOException;
		void afterRemoval(com.sovworks.eds.fs.Path p);
		void beforeModification(com.sovworks.eds.fs.Path p) throws IOException;
		void afterModification(com.sovworks.eds.fs.Path p);
	}

	public ActivityTrackingFSWrapper(FileSystem baseFs)
	{
		super(baseFs);
		_lastActivityTime = SystemClock.elapsedRealtime();
	}

	@Override
	public com.sovworks.eds.fs.Path getRootPath() throws IOException
	{
		return new Path(getBase().getRootPath());
	}

	@Override
	public com.sovworks.eds.fs.Path getPath(String pathString) throws IOException
	{
		return new Path(getBase().getPath(pathString));
	}

	public void setChangesListener(ChangeListener l)
	{
		_changesListener = l;
	}
	
	public long getLastActivityTime()
	{
		return _lastActivityTime;
	}		

	protected class Path extends PathWrapper
	{

		public Path(com.sovworks.eds.fs.Path path)
		{
			super(ActivityTrackingFSWrapper.this, path);
		}

		@Override
		public com.sovworks.eds.fs.File getFile() throws IOException
		{
			return new File(this, getBase().getFile());
		}

		@Override
		public com.sovworks.eds.fs.Directory getDirectory() throws IOException
		{
			return new Directory(this, getBase().getDirectory());
		}

		@Override
		protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
		{
			return ActivityTrackingFSWrapper.this.getPathFromBasePath(basePath);
		}
	}
	
	protected class File extends FileWrapper
	{
		public File(Path path,com.sovworks.eds.fs.File base)
		{
			super(path,base);
		}		
		
		@Override
		public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
		{
			com.sovworks.eds.fs.Path srcPath = getPath();
			beforeMove(this, newParent);
			super.moveTo(newParent);
			afterMove(srcPath, this);
		}

		@Override
		protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
		{
			return ActivityTrackingFSWrapper.this.getPathFromBasePath(basePath);
		}

		@Override
		public void delete() throws IOException
		{
			beforeDelete(this);
			super.delete();
			afterDelete(this);
		}
		
		@Override
		public InputStream getInputStream() throws IOException
		{
			final InputStream in = super.getInputStream();
			return new FilterInputStream(in)		
			{
				@Override
				public int read() throws IOException
				{	
					_lastActivityTime = SystemClock.elapsedRealtime();
					return in.read();			
				}

				@Override
				public int read(@NonNull byte[] b, int off, int len) throws IOException
				{	
					_lastActivityTime = SystemClock.elapsedRealtime();
					return in.read(b, off, len);			
				}
			};
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			final OutputStream out = super.getOutputStream();
			return new FilterOutputStream(out)
					{
						@Override
						public void write(int b) throws IOException
						{
                            _lastActivityTime = SystemClock.elapsedRealtime();
                            if(!_isChanged && _changesListener!=null)
                                _changesListener.beforeModification(getPath());
							out.write(b);
                            _isChanged = true;
						}
		
						@Override
						public void write(@NonNull byte[] b, int off, int len) throws IOException
						{
                            _lastActivityTime = SystemClock.elapsedRealtime();
                            if(!_isChanged && _changesListener!=null)
                                _changesListener.beforeModification(getPath());
							out.write(b, off, len);
							_isChanged = true;
						}		
						
						public void close() throws IOException
						{
							super.close();
							if(_changesListener!= null && _isChanged)
								_changesListener.afterModification(getPath());
						}
						
						private boolean _isChanged;
					};
		}

		@Override
		public RandomAccessIO getRandomAccessIO(AccessMode accessMode) throws IOException
		{
			return new ActivityTrackingFileIO(super.getRandomAccessIO(accessMode), getPath());
		}

	}
	
	protected class Directory extends DirectoryWrapper
	{

		public Directory(Path path,com.sovworks.eds.fs.Directory base)
		{
			super(path,base);			
		}

		@Override
		public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
		{
			com.sovworks.eds.fs.Path srcPath = getPath();
			beforeMove(this, newParent);
			super.moveTo(newParent);
			afterMove(srcPath, this);
		}

		@Override
		protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
		{
			return ActivityTrackingFSWrapper.this.getPathFromBasePath(basePath);
		}

		@Override
		public void delete() throws IOException
		{
			beforeDelete(this);
			super.delete();
			afterDelete(this);
		}

		@Override
		public com.sovworks.eds.fs.File createFile(String name) throws IOException
		{
			_lastActivityTime = SystemClock.elapsedRealtime();
			com.sovworks.eds.fs.File f = super.createFile(name);
			if(_changesListener!= null)
				_changesListener.afterModification(f.getPath());
			return f;
		}

		@Override
		public com.sovworks.eds.fs.Directory createDirectory(String name) throws IOException
		{
			_lastActivityTime = SystemClock.elapsedRealtime();
			com.sovworks.eds.fs.Directory f = super.createDirectory(name);
			if(_changesListener!= null)
				_changesListener.afterModification(f.getPath());
			return f;
		}

		@Override
		public Directory.Contents list() throws IOException
		{
			_lastActivityTime = SystemClock.elapsedRealtime();
			return super.list();
		}
		
	}

	protected class ActivityTrackingFileIO extends RandomAccessIOWrapper
	{
		public ActivityTrackingFileIO(RandomAccessIO base, com.sovworks.eds.fs.Path path) throws IOException
		{
			super(base);		
			_path = path;
		}		
		
		@Override
		public int read() throws IOException
		{	
			_lastActivityTime = SystemClock.elapsedRealtime();
			return super.read();			
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{	
			_lastActivityTime = SystemClock.elapsedRealtime();
			return super.read(b, off, len);			
		}

		@Override
		public void write(int b) throws IOException
		{
            _lastActivityTime = SystemClock.elapsedRealtime();
            if(!_isChanged && _changesListener!=null)
                _changesListener.beforeModification(_path);
            super.write(b);
            _isChanged = true;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException
		{			
			_lastActivityTime = SystemClock.elapsedRealtime();
            if(!_isChanged && _changesListener!=null)
                _changesListener.beforeModification(_path);
            super.write(b, off, len);
            _isChanged = true;
		}			
		
		@Override
		public void close() throws IOException
		{
			super.close();
            if(_changesListener!= null && _isChanged)
                _changesListener.afterModification(_path);
		}
	
		private final com.sovworks.eds.fs.Path _path;
		private boolean _isChanged;
	}
	
	protected long _lastActivityTime;

	protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
	{
		return basePath == null ? null : new Path(basePath);
	}
	
	private ChangeListener _changesListener;

	private void beforeMove(FSRecord srcRecord, com.sovworks.eds.fs.Directory newParent) throws IOException
	{
		_lastActivityTime = SystemClock.elapsedRealtime();
		if(_changesListener!=null)
		{
			_changesListener.beforeRemoval(srcRecord.getPath());
			com.sovworks.eds.fs.Path dst;
			try
			{
				dst = newParent.getPath().combine(srcRecord.getName());
			}
			catch (IOException e)
			{
				dst = null;
			}
			if(dst!=null)
				_changesListener.beforeModification(dst);
		}
	}

	private void afterMove(com.sovworks.eds.fs.Path srcPath, FSRecordWrapper srcRecord) throws IOException
	{
		if(_changesListener!= null)
		{
			_changesListener.afterRemoval(srcPath);
			_changesListener.afterModification(srcRecord.getPath());
		}
	}

	private void beforeDelete(FSRecord srcRecord) throws IOException
	{
		_lastActivityTime = SystemClock.elapsedRealtime();
		if(_changesListener!=null)
			_changesListener.beforeRemoval(srcRecord.getPath());
	}

	private void afterDelete(FSRecord srcRecord)
	{
		if(_changesListener!= null)
			_changesListener.afterRemoval(srcRecord.getPath());
	}

}
