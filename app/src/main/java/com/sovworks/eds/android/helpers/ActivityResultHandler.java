package com.sovworks.eds.android.helpers;

import java.util.ArrayList;
import java.util.List;

public class ActivityResultHandler
{
	public void addResult(Runnable r)
	{
		if(_isResumed)
			r.run();
		else
			_receivers.add(r);
	}
	
	public void handle()
	{
		for(Runnable r: _receivers)
			r.run();
		_receivers.clear();
		_isResumed = true;

	}

	public void onPause()
	{
		_isResumed = false;
	}

    public void clear()
    {
        _receivers.clear();
    }
	
	private final List<Runnable> _receivers = new ArrayList<Runnable>();
	private boolean _isResumed;

}
