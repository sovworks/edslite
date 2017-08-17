package com.sovworks.eds.android.service;

public interface ServiceTaskState {

	public boolean isCancelled();

	public void updateUI();

	public void setResult(Object result);

	public Object getResult();

	public Object getParam();

	public int getTaskId();

}