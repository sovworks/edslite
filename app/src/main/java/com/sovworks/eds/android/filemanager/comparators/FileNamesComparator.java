package com.sovworks.eds.android.filemanager.comparators;

import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.records.LocRootDirRecord;

import java.io.IOException;
import java.util.Comparator;

public class FileNamesComparator implements Comparator<BrowserRecord>
{
	public FileNamesComparator(boolean asc)
	{
		_asc = asc ? 1 : -1;
	}

	public int compare(BrowserRecord o1, BrowserRecord o2)
	{
		int res;
		try
		{
			res = compareDirs(o1, o2);
			if(res == 0)
				res = compareImpl(o1, o2);
			return res == 0 ? compareImpl(o1, o2) : res;
		}
		catch (IOException e)
		{
			Logger.log(e);
			return 0;
		}
	}

	protected int _asc;

	protected int compareDirs(BrowserRecord o1, BrowserRecord o2) throws IOException
	{
		String n1 = o1.getName();
		String n2 = o2.getName();
		if("..".equals(n1) || o1 instanceof LocRootDirRecord)
			return -1;
		if("..".equals(n2) || o2 instanceof LocRootDirRecord)
			return 1;
		if(o1.isFile() && o2.isFile())
			return 0;
		if(o1.isFile())
			return 1;
		if(o2.isFile())
			return -1;
		return 0;
	}

	protected int compareImpl(BrowserRecord o1, BrowserRecord o2) throws IOException
	{
		String n1 = o1.getName();
		if(n1 == null)
			n1 = "";
		String n2 = o2.getName();
		if(n2 == null)
			n2 = "";
		return  _asc*n1.compareToIgnoreCase(n2);
	}
}
