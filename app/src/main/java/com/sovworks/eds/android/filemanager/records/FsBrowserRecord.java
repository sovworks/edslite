package com.sovworks.eds.android.filemanager.records;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.filemanager.fragments.FileListViewFragment;
import com.sovworks.eds.android.helpers.CachedPathInfoBase;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import java.io.IOException;

public abstract class FsBrowserRecord extends CachedPathInfoBase implements BrowserRecord
{
    public static class RowViewInfo
    {
        public ListView listView;
        public View view;
        public int position;
    }

    public static void updateRowView(FileManagerActivity host, Object item)
    {
        updateRowView((FileListViewFragment) host.getFragmentManager().findFragmentByTag(FileListViewFragment.TAG), item);
    }

    public static void updateRowView(FileListViewFragment host, Object item)
    {
        RowViewInfo rvi = getCurrentRowViewInfo(host, item);
        if(rvi!=null)
            updateRowView(rvi);
    }

    public static void updateRowView(RowViewInfo rvi)
    {
        rvi.listView.getAdapter().getView(rvi.position, rvi.view, rvi.listView);
    }

    public static RowViewInfo getCurrentRowViewInfo(FileListViewFragment host, Object item)
    {
        if(host == null || host.isRemoving() || !host.isResumed())
            return null;
        ListView list = host.getListView();
        if(list == null)
            return null;
        int start = list.getFirstVisiblePosition();
        for(int i=start, j=list.getLastVisiblePosition();i<=j;i++)
            if(j<list.getCount() && item == list.getItemAtPosition(i))
            {
                RowViewInfo rvi = new RowViewInfo();
                rvi.view = list.getChildAt(i-start);
                rvi.position = i;
                rvi.listView = list;
                return rvi;
            }
        return null;
    }

    public static RowViewInfo getCurrentRowViewInfo(FileManagerActivity host, Object item)
    {
        if(host == null)
            return null;
        FileListViewFragment f = (FileListViewFragment) host.getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
        return getCurrentRowViewInfo(f, item);
    }

    @Override
    public int getViewType()
    {
        return 0;
    }

    @Override
    public void setSelected(boolean val)
    {
        _isSelected = val;
    }

    public boolean isSelected()
    {
        return _isSelected;
    }

    @Override
    public View createView(int position, ViewGroup parent)
    {
        if(_host == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) _host.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.fs_browser_row, parent, false);
        ((ViewGroup)v).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        updateView(v, position);
        return v;
    }

    @Override
    public void updateView(View view, final int position)
    {
        final FileListViewFragment hf = getHostFragment();
        //if(isSelected())
        //    //noinspection deprecation
        //    view.setBackgroundDrawable(getSelectedBackgroundDrawable(_context));
        CheckBox cb = view.findViewById(android.R.id.checkbox);
        if(cb!=null)
        {
            if(allowSelect() && (_host.isSelectAction() || hf.isInSelectionMode()) && (!_host.isSelectAction() || !_host.isSingleSelectionMode()))
            {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(isSelected());
                cb.setOnCheckedChangeListener((compoundButton, isChecked) ->
                {
                    if(isChecked)
                        hf.selectFile(FsBrowserRecord.this);
                    else
                        hf.unselectFile(FsBrowserRecord.this);
                });
                cb.setVisibility(View.VISIBLE);
            }
            else
                cb.setVisibility(View.INVISIBLE);
        }
        RadioButton rb = view.findViewById(R.id.radio);
        if(rb!=null)
        {
            if(allowSelect() && _host.isSelectAction() && _host.isSingleSelectionMode())
            {
                rb.setOnCheckedChangeListener(null);
                rb.setChecked(isSelected());
                rb.setOnCheckedChangeListener((compoundButton, isChecked) ->
                {
                    if(isChecked)
                        hf.selectFile(FsBrowserRecord.this);
                    else
                       hf.unselectFile(FsBrowserRecord.this);
                });
                rb.setVisibility(View.VISIBLE);
            }
            else
                rb.setVisibility(View.INVISIBLE);
        }

        TextView tv = view.findViewById(android.R.id.text1);
     	tv.setText(getName());

        ImageView iv = view.findViewById(android.R.id.icon);
        iv.setImageDrawable(getDefaultIcon());
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setOnClickListener(view1 ->
        {
            if (allowSelect())
            {
                if(isSelected())
                {
                    if(!_host.isSelectAction() || !_host.isSingleSelectionMode())
                        hf.unselectFile(FsBrowserRecord.this);
                }
                else
                    hf.selectFile(FsBrowserRecord.this);
            }
        });

        iv = view.findViewById(android.R.id.icon1);
        if(_miniIcon == null)
            iv.setVisibility(View.INVISIBLE);
        else
        {
            iv.setImageDrawable(_miniIcon);
            iv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateView()
    {
        updateRowView(_host, this);
    }

    @Override
    public void setExtData(ExtendedFileInfoLoader.ExtendedFileInfo data)
    {

    }

    @Override
    public ExtendedFileInfoLoader.ExtendedFileInfo loadExtendedInfo()
    {
        return null;
    }

    @Override
    public boolean allowSelect()
	{
        return true;
	}

	@Override
	public boolean open() throws Exception
	{		
		return false;
	}

    @Override
	public boolean openInplace() throws Exception
	{
		return false;
	}
	
	@Override
	public void setHostActivity(FileManagerActivity host)
	{
		_host = host;
	}

    @Override
    public boolean needLoadExtendedInfo()
    {
        return false;
    }

    @Override
    public void init(Location location, Path path) throws IOException
    {
        init(path);
        _locationId = location == null ? "" : location.getId();
    }

    public FsBrowserRecord(Context context)
	{
        _context = context;
	}

	protected final Context _context;
	protected String _locationId;
	protected FileManagerActivity _host;
    protected Drawable _miniIcon;

	protected abstract Drawable getDefaultIcon();

    protected FileListViewFragment getHostFragment()
    {
        return _host == null ? null : (FileListViewFragment) _host.getFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
    }

    private boolean _isSelected;

    /*private static Drawable _selectedItemBackground;

    private static synchronized Drawable getSelectedBackgroundDrawable(Context context)
    {
        if(_selectedItemBackground == null)
            //noinspection deprecation
            _selectedItemBackground = context.getResources().getDrawable(R.drawable.list_selected_item_background);
        return _selectedItemBackground;
    }*/

}
