package com.sovworks.eds.fs;

import java.io.IOException;

public interface DataInput
{
	/**
	 * Reads a byte of data from this file. The byte is returned as an integer in the range 0 to 255 (0x00-0x0ff). 
	 * @return the next byte of data, or -1 if the end of the file has been reached. 
	 * @throws IOException if an I/O error occurs. Not thrown if end-of-file has been reached.
	 */
	int read() throws IOException;
	
	/**
	 * Reads up to len bytes of data from this file into an array of bytes. 
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in array b at which the data is written.
	 * @param len the maximum number of bytes read. 
	 * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
	 * @throws IOException
	 */
	int read(byte[] b, int off, int len) throws IOException;
}
