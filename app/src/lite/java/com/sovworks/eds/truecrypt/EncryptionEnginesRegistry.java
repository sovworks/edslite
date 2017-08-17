package com.sovworks.eds.truecrypt;

import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.engines.AESXTS;
import com.sovworks.eds.crypto.engines.SerpentXTS;
import com.sovworks.eds.crypto.engines.TwofishXTS;

import java.util.Arrays;
import java.util.List;

public class EncryptionEnginesRegistry
{
    public static List<FileEncryptionEngine> getSupportedEncryptionEngines()
    {
        return Arrays.<FileEncryptionEngine>asList(
                new AESXTS(),
                new SerpentXTS(),
                new TwofishXTS()
        );
    }

    public static String getEncEngineName(EncryptionEngine eng)
    {
        if(eng instanceof AESXTS)
            return "AES";
        if(eng instanceof SerpentXTS)
            return "Serpent";
        if(eng instanceof TwofishXTS)
            return "Twofish";
        return String.format("%s-%s", eng.getCipherName(), eng.getCipherModeName());
    }
}
