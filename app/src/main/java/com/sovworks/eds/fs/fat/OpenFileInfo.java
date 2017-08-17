package com.sovworks.eds.fs.fat;

import com.sovworks.eds.fs.File;

class OpenFileInfo
{
	public OpenFileInfo(File.AccessMode mode,Object opTag)
	{
		this.accessMode = mode;		
		this.opTag = opTag;
	}

	public File.AccessMode accessMode;
	public int refCount = 1;
	public Object opTag;
}