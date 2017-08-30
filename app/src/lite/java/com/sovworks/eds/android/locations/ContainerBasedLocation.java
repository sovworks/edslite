package com.sovworks.eds.android.locations;

import android.content.Context;
import android.net.Uri;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.errors.WrongPasswordOrBadContainerException;
import com.sovworks.eds.android.helpers.ContainerOpeningProgressReporter;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.container.EdsContainer;
import com.sovworks.eds.container.VolumeLayout;
import com.sovworks.eds.container.VolumeLayoutBase;
import com.sovworks.eds.crypto.FileEncryptionEngine;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.crypto.SimpleCrypto;
import com.sovworks.eds.exceptions.WrongFileFormatException;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.locations.ContainerLocation;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;
import com.sovworks.eds.settings.Settings;
import com.sovworks.eds.settings.SettingsCommon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ContainerBasedLocation extends EDSLocationBase implements ContainerLocation
{
	public static final String URI_SCHEME = "eds-container";

	public static String getLocationId(LocationsManagerBase lm, Uri locationUri) throws Exception
	{
		Location containerLocation = getContainerLocationFromUri(locationUri, lm);
		return getLocationId(containerLocation);
	}

	public static String getLocationId(Location containerLocation)
	{
		return SimpleCrypto.calcStringMD5(containerLocation.getLocationUri().toString());
	}

    public static class ExternalSettings extends EDSLocationBase.ExternalSettings implements ContainerLocation.ExternalSettings
    {
        public ExternalSettings()
        {

        }

		@Override
        public void setContainerFormatName(String containerFormatName)
        {
            _containerFormatName = containerFormatName;
        }

		@Override
        public void setEncEngineName(String encEngineName)
        {
            _encEngineName = encEngineName;
        }

		@Override
        public void setHashFuncName(String hashFuncName)
        {
            _hashFuncName = hashFuncName;
        }

		@Override
        public String getContainerFormatName()
        {
            return _containerFormatName;
        }

		@Override
        public String getEncEngineName()
        {
            return _encEngineName;
        }

		@Override
        public String getHashFuncName()
        {
            return _hashFuncName;
        }

		@Override
        public void saveToJSONObject(JSONObject jo) throws JSONException
        {
            super.saveToJSONObject(jo);
            jo.put(SETTINGS_CONTAINER_FORMAT, _containerFormatName);
            jo.put(SETTINGS_ENC_ENGINE, _encEngineName);
            jo.put(SETTINGS_HASH_FUNC, _hashFuncName);
        }

        @Override
        public void loadFromJSONOjbect(JSONObject jo) throws JSONException
        {
            super.loadFromJSONOjbect(jo);
            _containerFormatName = jo.optString(SETTINGS_CONTAINER_FORMAT, null);
            _encEngineName = jo.optString(SETTINGS_ENC_ENGINE, null);
            _hashFuncName = jo.optString(SETTINGS_HASH_FUNC, null);
        }

        private static final String SETTINGS_CONTAINER_FORMAT = "container_format";
        private static final String SETTINGS_ENC_ENGINE = "encryption_engine";
        private static final String SETTINGS_HASH_FUNC = "hash_func";

        private String _containerFormatName, _hashFuncName, _encEngineName;
    }

    public ContainerBasedLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        this(getContainerLocationFromUri(uri, lm), null, context, settings);
		loadFromUri(uri);
    }

	public ContainerBasedLocation(ContainerBasedLocation sibling)
	{
		super(sibling);
	}
	
	public ContainerBasedLocation(Location containerLocation, Context context) throws IOException
	{
		this(containerLocation, null, context, UserSettings.getSettings(context));
	}
	
	public ContainerBasedLocation(Location containerLocation, EdsContainer cont, Context context, Settings settings)
	{
		super(settings, new SharedData(
				getLocationId(containerLocation),
				createInternalSettings(),
				containerLocation,
				context
		));
		getSharedData().container = cont;
	}

	@Override
	public void loadFromUri(Uri uri)
	{
		super.loadFromUri(uri);
		_currentPathString = uri.getPath();
	}

	@Override
	public void open() throws Exception
	{
		if(isOpenOrMounted())
			return;
		EdsContainer cnt = getEdsContainer();
		cnt.setContainerFormat(null);
		cnt.setEncryptionEngineHint(null);
		cnt.setHashFuncHint(null);
		cnt.setNumKDFIterations(0);
		if(_openingProgressReporter!=null)
			cnt.setProgressReporter((ContainerOpeningProgressReporter) _openingProgressReporter);
		ContainerFormatInfo cfi = getContainerFormatInfo();
		if(cfi != null)
		{
			cnt.setContainerFormat(cfi);
			VolumeLayout vl = cfi.getVolumeLayout();
			String name = getExternalSettings().getEncEngineName();
			if(name != null && !name.isEmpty())
				cnt.setEncryptionEngineHint((FileEncryptionEngine) VolumeLayoutBase.findEncEngineByName(vl.getSupportedEncryptionEngines(), name));

			name = getExternalSettings().getHashFuncName();
			if(name != null && !name.isEmpty())
				cnt.setHashFuncHint(VolumeLayoutBase.findHashFunc(vl.getSupportedHashFuncs(), name));
		}

		int numKDFIterations = getSelectedKDFIterations();
		if(numKDFIterations > 0)
			cnt.setNumKDFIterations(numKDFIterations);

		byte[] pass = getFinalPassword();
		try
		{
			cnt.open(pass);
		}
		catch(WrongFileFormatException e)
		{
			getSharedData().container = null;
			throw new WrongPasswordOrBadContainerException(getContext());
		}
		catch(Exception e)
		{
			getSharedData().container = null;
			throw e;
		}
		finally
		{
			if(pass!=null)
				Arrays.fill(pass, (byte) 0);
		}
	}

	@Override
	public Uri getLocationUri()
	{
		return makeUri(URI_SCHEME).build();
	}

    @Override
    public ExternalSettings getExternalSettings()
    {
        return (ExternalSettings)super.getExternalSettings();
    }

	@Override
	public boolean hasCustomKDFIterations()
	{
		ContainerFormatInfo cfi = getContainerFormatInfo();
		return cfi == null || cfi.hasCustomKDFIterationsSupport();
	}

	@Override
	public void close(boolean force) throws IOException
	{
		com.sovworks.eds.android.Logger.debug("Closing container at " + getLocation().getLocationUri());
		super.close(force);
		if(isOpen())
		{
			try
			{
				getSharedData().container.close();
			}
			catch(Throwable e)
			{
				if(!force)
					throw new IOException(e);
				else
					Logger.log(e);
			}
			getSharedData().container = null;
		}
		com.sovworks.eds.android.Logger.debug("Container has been closed");
	}

	@Override
	public boolean isOpen()
	{
		return getSharedData().container!=null && getSharedData().container.getVolumeLayout()!=null;
	}

	@Override
	public ContainerBasedLocation copy()
	{
		return new ContainerBasedLocation(this);
	}

	@Override
	public synchronized EdsContainer getEdsContainer() throws IOException
	{
		EdsContainer cnt = getSharedData().container;
		if(cnt == null)
		{
			cnt = initEdsContainer();
			getSharedData().container = cnt;
		}
		return cnt;
	}

	@Override
	public List<ContainerFormatInfo> getSupportedFormats()
	{
		return EdsContainer.getSupportedFormats();
	}

	protected static class SharedData extends EDSLocationBase.SharedData
	{
		public SharedData(String id, EDSLocationBase.InternalSettings settings, Location location, Context context)
		{
			super(id, settings, location, context);
		}

		public EdsContainer container;
	}

	public static final int MAX_PASSWORD_LENGTH = 64;

	@Override
	protected SharedData getSharedData()
	{
		return (SharedData)super.getSharedData();
	}

	protected EdsContainer initEdsContainer() throws IOException
	{
		return new EdsContainer(getLocation().getCurrentPath());
	}

	protected ContainerFormatInfo getContainerFormatInfo()
	{
		String name = getExternalSettings().getContainerFormatName();
		return name != null ? EdsContainer.findFormatByName(name) : null;
	}

	@Override
	protected byte[] getSelectedPassword()
	{
		byte[] pass = super.getSelectedPassword();
		if(pass!=null && pass.length>MAX_PASSWORD_LENGTH)
		{
			byte[] tmp = pass;
			pass = new byte[MAX_PASSWORD_LENGTH];
			System.arraycopy(tmp, 0, pass, 0, MAX_PASSWORD_LENGTH);
			SecureBuffer.eraseData(tmp);
		}
		return pass;
	}

    @Override
    protected ExternalSettings loadExternalSettings()
    {
        ExternalSettings res = new ExternalSettings();
		res.setProtectionKeyProvider(new ProtectionKeyProvider()
		{
			@Override
			public SecureBuffer getProtectionKey()
			{
				try
				{
					return UserSettings.getSettings(getContext()).getSettingsProtectionKey();
				}
				catch (SettingsCommon.InvalidSettingsPassword invalidSettingsPassword)
				{
					return null;
				}
			}
		});
        res.load(_globalSettings,getId());
        return res;
    }

	@Override
	protected FileSystem createBaseFS(boolean readOnly) throws IOException, UserException
	{
		return getSharedData().container.getEncryptedFS(readOnly);
	}
}
