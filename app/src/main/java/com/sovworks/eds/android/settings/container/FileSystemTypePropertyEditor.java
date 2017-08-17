package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.settings.GlobalConfig;

import java.util.ArrayList;
import java.util.List;

public class FileSystemTypePropertyEditor extends ChoiceDialogPropertyEditor
{

    public FileSystemTypePropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment,
                R.string.file_system_type,
                createContainerFragment.getString(R.string.file_system_type),
                createContainerFragment.getString(R.string.file_system_type_desc, GlobalConfig.EXFAT_MODULE_URL),
                createContainerFragment.getTag());
    }

    @Override
    protected int loadValue()
    {
        List<String> names = getEntries();
        FileSystemInfo cur = getHostFragment().
                getState().
                getParcelable(CreateContainerTaskFragmentBase.ARG_FILE_SYSTEM_TYPE);
        if (cur != null)
            return names.indexOf(cur.getFileSystemName());
        else if (!names.isEmpty())
            return 0;
        else
            return -1;
    }

    @Override
    protected void saveValue(int value)
    {
        List<FileSystemInfo> fs = FileSystemInfo.getSupportedFileSystems();
        FileSystemInfo selected = fs.get(value);
        getHostFragment().
                getState().
                putParcelable(CreateContainerTaskFragmentBase.ARG_FILE_SYSTEM_TYPE, selected);
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        List<FileSystemInfo> supportedFS = FileSystemInfo.getSupportedFileSystems();
        if (supportedFS != null)
        {
            for (FileSystemInfo fsInfo: supportedFS)
                res.add(fsInfo.getFileSystemName());
        }
        return res;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
