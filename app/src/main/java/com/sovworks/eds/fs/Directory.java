package com.sovworks.eds.fs;

import java.io.Closeable;
import java.io.IOException;

public interface Directory extends FSRecord
{
	interface Contents extends Iterable<Path>, Closeable
	{
		
	}
	Directory createDirectory(String name) throws IOException;
	File createFile(String name) throws IOException;
	Contents list() throws IOException;
	long getTotalSpace() throws IOException;
	long getFreeSpace() throws IOException;
}
