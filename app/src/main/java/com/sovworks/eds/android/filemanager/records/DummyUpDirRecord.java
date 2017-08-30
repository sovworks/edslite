package com.sovworks.eds.android.filemanager.records;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.fragments.FileListDataFragment;

import java.io.IOException;
import java.util.Stack;

public class DummyUpDirRecord extends FolderRecord
{
	public DummyUpDirRecord(Context context) throws IOException
	{
        super(context);
	}

	@Override
	public String getName()
	{
		return "..";
	}	
	
	@Override
	public boolean allowSelect()
	{
		return false;
	}
	
    @Override
	public boolean isFile()
	{
		return false;
	}

	@Override
	public boolean isDirectory()
	{
		return true;
	}

	@Override
	public boolean open() throws Exception
	{
        super.open();
		Stack<FileListDataFragment.HistoryItem> nh = _host.getFileListDataFragment().getNavigHistory();
		if(!nh.empty())
			nh.pop();
		return true;
	}

	@Override
	protected Drawable getDefaultIcon()
	{
		return getIcon(_host);
	}

	private static Drawable _icon;

    private static synchronized Drawable getIcon(Context context)
    {
        if(_icon == null && context!=null)
		{
			TypedValue typedValue = new TypedValue();
			context.getTheme().resolveAttribute(R.attr.folderUpIcon, typedValue, true);
			//noinspection deprecation
			_icon = context.getResources().getDrawable(typedValue.resourceId);
		}
        return _icon;
    }
			
}