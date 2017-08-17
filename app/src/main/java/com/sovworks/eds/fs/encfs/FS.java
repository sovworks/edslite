package com.sovworks.eds.fs.encfs;

import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.android.helpers.ProgressReporter;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.kdf.HMACSHA1KDF;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.exceptions.WrongPasswordException;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.codecs.data.AESDataCodecInfo;
import com.sovworks.eds.fs.encfs.codecs.name.BlockCSNameCodecInfo;
import com.sovworks.eds.fs.encfs.codecs.name.BlockNameCodecInfo;
import com.sovworks.eds.fs.encfs.codecs.name.NullNameCodecInfo;
import com.sovworks.eds.fs.encfs.codecs.name.StreamNameCodecInfo;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;
import com.sovworks.eds.fs.util.FileSystemWrapper;
import com.sovworks.eds.fs.util.StringPathUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FS extends FileSystemWrapper
{
    public static Iterable<DataCodecInfo> getSupportedDataCodecs()
    {
        return Arrays.asList(_supportedDataCodecs);
    }

    public static Iterable<NameCodecInfo> getSupportedNameCodecs()
    {
        return Arrays.asList(_supportedNameCodecs);
    }

    public static final int KEY_CHECKSUM_BYTES = 4;

    public static byte[] deriveKey(byte[] password, byte[] salt, int numIterations, int keySize, int ivSize, ProgressReporter pr) throws EncryptionEngineException, DigestException
    {
        HMACSHA1KDF kdf = new HMACSHA1KDF();
        kdf.setProgressReporter(pr);
        return kdf.deriveKey(
                password,
                salt,
                numIterations,
                keySize + ivSize
        );
    }

    public FS(Path rootPath, Config config, byte[] password) throws ApplicationException, IOException
    {
        super(rootPath.getFileSystem());
        _config = config;
        _rootRealPath = rootPath;
        _encryptionKey = new byte[config.getDataCodecInfo().getFileEncDec().getKeySize()];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(_encryptionKey);
        encryptVolumeKeyAndWriteConfig(password);
        _rootPath = new RootPath();
    }

    public FS(Path rootPath, byte[] password) throws IOException, ApplicationException
    {
        this(rootPath, password, null);
    }

    public FS(Path rootPath, byte[] password, ContainerOpeningProgressReporter progressReporter) throws IOException, ApplicationException
    {
        super(rootPath.getFileSystem());
        setOpeningProgressReporter(progressReporter);
        _config = readConfig(rootPath);
        _rootRealPath = rootPath;
        byte[] derivedKey = null;
        try
        {
            if(_progressReporter!=null)
            {
                _progressReporter.setCurrentEncryptionAlgName(_config.getDataCodecInfo().getName());
                _progressReporter.setCurrentKDFName("SHA1");
            }
            derivedKey = deriveKey(password);
            _encryptionKey = decryptVolumeKey(derivedKey);
            _rootPath = new RootPath();
        }
        catch (DigestException e)
        {
            throw new ApplicationException("Failed deriving the key", e);
        }
        finally
        {
            if(derivedKey!=null)
                Arrays.fill(derivedKey, (byte)0);
        }
    }

    public Config getConfig()
    {
        return _config;
    }

    public void setOpeningProgressReporter(ContainerOpeningProgressReporter r)
    {
        _progressReporter = r;
    }


    public void encryptVolumeKeyAndWriteConfig(byte[] password) throws ApplicationException, IOException
    {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[20];
        sr.nextBytes(salt);
        _config.setSalt(salt);
        byte[] derivedKey = null;
        try
        {
            derivedKey = deriveKey(password);
            _config.setEncryptedVolumeKey(encryptVolumeKey(derivedKey));
        }
        catch (DigestException e)
        {
            throw new ApplicationException("Failed deriving the key", e);
        }
        finally
        {
            if(derivedKey!=null)
                Arrays.fill(derivedKey, (byte)0);
        }
        _config.write(_rootRealPath);
    }

    @Override
    public com.sovworks.eds.fs.encfs.Path getRootPath()
    {
        return _rootPath;
    }

    @Override
    public synchronized com.sovworks.eds.fs.encfs.Path getPath(String pathString) throws IOException
    {
        return getPathFromRealPath(getBase().getPath(pathString));
    }

    public Path getEncFSRootPath()
    {
        return _rootRealPath;
    }

    @Override
    public void close(boolean force) throws IOException
    {
        if(_encryptionKey!=null)
        {
            Arrays.fill(_encryptionKey, (byte) 0);
            _encryptionKey = null;
        }
        /*
        try
        {
            super.close(force);
        }
        finally
        {
            if(_encryptionKey!=null)
            {
                Arrays.fill(_encryptionKey, (byte) 0);
                _encryptionKey = null;
            }
        }*/
    }

    synchronized com.sovworks.eds.fs.encfs.Path getPathFromRealPath(Path realPath) throws IOException
    {
        if(realPath == null)
            return null;
        if(realPath.equals(_rootRealPath))
            return _rootPath;
        com.sovworks.eds.fs.encfs.Path p = getCachedPath(realPath);
        if(p==null)
        {
            p = new com.sovworks.eds.fs.encfs.Path(
                    this,
                    realPath,
                    getConfig().getNameCodecInfo(),
                    _encryptionKey
            );
            _cache.put(realPath, p);
        }
        return p;
    }

    com.sovworks.eds.fs.encfs.Path getCachedPath(Path realPath) throws IOException
    {
        return _cache.get(realPath);
    }

    private class RootPath extends com.sovworks.eds.fs.encfs.Path
    {
        public RootPath()
        {
            super(FS.this, _rootRealPath, getConfig().getNameCodecInfo(), _encryptionKey);
            setDecodedPath(new StringPathUtil());
            setEncodedPath(new StringPathUtil());
        }

        @Override
        public boolean isRootDirectory() throws IOException
        {
            return true;
        }

        @Override
        public synchronized byte[] getChainedIV()
        {
            return new byte[getNamingCodecInfo().getEncDec().getIVSize()];
        }

        @Override
        public com.sovworks.eds.fs.encfs.Path getParentPath() throws IOException
        {
            return null;
        }
    }
    private static final DataCodecInfo[] _supportedDataCodecs = new DataCodecInfo[] { new AESDataCodecInfo() };
    private static final NameCodecInfo[] _supportedNameCodecs = new NameCodecInfo[] {
            new BlockNameCodecInfo(),
            new BlockCSNameCodecInfo(),
            new StreamNameCodecInfo(),
            new NullNameCodecInfo()
    };

    private final Path _rootRealPath;
    private final Map<com.sovworks.eds.fs.Path, com.sovworks.eds.fs.encfs.Path> _cache = new HashMap<>();
    private final RootPath _rootPath;
    private byte[] _encryptionKey;
    private Config _config;
    private ContainerOpeningProgressReporter _progressReporter;

    private byte[] deriveKey(byte[] password) throws EncryptionEngineException, DigestException
    {
        return deriveKey(
                password,
                getConfig().getSalt(),
                getConfig().getKDFIterations(),
                getConfig().getKeySize(),
                getConfig().getDataCodecInfo().getFileEncDec().getIVSize(),
                _progressReporter
        );
    }

    private byte[] decryptVolumeKey(byte[] derivedKey) throws EncryptionEngineException, WrongPasswordException
    {
        byte[] encryptedVolumeKey = getConfig().getEncryptedVolumeKey();
        int checksum = 0;
        for(int i=0;i<KEY_CHECKSUM_BYTES;i++)
            checksum = (checksum << 8) | (encryptedVolumeKey[i] & 0xFF);
        byte[] volumeKey = Arrays.copyOfRange(encryptedVolumeKey, KEY_CHECKSUM_BYTES, encryptedVolumeKey.length);
        EncryptionEngine ee = getConfig().getDataCodecInfo().getStreamEncDec();
        try
        {
            ee.setKey(derivedKey);
            ee.init();
            ee.setIV(ByteBuffer.allocate(ee.getIVSize()).putLong(checksum & 0xFFFFFFFFL).array());
            ee.decrypt(volumeKey, 0, volumeKey.length);
        }
        finally
        {
            ee.close();
        }

        MACCalculator cc = getConfig().getDataCodecInfo().getChecksumCalculator();
        try
        {
            cc.init(derivedKey);
            int checksum2 = cc.calc32(volumeKey, 0, volumeKey.length);
            if (checksum2 != checksum)
                throw new WrongPasswordException();
        }
        finally
        {
            cc.close();
        }
        return volumeKey;
    }

    private byte[] encryptVolumeKey(byte[] derivedKey) throws EncryptionEngineException
    {
        DataCodecInfo dataCodec = _config.getDataCodecInfo();
        byte[] volumeKey = _encryptionKey;
        int checksum;
        MACCalculator cc = dataCodec.getChecksumCalculator();
        try
        {
            cc.init(derivedKey);
            checksum = cc.calc32(volumeKey, 0, volumeKey.length);
        }
        finally
        {
            cc.close();
        }

        byte[] res = new byte[volumeKey.length + FS.KEY_CHECKSUM_BYTES];
        System.arraycopy(volumeKey, 0, res, FS.KEY_CHECKSUM_BYTES, volumeKey.length);

        EncryptionEngine ee = dataCodec.getStreamEncDec();
        try
        {
            ee.setKey(derivedKey);
            ee.init();
            ee.setIV(ByteBuffer.allocate(ee.getIVSize()).putLong(checksum & 0xFFFFFFFFL).array());
            ee.encrypt(res, FS.KEY_CHECKSUM_BYTES, volumeKey.length);
        }
        finally
        {
            ee.close();
        }

        for (int i = 1; i <= FS.KEY_CHECKSUM_BYTES; ++i)
        {
            res[FS.KEY_CHECKSUM_BYTES - i] = (byte)checksum;
            checksum >>= 8;
        }
        return res;
    }

    private Config readConfig(Path rootFolderPath) throws IOException, ApplicationException
    {
        Config cfg = new Config();
        cfg.read(rootFolderPath);
        return cfg;
    }
}
