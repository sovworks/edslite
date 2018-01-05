package com.sovworks.eds.android.providers;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Base64;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.providers.cursor.DocumentRootsCursor;
import com.sovworks.eds.android.providers.cursor.FSCursor;
import com.sovworks.eds.android.filemanager.tasks.LoadPathInfoObservable;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.SystemConfig;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

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
        return new DocumentRootsCursor(
                getContext(),
                getLocationsManager(),
                projection == null ? ALL_ROOT_COLUMNS : projection
        );
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException
    {
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(documentId));
            return new FSCursor(
                    getContext(),
                    loc,
                    projection == null ? ALL_DOCUMENT_COLUMNS : projection,
                    null,
                    null,
                    false
            );
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
            return LoadPathInfoObservable.create(loc).map(cpi -> cpi.isFile() ?
                    FileOpsService.getMimeTypeFromExtension(
                            getContext(),
                            new StringPathUtil(cpi.getName()).getFileExtension()
                    ) : DocumentsContract.Document.MIME_TYPE_DIR
            ).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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
        try
        {
            Location loc = getLocationsManager().getLocation(getLocationUriFromDocumentId(parentDocumentId));
            return new FSCursor(
                    getContext(),
                    loc,
                    projection == null ? ALL_DOCUMENT_COLUMNS : projection,
                    null,
                    null,
                    true
            );
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
            return Single.<ParcelFileDescriptor>create(s -> s.onSuccess(MainContentProvider.getParcelFileDescriptor(this, loc, mode, new Bundle()))).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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
            return Single.<String>create(em -> {
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

                em.onSuccess(getDocumentIdFromLocation(res));
            }).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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
            return Single.<String>create(em -> {
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

                em.onSuccess(getDocumentIdFromLocation(res));
            }).
                    subscribeOn(Schedulers.io()).
                    blockingGet();

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
            Completable.create(em -> {
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
                em.onComplete();
            }).
                    subscribeOn(Schedulers.io()).
                    blockingAwait();
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
            return Single.<String>create(em -> {
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
                em.onSuccess(getDocumentIdFromLocation(loc));
            }).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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
            return Single.<String>create(em -> {
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
                em.onSuccess(getDocumentIdFromLocation(res));
            }).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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
            return Single.<Boolean>create(em -> {
                Path parentPath = getLocationsManager().
                        getLocation(getLocationUriFromDocumentId(parentDocumentId)).getCurrentPath();
                Path testPath = getLocationsManager().
                        getLocation(getLocationUriFromDocumentId(documentId)).getCurrentPath();
                int maxParents = 0;
                while(testPath != null && !testPath.equals(parentPath) && maxParents++ < 1000)
                    testPath = testPath.getParentPath();

                em.onSuccess(testPath != null && maxParents < 1000);
            }).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
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

    protected LocationsManager getLocationsManager()
    {
        return LocationsManager.getLocationsManager(getContext(), true);
    }
}
