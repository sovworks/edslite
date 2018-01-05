package com.sovworks.eds.android.locations.dialogs;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.ConfirmationDialog;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.locations.fragments.LocationListBaseFragment;
import com.sovworks.eds.locations.Location;

public class RemoveLocationConfirmationDialog extends ConfirmationDialog
{
    public static final String TAG = "RemoveLocationConfirmationDialog";

    public static void showDialog(FragmentManager fm, Location loc)
    {
        DialogFragment f = new RemoveLocationConfirmationDialog();
        Bundle b = new Bundle();
        LocationsManager.storePathsInBundle(b,loc, null);
        f.setArguments(b);
        f.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        _loc = LocationsManager.getLocationsManager(getActivity()).getFromBundle(getArguments(), null);
    }

    @Override
	protected void onYes()
	{
        if(_loc == null)
            return;
        LocationListBaseFragment f = (LocationListBaseFragment) getFragmentManager().findFragmentByTag(LocationListBaseFragment.TAG);
        if(f == null)
            return;
        f.removeLocation(_loc);
	}

    @Override
    protected String getTitle()
    {
        return getString(R.string.do_you_really_want_to_remove_from_the_list, _loc == null ? "" : _loc.getTitle());
    }

    private Location _loc;
}
