package com.sovworks.eds.fs.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;


public class FilesCountAndSize
{
	public FilesCountAndSize()
	{
		
	}
	
	public int filesCount;
	public long totalSize;
	
	public static FilesCountAndSize getFilesCount(SrcDstCollection srcDstCol)
	{
		FilesCountAndSize result = new FilesCountAndSize();
		Iterator<?> iter = srcDstCol.iterator();
		while(iter.hasNext())
		{
			result.filesCount++;
			iter.next();
		}				
		
		return result;
	}
	
	public static FilesCountAndSize getFilesCountAndSize(boolean countDirs, SrcDstCollection srcDstCol)
	{
		FilesCountAndSize result = new FilesCountAndSize();
		for (SrcDst srcDst : srcDstCol)
		{
			try
			{
				getFilesCountAndSize(result,srcDst.getSrcLocation().getCurrentPath(),countDirs);
			}
			catch (IOException ignored)
			{
			}
		}
		return result;
	}

	public static void getFilesCountAndSize(FilesCountAndSize result, Path path,boolean countDirs) throws IOException
	{				
		if (path.isFile())
		{
			result.totalSize += path.getFile().getSize();
			result.filesCount++;
		}
		else if(countDirs)
			result.filesCount++;
	}
}