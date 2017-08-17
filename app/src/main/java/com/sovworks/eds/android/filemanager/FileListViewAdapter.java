package com.sovworks.eds.android.filemanager;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;

public class FileListViewAdapter extends ArrayAdapter<BrowserRecord>
{
    public FileListViewAdapter(Context context)
    {
        super(context, R.layout.fs_browser_row);
    }

    public void setCurrentLocationId(String locationId)
    {
        _currentLocationId = locationId;
    }

    @Override
    public int getItemViewType(int position)
    {
         BrowserRecord rec = getItem(position);
         return rec==null ? 0 : rec.getViewType();
    }

    @Override
    public int getViewTypeCount()
     {
         return 2;
     }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final BrowserRecord rec = getItem(position);
        if(rec.needLoadExtendedInfo())
            ExtendedFileInfoLoader.getInstance().requestExtendedInfo(_currentLocationId, rec);

        View v;
        if(convertView!=null)
        {
            v = convertView;
            rec.updateView(v, position);
        }
        else
            v = rec.createView(position, parent);
        v.setTag(rec);
        return v;
    }

    private String _currentLocationId;
}
