package com.sovworks.eds.android.filemanager.comparators;

import com.sovworks.eds.android.filemanager.records.BrowserRecord;

import java.io.IOException;
import java.util.Date;

public class ModDateComparator extends FileNamesComparator
{
	public ModDateComparator(boolean asc)
	{
		super(asc);
	}

	@Override
	protected int compareImpl(BrowserRecord o1, BrowserRecord o2) throws IOException
	{
		Date aDate = o1.getModificationDate();
		if(aDate == null)
			aDate = new Date();
		Date bDate = o2.getModificationDate();
		if(bDate == null)
			bDate = new Date();
		int res = _asc*aDate.compareTo(bDate);
		return res == 0 ? super.compareImpl(o1, o2) : res;
	}
}
