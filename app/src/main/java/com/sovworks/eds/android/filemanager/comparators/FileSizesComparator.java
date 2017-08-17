package com.sovworks.eds.android.filemanager.comparators;

import com.sovworks.eds.android.filemanager.records.BrowserRecord;

import java.io.IOException;

public class FileSizesComparator extends FileNamesComparator
{
	public FileSizesComparator(boolean asc)
	{
		super(asc);
	}

	@Override
	protected int compareDirs(BrowserRecord o1, BrowserRecord o2) throws IOException
	{
		int res = super.compareDirs(o1, o2);
		return res == 0 && (!o1.isFile() || !o2.isFile()) ?
				super.compareImpl(o1, o2)
			:
				res;
	}

	@Override
	protected int compareImpl(BrowserRecord o1, BrowserRecord o2) throws IOException
	{
		long aSize = o1.getSize();
		long bSize = o2.getSize();
		return (aSize == bSize)
				? super.compareImpl(o1, o2)
				: ((aSize < bSize)
						? -_asc
						: _asc
				);
	}
}
