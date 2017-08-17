package com.sovworks.eds.fs;

import java.io.IOException;

public interface RandomStorageAccess
{
	void seek(long position) throws IOException;

	long getFilePointer() throws IOException;
	
	long length() throws IOException;
}
