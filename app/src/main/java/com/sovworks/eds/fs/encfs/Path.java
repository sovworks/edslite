package com.sovworks.eds.fs.encfs;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.util.PathBase;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.StringPathUtil;

import java.io.IOException;

public class Path extends PathBase
{
    public Path(
            FS fs,
            com.sovworks.eds.fs.Path realPath,
            NameCodecInfo namingAlg,
            byte[] encryptionKey)
    {
        super(fs);
        _realPath = realPath;
        _namingInfo = namingAlg;
        _encryptionKey = encryptionKey;
    }

    @Override
    public FS getFileSystem()
    {
        return (FS)super.getFileSystem();
    }

    public com.sovworks.eds.fs.Path getRealPath()
    {
        return _realPath;
    }

    public NameCodecInfo getNamingCodecInfo()
    {
        return _namingInfo;
    }

    @Override
    public String getPathString()
    {
        return _realPath.getPathString();
    }

    @Override
    public String getPathDesc()
    {
        return getDecodedPath().toString();
    }

    @Override
    public boolean exists() throws IOException
    {
        return getRealPath().exists();
    }

    @Override
    public boolean isFile() throws IOException
    {
        return getRealPath().isFile();
    }

    @Override
    public boolean isDirectory() throws IOException
    {
        return getRealPath().isDirectory();
    }

    @Override
    public Path getParentPath() throws IOException
    {
        if(_realPath.equals(getFileSystem().getEncFSRootPath()))
            return null;
        com.sovworks.eds.fs.Path pp = _realPath.getParentPath();
        return pp == null ? null : getFileSystem().getPathFromRealPath(pp);
    }

    @Override
    public Path combine(String part) throws IOException
    {
        StringPathUtil encodedParts = calcCombinedEncodedParts(part);
        com.sovworks.eds.fs.Path newRealPath = _realPath.combine(encodedParts.getFileName());
        Path newPath = getFileSystem().getPathFromRealPath(newRealPath);
        if(newPath._decodedPath == null)
        {
            StringPathUtil decodedParts = _decodedPath;
            if (decodedParts != null)
                decodedParts = decodedParts.combine(part);
            newPath.setDecodedPath(decodedParts);
        }
        if(newPath._encodedPath == null)
            newPath.setEncodedPath(encodedParts);

        return newPath;
    }

    public StringPathUtil calcCombinedEncodedParts(String part) throws IOException
    {
        StringPathUtil encodedParts = getEncodedPath();
        NameCodec codec = _namingInfo.getEncDec();
        byte[] iv = _namingInfo.useChainedNamingIV() ? getChainedIV() : null;
        try
        {
            codec.init(_encryptionKey);
            if (iv != null)
                codec.setIV(iv);
            return encodedParts.combine(codec.encodeName(part));
        }
        finally
        {
            codec.close();
        }
    }

    @Override
    public Directory getDirectory() throws IOException
    {
        return new com.sovworks.eds.fs.encfs.Directory(
                this,
                getRealPath().getDirectory()
        );
    }

    @Override
    public File getFile() throws IOException
    {
        Config c = getFileSystem().getConfig();
        return new com.sovworks.eds.fs.encfs.File(
                this,
                getRealPath().getFile(),
                c.getDataCodecInfo(),
                _encryptionKey,
                c.useExternalFileIV() ? getChainedIV() : null,
                c.getBlockSize(),
                c.useUniqueIV(),
                c.allowHoles(),
                c.getMACBytes(),
                c.getMACRandBytes(),
                false
        );
    }

    @Override
    public StringPathUtil getPathUtil()
    {
        return getDecodedPath();
    }

    public synchronized StringPathUtil getDecodedPath()
    {
        if(_decodedPath == null)
            try
            {
                _decodedPath = decodePath();
            }
            catch (IOException e)
            {
                Logger.log(e);
                _decodedPath = new StringPathUtil(_realPath.getPathString());
            }
        return _decodedPath;
    }

    public synchronized StringPathUtil getEncodedPath() throws IOException
    {
        if(_encodedPath == null)
            _encodedPath = buildEncodedPathFromRealPath(_realPath);
        return _encodedPath;
    }

    public void setDecodedPath(StringPathUtil decodedPath)
    {
        _decodedPath = decodedPath;
    }

    public void setEncodedPath(StringPathUtil encodedPath)
    {
        _encodedPath = encodedPath;
    }

    public synchronized byte[] getChainedIV()
    {
        if(_chainedIV == null)
            try
            {
                _chainedIV = calcChaindedIV();
            }
            catch (IOException e)
            {
                Logger.log(e);
            }
        return _chainedIV;
    }

    @Override
    public boolean isRootDirectory() throws IOException
    {
        return getEncodedPath().isEmpty();
    }

    private final NameCodecInfo _namingInfo;
    private final com.sovworks.eds.fs.Path _realPath;
    private StringPathUtil _encodedPath;
    private final byte[] _encryptionKey;
    private byte[] _chainedIV;
    private StringPathUtil _decodedPath;


    private StringPathUtil decodePath() throws IOException
    {
        StringPathUtil encodedParts = getEncodedPath();
        Path parent = getParentPath();
        StringPathUtil decodedParent = parent == null ? new StringPathUtil() : parent.getDecodedPath();
        NameCodec codec = _namingInfo.getEncDec();
        try
        {
            codec.init(_encryptionKey);
            if (_namingInfo.useChainedNamingIV() && parent!=null)
                codec.setIV(parent.getChainedIV());
            String decodedName = codec.decodeName(encodedParts.getFileName());
            if(_namingInfo.useChainedNamingIV())
                _chainedIV = codec.getChainedIV(decodedName);
            return decodedParent.combine(decodedName);
        }
        finally
        {
            codec.close();
        }
    }

    private byte[] calcChaindedIV() throws IOException
    {
        NameCodec codec = _namingInfo.getEncDec();
        try
        {
            codec.init(_encryptionKey);
            if(_namingInfo.useChainedNamingIV())
            {
                Path parent = getParentPath();
                codec.setIV(parent!=null ? parent.getChainedIV() : null);
            }
            return codec.getChainedIV(getDecodedPath().getFileName());
        }
        finally
        {
            codec.close();
        }
    }

    StringPathUtil buildEncodedPathFromRealPath(com.sovworks.eds.fs.Path realPath) throws IOException
    {
        StringPathUtil encodedParts = new StringPathUtil();
        com.sovworks.eds.fs.Path rootPath = getFileSystem().getEncFSRootPath();
        while(!realPath.equals(rootPath))
        {
            encodedParts = new StringPathUtil(PathUtil.getNameFromPath(realPath), encodedParts);
            realPath = realPath.getParentPath();
            if(realPath == null)
                throw new IOException("Failed building path");
        }
        return encodedParts;
    }
}