package com.sovworks.eds.fs.util;

import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class PFDRandomAccessIO extends FDRandomAccessIO
{
    public PFDRandomAccessIO(ParcelFileDescriptor pfd)
    {
        super(pfd.getFd());
        _pfd = pfd;
    }

    @Override
    public void close() throws IOException
    {
        _pfd.close();
        super.close();
    }

    private final ParcelFileDescriptor _pfd;
}
