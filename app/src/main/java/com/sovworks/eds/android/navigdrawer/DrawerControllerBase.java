package com.sovworks.eds.android.navigdrawer;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class DrawerControllerBase
{
    public DrawerControllerBase(FileManagerActivity activity)
    {
        _activity = activity;
    }

    public void init(Bundle savedState)
    {
        _drawerLayout = _activity.findViewById(R.id.drawer_layout);
        _drawerListView = _activity.findViewById(R.id.left_drawer);

        //noinspection deprecation
        _drawerToggle = new ActionBarDrawerToggle(
                _activity,                  /* host Activity */
                _drawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        );

        // Set the drawer toggle as the DrawerListener
        //noinspection deprecation
        _drawerLayout.setDrawerListener(_drawerToggle);

        ActionBar ab = _activity.getActionBar();
        if(ab!=null)
        {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
        }

        List<DrawerMenuItemBase> list = fillDrawer();

        _drawerListView.setChoiceMode(ListView.CHOICE_MODE_NONE);

        if(savedState!=null)
        {
            ArrayList<DrawerMenuItemBase> copy = new ArrayList<>(list);
            for (DrawerMenuItemBase item : copy)
                item.restoreState(savedState);
        }
        _drawerListView.setOnItemClickListener((adapterView, view, i, l) ->
        {
            DrawerMenuItemBase item = (DrawerMenuItemBase) _drawerListView.getItemAtPosition(i);
            if(item!=null)
                item.onClick(view, i);
        });
        _drawerListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            DrawerMenuItemBase item = (DrawerMenuItemBase) _drawerListView.getItemAtPosition(position);
            return item != null && item.onLongClick(view, position);
        });
    }

    public void onPostCreate()
    {
        if(_drawerToggle!=null)
            _drawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        if(_drawerToggle!=null)
            _drawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(_drawerLayout == null)
            return false;

        if (item.getItemId() == android.R.id.home)
        {
            if (_drawerLayout.isDrawerOpen(_drawerListView))
                _drawerLayout.closeDrawer(_drawerListView);
             else
                _drawerLayout.openDrawer(_drawerListView);
            return true;
        }
        return false;
    }

    void closeDrawer()
    {
        _drawerLayout.closeDrawer(_drawerListView);
    }

    private void openDrawer()
    {
        _drawerLayout.openDrawer(_drawerListView);
    }

    public FileManagerActivity getMainActivity()
    {
        return _activity;
    }

    ListView getDrawerListView()
    {
        return _drawerListView;
    }

    @SuppressWarnings("unused")
    public DrawerLayout getDrawerLayout()
    {
        return _drawerLayout;
    }

    public boolean onBackPressed()
    {
       if(_drawerListView == null || !_drawerLayout.isDrawerOpen(_drawerListView))
            return false;
       for(int i=0;i<_drawerListView.getCount();i++)
       {
           DrawerMenuItemBase item = (DrawerMenuItemBase) _drawerListView.getItemAtPosition(i);
           if(item!=null && item.onBackPressed())
               return true;
       }
        _drawerLayout.closeDrawer(_drawerListView);
       return true;
    }

    public void onSaveInstanceState (Bundle outState)
    {
       if(_drawerListView == null)
            return;
       saveState(outState);
    }

    public void updateMenuItemViews()
    {
        ListView lv = getDrawerListView();
        if(lv != null)
        {
            DrawerAdapter adapter = (DrawerAdapter) lv.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    public void reloadItems()
    {
        if(_drawerListView == null)
            return;
        Bundle b = new Bundle();
        saveState(b);
        List<DrawerMenuItemBase> list = fillDrawer();
        ArrayList<DrawerMenuItemBase> copy = new ArrayList<>(list);
        for (DrawerMenuItemBase item : copy)
            item.restoreState(b);
    }

    public void showContainers()
    {
        openDrawer();
        DrawerAdapter da = (DrawerAdapter) _drawerListView.getAdapter();
        for(int i=0, l=da.getCount();i<l;i++)
        {
            DrawerMenuItemBase item = da.getItem(i);
            if(item instanceof DrawerContainersMenu)
            {
                DrawerContainersMenu dcm = (DrawerContainersMenu) item;
                if(!dcm.isExpanded())
                    dcm.rotateIconAndChangeState(da.getView(i, dcm.findView(_drawerListView), _drawerListView));
            }
        }

    }

    protected List<DrawerMenuItemBase> fillDrawer()
    {
        Intent i = getMainActivity().getIntent();
        boolean isSelectAction = getMainActivity().isSelectAction();
        ArrayList<DrawerMenuItemBase> list = new ArrayList<>();
        DrawerAdapter adapter = new DrawerAdapter(list);
        if(i.getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_BROWSE_CONTAINERS, true))
            adapter.add(new DrawerContainersMenu(this));
        if(i.getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_BROWSE_DEVICE, true))
            adapter.add(new DrawerLocalFilesMenu(this));
        if(!isSelectAction)
        {
            adapter.add(new DrawerSettingsMenuItem(this));
            adapter.add(new DrawerHelpMenuItem(this));
            adapter.add(new DrawerAboutMenuItem(this));
            adapter.add(new DrawerExitMenuItem(this));
        }
        _drawerListView.setAdapter(adapter);
        return list;
    }

    protected class DrawerAdapter extends ArrayAdapter<DrawerMenuItemBase>
	{

		DrawerAdapter(List<DrawerMenuItemBase> itemsList)
		{
			super(_activity, R.layout.drawer_folder, itemsList);
		}

		@Override
	    public int getItemViewType(int position)
	    {
	    	 DrawerMenuItemBase rec = getItem(position);
			 return rec==null ? 0 : rec.getViewType();
	    }

	    @Override
        public int getViewTypeCount()
	     {
	         return 4;
	     }

		@NonNull
        @Override
	    public View getView(int position, View convertView, @NonNull ViewGroup parent)
		{
			final DrawerMenuItemBase rec = getItem(position);
            View v;
            if (convertView != null)
            {
                v = convertView;
                rec.updateView(v, position);
            } else
                v = rec.createView(position, parent);
            v.setTag(rec);
	        return v;
	    }

	}
    private final FileManagerActivity _activity;
    private ListView _drawerListView;
    private DrawerLayout _drawerLayout;

    @SuppressWarnings("deprecation")
    private ActionBarDrawerToggle _drawerToggle;

    private void saveState(Bundle outState)
    {
        for(int i=0;i<_drawerListView.getCount();i++)
        {
            DrawerMenuItemBase item = (DrawerMenuItemBase) _drawerListView.getItemAtPosition(i);
            if(item!=null)
                item.saveState(outState);
        }
    }
}
