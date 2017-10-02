package com.sovworks.eds.android.tasks;

import android.os.Bundle;

import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.container.VolumeLayout;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.RandomAccessIO;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

import java.io.IOException;

public abstract class ChangeContainerPasswordTaskBase extends ChangeEDSLocationPasswordTask
{
    public static final String TAG = "com.sovworks.eds.android.tasks.ChangeContainerPasswordTask";
    //public static final String ARG_FIN_ACTIVITY = "fin_activity";

	@Override
	protected void changeLocationPassword() throws IOException, ApplicationException
    {
        ContainerLocation cont = (ContainerLocation)_location;
        setContainerPassword(cont);
        RandomAccessIO io = cont.getLocation().getCurrentPath().getFile().getRandomAccessIO(File.AccessMode.ReadWrite);
        try
        {
            VolumeLayout vl = cont.getEdsContainer().getVolumeLayout();
            vl.writeHeader(io);
        }
        finally
        {
            io.close();
        }
	}

	protected void setContainerPassword(ContainerLocation container) throws IOException
    {
        VolumeLayout vl = container.getEdsContainer().getVolumeLayout();
        Bundle args  = getArguments();
        SecureBuffer sb = Util.getPassword(args, LocationsManager.getLocationsManager(_context));
        vl.setPassword(sb.getDataArray());
        sb.close();
        if(args.containsKey(Openable.PARAM_KDF_ITERATIONS))
            vl.setNumKDFIterations(args.getInt(Openable.PARAM_KDF_ITERATIONS));
    }
}
