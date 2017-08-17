package com.sovworks.eds.android.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sovworks.eds.android.R;

public abstract class PropertyEditorBase implements PropertyEditor
{
	public static int getReqCode(int fieldIdx, int fieldRecCode)
	{
		return (fieldRecCode<<8) + fieldIdx;
	}
	
	public static int getFieldId(int reqCode)
	{
		return reqCode & 0xFF;
	}
	
	public static int getFieldReqCode(int reqCode)
	{
		return reqCode >> 8;
	}

	@Override
	public int getId()
	{
		return _propertyId;
	}
	
	@Override
	public void setId(int id)
	{
		_propertyId = id;
	}
	
	@Override
	public synchronized View getView(ViewGroup parent)
	{
		if(_view == null)
			_view = createView(parent);

		return _view;
	}

	@Override
	public PropertyEditor.Host getHost()
	{
		return _host;
	}	
	
	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(getFieldId(requestCode) == getStartPosition())
		{
			onPropertyRequestResult(getFieldReqCode(requestCode), resultCode, data);
			return true;
		}
		return false;
	}
	
	@Override
	public void onClick()
	{
		
	}

    @Override
    public void load()
    {
    }

    @Override
    public void save() throws Exception
	{
    }

    @Override
    public void save(Bundle b)
    {
    }

    @Override
    public void load(Bundle b)
    {
    }

	@Override
	public int getStartPosition()
	{
		return _startPosition;
	}

	@Override
	public void setStartPosition(int pos)
	{
		_startPosition = pos;
	}

	protected PropertyEditorBase(PropertyEditor.Host host , int layoutResId, int titleResId, int descResId)
	{
		_layoutResId = layoutResId;
		_host = host;
		_titleResId = titleResId;
		_descResId = descResId;
		_propertyId = _titleResId;
		_title = null;
		_desc = null;
		//_view = createView();
	}

	protected PropertyEditorBase(PropertyEditor.Host host, int propertyId , int layoutResId, String title, String desc)
	{
		_layoutResId = layoutResId;
		_host = host;
		_title = title;
		_desc = desc;
		_propertyId = propertyId;
		_titleResId = 0;
		_descResId = 0;
		_title = title;
		_desc = desc;
	}
	
	protected final int _layoutResId, _titleResId, _descResId;
	protected String _title, _desc;
	protected final PropertyEditor.Host _host;	
	protected View _view;
	
	protected View createView(ViewGroup parent)
	{
		LayoutInflater li = (LayoutInflater)getHost().getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);	  
		View view = li.inflate(_layoutResId, parent, false);
		TextView tv;
		tv = view.findViewById(R.id.title_edit);
		if (tv != null)
		{
			if(_titleResId != 0)
				tv.setText(_titleResId);
			else if(_title != null)
				tv.setText(_title);
		}

		tv = view.findViewById(R.id.desc);
		if (tv != null)
		{
			if (_descResId != 0)
				tv.setText(_descResId);
			else if(_desc != null)
				tv.setText(_desc);
			else
				tv.setVisibility(View.GONE);
		}
		return view;
	}
	
	protected String getBundleKey()
	{
		return "property_" + _propertyId;
	}
	
	protected void requestActivity(Intent i, int propRequestCode)
	{
		getHost().startActivityForResult(i, getReqCode(getStartPosition(), propRequestCode));
	}

	protected boolean isInstantSave()
	{
		return getHost().getPropertiesView().isInstantSave();
	}
	
	protected void onPropertyRequestResult(int propertyRequestCode, int resultCode, Intent data)
	{
	}

	private int _propertyId;
	private int _startPosition;

}
