package com.sovworks.eds.android.navigdrawer;

import android.content.Intent;
import android.os.Build;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.locations.DocumentTreeLocation;
import com.sovworks.eds.android.locations.ExternalStorageLocation;
import com.sovworks.eds.android.locations.InternalSDLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class DrawerLocalFilesMenuBase extends DrawerSubMenuBase
{
    @Override
    public String getTitle()
    {
        return getContext().getString(R.string.local_files);
    }

    public DrawerLocalFilesMenuBase(DrawerControllerBase drawerController)
    {
        super(drawerController);
        Intent i = getDrawerController().getMainActivity().getIntent();
        _allowDeviceLocations = i.getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_BROWSE_DEVICE, true);
        _allowDocumentTree = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && i.getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_BROWSE_DOCUMENT_PROVIDERS, true);
    }

    @Override
    protected Collection<DrawerMenuItemBase> getSubItems()
    {
        ArrayList<DrawerMenuItemBase> res = new ArrayList<>();
        FileManagerActivity act = getDrawerController().getMainActivity();
        Intent i = act.getIntent();
        for(Location loc: LocationsManager.getLocationsManager(act).getLoadedLocations(true))
            addLocationMenuItem(res, loc);

        if(act.isSelectAction() && i.getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_SELECT_FROM_CONTENT_PROVIDERS, false))
            res.add(new DrawerSelectContentProviderMenuItem(getDrawerController()));

        if(_allowDocumentTree)
            res.add(new DrawerManageLocalStorages(getDrawerController()));

        return res;
    }

    protected boolean _allowDeviceLocations, _allowDocumentTree;

    protected void addLocationMenuItem(List<DrawerMenuItemBase> list, Location loc)
    {
        if(loc instanceof InternalSDLocation && _allowDeviceLocations)
            list.add(new DrawerInternalSDMenuItem(loc, getDrawerController()));
        else if(loc instanceof ExternalStorageLocation && _allowDeviceLocations)
            list.add(new DrawerExternalSDMenuItem(loc, getDrawerController(), _allowDocumentTree));
        else if(loc instanceof DocumentTreeLocation && _allowDocumentTree)
            list.add(new DrawerDocumentTreeMenuItem(loc, getDrawerController()));
    }

}
