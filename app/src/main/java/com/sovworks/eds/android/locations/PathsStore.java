package com.sovworks.eds.android.locations;

import android.net.Uri;

import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PathsStore
{

	public PathsStore(LocationsManager lm)
    {
        _lm = lm;
    }

	public JSONObject getParamsStore()
    {
        return _params;
    }

	public Location getLocation()
    {
        return _location;
    }

	public void setLocation(Location location)
    {
        _location = location;
    }

	public ArrayList<Path> getPathsStore()
    {
        return _paths;
    }

	public boolean isPathStoreData(String data)
    {
        JSONObject jo;
        try
        {
            jo = new JSONObject(data);
            if (jo.has("location"))
				return true;

		}
        catch (JSONException ignored)
        {
		}

		try
        {
			Uri uri = Uri.parse(data);
            return uri.getPath() != null;

		}
        catch (Exception e)
        {
            return false;
        }
	}

	public boolean load(String data)
    {
        _paths.clear();
        _params = new JSONObject();
        JSONObject jo;
        try
        {
            jo = new JSONObject(data);
            if (jo.has("location"))
            {
                try
                {
                    loadFromJO(jo);
                }
                catch (Exception e)
                {
                    return false;
                }
                return true;
            }
        }
        catch (JSONException ignored)
        {
		}

		try
        {
            Uri uri = Uri.parse(data);
            loadFromUri(uri);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
	}

	@Override
    public String toString()
    {
        if (_location == null)
            return super.toString();
        if (_paths.isEmpty() && _params.length() == 0)
            return _location.getLocationUri().toString();
        JSONObject jo = new JSONObject();
        try
        {
            jo.put("location", _location.getLocationUri().toString());
            if (!_paths.isEmpty())
            {
                JSONArray ja = new JSONArray();
                for (Path p : _paths)
                    ja.put(p.getPathString());
                jo.put("paths", ja);
            }
            if (_params.length() > 0)
                jo.put("params", _params);
        }
        catch (JSONException e)
        {
            return "error";
        }
        return jo.toString();
    }

	private final LocationsManager _lm;
    private Location _location;
    private final ArrayList<Path> _paths = new ArrayList<>();
    private JSONObject _params = new JSONObject();

	private void loadFromJO(JSONObject jo) throws Exception
    {
        String uriString = jo.getString("location");
        Uri uri = Uri.parse(uriString);
        _location = _lm.getLocation(uri);
        if (jo.has("paths"))
        {
            JSONArray ja = jo.getJSONArray("paths");
            for (int i = 0; i < ja.length(); i++)
                _paths.add(_location.getFS().getPath(ja.getString(i)));
        }
        if (jo.has("params"))
            _params = jo.getJSONObject("params");

	}

	private void loadFromUri(Uri uri) throws Exception
    {
        _location = _lm.getLocation(uri);
        _paths.add(_location.getCurrentPath());
    }

}
