package com.sovworks.eds.fs.encfs;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.util.IteratorConverter;
import com.sovworks.eds.fs.util.DirectoryWrapper;
import com.sovworks.eds.fs.util.FilteredIterator;
import com.sovworks.eds.fs.util.StringPathUtil;

import java.io.IOException;
import java.util.Iterator;

public class Directory extends DirectoryWrapper
{
    public Directory(Path path, com.sovworks.eds.fs.Directory realDir) throws IOException
    {
        super(path, realDir);
    }

    @Override
    public String getName() throws IOException
    {
        return getPath().getDecodedPath().getFileName();
    }

    @Override
    public Contents list() throws IOException
    {
        final Contents contents = getBase().list();
        return new Contents()
        {
            @Override
            public void close() throws IOException
            {
                contents.close();
            }

            @Override
            public Iterator<com.sovworks.eds.fs.Path> iterator()
            {
                return new FilteringIterator(
                        new Directory.DirIterator(getPath().getFileSystem(), contents.iterator())
                );
            }
        };
    }

    @Override
    public Path getPath()
    {
        return (Path) super.getPath();
    }

    @Override
    public void rename(String newName) throws IOException
    {
        if(getPath().getNamingCodecInfo().useChainedNamingIV() || getPath().getFileSystem().getConfig().useExternalFileIV())
            throw new UnsupportedOperationException();
        StringPathUtil newEncodedPath = getPath().getParentPath().calcCombinedEncodedParts(newName);
        super.rename(newEncodedPath.getFileName());
    }

    @Override
    public File createFile(String name) throws IOException
    {
        StringPathUtil decodedPath = getPath().getDecodedPath();
        if(decodedPath!=null)
            decodedPath = decodedPath.combine(name);
        StringPathUtil newEncodedPath = getPath().calcCombinedEncodedParts(name);
        com.sovworks.eds.fs.encfs.File res = (com.sovworks.eds.fs.encfs.File) super.createFile(newEncodedPath.getFileName());
        res.getPath().setEncodedPath(newEncodedPath);
        if(decodedPath!=null)
            res.getPath().setDecodedPath(decodedPath);
        res.getOutputStream().close();
        return res;
    }

    @Override
    public com.sovworks.eds.fs.Directory createDirectory(String name) throws IOException
    {
        StringPathUtil decodedPath = getPath().getDecodedPath();
        if(decodedPath!=null)
            decodedPath = decodedPath.combine(name);
        StringPathUtil newEncodedPath = getPath().calcCombinedEncodedParts(name);
        Directory res = (Directory) super.createDirectory(newEncodedPath.getFileName());
        res.getPath().setEncodedPath(newEncodedPath);
        if(decodedPath!=null)
            res.getPath().setDecodedPath(decodedPath);
        return res;
    }

    @Override
    public void moveTo(com.sovworks.eds.fs.Directory dst) throws IOException
    {
        if(getPath().getNamingCodecInfo().useChainedNamingIV() || getPath().getFileSystem().getConfig().useExternalFileIV())
            throw new UnsupportedOperationException();
        super.moveTo(dst);
    }

    @Override
    protected com.sovworks.eds.fs.Path getPathFromBasePath(com.sovworks.eds.fs.Path basePath) throws IOException
    {
        return getPath().getFileSystem().getPathFromRealPath(basePath);
    }

    private static class DirIterator extends IteratorConverter<com.sovworks.eds.fs.Path, Path>
    {
        protected DirIterator(FS fs, Iterator<? extends com.sovworks.eds.fs.Path> srcIterator)
        {
            super(srcIterator);
            _fs = fs;
        }

        @Override
        protected Path convert(com.sovworks.eds.fs.Path src)
        {
            try
            {
                return _fs.getPathFromRealPath(src);
            }
            catch (IOException e)
            {
                Logger.log(e);
                return null;
            }
        }

        private final FS _fs;
    }

    private static class FilteringIterator extends FilteredIterator<com.sovworks.eds.fs.Path>
    {

        public FilteringIterator(Iterator<Path> base)
        {
            super(base);
        }

        @Override
        protected boolean isValidItem(com.sovworks.eds.fs.Path item)
        {
            try
            {
                Path p = (Path)item;
                return !(
                        (p.getParentPath().isRootDirectory() && Config.CONFIG_FILENAME.equals(p.getEncodedPath().getFileName()))
                        || p.getDecodedPath() == null
                );
            }
            catch (Throwable e)
            {
                Logger.log(e);
                return false;
            }
        }
    }
}
