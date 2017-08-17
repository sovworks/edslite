package com.sovworks.eds.android.navigdrawer;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.sovworks.eds.android.R;
import com.sovworks.eds.locations.Location;

public class DrawerDeviceRootMemoryItem extends DrawerLocationMenuItem
{
    public DrawerDeviceRootMemoryItem(Location location, DrawerControllerBase drawerController)
    {
        super(location, drawerController);
    }

    @Override
    public Drawable getIcon()
    {
        return getIcon(getDrawerController().getMainActivity());
    }

    private synchronized static Drawable getIcon(Context context)
    {
        if(_icon == null)
        {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.deviceRootIcon, typedValue, true);
            //noinspection deprecation
            _icon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _icon;
    }

    private static Drawable _icon;
}
