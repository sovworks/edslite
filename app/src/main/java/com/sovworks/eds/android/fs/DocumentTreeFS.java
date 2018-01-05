package com.sovworks.eds.android.fs;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.PFDRandomAccessIO;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DocumentTreeFS implements FileSystem
{
    public DocumentTreeFS(Context context, Uri rootUri)
    {
        _context = context;
        _rootPath = new DocumentPath(
                DocumentsContract.buildDocumentUriUsingTree(
                        rootUri,
                        DocumentsContract.getTreeDocumentId(rootUri)
                )
        );
    }

    @Override
    public Path getPath(String pathString) throws IOException
    {
        if (pathString.isEmpty())
            return getRootPath();
        return getPath(Uri.parse(pathString));
    }

    public Path getPath(Uri uri) throws IOException
    {
        return new DocumentPath(uri);
    }

    @Override
    public Path getRootPath()
    {
        return _rootPath;
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

    public class File implements com.sovworks.eds.fs.File
    {
        public File(DocumentPath path)
        {
            _path = path;
        }

        @Override
        public Path getPath()
        {
            return _path;
        }

        @Override
        public String getName() throws IOException
        {
            return _path.getFileName();
        }

        @Override
        public void rename(String newName) throws IOException
        {
            _path = _path.rename(newName);
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
            _path.delete();
        }

        @Override
        public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
        {
            Path np = Util.copyFile(this, newParent).getPath();
            delete();
            _path = (DocumentPath) np;
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            if (!_path.isFile())
                throw new FileNotFoundException(_path.getPathString());
            return _context.getContentResolver().openInputStream(_path.getDocumentUri());
        }

        @Override
        public OutputStream getOutputStream() throws IOException
        {
            return _context.getContentResolver().openOutputStream(_path.getDocumentUri());
        }

        @Override
        public RandomAccessIO getRandomAccessIO(File.AccessMode accessMode) throws IOException
        {
            ParcelFileDescriptor pfd = getFileDescriptor(accessMode);
            if(pfd == null)
                throw new UnsupportedOperationException();
            return new PFDRandomAccessIO(pfd);
        }

        @Override
        public long getSize() throws IOException
        {
            return _path.getSize();
        }

        @Override
        public ParcelFileDescriptor getFileDescriptor(File.AccessMode accessMode) throws IOException
        {
           return _context.getContentResolver().openFileDescriptor(
                    _path.getDocumentUri(),
                    Util.getStringModeFromAccessMode(accessMode)
            );
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

        private DocumentPath _path;
    }

    public class Directory implements com.sovworks.eds.fs.Directory
    {
        public Directory(DocumentPath path)
        {
            _path = path;
        }

        @Override
        public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
        {
            if(PathUtil.isParentDirectory(_path, newParent.getPath()))
                throw new IOException("Can't move the folder to its sub-folder");
            Path np = Util.copyFiles(_path, newParent);
            Util.deleteFiles(_path);
            _path = (DocumentPath) np;
        }

        @Override
        public void rename(String newName) throws IOException
        {
            _path = _path.rename(newName);
        }

        @Override
        public Path getPath()
        {
            return _path;
        }

        @Override
        public String getName() throws IOException
        {
            return _path.getFileName();
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
            _path.delete();
        }


        @Override
        public com.sovworks.eds.fs.Directory createDirectory(String name) throws IOException
        {
            Uri uri = DocumentsContract.createDocument(
                    _context.getContentResolver(),
                    _path.getDocumentUri(),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    name
            );
            if(uri == null)
                throw new IOException("Failed creating folder");
            return new Directory(new DocumentPath(uri));
        }

        @Override
        public com.sovworks.eds.fs.File createFile(String name) throws IOException
        {
            String mimeType = FileOpsService.getMimeTypeFromExtension(_context, new StringPathUtil(name).getFileExtension());
            Uri uri = DocumentsContract.createDocument(
                    _context.getContentResolver(),
                    _path.getDocumentUri(),
                    mimeType, name);
            if(uri == null)
                throw new IOException("Failed creating file");
            return new File(new DocumentPath(uri));
        }

        @Override
        public com.sovworks.eds.fs.Directory.Contents list() throws IOException
        {
            final Uri uri = _path.getDocumentUri();
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                    DocumentsContract.getDocumentId(uri));
            final ContentResolver resolver = _context.getContentResolver();
            final Cursor cursor = resolver.query(
                    childrenUri,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID + "!=?",
                    new String[] { ".android_secure" },
                    null
            );
            return new com.sovworks.eds.fs.Directory.Contents()
            {
                @Override
                public void close() throws IOException
                {
                    if (cursor != null)
                        cursor.close();
                }

                @Override
                public Iterator<Path> iterator()
                {
                    return new Iterator<Path>()
                    {
                        @Override
                        public void remove()
                        {

                        }

                        @Override
                        public Path next()
                        {
                            if (cursor == null)
                                throw new NoSuchElementException();
                            final String documentId = cursor.getString(0);
                            final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                                    documentId);
                            _hasNext = cursor.moveToNext();
                            Path newPath = new DocumentPath(documentUri);
                            synchronized (_parentsCache)
                            {
                                _parentsCache.put(newPath, _path);
                            }
                            return newPath;
                        }

                        @Override
                        public boolean hasNext()
                        {
                            return _hasNext;
                        }

                        private boolean _hasNext = cursor != null && cursor.moveToFirst();
                    };
                }
            };

        }

        @Override
        public long getTotalSpace() throws IOException
        {
            return getFreeSpace();
        }

        @Override
        public long getFreeSpace() throws IOException
        {
            return _rootPath.getBytesAvailable();
        }

        private DocumentPath _path;
    }

    public class DocumentPath implements Path
    {
        public DocumentPath(Uri uri)
        {
            _documentUri = uri;
        }

        @Override
        public String getPathDesc()
        {
            return getFileName();
        }

        public Date getLastModified() throws IOException
        {
            return new Date(queryForLong(getDocumentUri(), DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0));
        }

        public void setLastModified(Date dt) throws IOException
        {
            final ContentResolver resolver = _context.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, dt.getTime());
            try
            {
                if (resolver.update(getDocumentUri(), cv, null, null) == 0)
                    throw new IOException("Failed setting last modified time");
            }
            catch (UnsupportedOperationException e)
            {
                throw new IOException("Failed setting last modified time", e);
            }
        }

        public long getBytesAvailable() throws IOException
        {
            return queryForLong(getDocumentUri(), DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, 0);
        }

        public long getSize() throws IOException
        {
            return queryForLong(getDocumentUri(), DocumentsContract.Document.COLUMN_SIZE, 0);
        }

        public void delete() throws IOException
        {
            if (!DocumentsContract.deleteDocument(_context.getContentResolver(), getDocumentUri()))
                throw new IOException("Delete failed");
        }

        public DocumentPath rename(String newName) throws IOException
        {
            try
            {
                Path p = getParentPath();
                if(p!=null)
                {
                    p = p.combine(newName);
                    if(p.exists())
                        p.getFile().delete();
                }
            }
            catch (IOException ignored) {}

            final Uri newUri = DocumentsContract.renameDocument(_context.getContentResolver(), getDocumentUri(), newName);
            if (newUri == null)
                throw new IOException("Rename failed");
            else
                return new DocumentPath(newUri);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;
            if (other instanceof DocumentPath)
            {
                DocumentPath otherFP = (DocumentPath) other;
                return _documentUri.equals(otherFP._documentUri);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return _documentUri.hashCode();
        }

        @Override
        public boolean exists() throws IOException
        {
            Cursor c = null;
            try
            {
                c = _context.getContentResolver().query(_documentUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);
                return c != null && c.getCount() > 0;
            }
            catch (Exception e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
            }
            finally
            {
                closeQuietly(c);
            }
            return false;
        }

        @Override
        public String toString()
        {
            return getPathString();
        }

        @Override
        public boolean isFile() throws IOException
        {
            try
            {
                final String type = getRawType();
                return !(DocumentsContract.Document.MIME_TYPE_DIR.equals(type) || TextUtils.isEmpty(type));
            }
            catch (Exception e)
            {
                return false;
            }
        }

        @Override
        public boolean isDirectory() throws IOException
        {
            try
            {
                return isRootDirectory() || DocumentsContract.Document.MIME_TYPE_DIR.equals(getRawType());
            }
            catch (Exception e)
            {
                return false;
            }
        }

        @Override
        public FileSystem getFileSystem()
        {
            return DocumentTreeFS.this;
        }

        @Override
        public String getPathString()
        {
            return getPathUri().toString();
        }

        public Uri getPathUri()
        {
            return _documentUri;
        }

        public String getFileName()
        {
            try
            {
                return queryForString(getDocumentUri(), DocumentsContract.Document.COLUMN_DISPLAY_NAME, "unknown");
            }
            catch (IOException e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
                return getPathString();
            }
        }

        @Override
        public Path getParentPath() throws IOException
        {
            return DocumentTreeFS.this.getParentPath(this);
        }

        @Override
        public boolean isRootDirectory() throws IOException
        {
            return _rootPath._documentUri.equals(_documentUri);
        }

        @Override
        public Path combine(String part) throws IOException
        {
            Uri childUri = resolveDocumentUri(part);
            if(childUri == null)
                throw new FileNotFoundException();
            Path newPath = new DocumentPath(childUri);
            synchronized (_parentsCache)
            {
                _parentsCache.put(newPath, this);
            }
            return newPath;
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

        public Uri getDocumentUri() throws IOException
        {
            return _documentUri;
        }

        @Override
        public int compareTo(@NonNull Path another)
        {
            return _documentUri.compareTo(((DocumentPath)another)._documentUri);
        }

        private Uri _documentUri;

        private class ChildUriReceiver implements ResultReceiver
        {
            public ChildUriReceiver(String childName)
            {
                _childName = childName;
            }

            @Override
            public boolean nextResult(Cursor c)
            {
                if (!c.isNull(1) && _childName.equals(c.getString(1)))
                {
                    final String documentId = c.getString(0);
                    _uri = DocumentsContract.buildDocumentUriUsingTree(_documentUri,
                            documentId);
                    return false;
                }
                return true;
            }

            public Uri getChildUri()
            {
                return _uri;
            }

            private final String _childName;
            private Uri _uri;
        }

        private synchronized Uri resolveDocumentUri(final String childName)
        {
            ChildUriReceiver rec = new ChildUriReceiver(childName);
            listChildren(rec,
                    _documentUri,
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            );
            return rec.getChildUri();
        }

        private void listChildren(ResultReceiver res, Uri uri, String... columns)
        {
            final ContentResolver resolver = _context.getContentResolver();
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                    DocumentsContract.getDocumentId(uri));
            Cursor c = null;
            try
            {
                c = resolver.query(childrenUri, columns, null, null, null);
                if (c != null)
                    while (c.moveToNext())
                    {
                        if (!res.nextResult(c))
                            break;
                    }
            }
            catch (Exception e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
            }
            finally
            {
                closeQuietly(c);
            }
        }

        private String getRawType() throws IOException
        {
            return queryForString(getDocumentUri(), DocumentsContract.Document.COLUMN_MIME_TYPE, null);
        }

        private long queryForLong(Uri uri, String column, long defaultValue)
        {
            final ContentResolver resolver = _context.getContentResolver();
            Cursor c = null;
            try
            {
                c = resolver.query(uri, new String[]{column}, null, null, null);
                if (c != null && c.moveToFirst() && !c.isNull(0))
                    return c.getLong(0);
                else
                    return defaultValue;
            }
            catch (Exception e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
                return defaultValue;
            }
            finally
            {
                closeQuietly(c);
            }
        }

        private String queryForString(Uri uri, String column, String defaultValue)
        {
            final ContentResolver resolver = _context.getContentResolver();
            Cursor c = null;
            try
            {
                c = resolver.query(uri, new String[]{column}, null, null, null);
                if (c != null && c.moveToFirst() && !c.isNull(0))
                {
                    return c.getString(0);
                } else
                {
                    return defaultValue;
                }
            }
            catch (Exception e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
                return defaultValue;
            }
            finally
            {
                closeQuietly(c);
            }
        }

    }

    private interface ResultReceiver
    {
        boolean nextResult(Cursor c);
    }

    private final Context _context;
    private final DocumentPath _rootPath;
    private final Map<Path, Path> _parentsCache = new HashMap<>();

    private Path getParentPath(Path path) throws IOException
    {
        if(path.isRootDirectory())
            return null;
        synchronized (_parentsCache)
        {
            Path parentPath = _parentsCache.get(path);
            if(parentPath == null)
            {
                parentPath = findParentPath(_rootPath, path);
                if(parentPath == null)
                    throw new IOException("Couldn't find parent path for " + path.getPathString());
                _parentsCache.put(path, parentPath);
            }
            return parentPath;
        }
    }

    private Path findParentPath(Path startSearchPath, Path targetPath) throws IOException
    {
        if(!startSearchPath.isDirectory())
            return null;
        com.sovworks.eds.fs.Directory.Contents dc = startSearchPath.getDirectory().list();
        try
        {
            for(Path p: dc)
            {
                if(p.equals(targetPath))
                    return startSearchPath;
                if(p.isDirectory())
                {
                    Path res = findParentPath(p, targetPath);
                    if(res!=null)
                        return res;
                }
            }
        }
        finally
        {
            dc.close();
        }
        return null;
    }

    private static void closeQuietly(AutoCloseable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (RuntimeException rethrown)
            {
                throw rethrown;
            }
            catch (Exception ignored)
            {
            }
        }
    }
}


