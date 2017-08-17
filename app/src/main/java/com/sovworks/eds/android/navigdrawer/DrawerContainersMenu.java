package com.sovworks.eds.android.navigdrawer;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.EncFsLocationBase;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.EDSLocation;

import java.util.ArrayList;
import java.util.Collection;

public class DrawerContainersMenu extends DrawerSubMenuBase
{
    @Override
    public String getTitle()
    {
        return getContext().getString(R.string.containers);
    }

    public DrawerContainersMenu(DrawerControllerBase drawerController)
    {
        super(drawerController);
    }

    @Override
    protected Collection<DrawerMenuItemBase> getSubItems()
    {
        LocationsManager lm = LocationsManager.getLocationsManager(getContext());
        ArrayList<DrawerMenuItemBase> res = new ArrayList<>();
        for(EDSLocation loc: lm.getLoadedEDSLocations(true))
        {
            if(loc instanceof ContainerLocation)
                res.add(new DrawerContainerMenuItem(loc, getDrawerController()));
            else if(loc instanceof EncFsLocationBase)
                res.add(new DrawerEncFsMenuItem(loc, getDrawerController()));
        }
        res.add(new DrawerManageContainersMenuItem(getDrawerController()));

        return res;
    }
}
