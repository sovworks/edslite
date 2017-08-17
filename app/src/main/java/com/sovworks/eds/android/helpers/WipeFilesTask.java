package com.sovworks.eds.android.helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;

public class WipeFilesTask
{
	public interface ITask
	{
		boolean cancel();
		void progress(int sizeInc);
	}
	
	static public void wipeFileRnd(File file) throws IOException
    {
		wipeFileRnd(file, null);
    }
	
	public static void wipeFileRnd(File file,ITask task) throws IOException
    {			
		Random rg = new Random();
    	try
    	{
			byte[] buf = new byte[4*1024];    			
			long l = file.getSize();    	
	    	OutputStream s = file.getOutputStream();
	    	try
	    	{    	    		
    	    	for(long i=0;i<l;i+=buf.length)
    	    	{
    	    		if(task!=null && task.cancel())
    	    			return;
    	    		rg.nextBytes(buf);
    	    		s.write(buf);
    	    		long tmp = l - i - buf.length;
    	    		updStatus(task, tmp < 0 ? buf.length + tmp : buf.length);
    	    		
    	    	}
    	    	s.flush();
	    	}
	    	finally
	    	{
	    		s.close();
	    	}    			    	
    	}
	    finally
	    {
	    	file.delete();
	    }
    }
	
	public static void wipeFilesRnd(ITask task, Object syncer,boolean wipe,SrcDstCollection... records) throws IOException
	{
		for (SrcDstCollection col: records)
		{
			if(col!=null)
			{
				for(SrcDst rec: col)
				{
					if(task!=null && task.cancel())
						return;		
					Path p = rec.getSrcLocation().getCurrentPath();
					if(p.isFile())
					{
						if(syncer!=null)
						{
							synchronized (syncer)
							{
								wipeFile(p.getFile(),wipe,task);								
							}							
						}
						else
							wipeFile(p.getFile(),wipe,task);							
					}
					else if(p.isDirectory())
						p.getDirectory().delete();
				}
			}
		}
	}
	
	public static void wipeFile(File file,boolean wipe,ITask task) throws IOException
    {
		if(wipe)
			wipeFileRnd(file,task);
		else
		{
			file.delete();
			updStatus(task, 0);
		}
	}
	
	public WipeFilesTask(boolean wipe)
	{
		_wipe = wipe;		
	}	
	
	
	
	protected static void updStatus(ITask task, long sizeInc)
	{
		if(task == null)
			return;
		task.progress((int)sizeInc);	
	}
	
	protected Object _syncer;
	protected final boolean _wipe;
	
	protected ITask getITask()
	{
		return new ITask()
		{			
			@Override
			public boolean cancel()
			{
				return false;
			}

			@Override
			public void progress(int sizeInc)
			{			
				
			}
		};
	}
	
	protected void doWork(SrcDstCollection... records) throws Exception
	{
		wipeFilesRnd(getITask(), _syncer, _wipe, records);
	}	
}
