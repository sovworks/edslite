package com.sovworks.eds.android.locations.fragments;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialogBase;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.locations.tasks.AddExistingContainerTaskFragment;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragment;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.settings.container.ContainerFormatPropertyEditor;
import com.sovworks.eds.android.settings.container.ContainerPasswordPropertyEditor;
import com.sovworks.eds.android.settings.container.ContainerSizePropertyEditor;
import com.sovworks.eds.android.settings.container.EncryptionAlgorithmPropertyEditor;
import com.sovworks.eds.android.settings.container.FileSystemTypePropertyEditor;
import com.sovworks.eds.android.settings.container.FillFreeSpacePropertyEditor;
import com.sovworks.eds.android.settings.container.HashingAlgorithmPropertyEditor;
import com.sovworks.eds.android.settings.container.PIMPropertyEditor;
import com.sovworks.eds.android.settings.container.PathToContainerPropertyEditor;
import com.sovworks.eds.android.settings.encfs.BlockSizePropertyEditor;
import com.sovworks.eds.android.settings.encfs.DataCodecPropertyEditor;
import com.sovworks.eds.android.settings.encfs.EnableEmptyBlocksPropertyEditor;
import com.sovworks.eds.android.settings.encfs.ExternalFileIVPropertyEditor;
import com.sovworks.eds.android.settings.encfs.FilenameIVChainingPropertyEditor;
import com.sovworks.eds.android.settings.encfs.KeySizePropertyEditor;
import com.sovworks.eds.android.settings.encfs.MACBytesPerBlockPropertyEditor;
import com.sovworks.eds.android.settings.encfs.NameCodecPropertyEditor;
import com.sovworks.eds.android.settings.encfs.NumKDFIterationsPropertyEditor;
import com.sovworks.eds.android.settings.encfs.RandBytesPerBlockPropertyEditor;
import com.sovworks.eds.android.settings.encfs.UniqueIVPropertyEditor;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EDSLocationFormatter;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.container.VolumeLayout;

import java.io.File;

public abstract class CreateContainerFragmentBase extends CreateEDSLocationFragment implements PasswordDialogBase.PasswordReceiver
{
    public void changeUniqueIVDependentOptions()
    {
        boolean show = isEncFsFormat() && !_state.getBoolean(ARG_ADD_EXISTING_LOCATION, false) && _state.getBoolean(CreateEncFsTaskFragment.ARG_UNIQUE_IV, true) &&
                _state.getBoolean(CreateEncFsTaskFragment.ARG_CHAINED_NAME_IV, true);
        _propertiesView.setPropertyState(R.string.enable_filename_to_file_iv_chain, show);
    }

    public boolean isEncFsFormat()
    {
        return EDSLocationFormatter.FORMAT_ENCFS.equals(_state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
    }

    public VolumeLayout getSelectedVolumeLayout()
    {
        ContainerFormatInfo info = getCurrentContainerFormatInfo();
        return info == null ? null : info.getVolumeLayout();
    }

    @Override
    protected TaskFragment createAddExistingLocationTask()
    {
        return AddExistingContainerTaskFragment.newInstance(
                (Uri) _state.getParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION),
                !UserSettings.getSettings(getActivity()).neverSaveHistory(),
                _state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT)
        );
    }

    @Override
    protected TaskFragment createCreateLocationTask()
    {
        return isEncFsFormat() ? new CreateEncFsTaskFragment() : new CreateContainerTaskFragment();
    }

    @Override
    public void showCreateNewLocationProperties()
    {
        Uri uri = _state.getParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION);
        if(uri == null)
        {
            File path;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            else
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!path.exists() && !path.mkdirs())
                path = getContext().getFilesDir();
            if (path != null)
                path = new File(path, "new container.eds");
            if(path!=null)
            {
                _state.putParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION, Uri.parse(path.getPath()));
                getActivity().invalidateOptionsMenu();
            }
        }

        super.showCreateNewLocationProperties();
        _propertiesView.setPropertyState(R.string.container_format, true);

    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if(pr!=null)
            pr.onPasswordEntered(dlg);
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if(pr!=null)
            pr.onPasswordNotEntered(dlg);
    }

    @Override
    protected void createProperties()
    {
        super.createProperties();
        if(!_state.containsKey(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT))
            _state.putString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT, EdsContainer.getSupportedFormats().get(0).getFormatName());
    }

    @Override
    protected void createNewLocationProperties()
    {
        _propertiesView.addProperty(new ContainerFormatPropertyEditor(this));
        _propertiesView.addProperty(new PathToContainerPropertyEditor(this));
        _propertiesView.addProperty(new ContainerPasswordPropertyEditor(this));
        createContainerProperties();
        createEncFsProperties();
    }

    protected ContainerFormatInfo getCurrentContainerFormatInfo()
    {
        return EdsContainer.findFormatByName(_state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
    }

    protected void createContainerProperties()
    {
        _propertiesView.addProperty(new PIMPropertyEditor(this));
        _propertiesView.addProperty(new ContainerSizePropertyEditor(this));
        _propertiesView.addProperty(new EncryptionAlgorithmPropertyEditor(this));
        _propertiesView.addProperty(new HashingAlgorithmPropertyEditor(this));
        _propertiesView.addProperty(new FileSystemTypePropertyEditor(this));
        _propertiesView.addProperty(new FillFreeSpacePropertyEditor(this));
    }

    private void createEncFsProperties()
    {
        _propertiesView.addProperty(new DataCodecPropertyEditor(this));
        _propertiesView.addProperty(new NameCodecPropertyEditor(this));
        _propertiesView.addProperty(new KeySizePropertyEditor(this));
        _propertiesView.addProperty(new BlockSizePropertyEditor(this));
        _propertiesView.addProperty(new UniqueIVPropertyEditor(this));
        _propertiesView.addProperty(new FilenameIVChainingPropertyEditor(this));
        _propertiesView.addProperty(new ExternalFileIVPropertyEditor(this));
        _propertiesView.addProperty(new EnableEmptyBlocksPropertyEditor(this));
        _propertiesView.addProperty(new MACBytesPerBlockPropertyEditor(this));
        _propertiesView.addProperty(new RandBytesPerBlockPropertyEditor(this));
        _propertiesView.addProperty(new NumKDFIterationsPropertyEditor(this));
    }
}
