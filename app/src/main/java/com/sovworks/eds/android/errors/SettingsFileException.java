package com.sovworks.eds.android.errors;

import android.content.Context;

import com.sovworks.eds.android.R;

public class SettingsFileException extends UserException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SettingsFileException(Context context)
	{
		super(context,R.string.err_failed_processing_settings_file);
	}

}
