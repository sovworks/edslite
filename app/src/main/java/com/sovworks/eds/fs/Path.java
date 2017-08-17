package com.sovworks.eds.fs;

import java.io.IOException;

public interface Path extends Comparable<Path>
{
	FileSystem getFileSystem();
	String getPathString();
	boolean exists() throws IOException;
    boolean isFile() throws IOException;
    boolean isDirectory() throws IOException;
	String getPathDesc();
    boolean isRootDirectory() throws IOException;
    Path combine(String part) throws IOException;
    Directory getDirectory() throws IOException;
    File getFile() throws IOException;
	Path getParentPath() throws IOException;
}
