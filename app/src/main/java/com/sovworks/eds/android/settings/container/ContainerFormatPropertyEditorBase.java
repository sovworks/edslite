package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragment;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.android.settings.encfs.DataCodecPropertyEditor;
import com.sovworks.eds.android.settings.views.PropertiesView;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EDSLocationFormatter;
import com.sovworks.eds.container.EdsContainer;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerFormatPropertyEditorBase extends ChoiceDialogPropertyEditor
{
    public ContainerFormatPropertyEditorBase(CreateContainerFragmentBase createContainerFragment, int descResId)
    {
        super(
                createContainerFragment,
                R.string.container_format,
                descResId,
                createContainerFragment.getTag());
    }

    @Override
    protected List<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        for (ContainerFormatInfo i : EdsContainer.getSupportedFormats())
            res.add(i.getFormatName());
        res.add(EDSLocationFormatter.FORMAT_ENCFS);
        return res;
    }

    @Override
    protected int loadValue()
    {
        int formatId = getFormatIdFromName(getHostFragment().getState().getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
        boolean addExisting = getHostFragment().getState().getBoolean(CreateEDSLocationFragment.ARG_ADD_EXISTING_LOCATION);
        boolean isEncFs = isEncFs(formatId);
        getHostFragment().getPropertiesView().beginUpdate();
        try
        {
            updateEncFsProperties(isEncFs && !addExisting);
            updateContainerFormatProperties(!isEncFs && !addExisting, getSelectedContainerFormatInfo(formatId));
            updateCommonProperties(addExisting);
        }
        finally
        {
            getHostFragment().getPropertiesView().endUpdate(null);
        }
        //getHostFragment().getPropertiesView().loadProperties();
        return formatId;
    }


    @Override
    protected void saveValue(int value)
    {
        boolean addExisting = getHostFragment().getState().getBoolean(CreateEDSLocationFragment.ARG_ADD_EXISTING_LOCATION);
        getHostFragment().getPropertiesView().beginUpdate();
        try
        {
            if (isEncFs(value))
            {
                getHostFragment().getState().putString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT, EDSLocationFormatter.FORMAT_ENCFS);
                updateContainerFormatProperties(false, null);
                updateEncFsProperties(!addExisting);
            } else
            {
                ContainerFormatInfo cfi = getSelectedContainerFormatInfo(value);
                if (cfi != null)
                    getHostFragment().getState().putString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT, cfi.getFormatName());
                updateEncFsProperties(false);
                updateContainerFormatProperties(!addExisting, cfi);
            }
            updateCommonProperties(addExisting);
        }
        finally
        {
            getHostFragment().getPropertiesView().endUpdate(null);
        }
        //getHostFragment().getPropertiesView().loadProperties();
    }

    private boolean isEncFs(int formatId)
    {
        return formatId == EdsContainer.getSupportedFormats().size();
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragment) getHost();
    }

    protected int getFormatIdFromName(String formatName)
    {
        if (formatName == null)
            return 0;
        List<ContainerFormatInfo> supportedFormats = EdsContainer.getSupportedFormats();
        if(EDSLocationFormatter.FORMAT_ENCFS.equals(formatName))
            return supportedFormats.size();
        for (int i = 0; i < supportedFormats.size(); i++)
        {
            ContainerFormatInfo cfi = supportedFormats.get(i);
            if (cfi.getFormatName().equalsIgnoreCase(formatName))
                return i;
        }
        return 0;
    }

    protected ContainerFormatInfo getSelectedContainerFormatInfo(int formatId)
    {
        List<ContainerFormatInfo> fmts = EdsContainer.getSupportedFormats();
        return formatId >= fmts.size() ? null : fmts.get(formatId);
    }

    protected void updateContainerFormatProperties(boolean enable, ContainerFormatInfo cfi)
    {
        PropertiesView pm = getHost().getPropertiesView();
        pm.setPropertyState(R.string.container_size, enable);
        pm.setPropertyState(R.string.kdf_iterations_multiplier, enable && cfi.hasCustomKDFIterationsSupport());
        pm.setPropertyState(R.string.encryption_algorithm, enable);
        pm.setPropertyState(R.string.hash_algorithm, enable);
        pm.setPropertyState(R.string.fill_free_space_with_random_data, enable);
        pm.setPropertyState(R.string.file_system_type, enable);
    }

    private void updateEncFsProperties(boolean enable)
    {
        PropertiesView pm = getHost().getPropertiesView();
        pm.setPropertyState(DataCodecPropertyEditor.ID, enable);
        pm.setPropertyState(R.string.filename_encryption_algorithm, enable);
        pm.setPropertyState(R.string.block_size, enable);
        pm.setPropertyState(R.string.allow_empty_blocks, enable);
        pm.setPropertyState(R.string.enable_per_file_iv, enable);
        pm.setPropertyState(R.string.enable_filename_iv_chain, enable);
        pm.setPropertyState(R.string.key_size, enable);
        pm.setPropertyState(R.string.mac_bytes_per_block, enable);
        pm.setPropertyState(R.string.number_of_kdf_iterations, enable);
        pm.setPropertyState(R.string.add_rand_bytes, enable);
    }


    protected void updateCommonProperties(boolean addExisting)
    {
        PropertiesView pm = getHost().getPropertiesView();
        pm.setPropertyState(R.string.path_to_container, true);
        pm.setPropertyState(R.string.container_password, !addExisting);
    }

}
