package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.PropertiesHostWithLocation;
import com.sovworks.eds.android.settings.SwitchPropertyEditor;
import com.sovworks.eds.locations.Location;

public class UseExternalFileManagerPropertyEditor extends SwitchPropertyEditor
{
    public UseExternalFileManagerPropertyEditor(PropertiesHostWithLocation host)
    {
        super(host, R.string.use_external_file_manager, 0);
    }

    @Override
    public PropertiesHostWithLocation getHost()
    {
        return (PropertiesHostWithLocation) super.getHost();
    }

    @Override
    protected void saveValue(boolean value)
    {
        getLocation().getExternalSettings().setUseExtFileManager(value);
        if(getHost().getPropertiesView().isInstantSave())
            getLocation().saveExternalSettings();
    }

    @Override
    protected boolean loadValue()
    {
        return getLocation().getExternalSettings().useExtFileManager();
    }

    private Location getLocation()
    {
        return getHost().getTargetLocation();
    }
}
