package com.sovworks.eds.android.providers.cursor;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.AbstractCursor;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.format.Formatter;

import com.drew.lang.annotations.NotNull;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static com.sovworks.eds.android.providers.ContainersDocumentProviderBase.getDocumentIdFromLocation;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentRootsCursor extends AbstractCursor
{
    public DocumentRootsCursor(Context context, LocationsManager lm, @NotNull String[] projection)
    {
        _context = context;
        _lm = lm;
        _projection = projection;
        fillList();
    }

    @Override
    public int getCount()
    {
        return _locations.size();
    }

    @Override
    public String[] getColumnNames()
    {
        return _projection;
    }

    @Override
    public String getString(int column)
    {
        return String.valueOf(getValueFromCurrentLocation(column));
    }

    @Override
    public short getShort(int column)
    {
        return (short) getValueFromCurrentLocation(column);
    }

    @Override
    public int getInt(int column)
    {
        return (int) getValueFromCurrentLocation(column);
    }

    @Override
    public long getLong(int column)
    {
        return (long) getValueFromCurrentLocation(column);
    }

    @Override
    public float getFloat(int column)
    {
        return (float) getValueFromCurrentLocation(column);
    }

    @Override
    public double getDouble(int column)
    {
        return (double) getValueFromCurrentLocation(column);
    }

    @Override
    public boolean isNull(int column)
    {
        return getValueFromCurrentLocation(column) == null;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition)
    {
        _current = null;
        if(newPosition >=0 && newPosition < _locations.size())
            _current = getObservable(_locations.get(newPosition)).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
        return _current != null;
    }

    @Override
    public boolean requery()
    {
        fillList();
        _current = null;
        return super.requery();
    }

    private final LocationsManager _lm;
    private final String[] _projection;
    private final Context _context;
    private final List<EDSLocation> _locations = new ArrayList<>();

    private static class LocationInfo
    {
        Location location;
        long freeSpace, totalSpace;
        String title, documentId;

    }

    private Single<LocationInfo> _request;
    private LocationInfo _current;

    private void fillList()
    {
        _locations.clear();
        for(EDSLocation l: _lm.getLoadedEDSLocations(true))
            if(l.isOpen())
                _locations.add(l);
    }

    private Single<LocationInfo> getObservable(EDSLocation loc)
    {
        synchronized (this)
        {
            if(_request == null)
                try
                {
                    _request = createObservable(loc);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            return _request;
        }
    }

    private Single<LocationInfo> createObservable(EDSLocation loc) throws Exception
    {
        return Single.create(em -> {
            LocationInfo res = new LocationInfo();
            res.location = loc;
            try
            {
                res.freeSpace = loc.getFS().getRootPath().getDirectory().getFreeSpace();
            }
            catch (IOException e)
            {
                Logger.log(e);
            }
            try
            {
                res.totalSpace = loc.getFS().getRootPath().getDirectory().getTotalSpace();
            }
            catch (IOException e)
            {
                Logger.log(e);
            }
            res.title = loc.getTitle();
            Location tmp = loc.copy();
            tmp.setCurrentPath(loc.getFS().getRootPath());
            res.documentId = getDocumentIdFromLocation(tmp);
            em.onSuccess(res);
        });
    }

    private Object getValueFromCurrentLocation(int column)
    {
        if(_current == null)
            return null;
        return getValueFromCachedPathInfo(_current, _projection[column]);
    }

    private Object getValueFromCachedPathInfo(LocationInfo li, String columnName)
    {
        switch (columnName)
        {
            case DocumentsContract.Root.COLUMN_ROOT_ID:
                return li.location.getId();
            case DocumentsContract.Root.COLUMN_SUMMARY:
                return _context.getString(
                        R.string.container_info_summary,
                        Formatter.formatFileSize(_context, li.freeSpace),
                        Formatter.formatFileSize(_context, li.totalSpace)
                );
            case DocumentsContract.Root.COLUMN_FLAGS:
                return getFlags(li);
            case DocumentsContract.Root.COLUMN_TITLE:
                return li.title;
            case DocumentsContract.Root.COLUMN_DOCUMENT_ID:
                return li.documentId;
            case DocumentsContract.Root.COLUMN_MIME_TYPES:
                return "*/*";
            case DocumentsContract.Root.COLUMN_AVAILABLE_BYTES:
                return li.freeSpace;
            case DocumentsContract.Root.COLUMN_ICON:
                return R.drawable.ic_lock_open;
            default:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    return getMoreColumns(li, columnName);

        }
        return null;
    }

    private Object getMoreColumns(LocationInfo li, String columnName)
    {
        switch (columnName)
        {
            case DocumentsContract.Root.COLUMN_CAPACITY_BYTES:
                return li.totalSpace;
        }
        return null;
    }


    private int getFlags(LocationInfo li)
    {
        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports
        // creating documents. FLAG_SUPPORTS_RECENTS means your application's most
        // recently used documents will show up in the "Recents" category.
        // FLAG_SUPPORTS_SEARCH allows users to search all documents the application
        // shares.
        int flags = DocumentsContract.Root.FLAG_SUPPORTS_SEARCH;
        if(!li.location.isReadOnly())
            flags |= DocumentsContract.Root.FLAG_SUPPORTS_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            flags |= DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD;
        return flags;
    }
}
