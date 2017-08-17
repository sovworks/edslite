package com.sovworks.eds.container;


import com.sovworks.eds.crypto.FileEncryptionEngine;

import java.io.Closeable;

public interface EncryptedFileLayout extends Closeable
{
    long getEncryptedDataOffset();

    long getEncryptedDataSize(long fileSize);

    FileEncryptionEngine getEngine();

    void setEncryptionEngineIV(FileEncryptionEngine eng, long decryptedVolumeOffset);
}    