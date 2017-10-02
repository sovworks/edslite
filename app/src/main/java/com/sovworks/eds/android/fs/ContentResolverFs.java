package com.sovworks.eds.android.fs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;

import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.PFDRandomAccessIO;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class ContentResolverFs implements FileSystem 
{	
	public static String getFileNameByUri(ContentResolver cr,Uri uri)
	{
	    String fileName = uri.getLastPathSegment();
		if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()))
            fileName = uri.getLastPathSegment();
        else
	    {      
	        Cursor cursor = cr.query(uri, null, null, null, null);
            if(cursor!=null)
            try
            {
                if (cursor.moveToFirst())
                    return getFileNameFromCursor(cursor);
            }
            finally
            {
                cursor.close();
            }
        }

	    return fileName;
	}

	public static String getFileNameFromCursor(Cursor cursor)
	{
		if (cursor.moveToFirst())
		{
			int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			if(columnIndex >= 0)
				return cursor.getString(columnIndex);
			else
			{
				columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);//Instead of "_data"
				if (columnIndex >= 0)
				{
					Uri filePathUri = Uri.parse(cursor.getString(columnIndex));
					return filePathUri.getLastPathSegment();
				}
			}
		}
		return null;
	}

	public static ArrayList<com.sovworks.eds.fs.Path> fromSendIntent(Intent intent, ContentResolver contentResolver)
	{
		ContentResolverFs fs = new ContentResolverFs(contentResolver);
		ArrayList<com.sovworks.eds.fs.Path> paths = new ArrayList<>();
		Bundle extras = intent.getExtras();
		if(extras.containsKey(Intent.EXTRA_STREAM))
		{
			if(Intent.ACTION_SEND.equals(intent.getAction()))		
				paths.add(fs.new Path( (Uri)extras.getParcelable(Intent.EXTRA_STREAM)));
			else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
			{
				ArrayList<Parcelable> s = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
                if(s!=null)
                    for(Parcelable uri: s)
                        paths.add(fs.new Path( (Uri)uri));
			}			
		}
		
		return paths;
	}	
	
	public ContentResolverFs(ContentResolver contentResolver)
	{
		_contentResolver = contentResolver;					
	}
	
	@Override
	public com.sovworks.eds.fs.Path getPath(String pathString) throws IOException
	{			
 		return new Path(Uri.parse(pathString));
	}
	
	@Override
	public com.sovworks.eds.fs.Path getRootPath()
	{
		return new Path(new Uri.Builder().build());
	}
	
	@Override
	public void close(boolean force) throws IOException 
	{
		
		
	}

	@Override
	public boolean isClosed()
	{
		return false;
	}

	public class Path implements com.sovworks.eds.fs.Path
	{	
		
		public Path(Uri uri)
		{
			_uri = uri;
			
		}		
		
		public Uri getUri()
		{
			return _uri;
		}
		
		public Date getLastModified() throws IOException
		{
            Cursor cursor = queryPath();
            if(cursor != null)
            {
                if(cursor.moveToFirst())
                try
                {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);
                    if(columnIndex>=0)
                        return new Date(cursor.getLong(columnIndex));
                }
                finally
                {
                    cursor.close();
                }
            }
			if(ContentResolver.SCHEME_FILE.equals(getUri().getScheme()))
			{
				java.io.File f = new java.io.File(getUri().getPath());
				return new Date(f.lastModified());
			}
			return new Date();
		}

		public void setLastModified(Date dt) throws IOException
		{
			ContentValues cv = new ContentValues();
			cv.put(MediaStore.Images.Media.DATE_MODIFIED, dt.getTime());
			_contentResolver.update(_uri, cv, null, null);
		}

		public Cursor queryPath()
		{
			return  _contentResolver.query(_uri, null, null, null, null);
		}
		
		@Override
		public boolean equals(Object other)
		{
            return this == other || (other instanceof Path && _uri.equals(((Path) other)._uri));
        }
		
		@Override
		public int hashCode()
		{
			return _uri.hashCode();
		}

		@Override
		public boolean exists() throws IOException
		{
            Cursor cursor = _contentResolver.query(_uri, null, null, null, null);
            if(cursor!=null)
                try
                {
                    return cursor.moveToFirst();
                }
                finally
                {
                    cursor.close();
                }

            if(ContentResolver.SCHEME_FILE.equalsIgnoreCase(_uri.getScheme()))
			{
				java.io.File f = new java.io.File(_uri.getPath());
				return f.exists();
			}
			return false;	        
		}
		
		@Override
		public boolean isFile() throws IOException
		{
			return exists() && !isDirectory();
		}

		@Override
		public boolean isDirectory() throws IOException
        {
            if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(_uri.getScheme()))
            {
                java.io.File f = new java.io.File(_uri.getPath());
                return f.isDirectory();
            }
            String mime = _contentResolver.getType(_uri);
            return mime != null && mime.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE + "/");
        }

		@Override
		public FileSystem getFileSystem()
		{
			return ContentResolverFs.this;
		}

		@Override
		public String getPathString()
		{
			return _uri.toString();
		}

		@Override
		public String getPathDesc()
		{
			return getFileNameByUri(_contentResolver, _uri);
		}

		@Override
		public boolean isRootDirectory() throws IOException
		{
			return false;
		}

		@Override
		public com.sovworks.eds.fs.Path getParentPath() throws IOException
		{
			String pathString = _uri.getPath();
			if(pathString == null)
				return null;
			StringPathUtil pu = new StringPathUtil(pathString).getParentPath();
			if(pu == null)
				return getRootPath();
			Uri.Builder ub = _uri.buildUpon();
			ub.path(pu.toString());			
			return new Path(ub.build());
		}

		@Override
		public com.sovworks.eds.fs.Path combine(String part) throws IOException
		{
			final Cursor cursor = queryPath();
			if(cursor == null)
				throw new IOException("Can't make path");
			final int columnIndex = cursor.getColumnIndex(BaseColumns._ID);
			while(cursor.moveToNext())
			{
				String name = getFileNameFromCursor(cursor);
				if(name != null && name.equals(part))
				{
					Uri.Builder ub = _uri.buildUpon();
					ub.appendPath(cursor.getString(columnIndex));
					return new Path(ub.build());
				}
			}
			throw new IOException("Can't make path");
		}

		@Override
		public com.sovworks.eds.fs.Directory getDirectory() throws IOException
		{
			return new Directory(this);
		}

		@Override
		public com.sovworks.eds.fs.File getFile() throws IOException
		{
			return new File(this);
		}

		@Override
		public int compareTo(@NonNull com.sovworks.eds.fs.Path another)
		{
			return _uri.compareTo(((Path)another)._uri);
		}

		private final Uri _uri;
	}

	class Directory implements com.sovworks.eds.fs.Directory
	{
		public Directory(Path path)
		{
			_path = path;

		}

		@Override
		public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public com.sovworks.eds.fs.Path getPath()
		{
			return _path;
		}

		@Override
		public String getName() throws IOException
		{
			return getFileNameByUri(_contentResolver, _path.getUri());
		}

		@Override
		public void rename(String newName) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getLastModified() throws IOException
		{
			return _path.getLastModified();
		}

		@Override
		public void setLastModified(Date dt) throws IOException
		{
			_path.setLastModified(dt);
		}

		@Override
		public void delete() throws IOException
		{
			_contentResolver.delete(_path.getUri(),null,null);
		}

		@Override
		public com.sovworks.eds.fs.Directory createDirectory(String name) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public com.sovworks.eds.fs.File createFile(String name) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public com.sovworks.eds.fs.Directory.Contents list() throws IOException
		{
			final Cursor cursor = _path.queryPath();
			if(cursor == null)
				return null;
			final int columnIndex = cursor.getColumnIndex(BaseColumns._ID);
			cursor.moveToFirst();
			return new com.sovworks.eds.fs.Directory.Contents()
			{
				@Override
				public void close() throws IOException
				{
					cursor.close();
				}

				@Override
				public Iterator<com.sovworks.eds.fs.Path> iterator()
				{
					return new Iterator<com.sovworks.eds.fs.Path>()
					{
						@Override
						public void remove()
						{

						}

						@Override
						public com.sovworks.eds.fs.Path next()
						{
							Uri.Builder ub = _path.getUri().buildUpon();
							ub.appendPath(cursor.getString(columnIndex));
							Path path = new Path(ub.build());
							hasNext = cursor.moveToNext();
							return path;
						}

						@Override
						public boolean hasNext()
						{
							return hasNext;
						}

						private boolean hasNext = columnIndex>=0 && cursor.moveToFirst();
					};
				}
			};

		}

		@Override
		public long getTotalSpace() throws IOException
		{
			return 0;
		}

		@Override
		public long getFreeSpace() throws IOException
		{
			return 0;
		}

		private final Path _path;
	}

	class File implements com.sovworks.eds.fs.File
	{
		public File(Path path)
		{
			_path = path;
		}

		@Override
		public com.sovworks.eds.fs.Path getPath()
		{
			return _path;
		}

		@Override
		public String getName() throws IOException
		{
			return getFileNameByUri(_contentResolver, _path.getUri());
		}

		@Override
		public void rename(String newName) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getLastModified() throws IOException
		{
			return _path.getLastModified();
		}

		@Override
		public void setLastModified(Date dt) throws IOException
		{
			_path.setLastModified(dt);
		}

		@Override
		public void delete() throws IOException
		{
			_contentResolver.delete(_path.getUri(),null,null);
		}

		@Override
		public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			if(!_path.isFile())
				throw new FileNotFoundException(_path.getPathString());
			return _contentResolver.openInputStream(_path.getUri());
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			if(!_path.isFile())
				throw new FileNotFoundException(_path.getPathString());
			return _contentResolver.openOutputStream(_path.getUri());
		}

		@Override
		public RandomAccessIO getRandomAccessIO(AccessMode accessMode)
				throws IOException
		{
			ParcelFileDescriptor pfd = getFileDescriptor(accessMode);
			if(pfd == null)
				throw new UnsupportedOperationException();
			return new PFDRandomAccessIO(pfd);
		}

		@Override
		public long getSize() throws IOException
		{
			Cursor cursor = _path.queryPath();
			if(cursor!=null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						int columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
						if(columnIndex>=0 && !cursor.isNull(columnIndex))
							return cursor.getLong(columnIndex);
					}
				}
				finally
				{
					cursor.close();
				}
			}
			if(ContentResolver.SCHEME_FILE.equals(_path.getUri().getScheme()))
			{
				java.io.File f = new java.io.File(_path.getUri().getPath());
				return f.length();
			}
			return 0;
		}

		@Override
		public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode) throws IOException
		{
			return _contentResolver.openFileDescriptor(_path.getUri(), com.sovworks.eds.fs.util.Util.getStringModeFromAccessMode(accessMode));
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

		private final Path _path;
	}

	private final ContentResolver _contentResolver;	
}


