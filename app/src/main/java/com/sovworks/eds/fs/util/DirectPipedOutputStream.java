package com.sovworks.eds.fs.util;

import java.io.IOException;
import java.io.OutputStream;

public class DirectPipedOutputStream extends OutputStream
{
	public DirectPipedOutputStream(DirectPipedInputStream inp)
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
			int nb = 0;
			byte[] dest = _input.getBuffer();
			try
			{			
				if(dest == null)
					throw new IOException("Input stream is closed");
				nb = Math.min(len, _input.getRequestedBytes());
				System.arraycopy(buf, offset, dest, _input.getOffset(), nb);
				offset += nb;
				len -= nb;
			}
			finally
			{
				_input.releaseBuffer(nb);
			}
		}		
	}
		
	@Override
	public void close() throws IOException
	{		
		_input.finWrite();		
	}
	private final byte[] _oneByteBuffer = new byte[1];
	private final DirectPipedInputStream _input;
}
