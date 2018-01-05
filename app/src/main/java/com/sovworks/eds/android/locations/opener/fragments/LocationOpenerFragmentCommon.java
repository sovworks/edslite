package com.sovworks.eds.android.locations.opener.fragments;

import android.os.Bundle;

import com.sovworks.eds.android.dialogs.PasswordDialog;
import com.sovworks.eds.android.dialogs.PasswordDialogBase;
import com.sovworks.eds.android.errors.WrongPasswordOrBadContainerException;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.WrongPasswordException;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;

public class LocationOpenerFragmentCommon extends LocationOpenerBaseFragment implements PasswordDialog.PasswordReceiver
{


    public static class OpenLocationTaskFragment extends LocationOpenerBaseFragment.OpenLocationTaskFragment
    {

        @Override
        protected void procLocation(TaskState state, Location location, Bundle param) throws Exception
        {
            try
            {
                openLocation((Openable) location, param);
                regLocation((Openable)location);
            }
            catch(WrongPasswordException e)
            {
                throw new WrongPasswordOrBadContainerException(_context);
            }
            super.procLocation(state, location, param);
        }

        protected void openLocation(Openable location, Bundle param) throws Exception
        {
            if(location.isOpen())
                return;

            location.setOpeningProgressReporter(_openingProgressReporter);

            if(param.containsKey(Openable.PARAM_PASSWORD))
                location.setPassword(param.getParcelable(Openable.PARAM_PASSWORD));
            if(param.containsKey(Openable.PARAM_KDF_ITERATIONS))
                location.setNumKDFIterations(param.getInt(Openable.PARAM_KDF_ITERATIONS));

            location.open();

        }

        protected void regLocation(Openable location)
        {
            _locationsManager.regOpenedLocation(location);
        }
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        usePassword(getPasswordDialogResultBundle(dlg));
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        finishOpener(false, getTargetLocation());
    }

    @Override
	protected TaskFragment getOpenLocationTask()
	{
		return new OpenLocationTaskFragment();
	}

    protected Bundle getAskPasswordArgs()
    {
        Bundle args = new Bundle();
        args.putString(PasswordDialogBase.ARG_RECEIVER_FRAGMENT_TAG, getTag());
        Openable loc = getTargetLocation();
        LocationsManager.storePathsInBundle(args, loc, null);
        return args;
    }

    @Override
    protected Openable getTargetLocation()
    {
        return (Openable)super.getTargetLocation();
    }

    @Override
    protected void openLocation()
    {
        Openable ol = getTargetLocation();
        Bundle defaultArgs = getArguments();
        if(defaultArgs == null)
            defaultArgs = new Bundle();
        if(ol.isOpen())
            super.openLocation();
        else if (needPasswordDialog(ol, defaultArgs))
            askPassword();
        else
            startOpeningTask(initOpenLocationTaskParams(getTargetLocation()));
    }

    @Override
    protected Bundle initOpenLocationTaskParams(Location location)
    {
        Bundle args = super.initOpenLocationTaskParams(location);
        Bundle defaultArgs = getArguments();
        if(defaultArgs != null)
        {
            if(defaultArgs.containsKey(Openable.PARAM_PASSWORD)
                    && !args.containsKey(Openable.PARAM_PASSWORD))
            {
                String val = defaultArgs.getString(Openable.PARAM_PASSWORD);
                if(val!=null)
                    args.putParcelable(
                            Openable.PARAM_PASSWORD,
                            new SecureBuffer(val.toCharArray())
                    );
            }
            if(defaultArgs.containsKey(Openable.PARAM_KDF_ITERATIONS)
                    && !args.containsKey(Openable.PARAM_KDF_ITERATIONS))
                args.putInt(Openable.PARAM_KDF_ITERATIONS, args.getInt(Openable.PARAM_KDF_ITERATIONS));
        }
        return args;
    }

    protected void usePassword(Bundle passwordDialogResultBundle)
	{
        Bundle args = initOpenLocationTaskParams(getTargetLocation());
        updateOpenLocationTaskParams(args, passwordDialogResultBundle);
		startOpeningTask(args);
	}

	protected void updateOpenLocationTaskParams(Bundle args, Bundle passwordDialogResultBundle)
    {
        if(passwordDialogResultBundle.containsKey(Openable.PARAM_PASSWORD))
        {
            SecureBuffer sb = passwordDialogResultBundle.getParcelable(Openable.PARAM_PASSWORD);
            if(sb!=null && (sb.length() > 0 || !args.containsKey(Openable.PARAM_PASSWORD)))
                args.putParcelable(Openable.PARAM_PASSWORD, sb);
        }
        if(passwordDialogResultBundle.containsKey(Openable.PARAM_KDF_ITERATIONS))
            args.putInt(Openable.PARAM_KDF_ITERATIONS, passwordDialogResultBundle.getInt(Openable.PARAM_KDF_ITERATIONS));
    }

	protected void askPassword()
    {
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(getAskPasswordArgs());
        pd.show(getFragmentManager(), PasswordDialog.TAG);
    }

    protected boolean needPasswordDialog(Openable ol, Bundle defaultArgs)
    {
        if(defaultArgs == null)
            defaultArgs = new Bundle();
        return (ol.requirePassword() && !defaultArgs.containsKey(Openable.PARAM_PASSWORD)) ||
                (ol.requireCustomKDFIterations() && !defaultArgs.containsKey(Openable.PARAM_KDF_ITERATIONS));
    }

    protected Bundle getPasswordDialogResultBundle(PasswordDialog pd)
    {
        Bundle res = new Bundle();
        res.putAll(pd.getOptions());
        res.putParcelable(Openable.PARAM_PASSWORD, new SecureBuffer(pd.getPassword()));
        return res;
    }
}
