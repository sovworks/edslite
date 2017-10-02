package com.sovworks.eds.android.locations.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.text.format.Formatter;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialogBase;
import com.sovworks.eds.android.fragments.PropertiesFragmentBase;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.locations.opener.fragments.LocationOpenerBaseFragment;
import com.sovworks.eds.android.service.LocationsService;
import com.sovworks.eds.android.settings.ButtonPropertyEditor;
import com.sovworks.eds.android.settings.IntPropertyEditor;
import com.sovworks.eds.android.settings.PropertiesHostWithLocation;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.StaticPropertyEditor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.settings.container.ChangePasswordPropertyEditor;
import com.sovworks.eds.android.settings.container.OpenInReadOnlyModePropertyEditor;
import com.sovworks.eds.android.settings.container.SavePIMPropertyEditor;
import com.sovworks.eds.android.settings.container.SavePasswordPropertyEditor;
import com.sovworks.eds.android.settings.container.UseExternalFileManagerPropertyEditor;
import com.sovworks.eds.android.tasks.LoadLocationInfoTask;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class EDSLocationSettingsFragmentBase extends PropertiesFragmentBase implements LocationOpenerBaseFragment.LocationOpenerResultReceiver, PropertiesHostWithLocation, PasswordDialog.PasswordReceiver
{
    public static class LocationInfo
    {
        public String pathToLocation;
        public long totalSpace;
        public long freeSpace;
    }

    public EDSLocation getLocation()
    {
        return _location;
    }

    public void setLocation(EDSLocation container)
    {
        _location = container;
    }

	public ActivityResultHandler getResHandler()
	{
		return _resHandler;
	}

    public void onTargetLocationOpened(Bundle openerArgs, Location location)
    {
        onIntSettingsAvailable((EDSLocation)location);
    }

    public void onTargetLocationNotOpened(Bundle openerArgs)
    {

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

    public void saveExternalSettings()
	{
        _location.saveExternalSettings();
    }

    @Override
    public Location getTargetLocation()
    {
        return _location;
    }

    public TaskFragment getChangePasswordTask(PasswordDialog pd)
    {
        TaskFragment t = createChangePasswordTaskInstance();
        t.setArguments(getChangePasswordTaskArgs(pd));
        return t;
    }

    protected Bundle getChangePasswordTaskArgs(PasswordDialog dlg)
    {
        final Bundle args = new Bundle();
        SecureBuffer sb = new SecureBuffer();
        char[] tmp = dlg.getPassword();
        sb.adoptData(tmp);
        args.putParcelable(Openable.PARAM_PASSWORD, sb);
        LocationsManager.storePathsInBundle(args, getLocation(), null);
        return args;
    }

    protected abstract TaskFragment createChangePasswordTaskInstance();

    public TaskFragment.TaskCallbacks getLoadLocationInfoTaskCallbacks()
    {
        return new LoadLocationInfoTaskCallbacks();
    }

    public LocationInfo getLocationInfo()
    {
        return _locationInfo;
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if(pr!=null)
            pr.onPasswordEntered(dlg);
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if(pr!=null)
            pr.onPasswordNotEntered(dlg);
    }

    class LoadLocationInfoTaskCallbacks extends ProgressDialogTaskFragmentCallbacks
    {
        LoadLocationInfoTaskCallbacks()
        {
            super(getActivity(), R.string.loading);
        }

        @Override
        public void onCompleted(Bundle args, TaskFragment.Result result)
        {
            super.onCompleted(args, result);
            try
            {
                _locationInfo = (LocationInfo) result.getResult();
                _propertiesView.setPropertyState(R.string.path_to_container, true);
                _propertiesView.setPropertyState(R.string.uri_of_the_container, true);
                _propertiesView.setPropertiesState(Arrays.asList(
                        R.string.free_space, R.string.total_space
                ), _location.isOpenOrMounted());
                _propertiesView.loadProperties(Arrays.asList(
                        R.string.path_to_container,
                        R.string.uri_of_the_container,
                        R.string.free_space,
                        R.string.total_space
                        ),
                        null
                );

            }
            catch (Throwable e)
            {
                Logger.showAndLog(_context, e);
            }
        }
    }

    protected final ActivityResultHandler _resHandler = new ActivityResultHandler();
    protected EDSLocation _location;
    protected LocationInfo _locationInfo;

    protected abstract LocationOpenerBaseFragment getLocationOpener();

    protected LoadLocationInfoTask initLoadLocationInfoTask()
    {
        return LoadLocationInfoTask.newInstance(getLocation());
    }

    protected void startLoadLocationInfoTask()
    {
        getFragmentManager().
                beginTransaction().
                add(
                        initLoadLocationInfoTask(),
                        LoadLocationInfoTask.TAG
                ).
                commitAllowingStateLoss();
    }

    protected void onIntSettingsAvailable(EDSLocation loc)
    {
        setLocation(loc);
        showInternalSettings();
        getPropertiesView().loadProperties();
        startLoadLocationInfoTask();
    }

    @Override
    protected void createProperties()
    {
        _location = (EDSLocation) LocationsManager.
                getLocationsManager(getActivity()).
                getFromIntent(getActivity().getIntent(), null);
        if(_location == null)
        {
            getActivity().finish();
            return;
        }
        _propertiesView.setInstantSave(true);
        createAllProperties();
        initPropertiesState();
    }

    protected void createAllProperties()
    {
        createStdProperties(new ArrayList<Integer>());
    }

    protected void initPropertiesState()
    {
        if(_location == null)
            _propertiesView.setPropertiesState(false);
        else
        {
            if(_location.isOpenOrMounted())
                showInternalSettings();
            else
                hideInternalSettings();

            _propertiesView.setPropertyState(R.string.path_to_container, false);
            _propertiesView.setPropertyState(R.string.save_password, _location.hasPassword());
            _propertiesView.setPropertyState(R.string.remember_kdf_iterations_multiplier, _location.hasCustomKDFIterations());
            _propertiesView.setPropertyState(
                    R.string.use_external_file_manager,
                    UserSettings.getSettings(getActivity()).getExternalFileManagerInfo()!=null
            );
            startLoadLocationInfoTask();
        }
    }

    protected void createStdProperties(Collection<Integer> ids)
    {
        createInfoProperties(ids);
        createPasswordProperties(ids);
        createMiscProperties(ids);
    }

    protected void createInfoProperties(Collection<Integer> ids)
    {
        ids.add(_propertiesView.addProperty(new StaticPropertyEditor(this, R.string.path_to_container)
        {
            @Override
            protected String loadText()
            {
                return _locationInfo == null ? "" : _locationInfo.pathToLocation;
            }
        }));
        ids.add(_propertiesView.addProperty(new StaticPropertyEditor(this, R.string.uri_of_the_container)
        {
            @Override
            protected String loadText()
            {
                return _locationInfo == null ? "" : _location.getLocationUri().toString();
            }
        }));
        ids.add(_propertiesView.addProperty(new StaticPropertyEditor(this, R.string.total_space)
        {
            @Override
            protected String loadText()
            {
                if (!_location.isOpenOrMounted() || _locationInfo == null)
                    return "";
                return Formatter.formatFileSize(getHost().getContext(), _locationInfo.totalSpace);
            }
        }));
        ids.add(_propertiesView.addProperty(new StaticPropertyEditor(this, R.string.free_space)
        {
            @Override
            protected String loadText()
            {
                if (!_location.isOpenOrMounted() || _locationInfo == null)
                    return "";
                return Formatter.formatFileSize(getHost().getContext(), _locationInfo.freeSpace);
            }
        }));
    }

    protected void createPasswordProperties(Collection<Integer> ids)
    {
        ids.add(_propertiesView.addProperty(new ChangePasswordPropertyEditor(this)));
        ids.add(_propertiesView.addProperty(new SavePasswordPropertyEditor(this)));
        ids.add(_propertiesView.addProperty(new SavePIMPropertyEditor(this)));
    }

    protected void createMiscProperties(Collection<Integer> ids)
    {
        ids.add(_propertiesView.addProperty(new IntPropertyEditor(this, R.string.auto_close_container, R.string.auto_close_container_desc, getTag())
        {
            @Override
            protected int loadValue()
            {
                return _location.getExternalSettings().getAutoCloseTimeout() / 60000;
            }

            @Override
            protected void saveValue(int value)
            {
                _location.getExternalSettings().setAutoCloseTimeout(value * 60000);
                saveExternalSettings();
                LocationsService.registerInactiveContainerCheck(getContext(), _location);
            }

            @Override
            protected int getDialogViewResId()
            {
                return R.layout.settings_edit_num_lim4;
            }
        }));
        ids.add(_propertiesView.addProperty(new OpenInReadOnlyModePropertyEditor(this)));
        ids.add(_propertiesView.addProperty(new ButtonPropertyEditor(
                this,
                R.string.internal_container_settings,
                R.string.internal_container_settings_desc,
                R.string.open
        )
        {
            @Override
            protected void onButtonClick()
            {
                Bundle openerArgs = new Bundle();
                LocationsManager.storePathsInBundle(openerArgs, _location, null);
                openerArgs.putString(LocationOpenerBaseFragment.PARAM_RECEIVER_FRAGMENT_TAG, getTag());
                Fragment opener = getLocationOpener();
                opener.setArguments(openerArgs);
                getFragmentManager().
                        beginTransaction().
                        add(opener, LocationOpenerBaseFragment.getOpenerTag(_location)).
                        commit();
            }
        }));
        ids.add(_propertiesView.addProperty(new UseExternalFileManagerPropertyEditor(this)));
    }

	protected void showInternalSettings()
	{
		setInternalPropertiesEnabled(true);
	}

    protected void hideInternalSettings()
	{
		setInternalPropertiesEnabled(false);
	}

    protected void setInternalPropertiesEnabled(boolean enabled)
	{
		_propertiesView.setPropertyState(R.string.free_space, enabled && _locationInfo!=null);
		_propertiesView.setPropertyState(R.string.total_space, enabled && _locationInfo!=null);
		_propertiesView.setPropertyState(R.string.change_container_password, enabled);
        _propertiesView.setPropertyState(R.string.internal_container_settings, !enabled);
	}
}
