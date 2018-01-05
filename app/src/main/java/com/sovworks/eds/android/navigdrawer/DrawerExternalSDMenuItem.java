package com.sovworks.eds.android.navigdrawer;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.locations.opener.fragments.ExternalStorageOpenerFragment;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.locations.Location;

public class DrawerExternalSDMenuItem extends DrawerLocationMenuItem
{
    public static class Opener extends ExternalStorageOpenerFragment
    {
        @Override
        public void onLocationOpened(Location location)
        {
            ((FileManagerActivity)getActivity()).goTo(location);
        }
    }

    DrawerExternalSDMenuItem(Location location, DrawerControllerBase drawerController, boolean allowDocumentsAPI)
    {
        super(location, drawerController);
        _allowDocumentsAPI = allowDocumentsAPI;
    }

    @Override
    public Drawable getIcon()
    {
        return getIcon(getDrawerController().getMainActivity());
    }

    @Override
    protected LocationOpenerBaseFragment getOpener()
    {
        return _allowDocumentsAPI ? new Opener() : super.getOpener();
    }

    private synchronized static Drawable getIcon(Context context)
    {
        if(_icon == null)
        {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.extStorageIcon, typedValue, true);
            //noinspection deprecation
            _icon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _icon;
    }

    private static Drawable _icon;
    private final boolean _allowDocumentsAPI;
}
