package com.sovworks.eds.android.locations.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.fragments.PropertiesFragmentBase;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ActivityResultHandler;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.locations.dialogs.OverwriteContainerDialog;
import com.sovworks.eds.android.locations.tasks.AddExistingContainerTaskFragment;
import com.sovworks.eds.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.sovworks.eds.android.locations.tasks.CreateEDSLocationTaskFragment;
import com.sovworks.eds.android.settings.PropertiesHostWithStateBundle;
import com.sovworks.eds.android.settings.container.ExistingContainerPropertyEditor;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.util.concurrent.CancellationException;

public abstract class CreateEDSLocationFragmentBase extends PropertiesFragmentBase implements PropertiesHostWithStateBundle
{
    public static final String ARG_ADD_EXISTING_LOCATION = "com.sovworks.eds.android.ADD_EXISTING_CONTAINER";

    @Override
    public void onCreate(Bundle state)
    {
        if(state!=null)
            _state.putAll(state);
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState (Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putAll(_state);
    }

    @Override
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.create_location_menu, menu);
        menu.findItem(R.id.confirm).setTitle(R.string.create_new_container);
	}

    @Override
    public void onPrepareOptionsMenu (Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        MenuItem mi = menu.findItem(R.id.confirm);
        mi.setVisible(_state.containsKey(ARG_ADD_EXISTING_LOCATION));
        mi.setTitle(_state.getBoolean(ARG_ADD_EXISTING_LOCATION) ? R.string.add_container : R.string.create_new_container);
        boolean enabled = checkParams();
        mi.setEnabled(enabled);
        @SuppressWarnings("deprecation") StateListDrawable sld = (StateListDrawable) getActivity().getResources().getDrawable(R.drawable.ic_menu_done);
        if(sld!=null)
        {
            sld.setState(enabled ? new int[]{android.R.attr.state_enabled} : new int[0]);
            mi.setIcon(sld.getCurrent());
        }
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
            case R.id.confirm:
                if(_state.getBoolean(ARG_ADD_EXISTING_LOCATION))
                    startAddLocationTask();
                else
                    startCreateLocationTask();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        SecureBuffer sb = _state.getParcelable(Openable.PARAM_PASSWORD);
        if(sb!=null)
        {
            sb.close();
            _state.remove(Openable.PARAM_PASSWORD);
        }
    }

    @Override
    protected void initProperties(Bundle state)
    {
        _propertiesView.setInstantSave(true);
        super.initProperties(state);
    }

    public Bundle getState()
    {
        return _state;
    }

    public void setOverwrite(boolean val)
    {
        _state.putBoolean(CreateContainerTaskFragmentBase.ARG_OVERWRITE, val);
    }

    public void startCreateLocationTask()
    {
        try
        {
            _propertiesView.saveProperties();
            TaskFragment task = createCreateLocationTask();
            task.setArguments(_state);
            getFragmentManager().beginTransaction().add(task, CreateEDSLocationTaskFragment.TAG).commit();
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    public void startAddLocationTask()
    {
        try
        {
            _propertiesView.saveProperties();
            getFragmentManager().
                    beginTransaction().
                    add(createAddExistingLocationTask(), AddExistingContainerTaskFragment.TAG).commit();
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    public ActivityResultHandler getResHandler()
    {
        return _resHandler;
    }

    public void showAddExistingLocationProperties()
    {
        _state.putBoolean(ARG_ADD_EXISTING_LOCATION, true);
        _propertiesView.setPropertiesState(false);
        _propertiesView.setPropertyState(R.string.path_to_container, true);
        _propertiesView.setPropertyState(R.string.container_format, true);

    }

    public void showCreateNewLocationProperties()
    {
        _state.putBoolean(ARG_ADD_EXISTING_LOCATION, false);
        _propertiesView.setPropertiesState(false);
    }

    public TaskFragment.TaskCallbacks getAddExistingEDSLocationTaskCallbacks()
    {
        return new ProgressDialogTaskFragmentCallbacks(getActivity(), R.string.loading)
        {

            @Override
            public void onCompleted(Bundle args, TaskFragment.Result result)
            {
                try
                {
                    Location loc = (Location) result.getResult();
                    LocationsManager.broadcastLocationAdded(getContext(), loc);
                    Intent res = new Intent();
                    res.setData(loc.getLocationUri());
                    getActivity().setResult(Activity.RESULT_OK, res);
                    getActivity().finish();
                }
                catch (CancellationException ignored)
                {

                }
                catch (Throwable e)
                {
                    Logger.showAndLog(getActivity(), e);
                }
            }
        };
    }
    public TaskFragment.TaskCallbacks getCreateLocationTaskCallbacks()
    {
        return new CreateLocationTaskCallbacks();
    }

    protected final ActivityResultHandler _resHandler = new ActivityResultHandler();
    protected final Bundle _state = new Bundle();

    protected abstract TaskFragment createAddExistingLocationTask();
    protected abstract TaskFragment createCreateLocationTask();

    protected class CreateLocationTaskCallbacks implements TaskFragment.TaskCallbacks
    {

        @Override
        public void onPrepare(Bundle args)
        {

        }

        @Override
        public void onResumeUI(Bundle args)
        {
            _dialog = new ProgressDialog(getContext());
            _dialog.setMessage(getText(R.string.creating_container));
            _dialog.setIndeterminate(true);
            _dialog.setCancelable(true);
            _dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    CreateEDSLocationTaskFragment f = (CreateEDSLocationTaskFragment) getFragmentManager()
                            .findFragmentByTag(CreateContainerTaskFragmentBase.TAG);
                    if (f != null) f.cancel();
                }
            });
            _dialog.show();
        }

        @Override
        public void onSuspendUI(Bundle args)
        {
            _dialog.dismiss();
        }

        @Override
        public void onCompleted(Bundle args, TaskFragment.Result result)
        {
            if (result.isCancelled()) return;
            try
            {
                int res = (Integer) result.getResult();
                if (res == CreateContainerTaskFragmentBase.RESULT_REQUEST_OVERWRITE)
                    OverwriteContainerDialog
                            .showDialog(getFragmentManager());
                else
                {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
            catch (Throwable e)
            {
                Logger.showAndLog(getActivity(), result.getError());
            }
        }

        @Override
        public void onUpdateUI(Object state)
        {

        }

        private ProgressDialog _dialog;
    }

    @Override
    protected void createProperties()
    {
        createStartProperties();
        createNewLocationProperties();
        createExtProperties();

        if(_state.containsKey(ARG_ADD_EXISTING_LOCATION))
        {
            if(_state.getBoolean(ARG_ADD_EXISTING_LOCATION))
                showAddExistingLocationProperties();
            else
                showCreateNewLocationProperties();
        }
        else
            showAddExistingLocationRequestProperties();
    }

    protected void createStartProperties()
    {
        _propertiesView.addProperty(new ExistingContainerPropertyEditor(this));
    }

    protected void createNewLocationProperties()
    {
    }

    protected void createExtProperties()
    {

    }
    protected boolean checkParams()
    {
        Uri loc = _state.containsKey(CreateContainerTaskFragmentBase.ARG_LOCATION) ? (Uri)_state.getParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION) : null;
        return loc!=null && !loc.toString().isEmpty();
    }

    protected void showAddExistingLocationRequestProperties()
    {
        _propertiesView.setPropertiesState(false);
        _propertiesView.setPropertyState(R.string.create_new_container_or_add_existing_container, true);
    }
}
