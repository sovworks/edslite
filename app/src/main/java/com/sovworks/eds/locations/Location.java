package com.sovworks.eds.locations;

import android.content.Intent;
import android.net.Uri;

import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.fs.FileSystem;
import com.sovworks.eds.fs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public interface Location extends Cloneable
{
	interface ProtectionKeyProvider
	{
		SecureBuffer getProtectionKey();
	}
	interface ExternalSettings
	{
		void setProtectionKeyProvider(ProtectionKeyProvider p);
		String getTitle();
		void setTitle(String title);
		boolean isVisibleToUser();
		void setVisibleToUser(boolean val);
		void saveToJSONObject(JSONObject jo) throws JSONException;
		void loadFromJSONOjbect(JSONObject jo) throws JSONException;
		boolean useExtFileManager();
		void setUseExtFileManager(boolean val);
	}

	class DefaultExternalSettings implements ExternalSettings
	{
		@Override
		public void setProtectionKeyProvider(ProtectionKeyProvider p)
		{

		}

		@Override
		public String getTitle()
		{
			return "";
		}

		@Override
		public void setTitle(String title)
		{

		}

		@Override
		public boolean isVisibleToUser()
		{
			return _isVisibleToUser;
		}

		@Override
		public void setVisibleToUser(boolean val)
		{
			_isVisibleToUser = val;
		}

		@Override
		public void saveToJSONObject(JSONObject jo) throws JSONException
		{

		}

		@Override
		public void loadFromJSONOjbect(JSONObject jo) throws JSONException
		{

		}

		@Override
		public boolean useExtFileManager()
		{
			return false;
		}

		@Override
		public void setUseExtFileManager(boolean val)
		{

		}

		private boolean _isVisibleToUser;
	}

	String getTitle();
	String getId();
	FileSystem getFS() throws IOException;
	Path getCurrentPath() throws IOException;
	void setCurrentPath(Path path);
	Uri getLocationUri();
	void loadFromUri(Uri uri);
	Location copy();
	//void initFileSystem() throws Exception;
	void closeFileSystem(boolean force) throws IOException;
	boolean isFileSystemOpen();
	boolean isReadOnly();
	boolean isEncrypted();
	boolean isDirectlyAccessible();
	Uri getDeviceAccessibleUri(Path path);
	ExternalSettings getExternalSettings();
	void saveExternalSettings();
	Intent getExternalFileManagerLaunchIntent();
}
