package com.sovworks.eds.android.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.views.PropertiesView;

public abstract class PropertiesFragmentBase extends Fragment implements PropertyEditor.Host
{
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.properties_fragment, container, false);
        _propertiesView = view.findViewById(R.id.list);
        createProperties();
        initProperties(savedInstanceState);
        return view;
    }

    @SuppressLint("Override")
    @Override
    public Context getContext()
    {
        return getActivity();
    }

    @Override
    public PropertiesView getPropertiesView()
    {
        return _propertiesView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(!getPropertiesView().onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    protected PropertiesView _propertiesView;

    protected void initProperties(Bundle state)
    {
        try
        {
            if (state == null)
                _propertiesView.loadProperties();
            else
                _propertiesView.loadProperties(state);
        }
        catch (Throwable e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    protected abstract void createProperties();

    protected void setPropertiesEnabled(Iterable<Integer> propertyIds, boolean enabled)
    {
        _propertiesView.setPropertiesState(propertyIds, enabled);
    }
}
