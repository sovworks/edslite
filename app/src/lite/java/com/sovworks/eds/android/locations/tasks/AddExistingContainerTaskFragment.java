package com.sovworks.eds.android.locations.tasks;

import android.net.Uri;
import android.os.Bundle;

import com.sovworks.eds.locations.LocationsManager;

public class AddExistingContainerTaskFragment extends AddExistingContainerTaskFragmentBase
{
	public static AddExistingContainerTaskFragment newInstance(Uri containerLocationUri, boolean storeLink, String containerFormatName)
    {
        Bundle args = new Bundle();
        args.putBoolean(ARG_STORE_LINK, storeLink);
        args.putParcelable(LocationsManager.PARAM_LOCATION_URI, containerLocationUri);
        args.putString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT, containerFormatName);
        AddExistingContainerTaskFragment f = new AddExistingContainerTaskFragment();
        f.setArguments(args);
        return f;
    }
}
