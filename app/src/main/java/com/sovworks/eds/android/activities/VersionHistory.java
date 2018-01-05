package com.sovworks.eds.android.activities;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.settings.UserSettings;

import static com.sovworks.eds.android.settings.UserSettingsCommon.LAST_VIEWED_CHANGES;

public class VersionHistory extends Activity
{
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Util.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.changes_dialog);
		//setStyle(STYLE_NO_TITLE, R.style.Dialog);
		markAsRead();
		WebView vw = findViewById(R.id.changesWebView);
		vw.loadData(getString(R.string.changes_text), "text/html; charset=UTF-8", null);
		//vw.setBackgroundColor(0);
		//Spanned sp = Html.fromHtml( getString(R.string.promo_text));
		//((TextView)v.findViewById(R.id.promoTextView)).setText(sp);
		//tv.setText(sp);
		//((TextView)v.findViewById(R.id.promoTextView)).setText(Html.fromHtml(getString(R.string.promo_text)));
		findViewById(R.id.okButton).setOnClickListener(v -> finish());
	}

	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	private void markAsRead()
	{
		int vc = 135;
		//if(!GlobalConfig.isDebug())
		try 
		{            
            vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } 
		catch (Exception ignored)
		{}
		UserSettings s = UserSettings.getSettings(this);
		SharedPreferences.Editor edit = s.getSharedPreferences().edit();
		edit.putInt(LAST_VIEWED_CHANGES, vc);
		edit.commit();		
	}

}
