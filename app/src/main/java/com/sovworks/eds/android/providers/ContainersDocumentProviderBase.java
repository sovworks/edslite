package com.sovworks.eds.android.providers;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.Base64;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.SystemConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.KITKAT)
public abstract class ContainersDocumentProviderBase extends android.provider.DocumentsProvider
{
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Uri getUriFromLocation(Location location)
    {
        return DocumentsContract.buildTreeDocumentUri(
                ContainersDocumentProvider.AUTHORITY,
                getDocumentIdFromLocation(location)
        );
    }

    public static void notifyOpenedLocationsListChanged(Context context)
    {
        context.getContentResolver().notifyChange(
                DocumentsContract.buildRootsUri(ContainersDocumentProvider.AUTHORITY),
                null,
                false
        );
    }

    public static String getDocumentIdFromLocationUri(Uri uri)
    {
        return Base64.encodeToString(uri.toString().getBytes(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    public static String getDocumentIdFromLocation(Location location)
    {
        return getDocumentIdFromLocationUri(location.getLocationUri());
    }

    public static Uri getLocationUriFromDocumentId(String documentId)
    {
        byte[] locationUriBytes = Base64.decode(documentId, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        return Uri.parse(new String(locationUriBytes, Charset.defaultCharset()));
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException
    {
        checkProjection(projection, ALL_ROOT_COLUMNS);
        if(projection == null)
            projection = ALL_ROOT_COLUMNS;
        final MatrixCursor result =
                new MatrixCursor(projection);
        for(EDSLocation cont: getLocationsManager().getLoadedEDSLocations(true))
        {
            if(cont.isOpen())
                try
                {
                    addRootRow(result, cont);
                }
                catch (IOException e)
                {
                    Logger.log(e);
                }
        }
        return result;

    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException
    {
        checkProjection(projection, ALL_DOCUMENT_COLUMNS);
        if(projection == null)
            projection = ALL_DOCUMENT_COLUMNS;
        final MatrixCursor result =
                new MatrixCursor(projection);
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(documentId));
            addPathRow(result, loc,  loc.getCurrentPath());
            return result;
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Wrong document uri", e);
        }
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException
    {
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(documentId));
            Path path = loc.getCurrentPath();
            return path.isFile() ?
                    FileOpsService.getMimeTypeFromExtension(
                            getContext(),
                            new StringPathUtil(PathUtil.getNameFromPath(path)).getFileExtension()
                    ) :
                    DocumentsContract.Document.MIME_TYPE_DIR;
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Wrong document uri", e);
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException
    {
        checkProjection(projection, ALL_DOCUMENT_COLUMNS);
        if(projection == null)
            projection = ALL_DOCUMENT_COLUMNS;
        final MatrixCursor result =
                new MatrixCursor(projection);
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(parentDocumentId));
            Directory.Contents dc = loc.getCurrentPath().getDirectory().list();
            try
            {
                for(Path p: dc)
                    addPathRow(result, loc,  p);
                return result;
            }
            finally
            {
                dc.close();
            }
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Wrong folder uri", e);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException
    {
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(documentId));
            return MainContentProvider.getParcelFileDescriptor(this, loc.getLocationUri(), loc, mode, new Bundle());

        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException( "Wrong document uri", e);
        }

    }

    @Override
    public boolean onCreate()
    {
        SystemConfig.setInstance(new com.sovworks.eds.android.settings.SystemConfig(getContext()));
        return true;
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException
    {
        try
        {
            Path srcPath = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(sourceDocumentId)).
                    getCurrentPath();
            Location dstLocation = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(targetParentDocumentId));
            Directory dest = dstLocation.
                    getCurrentPath().
                    getDirectory();
            Location res = dstLocation.copy();
            if(srcPath.isDirectory())
                res.setCurrentPath(dest.createDirectory(srcPath.getDirectory().getName()).getPath());
            else if(srcPath.isFile())
                res.setCurrentPath(Util.copyFile(srcPath.getFile(), dest).getPath());
            Context context = getContext();
            if(context!=null)
                context.getContentResolver().notifyChange(getUriFromLocation(res), null);

            return getDocumentIdFromLocation(res);
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Copy failed", e);
        }
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException
    {
        try
        {
            Location srcLocation = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(sourceDocumentId));
            Path srcPath = srcLocation.
                    getCurrentPath();
            Location dstLocation = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(targetParentDocumentId));
            Directory dest = dstLocation.
                    getCurrentPath().
                    getDirectory();
            Location res = dstLocation.copy();
            if(srcPath.isDirectory())
            {
                String name = srcPath.getDirectory().getName();
                srcPath.getDirectory().moveTo(dest);
                res.setCurrentPath(dest.getPath().combine(name));
            }
            else if(srcPath.isFile())
            {
                String name = srcPath.getFile().getName();
                srcPath.getFile().moveTo(dest);
                res.setCurrentPath(dest.getPath().combine(name));
            }
            Context context = getContext();
            if(context!=null)
            {
                context.getContentResolver().notifyChange(getUriFromLocation(srcLocation), null);
                context.getContentResolver().notifyChange(getUriFromLocation(res), null);
            }

            return getDocumentIdFromLocation(res);
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Move failed", e);
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException
    {
        try
        {
            Location loc = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(documentId));
            Path srcPath = loc.getCurrentPath();
            if(srcPath.isFile())
                srcPath.getFile().delete();
            else if(srcPath.isDirectory())
                srcPath.getDirectory().delete();
            Context context = getContext();
            if(context!=null)
                context.getContentResolver().notifyChange(getUriFromLocation(loc), null);

        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Delete failed", e);
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException
    {
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(documentId)).copy();
            Path srcPath = loc.getCurrentPath();
            if(srcPath.isDirectory())
                srcPath.getDirectory().rename(displayName);
            else if(srcPath.isFile())
                srcPath.getFile().rename(displayName);
            Context context = getContext();
            if(context!=null)
                context.getContentResolver().notifyChange(getUriFromLocation(loc), null);
            loc.setCurrentPath(srcPath);
            if(context!=null)
                context.getContentResolver().notifyChange(getUriFromLocation(loc), null);
            return getDocumentIdFromLocation(loc);
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Rename failed", e);
        }
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException
    {
        try
        {
            Location res = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(parentDocumentId)).copy();
            Directory dest = res.getCurrentPath().getDirectory();
            if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType))
                res.setCurrentPath(dest.createDirectory(displayName).getPath());
            else
                res.setCurrentPath(dest.createFile(displayName).getPath());
            Context context = getContext();
            if(context!=null)
                context.getContentResolver().notifyChange(getUriFromLocation(res), null);
            return getDocumentIdFromLocation(res);
        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("Create failed", e);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId)
    {
        try
        {
            Path parentPath = getLocationsManager().
                getLocation(getLocationUriFromDocumentId(parentDocumentId)).getCurrentPath();
            Path testPath = getLocationsManager().
                    getLocation(getLocationUriFromDocumentId(documentId)).getCurrentPath();
            int maxParents = 0;
            while(testPath != null && !testPath.equals(parentPath) && maxParents++ < 1000)
                testPath = testPath.getParentPath();

            return testPath != null && maxParents < 1000;

        }
        catch (Exception e)
        {
            Logger.log(e);
            throw new IllegalArgumentException("isChildDocument failed", e);
        }
    }

    private static final String[] ALL_ROOT_COLUMNS = {
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_TITLE
    };

    private static final String[] ALL_DOCUMENT_COLUMNS = {
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_ICON,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_SUMMARY
    };

    private void checkProjection(String[] projection, String[] columns)
    {
        if(projection != null)
        {
            for(String col: projection)
                if(!Arrays.asList(columns).contains(col))
                    throw new IllegalArgumentException("Wrong projection column: " + col);
        }
    }

    private void addPathRow(MatrixCursor cur, Location loc, Path path) throws IOException
    {
        final MatrixCursor.RowBuilder row = cur.newRow();
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, PathUtil.getNameFromPath(path));
        Location tmp = loc.copy();
        tmp.setCurrentPath(path);
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getDocumentIdFromLocation(tmp));
        boolean ro = loc.isReadOnly();
        int flags = 0;
        if(!ro)
        {
            if(path.isFile())
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
            else if(path.isDirectory())
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_COPY |
                        DocumentsContract.Document.FLAG_SUPPORTS_MOVE;
        }
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Document.COLUMN_ICON, null);
        if(path.isFile())
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, path.getFile().getLastModified().getTime());
        else if(path.isDirectory())
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, path.getDirectory().getLastModified().getTime());
        else
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, path.isFile() ?
                FileOpsService.getMimeTypeFromExtension(getContext(), new StringPathUtil(PathUtil.getNameFromPath(path)).getFileExtension()) :
                        DocumentsContract.Document.MIME_TYPE_DIR
        );
        row.add(DocumentsContract.Document.COLUMN_SIZE, path.isFile() ? path.getFile().getSize() : null);
        row.add(DocumentsContract.Document.COLUMN_SUMMARY, null);
    }

    private void addRootRow(MatrixCursor cur, EDSLocation cont) throws IOException
    {
        Context context = getContext();
        if(context == null)
            return;
        final MatrixCursor.RowBuilder row = cur.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, cont.getId());
        row.add(DocumentsContract.Root.COLUMN_SUMMARY, context.getString(
                    R.string.container_info_summary,
                    Formatter.formatFileSize(context, cont.getFS().getRootPath().getDirectory().getFreeSpace()),
                    Formatter.formatFileSize(context, cont.getFS().getRootPath().getDirectory().getTotalSpace())
                )
        );

        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports
        // creating documents. FLAG_SUPPORTS_RECENTS means your application's most
        // recently used documents will show up in the "Recents" category.
        // FLAG_SUPPORTS_SEARCH allows users to search all documents the application
        // shares.
        int flags = DocumentsContract.Root.FLAG_SUPPORTS_SEARCH;
        if(!cont.isReadOnly())
            flags |= DocumentsContract.Root.FLAG_SUPPORTS_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            flags |= DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD;
        row.add(DocumentsContract.Root.COLUMN_FLAGS, flags);

        // COLUMN_TITLE is the root title (e.g. Gallery, Drive).
        row.add(DocumentsContract.Root.COLUMN_TITLE, cont.getTitle());
        // This document id cannot change once it's shared.
        Location tmp = cont.copy();
        tmp.setCurrentPath(cont.getFS().getRootPath());
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocumentIdFromLocation(tmp));

        // The child MIME types are used to filter the roots and only present to the
        //  user roots that contain the desired type somewhere in their file hierarchy.
        row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*");
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, cont.getFS().getRootPath().getDirectory().getFreeSpace());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            row.add(DocumentsContract.Root.COLUMN_CAPACITY_BYTES, cont.getFS().getRootPath().getDirectory().getTotalSpace());
        row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_lock_open);
    }

    protected LocationsManager getLocationsManager()
    {
        return LocationsManager.getLocationsManager(getContext(), true);
    }
}
