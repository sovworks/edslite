package com.sovworks.eds.fs.exfat;

import android.annotation.SuppressLint;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.settings.SystemConfig;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.fs.util.FileStat;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class ExFat implements FileSystem
{
    public static boolean isExFATImage(RandomAccessIO img) throws IOException
    {
        byte[] buf = new byte[8];
        img.seek(3);
        return Util.readBytes(img, buf) == buf.length && Arrays.equals(EXFAT_SIGN, buf);
    }

    private static final byte[] EXFAT_SIGN = new byte[]{'E', 'X', 'F', 'A', 'T', ' ', ' ', ' '};
    private static final int MIN_COMPATIBLE_NATIVE_MODULE_VERSION = 1001;

    public static void makeNewFS(RandomAccessIO img) throws IOException
    {
        makeNewFS(img, null, 0, 0, -1);
    }

    public static void makeNewFS(RandomAccessIO img, String label, int volumeSerial, long firstSector, int sectorsPerCluster) throws IOException
    {
        if(makeFS(img, label, volumeSerial, firstSector, sectorsPerCluster)!=0)
            throw new IOException("Failed formatting an ExFAT image");
    }

    public ExFat(RandomAccessIO exFatImage, boolean readOnly) throws IOException
    {
        _exfatImageFile = exFatImage;
        _exfatPtr = openFS(readOnly);
        if(_exfatPtr == 0)
            throw new IOException("Failed opening exfat file system");
    }

    @Override
    public Path getRootPath() throws IOException
    {
        return new ExFatPath(this, "/");
    }

    @Override
    public Path getPath(String pathString) throws IOException
    {
        return new ExFatPath(this, pathString);
    }

    @Override
    public void close(boolean force) throws IOException
    {
        synchronized (_sync)
        {
            if (_exfatPtr != 0)
            {
                closeFS();
                _exfatPtr = 0;
            }
        }
    }

    @Override
    public boolean isClosed()
    {
        return _exfatPtr == 0;
    }

    public long getFreeSpaceVolumeStartOffset()
    {
        return getFreeSpaceStartOffset();
    }

    public void overwriteFreeSpace() throws IOException
    {
        int res = randFreeSpace();
        if(res != 0)
            throw new IOException("Failed overwriting the free space. code " + res);

    }


    private static final String MODULE_NAME = "edsexfat";
    private static final String LIB_NAME = "lib" + MODULE_NAME + ".so";

    private enum ModuleState
    {
        Unknown,
        Absent,
        Incompatible,
        Installed
    }

    public static boolean isModuleInstalled()
    {
        return _nativeModuleState == ModuleState.Installed;
    }

    public static boolean isModuleIncompatible()
    {
        return _nativeModuleState == ModuleState.Incompatible;
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void loadNativeLibrary()
    {
        if(_nativeModuleState == ModuleState.Absent || _nativeModuleState == ModuleState.Unknown)
        {
            System.load(getModulePath().getAbsolutePath());
            _nativeModuleState = ModuleState.Incompatible;
            if (getVersion() < MIN_COMPATIBLE_NATIVE_MODULE_VERSION)
                throw new RuntimeException("Incompatible native exfat module version");
            Logger.debug("External exFAT module has been loaded.");
            _nativeModuleState = ModuleState.Installed;
        }
    }

    public static File getModulePath()
    {
        return new File(SystemConfig.getInstance().getFSMFolderPath(), LIB_NAME);
    }

    private static ModuleState _nativeModuleState = ModuleState.Unknown;
    static
    {
        if(_nativeModuleState == ModuleState.Unknown)
        {
            if(getModulePath().exists())
            {
                Logger.debug("Module file exists");
                try
                {
                    loadNativeLibrary();
                }
                catch (Throwable e)
                {
                    Logger.debug("Failed loading external exFAT module");
                    if(GlobalConfig.isDebug())
                        Logger.log(e);
                }
            }
            else
            try
            {
                System.loadLibrary(MODULE_NAME);
                Logger.debug("Built-in exFAT module has been loaded.");
                _nativeModuleState = ModuleState.Installed;
            }
            catch (Throwable e)
            {
                _nativeModuleState = ModuleState.Absent;
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private long _exfatPtr;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final RandomAccessIO _exfatImageFile;
    final Object _sync = new Object();

    private static native int makeFS(RandomAccessIO raio, String label, int volumeSerial, long firstSector, int sectorsPerCluster);

    native int readDir(String path, Collection<String> files);
    native int getAttr(FileStat stat, String path);
    native int makeDir(String path);
    native int makeFile(String path);
    native long getFreeSpace();
    native long getTotalSpace();
    native int rename(String oldPath, String newPath);
    native int delete(String path);
    native int rmdir(String path);
    native int truncate(long handle, long size);
    native long openFile(String path);
    native int closeFile(long handle);
    native long getSize(long handle);
    native int read(long handle, byte[] buf, int bufOffset, int count, long position);
    native int write(long handle, byte[] buf, int bufOffset, int count, long position);
    native int flush(long handle);
    native long openFS(boolean readOnly);
    native int closeFS();
    native long getFreeSpaceStartOffset();
    native int randFreeSpace();
    native int updateTime(String path, long time);
    native static int getVersion();
}
