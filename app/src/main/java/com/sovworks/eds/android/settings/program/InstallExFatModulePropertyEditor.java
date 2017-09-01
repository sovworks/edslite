package com.sovworks.eds.android.settings.program;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.filemanager.activities.FileManagerActivity;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.sovworks.eds.android.settings.ButtonPropertyEditor;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.exfat.ExFat;
import com.sovworks.eds.fs.std.StdFs;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.Util;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.File;
import java.io.IOException;

import static com.sovworks.eds.locations.LocationsManagerBase.PARAM_LOCATION_URI;

public class InstallExFatModulePropertyEditor extends ButtonPropertyEditor
{

    public static class InstallExfatModuleTask extends TaskFragment
    {
        public static final String TAG = "InstallExfatModuleTask";

        public static InstallExfatModuleTask newInstance(Uri moduleLocationUri)
        {
            Bundle args = new Bundle();
            args.putParcelable(PARAM_LOCATION_URI, moduleLocationUri);
            InstallExfatModuleTask f = new InstallExfatModuleTask();
            f.setArguments(args);
            return f;
        }

        @Override
        protected void initTask(Activity activity)
        {
            _context = activity.getApplicationContext();
        }

        @Override
        protected void doWork(TaskState uiUpdater) throws Exception
        {

            Location moduleLocation = LocationsManager.getLocationsManager(_context).getFromBundle(getArguments(), null);
            if(!moduleLocation.getCurrentPath().isFile())
                throw new UserException(_context, R.string.file_not_found);

            File targetPath = ExFat.getModulePath();
            Path targetFolderPath = StdFs.getStdFs().getPath(
                    targetPath.getParent()
            );
            PathUtil.makeFullPath(targetFolderPath);
            Util.copyFile(
                    moduleLocation.getCurrentPath().getFile(),
                    targetFolderPath.getDirectory(),
                    targetPath.getName());
            if(!ExFat.isModuleInstalled() && !ExFat.isModuleIncompatible())
            {
                ExFat.loadNativeLibrary();
                uiUpdater.setResult(true);
            }
            else
                uiUpdater.setResult(false);

        }

        @Override
        protected TaskCallbacks getTaskCallbacks(final Activity activity)
        {
            return new ProgressDialogTaskFragmentCallbacks(activity, R.string.loading)
            {
                @Override
                public void onCompleted(Bundle args, Result result)
                {
                    try
                    {
                        if((Boolean)result.getResult())
                            Toast.makeText(activity, R.string.module_has_been_installed, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(activity, R.string.restart_application, Toast.LENGTH_LONG).show();
                    }
                    catch (Throwable e)
                    {
                        Logger.showAndLog(_context, e);
                    }
                }
            };
        }

        private Context _context;
    }

    public InstallExFatModulePropertyEditor(Host host)
    {
        super(host,
                R.string.install_exfat_module,
                host.getContext().getString(R.string.install_exfat_module),
                host.getContext().getString(R.string.install_exfat_module_desc, GlobalConfig.EXFAT_MODULE_URL),
                host.getContext().getString(R.string.select_file));
    }

    @Override
    protected void onButtonClick()
    {
        try
        {
            requestActivity(getSelectPathIntent(), SELECT_PATH_REQ_CODE);
        }
        catch(Exception e)
        {
            Logger.showAndLog(getHost().getContext(), e);
        }
    }

    @Override
    protected void onPropertyRequestResult(int propertyRequestCode, int resultCode, Intent data)
    {
        if(propertyRequestCode == SELECT_PATH_REQ_CODE && resultCode == Activity.RESULT_OK && data!=null)
            onPathSelected(data);
    }

    private Intent getSelectPathIntent() throws IOException
    {
        return FileManagerActivity.getSelectPathIntent(
                getHost().getContext(),
                null,
                false,
                true,
                false,
                false,
                true,
                true);
    }

    private void onPathSelected(Intent result)
    {
        Uri uri = result.getData();
        getHost().
                getFragmentManager().
                beginTransaction().
                add(InstallExfatModuleTask.newInstance(uri), InstallExfatModuleTask.TAG)
                .commit();
    }

    private static final int SELECT_PATH_REQ_CODE = 1;
}
