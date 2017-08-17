package com.sovworks.eds.android.tasks;

import android.os.Bundle;

import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.locations.EncFsLocationBase;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.locations.LocationsManager;

import java.io.IOException;

public class ChangeEncFsPasswordTask extends ChangeEDSLocationPasswordTask
{
    public static final String TAG = "com.sovworks.eds.android.tasks.ChangeContainerPasswordTask";
    //public static final String ARG_FIN_ACTIVITY = "fin_activity";

	public static ChangeEncFsPasswordTask newInstance(EncFsLocationBase container, Bundle passwordDialogResult)
    {
        Bundle args = new Bundle();
        args.putAll(passwordDialogResult);
        LocationsManager.storePathsInBundle(args, container, null);
        ChangeEncFsPasswordTask f = new ChangeEncFsPasswordTask();
        f.setArguments(args);
        return f;
	}

    @Override
	protected void changeLocationPassword() throws IOException, ApplicationException
    {
        EncFsLocationBase loc = (EncFsLocationBase)_location;
        SecureBuffer sb = Util.getPassword(getArguments(), LocationsManager.getLocationsManager(_context));
        byte[] pd = sb.getDataArray();
        try
        {

            loc.getEncFs().encryptVolumeKeyAndWriteConfig(pd);
        }
        finally
        {
            SecureBuffer.eraseData(pd);
            sb.close();
        }
	}
}
