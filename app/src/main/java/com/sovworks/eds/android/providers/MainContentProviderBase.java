package com.sovworks.eds.android.providers;


import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.android.locations.PathsStore;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.FSRecord;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.settings.SystemConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class MainContentProviderBase extends ContentProvider
{
    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_NAME = OpenableColumns.DISPLAY_NAME;
    public static final String COLUMN_SIZE = OpenableColumns.SIZE;
    public static final String COLUMN_LAST_MODIFIED = MediaStore.MediaColumns.DATE_MODIFIED;
    public static final String COLUMN_IS_FOLDER = "is_folder";

    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_PATH = "path";

    public static final String MIME_TYPE_FILE_META = "vnd.android.cursor.item/file";
    public static final String MIME_TYPE_FOLDER_META = "vnd.android.cursor.dir/folder";
    public static final String MIME_TYPE_SELECTION = "vnd.android.cursor.dir/selection";

    public static final String OPTION_OFFSET = "offset";
    public static final String OPTION_NUM_BYTES = "num_bytes";

    public static boolean hasSelectionInClipboard(ClipboardManager clipboard)
    {
        if(!clipboard.hasPrimaryClip())
        {
            Logger.debug("hasSelectionInClipboard: clipboard doesn't have a primary clip");
            return false;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if(clip!=null)
        {
            if(GlobalConfig.isDebug())
                Logger.debug(String.format("hasSelectionInClipboard: clip = %s", clip));
            return clip.getItemCount() > 0 && MainContentProvider.isClipboardUri(clip.getItemAt(0).getUri());
        }

        Logger.debug("hasSelectionInClipboard: clip = null");
        return false;
    }
    public static boolean isClipboardUri(Uri providerUri)
    {
        return providerUri!=null &&
                MainContentProvider.AUTHORITY.equals(providerUri.getHost()) &&
                providerUri.getPathSegments().size() >= 2;
    }

    public static Location getLocationFromProviderUri(Context context, Uri providerUri)
    {
        try
        {
            return LocationsManager.getLocationsManager(context, true).
                    getLocation(getLocationUriFromProviderUri(providerUri));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Wrong location uri", e);
        }
    }

    public static Uri getLocationUriFromProviderUri(Uri providerUri)
    {
        String path = providerUri.getPath();
        if(path == null)
            return null;
        String[] parts = new StringPathUtil(path).getComponents();
        if(parts.length < 2)
            return null;
        String encodedLocationUri = parts[1];
        byte[] locationUriBytes = Base64.decode(encodedLocationUri, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        return Uri.parse(new String(locationUriBytes, Charset.defaultCharset()));
    }

    public static ParcelFileDescriptor getParcelFileDescriptor(final ContentProvider cp, Uri srcUri, final Location loc, String accessMode, Bundle opts)
    {
        try
        {
            File f = loc.getCurrentPath().getFile();
            File.AccessMode am = Util.getAccessModeFromString(accessMode);
            ParcelFileDescriptor fd = f.getFileDescriptor(am);
            if(fd!=null)
                return fd;
            /*if("r".equals(accessMode))
            {
                String mime = FileOpsService.getMimeTypeFromExtension(cp.getContext(), loc.getCurrentPath());
                // Start a new thread that pipes the stream data back to the caller.
                return cp.openPipeHelper(srcUri, mime, opts, f, "r".equals(accessMode) ? new PipeWriter() : new PipeReader());
            }*/

            Path parentPath = loc.getCurrentPath();
            try
            {
                parentPath = parentPath.getParentPath();
            }
            catch (IOException ignored){}
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                int mode = Util.getParcelFileDescriptorModeFromAccessMode(am);
                final Location tmpLocation = TempFilesMonitor.getTmpLocation(
                        loc,
                        parentPath,
                        cp.getContext(), UserSettings.getSettings(cp.getContext()).getWorkDir(),
                        false
                );
                File dst;
                if(mode != ParcelFileDescriptor.MODE_WRITE_ONLY && mode != ParcelFileDescriptor.MODE_TRUNCATE)
                    dst = Util.copyFile(f, tmpLocation.getCurrentPath().getDirectory(), f.getName());
                else
                {
                    Path p = PathUtil.buildPath(tmpLocation.getCurrentPath(), f.getName());
                    if (p!=null && p.isFile())
                        p.getFile().delete();
                    dst = tmpLocation.getCurrentPath().getDirectory().createFile(f.getName());
                }
                tmpLocation.setCurrentPath(dst.getPath());
                loc.setCurrentPath(parentPath);
                return "r".equals(accessMode) ?
                        ParcelFileDescriptor.open(
                            new java.io.File(dst.getPath().getPathString()),
                            Util.getParcelFileDescriptorModeFromAccessMode(am)
                        ) :
                        ParcelFileDescriptor.open(
                            new java.io.File(dst.getPath().getPathString()),
                            Util.getParcelFileDescriptorModeFromAccessMode(am),
                            new Handler(Looper.getMainLooper()),
                            new ParcelFileDescriptor.OnCloseListener()
                            {
                                @Override
                                public void onClose(IOException e)
                                {
                                    if (e != null)
                                        Logger.showAndLog(cp.getContext(), e);
                                    else
                                        FileOpsService.saveChangedFile(
                                                cp.getContext(),
                                                new SrcDstSingle(tmpLocation, loc)
                                        );
                                }
                            }
                        );
            }
            else
            {
                Location tmpLocation = TempFilesMonitor.getTmpLocation(
                        loc,
                        parentPath,
                        cp.getContext(), UserSettings.getSettings(cp.getContext()).getWorkDir(),
                        true
                );
                File dst = Util.copyFile(f, tmpLocation.getCurrentPath().getDirectory(), f.getName());
                loc.setCurrentPath(parentPath);
                TempFilesMonitor.getMonitor(cp.getContext()).addFileToMonitor(loc, dst.getPath());
                return ParcelFileDescriptor.open(
                        new java.io.File(dst.getPath().getPathString()),
                        Util.getParcelFileDescriptorModeFromAccessMode(am)
                );
            }
        }
        catch(IOException e)
        {
            Logger.log(e);
            throw new RuntimeException(e);
        }
    }

    public interface PathChecker
    {
        boolean checkPath(Path path);
    }

    public interface SearchFilter
    {
        String getName();
        PathChecker getChecker(Location location, String arg);
    }


    public static class SelectionBuilder
    {
        public void addCondition(String filterName, String arg)
        {
            if(_selectionBuilder.length() > 0)
                _selectionBuilder.append(' ');
            _selectionBuilder.append(filterName);
            _selectionArgs.add(arg);
        }

        public String getSelectionString()
        {
            return _selectionBuilder.toString();
        }

        public String[] getSelectionArgs()
        {
            String[] res = new String[_selectionArgs.size()];
            return _selectionArgs.toArray(res);
        }

        private final StringBuilder _selectionBuilder = new StringBuilder();
        private final ArrayList<String> _selectionArgs = new ArrayList<>();
    }

    public static class SelectionChecker
    {
        public SelectionChecker(Location location, String selectionString, String[] selectionArgs)
        {
            _location = location;
            if(selectionString!=null)
            {
                String[] filtNames = selectionString.split(" ");
                int i = 0;
                for (String filtName : filtNames)
                {
                    if(selectionArgs==null || i>=selectionArgs.length)
                        break;
                    PathChecker f = getFilter(filtName, selectionArgs[i++]);
                    if (f == null)
                        throw new IllegalArgumentException("Unsupported search filter: " + filtName);
                    else
                        _filters.add(f);
                }
            }
        }

        public boolean checkPath(Path path)
        {
            for(PathChecker pc: _filters)
                if(!pc.checkPath(path))
                    return false;
            return true;
        }

        protected final Location _location;
        protected final List<PathChecker> _filters = new ArrayList<>();

        private static final SearchFilter[] ALL_FILTERS = new SearchFilter[]{  };

        protected Collection<SearchFilter> getAllFilters()
        {
            return Arrays.asList(ALL_FILTERS);
        }

        private PathChecker getFilter(String filtName, String arg)
        {
            for(SearchFilter f: getAllFilters())
                if(f.getName().equals(filtName))
                    return f.getChecker(_location, arg);
            return null;
        }
    }

    /*
    public static Uri getCurrentSelectionUri()
    {
        return CURRENT_SELECTION_URI;
    }
    */

    public static Uri getContentUriFromLocation(Location loc, Path path)
    {
        Location copy = loc.copy();
        copy.setCurrentPath(path);
        return getContentUriFromLocation(copy);

    }

    public static Uri getContentUriFromLocation(Location loc)
    {
        Uri uri = getContentUriFromLocationUri(loc.getLocationUri());
        /*try
        {
            if (loc.getCurrentPath().isFile())
            {
                Uri.Builder ub = uri.buildUpon();
                ub.appendPath(loc.getCurrentPath().getFile().getName());
                uri = ub.build();
            }
        }
        catch (IOException e)
        {
            Logger.log(e);
        }*/
        return uri;
    }

    public static Uri getContentUriFromLocationUri(Uri locationUri)
    {
        Uri.Builder ub = CONTENT_URI.buildUpon();
        appendLocationUriToProviderUri(ub, locationUri);
        return ub.build();
    }

    public static Uri getMetaUriFromLocationUri(Uri locationUri)
    {
        Uri.Builder ub = META_URI.buildUpon();
        appendLocationUriToProviderUri(ub, locationUri);
        return ub.build();
    }

    public static void appendLocationUriToProviderUri(Uri.Builder ub, Uri locationUri)
    {
        ub.appendPath(Base64.encodeToString(locationUri.toString().getBytes(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
    }

    @Override
    public boolean onCreate()
    {
        SystemConfig.setInstance(new com.sovworks.eds.android.settings.SystemConfig(getContext()));
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
                if(sortOrder!=null)
                    throw new IllegalArgumentException("Sorting is not supported");
                return queryMeta(uri, projection == null ? new String[]{COLUMN_NAME, COLUMN_SIZE} : projection, selection, selectionArgs);
            case META_PATH_CODE:
                if(sortOrder!=null)
                    throw new IllegalArgumentException("Sorting is not supported");
                return queryMeta(uri, projection, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if(selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                if(sortOrder!=null)
                    throw new IllegalArgumentException("Sorting is not supported");
                return querySelection(projection);
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);

    }

    @Override
    public String getType(@NonNull Uri uri)
    {
        switch (_uriMatcher.match(uri))
        {
            case META_PATH_CODE:
                return getMetaMimeType(uri);
            case CONTENT_PATH_CODE:
                return getContentMimeType(uri);
            case CURRENT_SELECTION_PATH_CODE:
                return _currentSelection == null ? null : MIME_TYPE_SELECTION;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return insertMeta(uri, contentValues);
            case CURRENT_SELECTION_PATH_CODE:
                _currentSelection = null;
                setCurrentSelection(contentValues);
                return CURRENT_SELECTION_URI;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public int delete (@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return deleteFromFS(uri, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if(selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                if(_currentSelection != null)
                {
                    _currentSelection = null;
                    return 1;
                }
                return 0;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public int update (@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return updateFS(uri, values, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if(selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                setCurrentSelection(values);
                return 1;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return getContentMimeType(uri, mimeTypeFilter);
            case CURRENT_SELECTION_PATH_CODE:
                return null;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException
    {
        Location loc = getLocationFromProviderUri(uri);
        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getContentMimeType(loc, mimeTypeFilter);
        // If the MIME type is supported
        if (mimeTypes != null)
            return getAssetFileDescriptor(uri, loc, "r", opts == null ? new Bundle() : opts);

        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public AssetFileDescriptor openAssetFile (@NonNull Uri uri, @NonNull String mode)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                Location loc = getLocationFromProviderUri(uri);
                return getAssetFileDescriptor(uri, loc, mode, new Bundle());
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public ParcelFileDescriptor openFile (@NonNull Uri uri, @NonNull String mode)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                Location loc = getLocationFromProviderUri(uri);
                return getParcelFileDescriptor(uri, loc, mode, new Bundle());
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);

    }

    protected static final String[] ALL_META_COLUMNS = { COLUMN_ID, COLUMN_NAME, COLUMN_SIZE, COLUMN_LAST_MODIFIED, COLUMN_IS_FOLDER, COLUMN_PATH };
    protected static final String[] ALL_SELECTION_COLUMNS = { COLUMN_LOCATION };

    protected static final String META_PATH = "fs";
    protected static final int META_PATH_CODE = 10;
    protected static final Uri META_URI = Uri.parse("content://"
            + MainContentProvider.AUTHORITY + "/" + META_PATH);
    protected static final String CONTENT_PATH = "content";
    protected static final int CONTENT_PATH_CODE = 20;
    protected static final Uri CONTENT_URI = Uri.parse("content://"
            + MainContentProvider.AUTHORITY + "/" + CONTENT_PATH);
    protected static final String CURRENT_SELECTION_PATH = "selection";
    protected static final int CURRENT_SELECTION_PATH_CODE = 30;
    protected static final Uri CURRENT_SELECTION_URI = Uri.parse("content://"
            + MainContentProvider.AUTHORITY + "/" + CURRENT_SELECTION_PATH);


    private static final UriMatcher _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static
    {
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, META_PATH + "/*", META_PATH_CODE);
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, CONTENT_PATH + "/*", CONTENT_PATH_CODE);
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, CURRENT_SELECTION_PATH, CURRENT_SELECTION_PATH_CODE);
    }

    private PathsStore _currentSelection;

    public static class PipeWriter implements PipeDataWriter<File>
    {

        @Override
        public void writeDataToPipe(@NonNull ParcelFileDescriptor output, @NonNull Uri uri, @NonNull String mimeType, Bundle opts, File file)
        {
            try
            {
                FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
                try
                {
                    Util.copyFileToOutputStream(fout, file, opts.getLong(OPTION_OFFSET, 0), opts.getLong(OPTION_NUM_BYTES, -1), null);
                }
                finally
                {
                    fout.close();
                }
            }
            catch (IOException e)
            {
                Logger.log(e);
            }
        }
    }

    public static class PipeReader implements PipeDataWriter<File>
    {

        @Override
        public void writeDataToPipe(@NonNull ParcelFileDescriptor input, @NonNull Uri uri, @NonNull String mimeType, Bundle opts, File file)
        {
            try
            {
                FileInputStream fin = new FileInputStream(input.getFileDescriptor());
                try
                {
                    Util.copyFileFromInputStream(fin, file, opts.getLong(OPTION_OFFSET, 0), opts.getLong(OPTION_NUM_BYTES, -1), null);
                }
                finally
                {
                    fin.close();
                }
            }
            catch (IOException e)
            {
                Logger.log(e);
            }
        }
    }

    protected AssetFileDescriptor getAssetFileDescriptor(Uri srcUri, Location loc, String accessMode, Bundle opts)
    {
        ParcelFileDescriptor pfd = getParcelFileDescriptor(srcUri, loc, accessMode, opts);
        return new AssetFileDescriptor(
                pfd,
                opts.getLong(OPTION_OFFSET, 0),
                opts.getLong(OPTION_NUM_BYTES, AssetFileDescriptor.UNKNOWN_LENGTH)
        );
    }

    protected ParcelFileDescriptor getParcelFileDescriptor(Uri srcUri, final Location loc, String accessMode, Bundle opts)
    {
        return getParcelFileDescriptor(this, srcUri, loc, accessMode, opts);
    }

    protected int updateFS(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        Location loc = getLocationFromProviderUri(uri);
        try
        {
            Path path = loc.getCurrentPath();
            SelectionChecker sc = new SelectionChecker(loc, selection, selectionArgs);
            if(sc.checkPath(path))
            {
                if (path.isFile())
                {
                    path.getFile().rename(values.getAsString(COLUMN_NAME));
                    return 1;
                }
                else if (path.isDirectory())
                {
                    path.getDirectory().rename(values.getAsString(COLUMN_NAME));
                    return 1;
                }
            }
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return 0;
    }

    protected int deleteFromFS(Uri uri, String selection, String[] selectionArgs)
    {
        Location loc = getLocationFromProviderUri(uri);
        try
        {
            Path path = loc.getCurrentPath();
            SelectionChecker sc = new SelectionChecker(loc, selection, selectionArgs);
            if(sc.checkPath(path))
            {
                if (path.isFile())
                {
                    path.getFile().delete();
                    return 1;
                } else if (path.isDirectory())
                {
                    path.getDirectory().delete();
                    return 1;
                }
            }
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return 0;
    }

    protected Uri insertMeta(Uri uri, ContentValues contentValues)
    {
        Location loc = getLocationFromProviderUri(uri);
        try
        {
            Path basePath = loc.getCurrentPath();
            if (!basePath.isDirectory())
                throw new IllegalArgumentException("Wrong parent folder: " + basePath);
            String name = contentValues.getAsString(COLUMN_NAME);
            FSRecord res = contentValues.getAsBoolean(COLUMN_IS_FOLDER) ?
                    basePath.getDirectory().createDirectory(name) :
                    basePath.getDirectory().createFile(name);
            loc.setCurrentPath(res.getPath());
            return loc.getLocationUri();
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return null;
    }

    protected void setCurrentSelection(ContentValues contentValues)
    {

        LocationsManager lm = LocationsManager.getLocationsManager(getContext(), true);
        PathsStore selection = _currentSelection;
        if(selection==null)
            selection = new PathsStore(lm);
        if(contentValues.containsKey(COLUMN_LOCATION))
            try
            {
                selection.setLocation(lm.getLocation(Uri.parse(contentValues.getAsString(COLUMN_LOCATION))));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Failed getting location from uri");
            }
        if(contentValues.containsKey(COLUMN_PATH))
        {
            if(selection.getLocation() == null)
                throw new IllegalArgumentException("Location is not set");
            try
            {
                selection.getPathsStore().add(selection.getLocation().getFS().getPath(contentValues.getAsString(COLUMN_PATH)));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Failed getting location from uri");
            }
        }
        _currentSelection = selection;
    }

    protected SelectionChecker getSelectionChecker(Location loc, String selection, String[] selectionArgs)
    {
        return new SelectionChecker(loc, selection, selectionArgs);
    }

    protected Cursor queryMeta(Uri uri, String[] projection, String selection, String[] selectionArgs)
    {
        checkProjection(projection, ALL_META_COLUMNS);
        if(projection == null)
            projection = ALL_META_COLUMNS;
        Location loc = getLocationFromProviderUri(uri);
        try
        {
            SelectionChecker sc = getSelectionChecker(loc, selection, selectionArgs);
            MatrixCursor res = new MatrixCursor(projection);
            queryMeta(res, sc, loc);
            return res;

        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return null;
    }

    protected void queryMeta(MatrixCursor res, SelectionChecker sc, Location loc) throws IOException
    {
        Path path = loc.getCurrentPath();
        if (path.isFile() && sc.checkPath(path))
            addFileRow(res, 1, path, path.getFile().getName());
        else if (path.isDirectory())
            listFolder(res, path, sc);
    }

    protected Cursor querySelection(String[] projection)
    {
        checkProjection(projection, ALL_SELECTION_COLUMNS);
        if(projection == null)
            projection = ALL_SELECTION_COLUMNS;
        PathsStore selection = _currentSelection;
        MatrixCursor res = new MatrixCursor(projection);
        if(selection == null)
            return res;
        Location loc = selection.getLocation();
        for(Path p: selection.getPathsStore())
        {
            Location copy = loc.copy();
            copy.setCurrentPath(p);
            res.addRow(Collections.singletonList(copy.getLocationUri().toString()));
        }
        return res;
    }

    protected String[] getContentMimeType(Uri uri, String requestedMime)
    {
        Location loc = getLocationFromProviderUri(uri);
        return getContentMimeType(loc, requestedMime);
    }

    protected String getContentMimeType(Uri uri)
    {
        Location loc = getLocationFromProviderUri(uri);
        return getContentMimeType(loc);
    }

    protected String getContentMimeType(Location loc)
    {
        try
        {
            Path path = loc.getCurrentPath();
            if(!path.isFile())
                return null;
            return FileOpsService.getMimeTypeFromExtension(getContext(), loc.getCurrentPath());
        }
        catch (IOException e)
        {
            Logger.log(e);
            return null;
        }
    }

    protected String[] getContentMimeType(Location loc, String requestedMime)
    {
        ClipDescription cd = new ClipDescription(null, new String[] { getContentMimeType(loc) });
        return cd.filterMimeTypes(requestedMime);
    }


    protected String getMetaMimeType(Uri uri)
    {
        Location loc = getLocationFromProviderUri(uri);
        try
        {
            return getMetaMimeType(loc.getCurrentPath());
        }
        catch (IOException e)
        {
            Logger.log(e);
            return null;
        }
    }

    protected String getMetaMimeType(Path path) throws IOException
    {
        if(path.isFile())
            return MIME_TYPE_FILE_META;
        if(path.isDirectory())
            return MIME_TYPE_FOLDER_META;
        return null;
    }

    protected void addFileRow(MatrixCursor cur, long id, Path path, String title) throws IOException
    {
        MatrixCursor.RowBuilder rb = cur.newRow();
        for(String col: cur.getColumnNames())
        {
            switch (col)
            {
                case COLUMN_ID:
                    rb.add(id);
                    break;
                case COLUMN_NAME:
                    rb.add(title);
                    break;
                case COLUMN_IS_FOLDER:
                    rb.add(true);
                    break;
                case COLUMN_LAST_MODIFIED:
                    rb.add(path.getFile().getLastModified().getTime());
                    break;
                case COLUMN_SIZE:
                    rb.add(path.getFile().getSize());
                    break;
                case COLUMN_PATH:
                    rb.add(path.getPathString());
            }
        }
    }

    protected void addFolderRow(MatrixCursor cur, long id, Path path, String title) throws IOException
    {
        MatrixCursor.RowBuilder rb = cur.newRow();
        for(String col: cur.getColumnNames())
        {
            switch (col)
            {
                case COLUMN_ID:
                    rb.add(id);
                case COLUMN_NAME:
                    rb.add(title);
                    break;
                case COLUMN_IS_FOLDER:
                    rb.add(true);
                    break;
                case COLUMN_LAST_MODIFIED:
                    rb.add(path.getDirectory().getLastModified().getTime());
                    break;
            }
        }
    }


    protected void listFolder(MatrixCursor cur, Path path, SelectionChecker sc) throws IOException
    {
        Directory.Contents dc = path.getDirectory().list();
        try
        {
            listFiles(cur, dc, sc);
        }
        finally
        {
            dc.close();
        }
    }

    protected void listFiles(MatrixCursor cur, Iterable<Path> it, SelectionChecker sc) throws IOException
    {
        int id = 1;
        for(Path p: it)
        {
            if(sc.checkPath(p))
            {
                if (p.isFile())
                    addFileRow(cur, id++, p, p.getFile().getName());
                else if (p.isDirectory())
                    addFolderRow(cur, id++, p, p.getDirectory().getName());
            }
        }
    }

    protected void checkProjection(String[] projection, String[] columns)
    {
        if(projection != null)
        {
            for(String col: projection)
                if(!Arrays.asList(columns).contains(col))
                    throw new IllegalArgumentException("Wrong projection column: " + col);
        }
    }


    protected Location getLocationFromProviderUri(Uri providerUri)
    {
        return getLocationFromProviderUri(getContext(), providerUri);
    }
}
