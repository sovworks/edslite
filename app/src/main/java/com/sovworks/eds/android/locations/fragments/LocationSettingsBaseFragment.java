package com.sovworks.eds.android.locations.fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.fragments.PropertiesFragmentBase;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.settings.container.UseExternalFileManagerPropertyEditor;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.settings.PropertiesHostWithLocation;
import com.sovworks.eds.android.settings.TextPropertyEditor;
import com.sovworks.eds.locations.LocationBase;

public abstract class LocationSettingsBaseFragment extends PropertiesFragmentBase implements PropertiesHostWithLocation
{
    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        _location = (LocationBase) LocationsManager.getLocationsManager(getActivity()).getFromIntent(
                getActivity().getIntent(),
                null
        );
        setHasOptionsMenu(true);
    }

    @Override
    protected void initProperties(Bundle state)
    {
        if(_location == null)
        {
            _location = createNewLocation();
            _location.getExternalSettings().setVisibleToUser(true);
        }
        else
            _propertiesView.setInstantSave(true);

        super.initProperties(state);
    }

    public void saveExternalSettings()
    {
        _location.saveExternalSettings();
    }

    @Override
    public LocationBase getTargetLocation()
    {
        return _location;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(_propertiesView.isInstantSave())
        {
            saveExternalSettings();
            LocationsManager.broadcastLocationChanged(getActivity(), _location);
        }
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.create_location_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        MenuItem mi = menu.findItem(R.id.confirm);
        mi.setVisible(!_propertiesView.isInstantSave());
        mi.setEnabled(isValidData());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
            case R.id.confirm:
                addNewLocation();
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    protected boolean isValidData()
    {
        return true;
    }

    protected abstract LocationBase createNewLocation();

    @Override
    protected void createProperties()
    {
        _propertiesView.addProperty(new TextPropertyEditor(this, R.string.title, 0, getTag())
        {
            @Override
            protected String loadText()
            {
                return _location.getTitle();
            }
            @Override
            protected void saveText(String text)
            {
                _location.getExternalSettings().setTitle(text.trim());
                if(getPropertiesView().isInstantSave())
                    saveExternalSettings();
            }
        });
        int id = _propertiesView.addProperty(new UseExternalFileManagerPropertyEditor(this));
        _propertiesView.setPropertyState(id, UserSettings.getSettings(getActivity()).getExternalFileManagerInfo()!=null);
    }

    private LocationBase _location;

    private void addNewLocation()
    {
        try
        {
            getPropertiesView().saveProperties();
            LocationsManager.getLocationsManager(getActivity()).addNewLocation(_location, true);
            saveExternalSettings();
            LocationsManager.broadcastLocationAdded(getActivity(), _location);
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }
}
