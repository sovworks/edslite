package com.sovworks.eds.fs.util;

import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;

import java.io.IOException;

public class PathUtil
{
    public static Path changeFileName(Path path, String newFileName) throws IOException
    {
        Path bp = path.getParentPath();
        if(bp == null)
            throw new IllegalArgumentException("Can't change filename of the root path");
        return bp.combine(newFileName);
    }

    public static String getNameFromPath(Path path) throws IOException
    {
        return path.isFile() ?
                path.getFile().getName() :
                (
                        path.isDirectory() ?
                                path.getDirectory().getName() :
                                (path instanceof PathBase ? ((PathBase)path).getPathUtil().getFileName() : path.getPathDesc())
                );
    }

    public static void makeFullPath(Path path) throws IOException
    {
        if(!path.exists())
        {
            StringPathUtil pu = new StringPathUtil(path.getPathString());
            Path bp = path.getParentPath();
            if(bp!=null)
            {
                makeFullPath(bp);
                bp.getDirectory().createDirectory(pu.getFileName());
            }
        }
        else if(!path.isDirectory())
            throw new IOException("Can't create path: " + path.getPathString());
    }

    public static StringPathUtil buildStringPathUtil(Path path) throws IOException
    {
        StringPathUtil res = new StringPathUtil();
        while(path!=null && path.exists() && !path.isRootDirectory())
        {
            res = new StringPathUtil(getNameFromPath(path), res);
            path = path.getParentPath();
        }
        return res;
    }

    public static boolean isParentDirectory(Path testParentPath, Path testPath) throws IOException
    {
        if(testParentPath instanceof PathBase && testPath instanceof PathBase)
            return isParentDirectory((PathBase)testParentPath, (PathBase)testPath);
        return isParentDirectoryRec(testParentPath, testPath);

    }

    public static boolean isParentDirectory(PathBase testParentPath, PathBase testPath)
    {
        return testParentPath.getPathUtil().isParentDir(testPath.getPathUtil());
    }

    public static boolean isParentDirectoryRec(Path testParentPath, Path testPath) throws IOException
    {
        while(true)
        {
            if(testPath.isRootDirectory())
                return false;
            Path parentPath = testPath.getParentPath();
            if(parentPath == null)
                return false;
            if(parentPath.equals(testParentPath))
                return true;
            testPath = parentPath;
        }
    }

    public static Path unwrapPath(Path wrappedPath)
    {
        Path path = wrappedPath;
        while(path instanceof PathWrapper)
            path = ((PathWrapper)path).getBase();
        return path;
    }

    public static Path buildPath(Path startPath, String... parts)
    {
        Path path = startPath;
        for(String p: parts)
            try
            {
                path = path.combine(p);
            }
            catch (IOException e)
            {
                return null;
            }
        return path;
    }

    public static boolean exists(Path startPath, String... parts)
    {
        Path path = buildPath(startPath, parts);
        try
        {
            return path!=null && path.exists();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static boolean isFile(Path startPath, String... parts)
    {
        Path path = buildPath(startPath, parts);
        try
        {
            return path!=null && path.isFile();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static boolean isDirectory(Path startPath, String... parts)
    {
        Path path = buildPath(startPath, parts);
        try
        {
            return path!=null && path.isDirectory();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static File getFile(Path startPath, String... parts) throws IOException
    {
        if(parts.length == 0)
        {
            if(startPath.isFile())
                return startPath.getFile();
            throw new IOException("Start path is not a file");
        }
        Path prevPath = startPath;
        for(int i=0;i<parts.length - 1;i++)
        {
            String p = parts[i];
            Path path = buildPath(prevPath, p);
            if(path == null || !path.exists())
                prevPath = prevPath.getDirectory().createDirectory(p).getPath();
            else
                prevPath = path;
        }
        String p = parts[parts.length - 1];
        Path path = buildPath(prevPath, p);
        if(path == null || !path.exists())
            return prevPath.getDirectory().createFile(p);
        else
            return path.getFile();
    }

    public static Directory getDirectory(Path startPath, String... parts) throws IOException
    {
        if(parts.length == 0)
        {
            if(startPath.isDirectory())
                return startPath.getDirectory();
            throw new IOException("Start path is not a directory");
        }
        Path prevPath = startPath;
        for (String p : parts)
        {
            Path path = buildPath(prevPath, p);
            if (path == null || !path.exists())
                prevPath = prevPath.getDirectory().createDirectory(p).getPath();
            else
                prevPath = path;
        }
        return prevPath.getDirectory();
    }
}
