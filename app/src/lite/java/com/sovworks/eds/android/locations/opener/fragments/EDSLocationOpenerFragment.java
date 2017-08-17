package com.sovworks.eds.android.locations.opener.fragments;

import android.os.Build;
import android.os.Bundle;

import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.providers.ContainersDocumentProviderBase;
import com.sovworks.eds.android.service.LocationsService;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

public class EDSLocationOpenerFragment extends LocationOpenerFragment implements LocationOpenerBaseFragment.LocationOpenerResultReceiver
{

    public static class OpenLocationTaskFragment extends LocationOpenerFragment.OpenLocationTaskFragment
    {
		@Override
        protected void openLocation(Openable location, Bundle param) throws Exception
        {
            if(!location.isOpen())
            {
                super.openLocation(location, param);
                if(location instanceof EDSLocation)
                {
                    LocationsService.registerInactiveContainerCheck(_context, (EDSLocation) location);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                        ContainersDocumentProviderBase.notifyOpenedLocationsListChanged(_context);
                }
            }
        }
    }

    @Override
    public void onTargetLocationOpened(Bundle openerArgs, Location location)
    {
        openLocation();
    }

    @Override
    public void onTargetLocationNotOpened(Bundle openerArgs)
    {
        finishOpener(false, null);
    }

    @Override
    protected void openLocation()
    {
        EDSLocation cbl = getTargetLocation();
        Location baseLocation = cbl.getLocation();
        if(baseLocation instanceof Openable && !((Openable)baseLocation).isOpen())
        {
            LocationOpenerBaseFragment f = getDefaultOpenerForLocation(baseLocation);
            Bundle b = new Bundle();
            LocationsManager.storePathsInBundle(b, baseLocation, null);
            b.putString(PARAM_RECEIVER_FRAGMENT_TAG, getTag());
            f.setArguments(b);
            getFragmentManager().beginTransaction().add(f, getOpenerTag(baseLocation)).commit();
        }
        else
            super.openLocation();
    }

    @Override
    protected EDSLocation getTargetLocation()
    {
        return (EDSLocation)super.getTargetLocation();
    }

    @Override
	protected TaskFragment getOpenLocationTask()
	{
		return new OpenLocationTaskFragment();
	}
}
