package com.sovworks.eds.fs.encfs;

import android.os.ParcelFileDescriptor;

import com.sovworks.eds.container.EncryptedFileLayout;
import com.sovworks.eds.crypto.EncryptedFile;
import com.sovworks.eds.crypto.EncryptedInputStream;
import com.sovworks.eds.crypto.EncryptedOutputStream;
import com.sovworks.eds.crypto.EncryptionEngine;
import com.sovworks.eds.crypto.EncryptionEngineException;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.MACFile;
import com.sovworks.eds.crypto.MACInputStream;
import com.sovworks.eds.crypto.MACOutputStream;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.encfs.ciphers.BlockAndStreamCipher;
import com.sovworks.eds.fs.encfs.macs.MACCalculator;
import com.sovworks.eds.fs.util.FileWrapper;
import com.sovworks.eds.fs.util.RandomAccessInputStream;
import com.sovworks.eds.fs.util.RandomAccessOutputStream;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.fs.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

public class File extends FileWrapper
{
    public File(
            Path path,
            com.sovworks.eds.fs.File realFile,
            DataCodecInfo encryptionInfo,
            byte[] encryptionKey,
            byte[] externalIV,
            int fileBlockSize,
            boolean enableHeader,
            boolean allowEmptyParts,
            int macBytes,
            int randBytes,
            boolean forceDecode
    ) throws IOException
    {
        super(path, realFile);
        _enableIVHeader = enableHeader;
        _encryptionInfo = encryptionInfo;
        _encryptionKey = encryptionKey;
        _externalIV = externalIV == null ? null : externalIV.clone();
        _fileBlockSize = fileBlockSize;
        _allowEmptyParts = allowEmptyParts;
        _macBytes = macBytes;
        _randBytes = randBytes;
        _forceDecode = forceDecode;
    }

    @Override
    public Path getPath()
    {
        return (Path) super.getPath();
    }

    @Override
    public String getName() throws IOException
    {
        return getPath().getDecodedPath().getFileName();
    }

    @Override
    public ParcelFileDescriptor getFileDescriptor(AccessMode accessMode)
    {
        return null;
    }

