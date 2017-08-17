package com.sovworks.eds.android.settings;

import com.sovworks.eds.locations.Location;

public interface PropertiesHostWithLocation extends PropertyEditor.Host
{
    Location getTargetLocation();
}
