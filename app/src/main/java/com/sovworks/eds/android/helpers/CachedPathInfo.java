package com.sovworks.eds.android.helpers;

import java.io.IOException;
import java.util.Date;

import com.sovworks.eds.fs.Path;

public interface CachedPathInfo
{	
	Path getPath();
	String getPathDesc();
	String getName();
	boolean isFile();
	boolean isDirectory();	
	Date getModificationDate();
	long getSize();
	void init(Path path) throws IOException;
}
