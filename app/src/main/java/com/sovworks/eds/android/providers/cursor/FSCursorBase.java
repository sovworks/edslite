package com.sovworks.eds.android.providers.cursor;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.AbstractCursor;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.drew.lang.annotations.NotNull;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.Location;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static com.sovworks.eds.android.providers.ContainersDocumentProviderBase.getDocumentIdFromLocation;

public abstract class FSCursorBase extends AbstractCursor
{
    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_NAME = OpenableColumns.DISPLAY_NAME;
    public static final String COLUMN_TITLE = MediaStore.MediaColumns.TITLE;
    public static final String COLUMN_SIZE = OpenableColumns.SIZE;
    public static final String COLUMN_LAST_MODIFIED = MediaStore.MediaColumns.DATE_MODIFIED;
    public static final String COLUMN_IS_FOLDER = "is_folder";
    public static final String COLUMN_PATH = "path";

    public FSCursorBase(Context context, Location location, @NotNull String[] projection, String selection, String[] selectionArgs, boolean listDir)
    {
        _context = context;
        _location = location;
        _selection = selection;
        _selectionArgs = selectionArgs;
        _projection = projection;
        _listDir = listDir;
    }

    @Override
    public int getCount()
    {
        int[] res = new int[1];
        getObservable().
                subscribeOn(Schedulers.io()).
                blockingForEach(info -> res[0]++);
        return res[0];
    }

    @Override
    public String[] getColumnNames()
    {
        return _projection;
    }

    @Override
    public String getString(int column)
    {
        return String.valueOf(getValueFromCurrentCPI(column));
    }

    @Override
    public short getShort(int column)
    {
        return (short) getValueFromCurrentCPI(column);
    }

    @Override
    public int getInt(int column)
    {
        return (int) getValueFromCurrentCPI(column);
    }

    @Override
    public long getLong(int column)
    {
        return (long)getValueFromCurrentCPI(column);
    }

    @Override
    public float getFloat(int column)
    {
        return (float) getValueFromCurrentCPI(column);
    }

    @Override
    public double getDouble(int column)
    {
        return (double) getValueFromCurrentCPI(column);
    }

    @Override
    public boolean isNull(int column)
    {
        return getValueFromCurrentCPI(column) == null;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition)
    {
        try
        {
            _current = getObservable().
                    elementAt(newPosition).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
        }
        catch (Exception e)
        {
            Logger.log(e);
            _current = null;
        }
        return _current != null;
    }

    final Location _location;
    final String _selection;
    final String[] _selectionArgs;
    private final String[] _projection;
    private final Context _context;
    final boolean _listDir;

    private Observable<CachedPathInfo> _request;
    private CachedPathInfo _current;

    private Observable<CachedPathInfo> getObservable()
    {
        synchronized (this)
        {
            if(_request == null)
                try
                {
                    _request = createObservable();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            return _request;
        }
    }

    protected abstract Observable<CachedPathInfo> createObservable() throws Exception;

    private Object getValueFromCurrentCPI(int column)
    {
        if(_current == null)
            return null;
        return getValueFromCachedPathInfo(_current, _projection[column]);
    }

    private Object getValueFromCachedPathInfo(CachedPathInfo cpi, String columnName)
    {
        switch (columnName)
        {
            case COLUMN_ID:
                return (long)cpi.getPath().getPathString().hashCode();
            case COLUMN_NAME: //equals to DocumentsContract.Document.COLUMN_DISPLAY_NAME
            case COLUMN_TITLE:
                return cpi.getName();
            case COLUMN_IS_FOLDER:
                return cpi.isDirectory();
            case COLUMN_LAST_MODIFIED:
                return cpi.getModificationDate().getTime();
            case COLUMN_SIZE:
                return cpi.getSize();
            case COLUMN_PATH:
                return cpi.getPath().getPathString();
            default:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    return getDocumentValue(cpi, columnName);

        }
        return null;
    }

    private Object getDocumentValue(CachedPathInfo cpi, String columnName)
    {
        switch (columnName)
        {
            case DocumentsContract.Document.COLUMN_DISPLAY_NAME:
                return cpi.getName();
            case DocumentsContract.Document.COLUMN_DOCUMENT_ID:
                Location tmp = _location.copy();
                tmp.setCurrentPath(cpi.getPath());
                return getDocumentIdFromLocation(tmp);
            case DocumentsContract.Document.COLUMN_FLAGS:
                return getDocumentFlags(cpi);
            //icon is null
            //case DocumentsContract.Document.COLUMN_ICON:
            case DocumentsContract.Document.COLUMN_LAST_MODIFIED:
                return cpi.getModificationDate().getTime();
            case DocumentsContract.Document.COLUMN_MIME_TYPE:
                return getDocumentMimeType(cpi);
            case DocumentsContract.Document.COLUMN_SIZE:
                return cpi.getSize();
            //summary is null
            //case DocumentsContract.Document.COLUMN_SUMMARY:
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private int getDocumentFlags(CachedPathInfo cpi)
    {
        boolean ro = _location.isReadOnly();
        int flags = 0;
        if(!ro)
        {
            if(cpi.isFile())
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
            else if(cpi.isDirectory())
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_COPY |
                        DocumentsContract.Document.FLAG_SUPPORTS_MOVE;
        }
        return flags;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getDocumentMimeType(CachedPathInfo cpi)
    {
        return cpi.isFile() ?
                FileOpsService.getMimeTypeFromExtension(_context, new StringPathUtil(cpi.getName()).getFileExtension()) :
                DocumentsContract.Document.MIME_TYPE_DIR;
    }


}
