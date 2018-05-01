package com.sovworks.eds.android.locations.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.AppInitHelper;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.trello.rxlifecycle2.components.RxActivity;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

public class OpenLocationsActivity extends RxActivity
{
    public static class MainFragment extends Fragment implements LocationOpenerBaseFragment.LocationOpenerResultReceiver
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
            if (!_targetLocations.isEmpty())
                _targetLocations.remove(0);
            openNextLocation();
        }

        @Override
        public void onTargetLocationNotOpened(Bundle openerArgs)
        {
            if (!_targetLocations.isEmpty())
                _targetLocations.remove(0);
            if(_targetLocations.isEmpty())
            {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
            else
                openNextLocation();
        }

        protected void onFirstStart()
        {
            openNextLocation();
        }

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
                String openerTag = LocationOpenerBaseFragment.getOpenerTag(loc);
                if (getFragmentManager().findFragmentByTag(openerTag) != null)
                    return;
                loc.getExternalSettings().setVisibleToUser(true);
                loc.saveExternalSettings();
                Bundle args = new Bundle();
                setOpenerArgs(args, loc);
                LocationOpenerBaseFragment opener = LocationOpenerBaseFragment.
                        getDefaultOpenerForLocation(loc);
                opener.setArguments(args);
                getFragmentManager().
                        beginTransaction().
                        add(
                                opener,
                                openerTag
                        ).
                        commit();
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
        AppInitHelper.
                createObservable(this).
                compose(bindToLifecycle()).
                subscribe(this::addMainFragment, err ->
                {
                    if(!(err instanceof CancellationException))
                        Logger.log(err);
                });
    }

    protected MainFragment createFragment()
    {
        return new MainFragment();
    }

    protected void addMainFragment()
    {
        FragmentManager fm = getFragmentManager();
        if(fm.findFragmentByTag(MainFragment.TAG) == null)
            fm.beginTransaction().add(createFragment(), MainFragment.TAG).commit();
    }
}
