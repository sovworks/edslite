package com.sovworks.eds.android.locations.tasks;

import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.ContainerFormatter;
import com.sovworks.eds.container.ContainerFormatterBase;
import com.sovworks.eds.container.EDSLocationFormatter;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

public abstract class CreateContainerTaskFragmentBase extends CreateEDSLocationTaskFragment
{
    public static ContainerFormatInfo getContainerFormatByName(String name)
    {
        for (ContainerFormatInfo ci : EdsContainer.getSupportedFormats())
            if (ci.getFormatName().equals(name))
                return ci;
        return null;
    }

    public static final String ARG_CONTAINER_FORMAT = "com.sovworks.eds.android.CONTAINER_FORMAT";
    public static final String ARG_CIPHER_MODE_NAME = "com.sovworks.eds.android.CIPHER_MODE_NAME";
    public static final String ARG_HASHING_ALG = "com.sovworks.eds.android.HASHING_ALG";
    public static final String ARG_SIZE = "com.sovworks.eds.android.SIZE";
    public static final String ARG_FILL_FREE_SPACE = "com.sovworks.eds.android.FILL_FREE_SPACE";
    public static final String ARG_FILE_SYSTEM_TYPE = "com.sovworks.eds.android.FILE_SYSTEM_TYPE";

    @Override
    protected EDSLocationFormatter createFormatter()
    {
        return new ContainerFormatter();
    }

    @Override
    protected void initFormatter(TaskState state, EDSLocationFormatter formatter, SecureBuffer password) throws Exception
    {
        super.initFormatter(state, formatter, password);
        Bundle args = getArguments();
        ContainerFormatterBase cf = (ContainerFormatterBase)formatter;
        cf.setContainerFormat(getContainerFormatByName(args.getString(ARG_CONTAINER_FORMAT)));
        cf.setContainerSize(args.getInt(ARG_SIZE) * 1024L * 1024L);
        cf.setNumKDFIterations(args.getInt(Openable.PARAM_KDF_ITERATIONS, 0));
        FileSystemInfo fst = args.getParcelable(ARG_FILE_SYSTEM_TYPE);
        if(fst!=null)
            cf.setFileSystemType(fst);
        String encAlgName = args.getString(ARG_CIPHER_NAME);
        String encModeName = args.getString(ARG_CIPHER_MODE_NAME);
        if (encAlgName != null && encModeName != null)
            cf.setEncryptionEngine(encAlgName, encModeName);
        String hashAlgName = args.getString(ARG_HASHING_ALG);
        if (hashAlgName != null)
            cf.setHashFunc(hashAlgName);
        cf.enableFreeSpaceRand(args.getBoolean(ARG_FILL_FREE_SPACE));
    }

    @Override
    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        Path path = locationLocation.getCurrentPath();
        if (path.exists() && path.isDirectory())
            throw new UserException(_context,
                    R.string.container_file_name_is_not_specified);
        if (args.getInt(ARG_SIZE) < 1)
            throw new UserException(getActivity(),
                    R.string.err_container_size_is_too_small);

        if (!getArguments().getBoolean(ARG_OVERWRITE, false))
        {
            if (path.exists()
                    && path.isFile()
                    && path.getFile().getSize() > 0)
            {
                state.setResult(RESULT_REQUEST_OVERWRITE);
                return false;
            }
        }
        return true;
    }

}
