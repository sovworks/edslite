package com.sovworks.eds.android.helpers;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.TypedValue;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.settings.SettingsCommon;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UtilBase
{
	public static void setDialogStyle(DialogFragment df)
	{
		TypedValue typedValue = new TypedValue();
		df.getActivity().getTheme().resolveAttribute(R.attr.dialogStyle, typedValue, true);
		df.setStyle(DialogFragment.STYLE_NO_TITLE, typedValue.resourceId);
	}

	public static void setDialogStyle(Activity act)
	{
		int theme = UserSettings.getSettings(act.getApplicationContext()).getCurrentTheme();
		act.setTheme(theme == SettingsCommon.THEME_DARK ? R.style.Dialog_Dark : R.style.Dialog);
	}

	public static String getSystemInfoString()
	{
		//noinspection deprecation
		return String.format("Build.BOARD: %s\n", Build.BOARD) +
				String.format("Build.BOOTLOADER: %s\n", Build.BOOTLOADER) +
				String.format("Build.BRAND: %s\n", Build.BRAND) +
				String.format("Build.CPU_ABI: %s\n", Build.CPU_ABI) +
				String.format("Build.CPU_ABI2: %s\n", Build.CPU_ABI2) +
				String.format("Build.DEVICE: %s\n", Build.DEVICE) +
				String.format("Build.DISPLAY: %s\n", Build.DISPLAY) +
				String.format("Build.HARDWARE: %s\n", Build.HARDWARE) +
				String.format("Build.ID: %s\n", Build.ID) +
				String.format("Build.MODEL: %s\n", Build.MODEL) +
				String.format("Build.MANUFACTURER: %s\n", Build.MANUFACTURER) +
				String.format("Build.PRODUCT: %s\n", Build.PRODUCT) +
				String.format("Build.TAGS: %s\n", Build.TAGS) +
				String.format("Build.TYPE: %s\n", Build.TYPE) +
				String.format("Build.VERSION.RELEASE: %s\n", Build.VERSION.RELEASE) +
				String.format("os.name: %s\n", System.getProperty("os.name")) +
				String.format("os.arch: %s\n", System.getProperty("os.arch")) +
				String.format("os.version: %s\n", System.getProperty("os.version"));
	}

	public static String storeElementsToString(Collection<?> elements)
	{
		JSONArray ja = new JSONArray(elements);
		return ja.toString();
    }

	public static List<String> loadStringArrayFromString(String s) throws JSONException
	{
		ArrayList<String> res = new ArrayList<>();
		if(s!=null && s.length()>0)
		{
			JSONArray ja = new JSONArray(s);
			for(int i=0;i<ja.length();i++)
				res.add(ja.getString(i));
		}
		return res;
	}

  public static Bitmap loadDownsampledImage(Path path,int reqWidth,int reqHeight) throws IOException
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BufferedInputStream data = new BufferedInputStream(path.getFile().getInputStream());
		try
		{
			BitmapFactory.decodeStream(data, null, options);
		}
		finally
		{
			data.close();
		}
		// Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    data = new BufferedInputStream(path.getFile().getInputStream());
		try
		{
			return BitmapFactory.decodeStream(data, null, options);
		}
		finally
		{
			data.close();
		}
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;

	    if (height > reqHeight || width > reqWidth)
	    {
	        inSampleSize = Math.max(
		        				Math.round((float)height / (float)reqHeight),
		        				Math.round((float)width / (float)reqWidth)
	        				);
	    }
	    return inSampleSize;
	}
}

