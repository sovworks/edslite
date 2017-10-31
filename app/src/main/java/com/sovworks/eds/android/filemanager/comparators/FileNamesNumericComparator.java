package com.sovworks.eds.android.filemanager.comparators;

import com.sovworks.eds.android.helpers.CachedPathInfo;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNamesNumericComparator<T extends CachedPathInfo> extends FileNamesComparator<T>
{
	public FileNamesNumericComparator(boolean asc)
	{
		super(asc);
	}

	protected int compareImpl(T o1, T o2) throws IOException
	{
		String n1 = o1.getName();
		if(n1 == null)
			n1 = "";
		String n2 = o2.getName();
		if(n2 == null)
			n2 = "";

		boolean b1 = false, b2 = false;
		int v1 = 0, v2 = 0;
		Matcher m = PATTERN.matcher(n1);
		if(m.find())
		{
			v1 = Integer.parseInt(m.group(1));
			b1 = true;
		}
		m = PATTERN.matcher(n2);
		if(m.find())
		{
			v2 = Integer.parseInt(m.group(1));
			b2 = true;
		}
		if(b1 && b2)
		{
			int res = _asc * Integer.valueOf(v1).compareTo(v2);
			return res == 0 ? super.compareImpl(o1, o2) : res;
		}
		if(b1)
			return -_asc;
		if(b2)
			return _asc;
		return super.compareImpl(o1, o2);
	}

	private static final Pattern PATTERN = Pattern.compile("((?:-|\\+)?[0-9]+)", 0);
}
