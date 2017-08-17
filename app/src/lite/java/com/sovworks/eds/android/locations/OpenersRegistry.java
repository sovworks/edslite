package com.sovworks.eds.android.locations;

import com.sovworks.eds.android.locations.opener.fragments.ContainerOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.EDSLocationOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.EncFSOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerFragment;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

public class OpenersRegistry
{
    public static LocationOpenerBaseFragment getDefaultOpenerForLocation(Location location)
    {
        if(location instanceof ContainerLocation)
            return new ContainerOpenerFragment();
        if(location instanceof EncFsLocationBase)
            return new EncFSOpenerFragment();
        if(location instanceof EDSLocation)
            return new EDSLocationOpenerFragment();
        if(location instanceof Openable)
            return new LocationOpenerFragment();
        return new LocationOpenerBaseFragment();
    }
}
