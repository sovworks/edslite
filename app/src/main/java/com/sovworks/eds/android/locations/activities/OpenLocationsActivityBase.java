package com.sovworks.eds.android.locations.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.dialogs.MasterPasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;

import java.util.ArrayList;

public abstract class OpenLocationsActivityBase extends Activity
{
    public static class MainFragment extends Fragment implements LocationOpenerBaseFragment.LocationOpenerResultReceiver, MasterPasswordDialog.PasswordReceiver
    {
        public static final String TAG = "com.sovworks.eds.android.locations.activities.OpenLocationsActivity.MainFragment";

        @Override
        public void onCreate(Bundle state)
        {
            super.onCreate(state);
            _locationsManager = LocationsManager.getLocationsManager(getActivity());
            try
            {
                _targetLocations = state == null ?
                        _locationsManager.getLocationsFromIntent(getActivity().getIntent())
                        :
                        _locationsManager.getLocationsFromBundle(state);
            }
            catch (Exception e)
            {
                Logger.showAndLog(getActivity(), e);
            }
            onFirstStart();
        }



        @Override
        public void onSaveInstanceState(Bundle outState)
        {
            super.onSaveInstanceState(outState);
            LocationsManager.storeLocationsInBundle(outState, _targetLocations);
        }

        @Override
        public void onTargetLocationOpened(Bundle openerArgs, Location location)
        {
            openNextLocation();
        }

        @Override
        public void onTargetLocationNotOpened(Bundle openerArgs)
        {
            if(_targetLocations.isEmpty())
            {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
            else
                openNextLocation();
        }

        @Override
        public void onPause()
        {
            _resHandler.onPause();
            super.onPause();
        }

        @Override
        public void onResume()
        {
            super.onResume();
            _resHandler.handle();
        }

        public void startOpeningLocations()
        {
            if(MasterPasswordDialog.checkMasterPasswordIsSet(getActivity(), getFragmentManager(), getTag()))
                openNextLocation();
        }

        protected void onFirstStart()
        {
            if(MasterPasswordDialog.checkMasterPasswordIsSet(getActivity(), getFragmentManager(), getTag()))
                openNextLocation();
        }

        //master passsword is set
        @Override
        public void onPasswordEntered(PasswordDialog dlg)
        {
            openNextLocation();
        }

        //master passsword is not set
        @Override
        public void onPasswordNotEntered(PasswordDialog dlg)
        {
            if(MasterPasswordDialog.checkSettingsKey(getActivity()))
                openNextLocation();
            else
                getActivity().finish();
        }

        private final ActivityResultHandler _resHandler = new ActivityResultHandler();
        private ArrayList<Location> _targetLocations;
        private LocationsManager _locationsManager;

        private void openNextLocation()
        {
            if(_targetLocations.isEmpty())
            {
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }
            else
            {
                Location loc = _targetLocations.get(0);
                loc.getExternalSettings().setVisibleToUser(true);
                loc.saveExternalSettings();
                _targetLocations.remove(0);
                Bundle args = new Bundle();
                setOpenerArgs(args, loc);
                LocationOpenerBaseFragment opener = LocationOpenerBaseFragment.getDefaultOpenerForLocation(loc);
                opener.setArguments(args);
                getFragmentManager().beginTransaction().add(opener, LocationOpenerBaseFragment.getOpenerTag(loc)).commit();
            }
        }

        protected void setOpenerArgs(Bundle args, Location loc)
        {
            args.putAll(getActivity().getIntent().getExtras());
            args.putString(LocationOpenerBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG, getTag());
            LocationsManager.storePathsInBundle(args, loc, null);
        }
    }

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
        Util.setTheme(this);
	    super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        if(savedInstanceState == null)
            getFragmentManager().beginTransaction().add(createFragment(), MainFragment.TAG).commit();
    }

    protected MainFragment createFragment()
    {
        return new MainFragment();
    }
}
