package com.sovworks.eds.android.filemanager.comparators;

import com.sovworks.eds.android.helpers.CachedPathInfo;

import java.io.IOException;
import java.util.Date;

public class ModDateComparator<T extends CachedPathInfo> extends FileNamesComparator<T>
{
	public ModDateComparator(boolean asc)
	{
		super(asc);
	}

	@Override
	protected int compareImpl(T o1, T o2) throws IOException
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
