package com.sovworks.eds.android.filemanager.activities;

import com.sovworks.eds.android.navigdrawer.DrawerController;
import com.sovworks.eds.locations.Location;

public class FileManagerActivity extends FileManagerActivityBase
{
    public static void openFileManager(FileManagerActivity fm, Location location)
    {
        fm.goTo(location);
    }

    @Override
    protected DrawerController createDrawerController()
    {
        return new DrawerController(this);
    }

    @Override
    protected void showPromoDialogIfNeeded()
    {
        if(_settings.getLastViewedPromoVersion() < 199)
            super.showPromoDialogIfNeeded();
    }
}
