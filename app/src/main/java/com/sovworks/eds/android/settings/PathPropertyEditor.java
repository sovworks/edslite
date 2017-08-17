package com.sovworks.eds.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;

import java.io.IOException;

public abstract class PathPropertyEditor extends TextPropertyEditor
{

	public PathPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId, String hostFragmentTag)
	{
		super(host, R.layout.settings_path_editor, titleResId, descResId, hostFragmentTag);
	}
	
	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
		Button b = (Button)view.findViewById(android.R.id.button2);
		if(b!=null)
			b.setOnClickListener(new OnClickListener()
			{				
				@Override
				public void onClick(View v)
				{
					try
					{
						requestActivity(getSelectPathIntent(), SELECT_PATH_REQ_CODE);
					}
					catch(Exception e)
					{
						Logger.showAndLog(getHost().getContext(), e);
					}
				}
			});
		return view;
	}
	
	private static final int SELECT_PATH_REQ_CODE = 1;
	
	@Override
	protected void onPropertyRequestResult(int propertyRequestCode, int resultCode, Intent data)
	{
		if(propertyRequestCode == SELECT_PATH_REQ_CODE && resultCode == Activity.RESULT_OK && data!=null)
			onPathSelected(data);	
	}
	
	protected void onPathSelected(Intent result)
	{
		Uri uri = result.getData();
		onTextChanged(uri == null ? "" : uri.toString());
	}
	
	protected Intent getSelectPathIntent() throws IOException
	{
		return FileManagerActivity.getSelectPathIntent(
				getHost().getContext(),
				null,
				false,
				false,
				true,
				true,
				true,
				false);
	}
}
