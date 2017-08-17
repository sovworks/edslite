package com.sovworks.eds.android.navigdrawer;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.ContainerBasedLocation;

public class DrawerManageContainersMenuItem extends DrawerManageLocationMenuItem
{
    public DrawerManageContainersMenuItem(DrawerControllerBase drawerController)
    {
        super(drawerController);
    }

    @Override
    protected String getLocationType()
    {
        return ContainerBasedLocation.URI_SCHEME;
    }

    @Override
    public String getTitle()
        {
            return getContext().getString(R.string.manage_containers);
    }

}
