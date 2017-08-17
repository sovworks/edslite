package com.sovworks.eds.android.activities;

import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.fragments.PropertiesFragmentBase;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.settings.PathPropertyEditor;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.TextPropertyEditor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.widgets.LocationShortcutWidget;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;
import com.sovworks.eds.settings.Settings;

import java.io.IOException;

public abstract class LocationShortcutWidgetConfigActivityBase extends SettingsBaseActivity
{

    public static class MainFragment extends PropertiesFragmentBase
    {
        public class TargetPathPropertyEditor extends PathPropertyEditor
        {
            TargetPathPropertyEditor()
            {
                super(MainFragment.this,R.string.target_path, 0, getTag());
            }

            @Override
            protected String loadText()
            {
                return _state.getString(ARG_URI);
            }

            @Override
            protected void saveText(String text) throws Exception
            {
                _state.putString(ARG_URI, text);
            }

            @Override
            protected Intent getSelectPathIntent() throws IOException
            {
                return FileManagerActivity.getSelectPathIntent(
                        getContext(),
                        null,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true);
            }

        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        protected void createProperties()
        {
            _propertiesView.addProperty(new TextPropertyEditor(this, R.string.enter_widget_title, 0, getTag())
            {
                @Override
                protected String loadText()
                {
                    return _state.getString(ARG_TITLE);
                }

                @Override
                protected void saveText(String text) throws Exception
                {
                    _state.putString(ARG_TITLE, text);
                }
            });
            _propertiesView.addProperty(getPathPE());
        }

        @Override
        public void onCreateOptionsMenu (Menu menu, MenuInflater inflater)
        {
            inflater.inflate(R.menu.widget_config_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem)
        {
            switch (menuItem.getItemId())
            {
                case R.id.confirm:
                    createWidget();
                    return true;
                default:
                    return super.onOptionsItemSelected(menuItem);
            }
        }

        private static final String ARG_TITLE = "title";
        private static final String ARG_URI = "uri";

        private final Bundle _state = new Bundle();

        protected PropertyEditor getPathPE()
        {
            return new TargetPathPropertyEditor();
        }

        private void createWidget()
        {
            try
            {
                _propertiesView.saveProperties();
                String title = _state.getString(ARG_TITLE);
                String path = _state.getString(ARG_URI);
                if(title==null || title.trim().isEmpty() || path==null || path.trim().isEmpty())
                    return;

                initWidgetFields(title, path);

                Intent resultValue = new Intent();
                resultValue.putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        getWidgetId()
                );
                getActivity().setResult(RESULT_OK, resultValue);
                getActivity().finish();
            }
            catch (Exception e)
            {
                Logger.showAndLog(getActivity(), e);
            }
        }

        private void initWidgetFields(String title,String path) throws Exception
        {
            int widgetId = getWidgetId();
            Location target = LocationsManager.getLocationsManager(getActivity()).getDefaultLocationFromPath(path);
            Settings.LocationShortcutWidgetInfo info = new Settings.LocationShortcutWidgetInfo();
            info.widgetTitle = title;
            info.locationUriString = target.getLocationUri().toString();
            UserSettings.getSettings(getContext()).setLocationShortcutWidgetInfo(widgetId, info);
            LocationShortcutWidget.setWidgetLayout(
                    getContext(),
                    AppWidgetManager.getInstance(getContext()),
                    widgetId,
                    info,
                    (!(target instanceof Openable)) || ((Openable) target).isOpen()
            );
        }

        private int getWidgetId()
        {
            return getActivity().getIntent().getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }
	
	@Override
    public void onCreate(Bundle icicle)
	{
        Util.setTheme(this);
        super.onCreate(icicle);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected Fragment getSettingsFragment()
    {
        return new MainFragment();
    }
}
