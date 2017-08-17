package com.sovworks.eds.android.settings.container;

import android.content.Intent;
import android.net.Uri;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragment;
import com.sovworks.eds.android.locations.fragments.CreateContainerFragmentBase;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.settings.PathPropertyEditor;

import java.io.IOException;


public abstract class PathToContainerPropertyEditorBase extends PathPropertyEditor
{

    public PathToContainerPropertyEditorBase(CreateContainerFragmentBase createEDSLocationFragment)
    {
        super(createEDSLocationFragment,
                R.string.path_to_container,
                0,
                createEDSLocationFragment.getTag());
    }

    @Override
    protected void onTextChanged(final String newValue)
    {
        super.onTextChanged(newValue);
        getHostFragment().getActivity().invalidateOptionsMenu();
    }

    protected CreateContainerFragment getHostFragment()
    {
        return (CreateContainerFragment) getHost();
    }


    @Override
    protected Intent getSelectPathIntent() throws IOException
    {
        boolean addExisting = getHostFragment().getState().getBoolean(CreateEDSLocationFragment.ARG_ADD_EXISTING_LOCATION);
        boolean isEncFs = getHostFragment().isEncFsFormat();
        Intent i = FileManagerActivity.getSelectPathIntent(
                getHost().getContext(),
                null,
                false,
                true,
                isEncFs || addExisting,
                !addExisting,
                true,
                true);
        i.putExtra(FileManagerActivity.EXTRA_ALLOW_SELECT_FROM_CONTENT_PROVIDERS, true);
        return i;
    }

    @Override
    protected void saveText(String text)
    {
        getHostFragment().getState().putParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION, Uri.parse(text));
    }

    @Override
    protected String loadText()
    {
        Uri uri = getHostFragment().getState().getParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION);
        return uri!=null ? uri.toString() : null;
    }
}
