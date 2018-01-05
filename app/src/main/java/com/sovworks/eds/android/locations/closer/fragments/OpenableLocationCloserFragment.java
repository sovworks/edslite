package com.sovworks.eds.android.locations.closer.fragments;


import android.content.Context;
import android.os.Build;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.TempFilesMonitor;
import com.sovworks.eds.android.helpers.WipeFilesTask;
import com.sovworks.eds.android.providers.ContainersDocumentProviderBase;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.service.LocationsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.util.SrcDstRec;
import com.sovworks.eds.fs.util.SrcDstSingle;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.io.IOException;

public class OpenableLocationCloserFragment extends LocationCloserBaseFragment
{
    public static void wipeMirror(Context context, Location location) throws IOException
    {
        Location mirrorLocation = FileOpsService.getMirrorLocation(
                UserSettings.getSettings(context).getWorkDir(),
                context,
                location.getId()
        );
        if(mirrorLocation.getCurrentPath().exists())
        {
            SrcDstRec sdr = new SrcDstRec(new SrcDstSingle(
                    mirrorLocation,
                    null
            )
            );
            sdr.setIsDirLast(true);
            WipeFilesTask.wipeFilesRnd(
                    null,
                    TempFilesMonitor.getMonitor(context).getSyncObject(),
                    true,
                    sdr
            );
        }
    }

    public static void closeLocation(Context context, Openable location, boolean forceClose) throws IOException
    {
        try
        {
            location.close(forceClose);
        }
        catch (Exception e)
        {
            if(forceClose)
                Logger.log(e);
            else
                throw e;
        }
        makePostCloseCheck(context, location);
        wipeMirror(context, location);
    }

    public static void makePostCloseCheck(Context context, Location loc)
    {
        if(loc instanceof Openable && LocationsManager.isOpen(loc))
            return;
        LocationsManager lm = LocationsManager.getLocationsManager(context);
        LocationsManager.broadcastLocationChanged(context, loc);
        lm.unregOpenedLocation(loc);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && loc instanceof EDSLocation)
            ContainersDocumentProviderBase.notifyOpenedLocationsListChanged(context);

        if(!lm.hasOpenLocations())
        {
            lm.broadcastAllContainersClosed();
            LocationsService.stopService(context);
        }
    }

    public static class CloseLocationTaskFragment extends LocationCloserBaseFragment.CloseLocationTaskFragment
    {
        @Override
        protected void procLocation(TaskState state, Location location) throws Exception
        {
            boolean fc = getArguments().getBoolean(ARG_FORCE_CLOSE, UserSettings.getSettings(_context).alwaysForceClose());
            try
            {
                closeLocation(_context, (Openable)location, fc);
            }
            catch (Exception e)
            {
                if(fc)
                    Logger.log(e);
                else
                    throw e;
            }
        }
    }

    @Override
    protected TaskFragment getCloseLocationTask()
    {
        return new CloseLocationTaskFragment();
    }
}
