package com.sovworks.eds.fs;

import java.io.IOException;

public interface DataOutput
{
	/**
	 * Writes the specified byte to this file. The write starts at the current file pointer. 
	 * @param b the byte to be written. 
	 * @throws IOException
	 */
	void write(int b) throws IOException;
	
	/**
	 * Writes len bytes from the specified byte array starting at offset off to this file.
	 * @param b the data.
	 * @param off - the start offset in the data.
	 * @param len - the number of bytes to write. 
	 * @throws IOException
	 */
	void write(byte[] b, int off, int len) throws IOException;
	
	void flush() throws IOException;
}
