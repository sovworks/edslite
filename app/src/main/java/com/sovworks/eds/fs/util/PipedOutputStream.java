package com.sovworks.eds.fs.util;

import java.io.IOException;
import java.io.OutputStream;

public class PipedOutputStream extends OutputStream
{
	public PipedOutputStream(PipedInputStream inp)
	{
		_input = inp;
	}

	@Override
	public synchronized void write(int oneByte) throws IOException
	{
		_oneByteBuffer[0] = (byte)oneByte;
		write(_oneByteBuffer, 0, 1);
	}
	
	@Override
	public void write(byte[] buf, int offset, int len) throws IOException
	{
		if (len <= 0) return;	
		while(len>0)
		{
			int nb = _input.write(buf, offset, len);
			if(nb == -1)
				throw new IOException("Input stream is closed");
			offset += nb;
			len -= nb;
		}		
	}
	
	@Override
	public void flush()
	{
		_input.notifyBuffer();
	}
	
	@Override
	public void close() throws IOException
	{		
		_input.finWrite();		
	}


	private final byte[] _oneByteBuffer = new byte[1];
	private final PipedInputStream _input;
}
