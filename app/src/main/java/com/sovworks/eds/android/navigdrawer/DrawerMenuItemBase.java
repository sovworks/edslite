package com.sovworks.eds.android.navigdrawer;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sovworks.eds.android.R;

public abstract class DrawerMenuItemBase
{
    public abstract String getTitle();

    public void onClick(View view, int position)
    {
        getDrawerController().closeDrawer();
    }

    public boolean onLongClick(View view, int position)
    {
        return false;
    }

    public boolean onBackPressed()
    {
        return false;
    }

    public Drawable getIcon()
    {
        return null;
    }

    public int getViewType()
    {
        return 0;
    }

    public void saveState(Bundle state)
    {

    }

    public void restoreState(Bundle state)
    {

    }

    public View createView(int position, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) getDrawerController().getMainActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View v = inflater.inflate(getLayoutId(), parent, false);
        updateView(v, position);
        return v;
    }

    public void updateView(View view, @SuppressWarnings("UnusedParameters") int position)
    {
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText(getTitle());
        ImageView iconView = view.findViewById(android.R.id.icon);
        if(iconView!=null)
        {
            iconView.setContentDescription(getTitle());
            Drawable icon = getIcon();
            if (icon == null)
                iconView.setVisibility(View.INVISIBLE);
            else
            {
                iconView.setVisibility(View.VISIBLE);
                iconView.setImageDrawable(icon);
            }
        }
    }

    public View updateView()
    {
        ListView list = getDrawerController().getDrawerListView();
        int start = list.getFirstVisiblePosition();
        for(int i=start, j=list.getLastVisiblePosition();i<=j;i++)
            if(this == list.getItemAtPosition(i))
                return getAdapter().getView(i, list.getChildAt(i-start),list);
        return null;
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    protected int getLayoutId()
    {
        return R.layout.drawer_item;
    }

    protected DrawerMenuItemBase(DrawerControllerBase drawerController)
    {
        _drawerController = drawerController;
    }

    protected ArrayAdapter<DrawerMenuItemBase> getAdapter()
    {
        //noinspection unchecked
        return (ArrayAdapter<DrawerMenuItemBase>) getDrawerController().getDrawerListView().getAdapter();
    }

    protected int getPositionInAdapter()
    {
        return getAdapter().getPosition(this);/*
        ArrayAdapter<?> adapter = getAdapter();
        for(int i=0;i<adapter.getCount();i++)
            if(adapter.getItem(i) == this)
                return i;
        return -1;*/
    }

    protected DrawerControllerBase getDrawerController()
    {
        return _drawerController;
    }

    protected Context getContext()
    {
        return getDrawerController().getMainActivity();
    }

    private final DrawerControllerBase _drawerController;
}
