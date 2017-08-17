package com.sovworks.eds.fs.util;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class StringPathUtil implements Comparable<StringPathUtil>
{

	public static List<String> splitPath(String path)
	{
		ArrayList<String> res = new ArrayList<>();
		if(path != null)
			for(String s: path.split(Pattern.quote(File.separator)))
			{
				if(!s.trim().isEmpty())
					res.add(s);
			}
		return res;
	}	
	
	public static String joinPath(String... components)
	{
		return joinPath(Arrays.asList(components),0,components.length);
	}
	
	public static String joinPath(List<String> components,int off,int count)
	{
		if(count == 0)
			return File.separator;
		StringBuilder res = new StringBuilder(File.separator);
		for(int i=0;i<count;i++)
		{
			res.append(components.get(i+off));
			res.append(File.separatorChar);
		}
		res.deleteCharAt(res.length() - 1);
		return res.toString();
	}
	
	public static String getSubPath(String srcPath,int numToRemove)
	{
		List<String> components = splitPath(srcPath);
		if(components.size()<numToRemove + 1)
			return "";		
		return joinPath(components,numToRemove,components.size() - numToRemove);
	}
	
	public static String getSubPath(String srcPath,String parentPath)
	{
		return getSubPath(srcPath,splitPath(parentPath).size());
	}
	
	public static String getFileNameWithoutExtension(String fn)
	{		
		int dotIndex = fn.lastIndexOf('.');
		return dotIndex > 0 ? fn.substring(0, dotIndex) : fn;
	}

	public static String getFileExtension(String fn)
	{		
		int dotIndex = fn.lastIndexOf('.');
		return dotIndex > 0 ? fn.substring(dotIndex + 1) : "";
	}
	
	public StringPathUtil()
	{
		_components = new ArrayList<>();
	}
	
	public StringPathUtil(String pathString)
	{
		_components = splitPath(pathString);
	}

	public StringPathUtil(String... components)
	{		
		_components = Arrays.asList(components);
	}

	public StringPathUtil(List<String> components)
	{
		_components = new ArrayList<>(components);
	}
	
	public StringPathUtil(StringPathUtil p1, String... components)
	{
		this(p1, Arrays.asList(components));
	}

	public StringPathUtil(StringPathUtil p1, List<String> components)
	{
		_components = new ArrayList<>(p1._components);
		_components.addAll(components);
	}

	public StringPathUtil(String part, StringPathUtil parts)
	{
		_components = new ArrayList<>(parts._components);
		_components.add(0, part);
	}

	public StringPathUtil(StringPathUtil p1, StringPathUtil p2)
	{
		this(p1,p2._components);
	}	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof StringPathUtil)
		{
			List<String> ocomponents = ((StringPathUtil)o)._components;
			if(ocomponents.size() != _components.size())
				return false;
			for(int i=0;i<ocomponents.size();i++)
				if(!ocomponents.get(i).equalsIgnoreCase(_components.get(i)))
					return false;
					
			return true;
		}
		
		if(o instanceof String)		
			return equals(new StringPathUtil((String)o));
		
		return super.equals(o);		
	}
	
	@Override
	public int hashCode()
	{
		int res = 0;
		for(String c: _components)
			res ^= c.toLowerCase().hashCode();
		return res;
	}	
	
	public StringPathUtil combine(String part)
	{
		return combine(new StringPathUtil(part));
	}

	public StringPathUtil combine(StringPathUtil part)
	{
		return new StringPathUtil(this, part);
	}	
	
	public boolean isEmpty()
	{
		return _components.isEmpty();
	}
	
	public boolean isSpecial()
	{
		String n = getFileName();
		return n.equals(".") || n.equals("..");
	}

	public String[] getComponents()
	{
		return _components.toArray(new String[_components.size()]);
	}

	public int getNumComponents()
	{
		return _components.size();
	}

	public StringPathUtil getParentPath()
	{
		if(_components.size() < 2)
			return new StringPathUtil();
		return new StringPathUtil(_components.subList(0, _components.size() - 1));
	}
	
	public StringPathUtil getSubPath(int numToRemove)
	{
		if(_components.size() < numToRemove + 1)
			return new StringPathUtil();
		return new StringPathUtil(_components.subList(numToRemove, _components.size()));
	}
	
	public StringPathUtil getSubPath(StringPathUtil parentPath)
	{
		return getSubPath(parentPath._components.size());
	}

	public String getFileName()
	{
		return !_components.isEmpty() ? _components.get(_components.size() - 1) : "";
	}

	public String getFileNameWithoutExtension()
	{
		return getFileNameWithoutExtension(getFileName());
	}

	public String getFileExtension()
	{
		return getFileExtension(getFileName());
	}
	
	public boolean isParentDir(StringPathUtil subPath)
	{
		int s = _components.size();
		if(subPath._components.size()<=s)
			return false;
		
		for(int i=0;i<s;i++)
			if(!_components.get(i).equalsIgnoreCase(subPath._components.get(i)))
				return false;

		return true;
	}

	@Override
	public String toString()
	{
		return joinPath(_components, 0, _components.size());
	}
	
	@Override
	public int compareTo(@NonNull StringPathUtil other)
	{
		return toString().compareTo(other.toString());
	}	
	
	protected final List<String> _components;
}
