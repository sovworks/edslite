package com.sovworks.eds.locations;


import com.sovworks.eds.fs.util.ContainerFSWrapper;

import java.io.IOException;

public interface EDSLocation extends OMLocation
{
    interface ExternalSettings extends OMLocation.ExternalSettings
    {
        boolean shouldOpenReadOnly();
        void setOpenReadOnly(boolean val);
        int getAutoCloseTimeout();
        void setAutoCloseTimeout(int timeout);
    }

    interface InternalSettings
    {
    }

    @Override
    ExternalSettings getExternalSettings();
    InternalSettings getInternalSettings();
    void applyInternalSettings() throws IOException;
    void readInternalSettings() throws IOException;
    void writeInternalSettings() throws IOException;
    long getLastActivityTime();
    Location getLocation();
    @Override
    ContainerFSWrapper getFS() throws IOException;
    @Override
    EDSLocation copy();
}
