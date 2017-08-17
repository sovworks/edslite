package com.sovworks.eds.fs.util;



public class FilesOperationStatus implements Cloneable
{
	public FilesCountAndSize processed;
	public FilesCountAndSize total;
	public String fileName;
	
	public long prevUpdateTime, prevProcSize;

	public FilesOperationStatus()
	{
		total = new FilesCountAndSize();
		processed = new FilesCountAndSize();
	}
	
	public void updateProcessed()
	{
		
	}

	public FilesOperationStatus clone()
	{
		FilesOperationStatus res = new FilesOperationStatus();
		res.total = total;
		res.processed = processed;
		res.fileName = fileName;
		return res;
	}
}