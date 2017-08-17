package com.sovworks.eds.android.helpers;

public interface ContainerOpeningProgressReporter extends ProgressReporter
{
    void setCurrentKDFName(String name);

    void setCurrentEncryptionAlgName(String name);

    void setContainerFormatName(String name);

    void setIsHidden(boolean val);
}
