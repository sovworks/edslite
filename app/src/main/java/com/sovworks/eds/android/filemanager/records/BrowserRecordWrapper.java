package com.sovworks.eds.android.filemanager.records;

import android.view.View;
import android.view.ViewGroup;

import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import java.io.IOException;
import java.util.Date;

public class BrowserRecordWrapper implements BrowserRecord
{

    @Override
    public void init(Location location, Path path) throws IOException
    {
        _base.init(location, path);

    }

    @Override
    public String getName()
    {
        return _base.getName();
    }

    @Override
    public boolean open() throws Exception
    {
        return _base.open();
    }

    @Override
    public boolean openInplace() throws Exception
    {
        return _base.openInplace();
    }

    @Override
    public boolean allowSelect()
    {
        return _base.allowSelect();
    }

    @Override
    public boolean isSelected()
    {
        return _base.isSelected();
    }

    @Override
    public void setSelected(boolean val)
    {
        _base.setSelected(val);
    }

    @Override
    public void setHostActivity(FileManagerActivity host)
    {
        _host = host;
        _base.setHostActivity(host);
    }

    @Override
    public int getViewType()
    {
        return _base.getViewType();
    }

    @Override
    public View createView(int position, ViewGroup parent)
    {
        return _base.createView(position, parent);
    }

    @Override
    public void updateView(View view, int position)
    {
        _base.updateView(view, position);
    }

    @Override
    public void updateView()
    {
        FsBrowserRecord.updateRowView(_host, this);
    }

    @Override
    public void setExtData(ExtendedFileInfoLoader.ExtendedFileInfo data)
    {
        _base.setExtData(data);
    }

    @Override
    public ExtendedFileInfoLoader.ExtendedFileInfo loadExtendedInfo()
    {
        return _base.loadExtendedInfo();
    }

    @Override
    public boolean needLoadExtendedInfo()
    {
        return _base.needLoadExtendedInfo();
    }

    @Override
    public Path getPath()
    {
        return _base.getPath();
    }

    @Override
    public String getPathDesc()
    {
        return _base.getPathDesc();
    }

    @Override
    public boolean isFile()
    {
        return _base.isFile();
    }

    @Override
    public boolean isDirectory()
    {
        return _base.isDirectory();
    }

    @Override
    public Date getModificationDate()
    {
        return _base.getModificationDate();
    }

    @Override
    public long getSize()
    {
        return _base.getSize();
    }

    @Override
    public void init(Path path) throws IOException
    {
        _base.init(path);
    }

    public BrowserRecord getBaseRecord()
    {
        return _base;
    }

    public FileManagerActivity getHost()
    {
        return _host;
    }

    protected BrowserRecordWrapper(BrowserRecord base)
    {
        _base = base;

    }

    private final BrowserRecord _base;
    private FileManagerActivity _host;
}
