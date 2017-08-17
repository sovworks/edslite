package com.sovworks.eds.container;

import com.sovworks.eds.android.locations.EncFsLocation;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.AlgInfo;
import com.sovworks.eds.fs.encfs.Config;
import com.sovworks.eds.fs.encfs.DataCodecInfo;
import com.sovworks.eds.fs.encfs.FS;
import com.sovworks.eds.fs.encfs.NameCodecInfo;
import com.sovworks.eds.locations.EDSLocation;
import com.sovworks.eds.locations.Location;

import java.io.IOException;

public class EncFsFormatter extends EDSLocationFormatter
{
	public static AlgInfo findInfoByName(Config config, Iterable<? extends AlgInfo> supportedAlgs, String name)
	{
		for(AlgInfo info: supportedAlgs)
		{
			if(name.equals(info.getName()))
				return info.select(config);
		}
		throw new IllegalArgumentException("Unsupported codec: " + name);
	}

	public EncFsFormatter()
	{
		_config.initNew(_context);
	}

	public void setDataCodecName(String name)
	{
		_dataCodecName = name;
	}

	public void setNameCodecName(String name)
	{
		_nameCodecName = name;
	}

	public final Config getConfig()
	{
		return _config;
	}

	protected String _dataCodecName, _nameCodecName;
	protected final Config _config = new Config();

	@Override
	protected EDSLocation createLocation(Location location) throws IOException, ApplicationException
	{
		Path targetPath = location.getCurrentPath();
		if(targetPath.isFile())
		{
			Path tmp = targetPath.getParentPath();
			if(tmp!=null)
			{
				targetPath = tmp;
				location.setCurrentPath(targetPath);
			}
		}
			/*
		if(!targetPath.isDirectory())
		{
			Path parentPath = targetPath.getParentPath();
			if(parentPath!=null)
			{
				String fn = PathUtil.getNameFromPath(targetPath);
				if(fn!=null)
					parentPath.getDirectory().createDirectory(fn);
			}
		}*/
		if(_dataCodecName != null)
			_config.setDataCodecInfo((DataCodecInfo) findInfoByName(_config, FS.getSupportedDataCodecs(), _dataCodecName));
		if(_nameCodecName != null)
			_config.setNameCodecInfo((NameCodecInfo) findInfoByName(_config, FS.getSupportedNameCodecs(), _nameCodecName));
		byte[] pd = _password == null ? new byte[0] : _password.getDataArray();
		try
		{
			return new EncFsLocation(
					location,
					new FS(targetPath, _config, pd),
					_context,
					UserSettings.getSettings(_context));
		}
		finally
		{
			SecureBuffer.eraseData(pd);
		}
	}
}
