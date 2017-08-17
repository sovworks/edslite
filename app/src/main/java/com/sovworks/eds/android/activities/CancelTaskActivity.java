package com.sovworks.eds.android.activities;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.service.FileOpsService;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class CancelTaskActivity extends Activity
{
	public static final String ACTION_CANCEL_TASK = "com.sovworks.eds.android.CANCEL_TASK";

	public static Intent getCancelTaskIntent(Context context,int taskId)
	{
		Intent i = new Intent(context,CancelTaskActivity.class);
		i.setAction(ACTION_CANCEL_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(FileOpsService.INTENT_PARAM_TASK_ID, taskId);
		return i;
	}
	
	public static PendingIntent getCancelTaskPendingIntent(Context context,int taskId)
	{
		return PendingIntent.getActivity(context, taskId, getCancelTaskIntent(context, taskId), PendingIntent.FLAG_UPDATE_CURRENT);
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Util.setDialogStyle(this);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.cancel_task_activity);
	}
		
	public void onYesClick(View v)
	{		
		FileOpsService.cancelTask(this);		
	}
	
	public void onNoClick(View v)
	{
		finish();
	}

}
