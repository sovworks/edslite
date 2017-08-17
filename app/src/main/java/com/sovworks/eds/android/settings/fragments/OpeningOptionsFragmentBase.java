package com.sovworks.eds.android.settings.fragments;

import android.os.Bundle;

import com.sovworks.eds.android.fragments.PropertiesFragmentBase;
import com.sovworks.eds.android.settings.container.UseExternalFileManagerPropertyEditor;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.settings.PropertiesHostWithLocation;
import com.sovworks.eds.android.settings.PropertiesHostWithStateBundle;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.settings.container.OpenInReadOnlyModePropertyEditor;
import com.sovworks.eds.android.settings.container.PIMPropertyEditor;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.settings.Settings;

public abstract class OpeningOptionsFragmentBase extends PropertiesFragmentBase implements PropertiesHostWithStateBundle, PropertiesHostWithLocation
{
    public void saveExternalSettings()
    {
        _location.saveExternalSettings();
    }

    @Override
    public Bundle getState()
    {
        return _state;
    }

    @Override
    public Location getTargetLocation()
    {
        return _location;
    }

    @Override
    protected void createProperties()
    {
        _location = (Openable) LocationsManager.
                getLocationsManager(getActivity()).
                getFromIntent(getActivity().getIntent(), null);
        if(_location == null)
        {
            getActivity().finish();
            return;
        }
        _settings = UserSettings.getSettings(getActivity());
        _propertiesView.setInstantSave(true);
        Bundle extras = getActivity().getIntent().getExtras();
        if(extras!=null)
            _state.putAll(extras);
        createOpenableProperties();
        if(_location instanceof EDSLocation)
            createEDSLocationProperties();
        if(_location instanceof ContainerLocation)
            createContainerProperties();
    }

    protected Openable _location;
    protected Settings _settings;

    protected void createEDSLocationProperties()
    {
        _propertiesView.addProperty(new OpenInReadOnlyModePropertyEditor(this));
    }

    protected void createOpenableProperties()
    {
        int id = _propertiesView.addProperty(new PIMPropertyEditor(this));
        if(!_location.hasCustomKDFIterations())
            _propertiesView.setPropertyState(id, false);
        id = _propertiesView.addProperty(new UseExternalFileManagerPropertyEditor(this));
        if(_settings.getExternalFileManagerInfo() == null)
            _propertiesView.setPropertyState(id, false);
    }

    protected void createContainerProperties()
    {
    }

    private final Bundle _state = new Bundle();
}
