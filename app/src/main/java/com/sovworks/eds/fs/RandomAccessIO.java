package com.sovworks.eds.fs;

import java.io.Closeable;
import java.io.IOException;

public interface RandomAccessIO extends Closeable,RandomStorageAccess,DataInput,DataOutput
{
	void setLength(long newLength) throws IOException; 
}
