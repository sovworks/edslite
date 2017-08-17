package com.sovworks.eds.android.locations.dialogs;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.ConfirmationDialog;
import com.sovworks.eds.android.locations.closer.fragments.LocationCloserBaseFragment;

public class ForceCloseDialog extends ConfirmationDialog
{
    public static final String ARG_LOCATION_TITLE = "com.sovworks.eds.android.LOCATION_TITLE";
    public static final String ARG_CLOSER_ARGS = "com.sovworks.eds.android.CLOSER_ARGS";
    public static final String ARG_CLOSER_CLASS_NAME = "com.sovworks.eds.android.CLOSER_CLASS_NAME";

    public static final String TAG = "ForceCloseDialog";

    public static void showDialog(FragmentManager fm, String closerTag, String locTitle, String closerClassName, Bundle closerArgs)
    {
        DialogFragment f = new ForceCloseDialog();
        Bundle b = new Bundle();
        b.putString(ARG_LOCATION_TITLE, locTitle);
        b.putString(ARG_CLOSER_CLASS_NAME, closerClassName);
        if(closerArgs!=null)
            b.putBundle(ARG_CLOSER_ARGS, closerArgs);
        b.putString(LocationCloserBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG, closerTag);
        f.setArguments(b);
        f.show(fm, TAG);
    }

	@Override
	protected void onYes()
	{
        Bundle closerArgs = getArguments().getBundle(ARG_CLOSER_ARGS);
        if(closerArgs == null)
            closerArgs = new Bundle();
        closerArgs.putBoolean(LocationCloserBaseFragment.ARG_FORCE_CLOSE, true);
        LocationCloserBaseFragment closer = (LocationCloserBaseFragment) Fragment.instantiate(
                getActivity(),
                getArguments().getString(ARG_CLOSER_CLASS_NAME),
                closerArgs
        );
        getFragmentManager().beginTransaction().add(closer, getArguments().getString(LocationCloserBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG)).commit();
	}

    @Override
    protected String getTitle()
    {
        return getString(R.string.force_close_request, getArguments().getString(ARG_LOCATION_TITLE));
    }
}
