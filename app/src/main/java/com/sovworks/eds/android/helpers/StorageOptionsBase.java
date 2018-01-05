package com.sovworks.eds.android.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.settings.Settings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
public abstract class StorageOptionsBase
{
    private static String readMounts()
    {
        try
        {
            return readMountsStd();
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return "";
    }

    public static class StorageInfo
    {
        public String label, path, dev, type, flags[];
        public boolean isExternal, isReadOnly;
    }

    public static synchronized List<StorageInfo> getStoragesList(Context context)
    {
        if(_storagesList == null)
            loadStorageList(context);
        return _storagesList;
    }

    public static void reloadStorageList(Context context)
    {
        _storagesList = loadStorageList(context);
    }

    private static List<StorageInfo> loadStorageList(Context context)
    {
        StorageOptionsBase so = new StorageOptions(context);
        return so.buildStoragesList();
    }

    public static StorageInfo getDefaultDeviceLocation(Context context)
    {
        for(StorageInfo si: getStoragesList(context))
            if(!si.isExternal)
                return si;
        return !getStoragesList(context).isEmpty() ? getStoragesList(context).get(0) : null;
    }

    private static List<StorageInfo> _storagesList;

    StorageOptionsBase(Context context)
    {
        _context = context;
    }

    public final Context getContext()
    {
        return _context;
    }

    private List<StorageInfo> buildStoragesList()
    {
        ArrayList<StorageInfo> res = new ArrayList<>();
        int extStoragesCounter = 1;
        StorageInfo si = getDefaultStorage();
        if(si!=null)
        {
            res.add(si);
            if(si.isExternal)
                extStoragesCounter++;
        }
        addFromMountsFile(res, extStoragesCounter);
        return res;
    }

    public List<StorageInfo> readAllMounts()
    {
        return parseMountsFile(readMountsFile());
    }

    private static boolean isStorageAdded(Collection<StorageInfo> storages, String devPath, String mountPath)
    {
        StringPathUtil dpu = new StringPathUtil(devPath);
        StringPathUtil mpu = new StringPathUtil(mountPath);
        for(StorageInfo si: storages)
        {
            StringPathUtil spu = new StringPathUtil(si.path);
            if (spu.equals(mpu) || spu.equals(dpu))
                return true;
            if(((mountPath.startsWith("/mnt/media_rw/") && si.path.startsWith("/storage/")) ||
                    (si.path.startsWith("/mnt/media_rw/") && mountPath.startsWith("/storage/"))) &&
                    spu.getFileName().equals(mpu.getFileName()))
                return true;
        }
        return false;
    }

    @SuppressLint("ObsoleteSdkInt")
    private StorageInfo getDefaultStorage()
    {
        String defPathState = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(defPathState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(defPathState))
        {
            StorageInfo info = new StorageInfo();
            if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD ||
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && !Environment.isExternalStorageRemovable()) ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && (!Environment.isExternalStorageRemovable() || Environment.isExternalStorageEmulated()))
                    )
                info.label = _context.getString(R.string.built_in_memory_card);
            else
            {
                info.isExternal = true;
                info.label = _context.getString(R.string.external_storage) + " 1";
            }
            info.path = Environment.getExternalStorageDirectory().getPath();
            return info;
        }
        return null;
    }

    protected String readMountsFile()
    {
        return readMounts();
    }

    static String readMountsStd() throws IOException
    {
        Logger.debug("StorageOptions: trying to get mounts using std fs.");
        FileInputStream finp = new FileInputStream("/proc/mounts");
        InputStream inp = new BufferedInputStream(finp);
        try
        {
            return com.sovworks.eds.fs.util.Util.readFromFile(inp);
        }
        finally
        {
            inp.close();
        }
    }

    private int addFromMountsFile(Collection<StorageInfo> storages, int extCounter)
    {
        ArrayList<StorageInfo> mounts = parseMountsFile(readMountsFile());
        if (mounts.isEmpty())
            return extCounter;
        Settings settings = UserSettings.getSettings(_context);
        for(StorageInfo si: mounts)
        {
            if (si.type.equals("vfat") || si.path.startsWith("/mnt/") || si.path.startsWith("/storage/"))
            {
                if (isStorageAdded(storages, si.dev, si.path))
                    continue;
                if ((si.dev.startsWith("/dev/block/vold/") &&
                        (!si.path.startsWith("/mnt/secure")
                                && !si.path.startsWith("/mnt/asec")
                                && !si.path.startsWith("/mnt/obb")
                                && !si.dev.startsWith("/dev/mapper")
                                && !si.type.equals("tmpfs"))
                        ) || (
                        (si.dev.startsWith("/dev/fuse") || si.dev.startsWith("/mnt/media")) && si.path.startsWith("/storage/") && !si.path.startsWith("/storage/emulated")
                        )
                   )
                {
                    si.label = _context.getString(R.string.external_storage) + " " + extCounter;
                    if(checkMountPoint(settings, si))
                    {
                        storages.add(si);
                        extCounter++;
                    }
                }
            }
        }
        return extCounter;
    }

    ArrayList<StorageInfo> parseMountsFile(String mountsStr)
    {
        ArrayList<StorageInfo> res = new ArrayList<>();
        if(mountsStr == null || mountsStr.isEmpty())
            return res;
        Pattern p = Pattern.compile("^([^\\s]+)\\s+([^\\s+]+)\\s+([^\\s+]+)\\s+([^\\s+]+).*?$", Pattern.MULTILINE);
        Matcher m = p.matcher(mountsStr);
        while (m.find())
        {
            String dev = m.group(1);
            String mountPath = m.group(2);
            String type = m.group(3);
            String[] flags = m.group(4).split(",");
            StorageInfo si = new StorageInfo();
            si.path = mountPath;
            si.dev = dev;
            si.type = type;
            si.flags = flags;
            si.isExternal = true;
            si.isReadOnly = Arrays.asList(flags).contains("ro");
            res.add(si);
        }
        return res;
    }

    protected boolean checkMountPoint(Settings s, StorageOptionsBase.StorageInfo si)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return true;
        File f = new File(si.path);
        return f.isDirectory() && !si.path.startsWith("/mnt/media_rw");
    }

    private final Context _context;

}