    @Override
    public RandomAccessIO getRandomAccessIO(AccessMode accessMode) throws IOException
    {
        RandomAccessIO base = super.getRandomAccessIO(accessMode);
        try
        {
            switch (accessMode)
            {
                case Read:
                {
                    if (_enableIVHeader && getBase().getSize() < Header.SIZE)
                        return base;
                    return initEncryptedFile(
                            base,
                            initFileLayout(_enableIVHeader ? new RandomAccessInputStream(base) : null)
                    );
                }
                case ReadWrite:
                    if (getPath().exists() && getBase().getSize() >= Header.SIZE)
                        return initEncryptedFile(
                                base,
                                initFileLayout(_enableIVHeader ? new RandomAccessInputStream(base) : null)
                        );
                case ReadWriteTruncate:
                case Write:
                    return initEncryptedFile(
                            base,
                            initFileLayout(_enableIVHeader ? new RandomAccessOutputStream(base) : null)
                    );
                case WriteAppend:
                    if(_enableIVHeader)
                        throw new IllegalArgumentException("Can't write header in WriteAppend mode");
                    return initEncryptedFile(
                            base,
                            initFileLayout((RandomAccessOutputStream)null)
                    );
                default:
                    throw new IllegalArgumentException("Wrong access mode");
            }
        }
        catch(Throwable e)
        {
            base.close();
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        OutputStream base = super.getOutputStream();
        try
        {
            FileLayout fl = initFileLayout(base);
            EncryptedOutputStream out = new EncryptedOutputStream(base, fl);
            out.setAllowEmptyParts(_allowEmptyParts);
            if(_macBytes > 0 || _randBytes > 0)
            {
                MACCalculator mac = _encryptionInfo.getChecksumCalculator();
                mac.init(_encryptionKey);
                return new MACOutputStream(
                        out,
                        mac,
                        _fileBlockSize,
                        _macBytes,
                        _randBytes
                );
            }
            return out;
        }
        catch(Throwable e)
        {
            base.close();
            throw new IOException(e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        InputStream base = super.getInputStream();
        try
        {
            FileLayout fl = initFileLayout(base);
            EncryptedInputStream inp = new EncryptedInputStream(base, fl);
            inp.setAllowEmptyParts(_allowEmptyParts);
            if(_macBytes > 0 || _randBytes > 0)
            {
                MACCalculator mac = _encryptionInfo.getChecksumCalculator();
                mac.init(_encryptionKey);
                MACInputStream minp = new MACInputStream(
                        inp,
                        mac,
                        _fileBlockSize,
                        _macBytes,
                        _randBytes,
                        _forceDecode
                );
                minp.setAllowEmptyParts(_allowEmptyParts);
                return minp;
            }
            return inp;
        }
        catch(Throwable e)
        {
            base.close();
            throw new IOException(e);
        }
    }

    @Override
    public long getSize() throws IOException
    {
        long size = super.getSize();
        if(_enableIVHeader && size >= Header.SIZE)
            size -= Header.SIZE;
        if(_randBytes > 0 || _macBytes > 0)
            size = MACFile.calcVirtPosition(size, _fileBlockSize - _randBytes - _macBytes, _randBytes + _macBytes);

        return size;
    }

    @Override
    public void rename(String newName) throws IOException
    {
        StringPathUtil newEncodedPath = getPath().getParentPath().calcCombinedEncodedParts(newName);
        if(_externalIV!=null || getPath().getNamingCodecInfo().useChainedNamingIV())
        {
            com.sovworks.eds.fs.File newFile = getPath().getParentPath().getDirectory().createFile(newName);
            OutputStream out = newFile.getOutputStream();
            try
            {
                copyToOutputStream(out, 0, 0, null);
            }
            finally
            {
                out.close();
            }
            delete();
            setPath(newFile.getPath());
        }
        else
            super.rename(newEncodedPath.getFileName());
    }

    @Override
    public void moveTo(com.sovworks.eds.fs.Directory newParent) throws IOException
    {
        if(_externalIV!=null || getPath().getNamingCodecInfo().useChainedNamingIV())
        {
            com.sovworks.eds.fs.File newFile = newParent.createFile(getName());
            OutputStream out = newFile.getOutputStream();
            try
            {
                copyToOutputStream(out, 0, 0, null);
            }
            finally
            {
                out.close();
            }
            delete();
            setPath(newFile.getPath());
        }
        else
            super.moveTo(newParent);
    }

    @Override
    protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
    {
        return getPath().getFileSystem().getPathFromRealPath(basePath);
    }

    protected RandomAccessIO initEncryptedFile(RandomAccessIO base, EncryptedFileLayout fl) throws FileNotFoundException
    {
        EncryptedFile ef = new EncryptedFile(base,fl, 1)
        {
            @Override
            public synchronized void close(boolean closeBase) throws IOException
            {
                try
                {
                    super.close(closeBase);
                }
                finally
                {
                    _layout.close();
                }
            }
        };
        ef.setAllowSkip(_allowEmptyParts);
        if(_macBytes > 0 || _randBytes > 0)
        {
            MACCalculator mac = _encryptionInfo.getChecksumCalculator();
            mac.init(_encryptionKey);
            MACFile mf = new MACFile(
                    ef,
                    mac,
                    _fileBlockSize,
                    _macBytes,
                    _randBytes,
                    _forceDecode
                    );
            mf.setAllowSkip(_allowEmptyParts);
            return mf;
        }
        return ef;
    }


    private static class Header
    {
        static final int SIZE = 8;

        public void load(byte[] data)
        {
            _iv = data;
        }

        public byte[] save()
        {
            return _iv.clone();
        }

        public void initNew()
        {
            _iv = new byte[8];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(_iv);
        }

        public byte[] getIV()
        {
            return _iv;
        }

        public void setIV(byte[] iv)
        {
            _iv = iv;
        }

        private byte[] _iv;
    }

    private static class FileLayout implements EncryptedFileLayout
    {
        public FileLayout(FileEncryptionEngine dataEncDec, int encryptedDataOffset, byte[] fileIV)
        {
            _encryptedDataOffset = encryptedDataOffset;
            _dataEncDec = dataEncDec;
            _fileIV = fileIV;
        }

        @Override
        public long getEncryptedDataOffset()
        {
            return _encryptedDataOffset;
        }

        @Override
        public long getEncryptedDataSize(long fileSize)
        {
            return fileSize - _encryptedDataOffset;
        }

        @Override
        public FileEncryptionEngine getEngine()
        {
            return _dataEncDec;
        }

        @Override
        public void setEncryptionEngineIV(FileEncryptionEngine eng, long decryptedVolumeOffset)
        {
            byte[] iv = new byte[eng.getIVSize()];
            ByteBuffer.wrap(iv).order(ByteOrder.BIG_ENDIAN).putLong(decryptedVolumeOffset/_dataEncDec.getFileBlockSize());
            if(_fileIV!=null)
                for(int i = 0;i < _fileIV.length;i++)
                    iv[i] ^= _fileIV[i];
            eng.setIV(iv);
        }

        @Override
        public void close() throws IOException
        {
            _dataEncDec.close();
        }

        private final int _encryptedDataOffset;
        private final FileEncryptionEngine _dataEncDec;
        private final byte[] _fileIV;
    }

    private final boolean _enableIVHeader, _allowEmptyParts, _forceDecode;
    private final DataCodecInfo _encryptionInfo;
    private final byte[] _encryptionKey, _externalIV;
    private final int _macBytes, _randBytes, _fileBlockSize;


    private FileLayout initFileLayout(OutputStream out) throws IOException, ApplicationException
    {
        Header h;
        if(_enableIVHeader)
        {
            h = initNewHeader();
            writeHeader(out, h);
        }
        else
            h = null;
        return initFileLayout(h);
    }

    private FileLayout initFileLayout(InputStream inp) throws IOException, EncryptionEngineException
    {
        return initFileLayout(_enableIVHeader ? readHeader(inp) : null);
    }

    private FileLayout initFileLayout(Header h) throws EncryptionEngineException
    {
        FileEncryptionEngine ee = new BlockAndStreamCipher(
                _encryptionInfo.getFileEncDec(),
                _encryptionInfo.getStreamEncDec()
        );
        ee.setKey(_encryptionKey);
        ee.init();
        return h == null ? new FileLayout(ee, 0, null) : new FileLayout(ee, Header.SIZE, h.getIV());
    }

    private Header initNewHeader()
    {
        Header header = new Header();
        header.initNew();
        return header;
    }

    public Header readHeader(InputStream input) throws IOException
    {
        byte[] buf = new byte[Header.SIZE];
        if(Util.readBytes(input, buf)!=Header.SIZE)
            throw new IOException("Failed reading header");
        EncryptionEngine ee = _encryptionInfo.getStreamEncDec();
        try
        {
            ee.setKey(_encryptionKey);
            ee.init();
            ee.setIV(_externalIV);
            ee.decrypt(buf, 0, buf.length);
        }
        catch (EncryptionEngineException e)
        {
            throw new IOException(e);
        }
        finally
        {
            ee.close();
        }
        Header header = new Header();
        header.load(buf);
        return header;
    }

    public void writeHeader(OutputStream output, Header header) throws IOException
    {
        byte[] data = header.save();
        EncryptionEngine ee = _encryptionInfo.getStreamEncDec();
        try
        {
            ee.setKey(_encryptionKey);
            ee.init();
            ee.setIV(_externalIV);
            ee.encrypt(data, 0, data.length);
        }
        catch (EncryptionEngineException e)
        {
            throw new IOException(e);
        }
        finally
        {
            ee.close();
        }
        output.write(data);
    }
}
