package com.sovworks.eds.locations;


public interface OMLocation extends Openable
{
    interface ExternalSettings extends Location.ExternalSettings
    {
        void setPassword(byte[] password);
        byte[] getPassword();
        void setCustomKDFIterations(int val);
        int getCustomKDFIterations();
        boolean hasPassword();
    }
    @Override
    ExternalSettings getExternalSettings();
    boolean isOpenOrMounted();
}
