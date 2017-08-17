package com.sovworks.eds.container;


import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

public interface VolumeLayout extends EncryptedFileLayout
{

    void initNew();

    boolean readHeader(RandomAccessIO input) throws IOException, ApplicationException;

    void writeHeader(RandomAccessIO output) throws  IOException, ApplicationException;

	void formatFS(RandomAccessIO output, FileSystemInfo fsInfo) throws ApplicationException, IOException;

    void setEngine(FileEncryptionEngine enc);

    void setHashFunc(MessageDigest hf);

    MessageDigest getHashFunc();

    void setPassword(byte[] password);

    void setNumKDFIterations(int num);

    List<FileEncryptionEngine> getSupportedEncryptionEngines();

    List<MessageDigest> getSupportedHashFuncs();

    void setOpeningProgressReporter(ContainerOpeningProgressReporter reporter);
}    