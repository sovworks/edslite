package com.sovworks.eds.android.navigdrawer;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;

import com.sovworks.eds.android.R;
import com.sovworks.eds.settings.GlobalConfig;

public class DrawerHelpMenuItem extends DrawerMenuItemBase
{

    public DrawerHelpMenuItem(DrawerControllerBase drawerController)
    {
        super(drawerController);
    }

    @Override
    public String getTitle()
    {
        return getDrawerController().getMainActivity().getString(R.string.help);
    }

    @Override
    public void onClick(View view, int position)
    {
        super.onClick(view, position);
        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.HELP_URL)));
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
            context.getTheme().resolveAttribute(R.attr.helpIcon, typedValue, true);
            //noinspection deprecation
            _icon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _icon;
    }

    private static Drawable _icon;

}
