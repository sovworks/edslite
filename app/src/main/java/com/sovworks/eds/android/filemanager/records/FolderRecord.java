package com.sovworks.eds.android.filemanager.records;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sovworks.eds.android.R;

import java.io.IOException;

public class FolderRecord extends FsBrowserRecord
{

	public FolderRecord(Context context) throws IOException
	{			
		super(context);
	}

    @Override
    public int getViewType()
    {
        return 1;
    }

    @Override
    public View createView(int position, ViewGroup parent)
    {
        if(_host == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) _host.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.fs_browser_folder_row, parent, false);
        ((ViewGroup)v).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        updateView(v, position);
        return v;
    }

    @Override
    public boolean allowSelect()
	{
		return _host.allowFolderSelect();
	}

	@Override
	public boolean open() throws Exception
	{
		if(_path!=null)
			_host.goTo(_path);
		return true;
	}

    @Override
    public boolean openInplace() throws Exception
    {
        _host.showProperties(null, true);
        return open();
    }

	@Override
	protected Drawable getDefaultIcon()
	{
		return getFolderIcon(_host);
	}


    private static Drawable _folderIcon;

    private static synchronized Drawable getFolderIcon(Context context)
    {
        if(_folderIcon == null && context!=null)
        {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.folderIcon, typedValue, true);
            //noinspection deprecation
            _folderIcon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _folderIcon;
    }
	
}