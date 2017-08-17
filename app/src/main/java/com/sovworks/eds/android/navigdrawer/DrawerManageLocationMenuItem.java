package com.sovworks.eds.android.navigdrawer;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.activities.LocationListActivity;

public abstract class DrawerManageLocationMenuItem extends DrawerMenuItemBase
{
    @Override
    public void onClick(View view, int position)
    {
        Intent i = new Intent(getContext(), LocationListActivity.class);
        i.putExtra(LocationListActivity.EXTRA_LOCATION_TYPE, getLocationType());
        getContext().startActivity(i);
        super.onClick(view, position);
    }

    @Override
    public Drawable getIcon()
    {
        return getIcon(getContext());
    }

    @Override
    public int getViewType()
    {
        return 3;
    }

    @Override
    protected int getLayoutId()
    {
        return R.layout.drawer_folder_item;
    }

    protected DrawerManageLocationMenuItem(DrawerControllerBase drawerController)
    {
        super(drawerController);
    }

    protected abstract String getLocationType();

    private static Drawable _icon;

    private synchronized static Drawable getIcon(Context context)
    {
        if(_icon == null)
        {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.manageLocationsIcon, typedValue, true);
            //noinspection deprecation
            _icon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _icon;
    }
}
