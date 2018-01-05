package com.sovworks.eds.android.filemanager.tasks;

import com.sovworks.eds.android.filemanager.DirectorySettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.ContainerFSWrapper;
import com.sovworks.eds.locations.Location;

import org.json.JSONException;

import java.io.IOException;

import io.reactivex.Maybe;

public class LoadDirSettingsObservable
{
    public static DirectorySettings getDirectorySettings(Path path) throws IOException
    {
        return path.getFileSystem() instanceof ContainerFSWrapper ?
                ((ContainerFSWrapper) path.getFileSystem()).getDirectorySettings(path)
                :
                loadDirectorySettings(path);
    }

    public static DirectorySettings loadDirectorySettings(Path path) throws IOException
    {
        Path dsPath;
        try
        {
            dsPath = path.combine(DirectorySettings.FILE_NAME);
        }
        catch(IOException e)
        {
            return null;
        }
        try
        {
            return dsPath.isFile() ?
                    new DirectorySettings(com.sovworks.eds.fs.util.Util.readFromFile(dsPath))
                    :
                    null;

        }
        catch (JSONException e)
        {
            throw new IOException(e);
        }
    }

    public static Maybe<DirectorySettings> create(Location targetLocation)
    {
        return Maybe.create(s -> {
            Path p = targetLocation.getCurrentPath();
            if(p.isFile())
                p = p.getParentPath();
            DirectorySettings ds = getDirectorySettings(p);
            if(ds == null)
                s.onComplete();
            else
                s.onSuccess(ds);
        });
    }
}
