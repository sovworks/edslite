package com.sovworks.eds.android.filemanager;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DirectorySettings
{
	public static final String FILE_NAME = ".eds";
	
	public static final String PARAM_HIDDEN_FILES = "hidden_files_masks";

	public DirectorySettings()
	{

	}

	public DirectorySettings(String storedSettings) throws JSONException
	{
		this(new JSONObject(storedSettings));
	}

	public DirectorySettings(JSONObject settings)
	{
		_hiddenFilesList = getArrayParam(settings, PARAM_HIDDEN_FILES);
	}

	public ArrayList<String> getHiddenFilesMasks()
	{
		return _hiddenFilesList;
	}
	
	public void setHiddenFilesMasks(Collection<? extends String> masks)
	{
		_hiddenFilesList = new ArrayList<>();
		_hiddenFilesList.addAll(masks);
	}
	public String saveToString()
	{
		try
		{
			JSONObject jo = new JSONObject();
			if(_hiddenFilesList!=null) jo.put(PARAM_HIDDEN_FILES, new JSONArray(_hiddenFilesList));
			return jo.toString();
		}
		catch (JSONException e)
		{
			return "";
		}
	}

	public void saveToDir(Directory dir) throws IOException
	{
		Util.writeToFile(dir, DirectorySettings.FILE_NAME, saveToString());
	}

	private ArrayList<String> _hiddenFilesList;

	private static ArrayList<String> getArrayParam(JSONObject o,String name)
	{
		ArrayList<String> result = new ArrayList<>();
		JSONArray entries = getParam(o, name, (JSONArray) null);
		if (entries != null)
		{			
			for (int i = 0; i < entries.length(); i++)
			{
				String entry = entries.optString(i, null);
				if (entry != null) result.add(entry);
			}
		}		
		return result;
	}

	private static JSONArray getParam(JSONObject o, String name, JSONArray defaultValue)
	{
		try
		{
			return o.getJSONArray(name);
		}
		catch (JSONException e)
		{
			return defaultValue;
		}
	}

	private static String getParam(JSONObject o, String name, String defaultValue)
	{
		try
		{
			return o.getString(name);
		}
		catch (JSONException e)
		{
			return defaultValue;
		}
	}

}
