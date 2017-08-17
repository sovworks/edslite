package com.sovworks.eds.android.navigdrawer;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.DocumentTreeLocation;

public class DrawerManageLocalStorages extends DrawerManageLocationMenuItem
{
    public DrawerManageLocalStorages(DrawerControllerBase drawerController)
    {
        super(drawerController);
    }

    @Override
    protected String getLocationType()
    {
        return DocumentTreeLocation.URI_SCHEME;
    }

    @Override
    public String getTitle()
        {
            return getContext().getString(R.string.manage_local_storages);
    }

}
