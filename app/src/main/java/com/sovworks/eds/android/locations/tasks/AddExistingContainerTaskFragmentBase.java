package com.sovworks.eds.android.locations.tasks;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.locations.EncFsLocation;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.container.ContainerFormatter;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.Config;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;

public abstract class AddExistingContainerTaskFragmentBase extends AddExistingEDSLocationTaskFragment
{
    public static final String TAG = "com.sovworks.eds.android.locations.tasks.AddExistingContainerTaskFragment";

    @Override
    protected EDSLocation createEDSLocation(Location locationLocation) throws Exception
    {
        Logger.debug("Adding EDS loc at " + locationLocation.getLocationUri());
        Path cp = locationLocation.getCurrentPath();
        boolean isEncFs = false;
        if(cp.isFile())
        {
            String fn = cp.getFile().getName();
            if(Config.CONFIG_FILENAME.equalsIgnoreCase(fn) || Config.CONFIG_FILENAME2.equalsIgnoreCase(fn))
            {
                Path parentPath = cp.getParentPath();
                if(parentPath!=null)
                {
                    locationLocation.setCurrentPath(parentPath);
                    isEncFs = true;
                }
            }
        }
        else if(cp.isDirectory())
        {
            Path cfgPath = Config.getConfigFilePath(cp.getDirectory());
            if(cfgPath == null)
                throw new UserException("EncFs config file doesn't exist", R.string.encfs_config_file_not_found);
            isEncFs = true;
        }
        else
            throw new UserException("Wrong path", R.string.wrong_path);

        if(isEncFs)
            return new EncFsLocation(locationLocation, _context);
        else
            return createContainerBasedLocation(locationLocation);
    }

    protected ContainerLocation createContainerBasedLocation(Location locationLocation) throws Exception
    {
        Settings settings = UserSettings.getSettings(_context);
        return createContainerLocationBase(locationLocation, settings);
    }

    protected ContainerLocation createContainerLocationBase(Location locationLocation, Settings settings) throws IOException
    {
        String formatName = getArguments().getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT);
        if(formatName == null)
            formatName = "";
        return ContainerFormatter.createBaseContainerLocationFromFormatInfo(
                formatName,
                locationLocation,
                null,
                _context,
                settings
        );
    }
}
