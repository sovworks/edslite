package com.sovworks.eds.fs;

import java.io.IOException;

public interface FileSystem
{
	Path getRootPath() throws IOException;
	Path getPath(String pathString) throws IOException;
	void close(boolean force) throws IOException;
	boolean isClosed();
}
