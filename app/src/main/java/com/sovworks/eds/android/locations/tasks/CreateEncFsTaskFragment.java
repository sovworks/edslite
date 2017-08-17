package com.sovworks.eds.android.locations.tasks;

import android.os.Bundle;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.container.EDSLocationFormatter;
import com.sovworks.eds.container.EncFsFormatter;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.Config;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.Openable;

public class CreateEncFsTaskFragment extends CreateEDSLocationTaskFragment
{
    public static final String TAG = "com.sovworks.eds.android.locations.tasks.CreateEncFsTaskFragment";

    public static final String ARG_NAME_CIPHER_NAME = "com.sovworks.eds.android.NAME_CIPHER_NAME";
    public static final String ARG_KEY_SIZE = "com.sovworks.eds.android.KEY_SIZE";
    public static final String ARG_BLOCK_SIZE = "com.sovworks.eds.android.BLOCK_SIZE";
    public static final String ARG_MAC_BYTES = "com.sovworks.eds.android.MAC_BYTES";
    public static final String ARG_RAND_BYTES = "com.sovworks.eds.android.RAND_BYTES";
    public static final String ARG_UNIQUE_IV = "com.sovworks.eds.android.UNIQUE_IV";
    public static final String ARG_EXTERNAL_IV = "com.sovworks.eds.android.EXTERNAL_IV";
    public static final String ARG_CHAINED_NAME_IV = "com.sovworks.eds.android.CHAINED_NAME_IV";
    public static final String ARG_ALLOW_EMPTY_BLOCKS = "com.sovworks.eds.android.ALLOW_EMPTY_BLOCKS";

    @Override
    protected EDSLocationFormatter createFormatter()
    {
        return new EncFsFormatter();
    }

    @Override
    protected void initFormatter(TaskState state, EDSLocationFormatter formatter, SecureBuffer password) throws Exception
    {
        super.initFormatter(state, formatter, password);
        Bundle args = getArguments();
        EncFsFormatter cf = (EncFsFormatter)formatter;
        cf.setDataCodecName(args.getString(ARG_CIPHER_NAME));
        cf.setNameCodecName(args.getString(ARG_NAME_CIPHER_NAME));
        Config c = cf.getConfig();
        c.setKeySize(args.getInt(ARG_KEY_SIZE));
        c.setBlockSize(args.getInt(ARG_BLOCK_SIZE));
        c.setKDFIterations(args.getInt(Openable.PARAM_KDF_ITERATIONS));
        c.setMACBytes(args.getInt(ARG_MAC_BYTES));
        c.setMACRandBytes(args.getInt(ARG_RAND_BYTES));
        c.useUniqueIV(args.getBoolean(ARG_UNIQUE_IV));
        c.useExternalFileIV(args.getBoolean(ARG_EXTERNAL_IV));
        c.useChainedNameIV(args.getBoolean(ARG_CHAINED_NAME_IV));
        c.allowHoles(args.getBoolean(ARG_ALLOW_EMPTY_BLOCKS));
    }

    @Override
    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        Path path = locationLocation.getCurrentPath();
        if(path.isFile())
            path = path.getParentPath();
        if (path == null || !path.isDirectory())
            throw new UserException(_context,
                    R.string.wrong_encfs_root_path);
        if (!args.getBoolean(ARG_OVERWRITE, false))
        {
            Path cfgPath = PathUtil.buildPath(path, Config.CONFIG_FILENAME);
            if (cfgPath!=null && cfgPath.isFile()
                    && cfgPath.getFile().getSize() > 0)
            {
                state.setResult(RESULT_REQUEST_OVERWRITE);
                return false;
            }
        }

        return true;
    }
}
