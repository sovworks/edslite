package com.sovworks.eds.android.service;

import android.content.Context;
import android.content.Intent;

public interface Task
{
	Object doWork(Context context, Intent i) throws Throwable;
	void onCompleted(Result result);
	void cancel();
}
