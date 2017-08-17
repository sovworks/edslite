package com.sovworks.eds.android.service;

public class Result
{
	public Result()
	{
		this(null);
	}
	
	public Result(Object result)
	{
		_result = result;
		_error = null;
		_isCancelled = false;
	}
	
	public Result(Throwable error,boolean isCancelled)
	{
		_result = null;
		_error = error;
		_isCancelled = isCancelled;
	}

	public Throwable getError()
	{
		return _error;
	}
	
	public boolean isCancelled()
	{
		return _isCancelled;
	}
	
	public Object getResult() throws Throwable		 
	{
		if(_error!=null)
			throw _error;
		return _result;
	}
		
	private final boolean _isCancelled;
	private final Throwable _error;
	private final Object _result;
}