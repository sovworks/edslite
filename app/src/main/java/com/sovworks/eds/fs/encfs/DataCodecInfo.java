package com.sovworks.eds.fs.encfs;

import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;

public interface DataCodecInfo extends AlgInfo
{
    FileEncryptionEngine getFileEncDec();
    EncryptionEngine getStreamEncDec();
    MACCalculator getChecksumCalculator();
}
