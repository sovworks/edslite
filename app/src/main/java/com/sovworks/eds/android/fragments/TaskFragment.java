package com.sovworks.eds.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.ProgressReporter;
import com.sovworks.eds.settings.GlobalConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

public abstract class TaskFragment extends Fragment
{
	public static final String ARG_HOST_FRAGMENT = "com.sovworks.eds.android.HOST_FRAGMENT_TAG";

	public static synchronized void addEventListener(EventListener listener)
	{
		if(GlobalConfig.isDebug())
			_eventListeners.add(new WeakReference<>(listener));
	}


	public enum EventType
	{
		Added,
		Removed
	}

	public static synchronized void onEvent(EventType eventType, TaskFragment tf)
	{
		if(GlobalConfig.isDebug())
		{
			for (WeakReference<EventListener> wr : _eventListeners)
			{
				EventListener el = wr.get();
				if (el != null)
					el.onEvent(eventType, tf);
			}
		}
	}

	private static List<WeakReference<EventListener>> _eventListeners;
	static
	{
		if(GlobalConfig.isDebug())
		{
			_eventListeners = new ArrayList<>();
		}
	}

	public interface EventListener
	{
		void onEvent(EventType eventType, TaskFragment tf);
	}

	public interface TaskCallbacks
	{
		void onPrepare(Bundle args);

		void onUpdateUI(Object state);
		
		void onResumeUI(Bundle args);
		
		void onSuspendUI(Bundle args);

		void onCompleted(Bundle args,Result result);	
	}	
	
	public interface CallbacksProvider
	{
		TaskCallbacks getCallbacks(String fragmentTag);
	}
	
	public static class Result
	{
		public Result()
		{
			this(null);
		}
		
		public Result(Object result)
		{
			_result = result;
			_error = null;
			_isCancelled = false;
		}
		
		public Result(Throwable error,boolean isCancelled)
		{
			_result = null;
			_error = error;
			_isCancelled = isCancelled;
		}
	
		public Throwable getError()
		{
			return _error;
		}
		
		public boolean isCancelled()
		{
			return _isCancelled;
		}
		
		public Object getResult() throws Throwable		 
		{
			if(_error!=null)
				throw _error;
			return _result;
		}
		
		private final boolean _isCancelled;
		private final Throwable _error;
		private final Object _result;
	}
	
	/**
	 * Hold a reference to the parent Activity so we can report the task's
	 * current progress and results. The Android framework will pass us a
	 * reference to the newly created Activity after each configuration change.
	 */
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if(_callbacks == null)
			initCallbacks();
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		if(_callbacks == null)
			initCallbacks();	
	}

	/**
	 * This method will only be called once when the retained Fragment is first
	 * created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		onEvent(EventType.Added, this);
		super.onCreate(savedInstanceState);
		// Retain this fragment across configuration changes.
		setRetainInstance(true);
		Logger.debug(String.format("TaskFragment %s has been created. Args=%s", TaskFragment.this,getArguments()));
        initTask(getActivity());
		_task.execute();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(_callbacks!=null)		
			_callbacks.onResumeUI(getArguments());		
	}
	
	@Override
	public void onPause ()
	{
		if(_callbacks!=null)
			_callbacks.onSuspendUI(getArguments());
		super.onPause();
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		_callbacks = null;
	}
	
	public synchronized void cancel()
	{
		_task.cancel(false);
	}	
	
	public interface TaskState
	{
		boolean isTaskCancelled();
		void updateUI(Object state);
		void setResult(Object result);
	}

	public static class TaskStateProgressReporter implements ProgressReporter
	{
		public TaskStateProgressReporter(TaskState ts)
		{
			_ts = ts;
		}

		private final TaskState _ts;

		@Override
		public void setText(CharSequence text)
		{
			_ts.updateUI(text);
		}

		@Override
		public void setProgress(int progress)
		{
			_ts.updateUI(progress);
		}

		@Override
		public boolean isCancelled()
		{
			return _ts.isTaskCancelled();
		}
	}
	
	protected TaskCallbacks getTaskCallbacks(Activity activity)
	{
        if(activity instanceof CallbacksProvider)
            return ((CallbacksProvider)activity).getCallbacks(getTag());
        if(activity instanceof TaskCallbacks)
		    return (TaskCallbacks) activity;
        return null;
	}

    protected void initTask(Activity activity)
    {

    }

	protected abstract void doWork(TaskState state) throws Throwable;

	protected void detachTask()
	{
		FragmentManager fm = getFragmentManager();
		if(fm!=null)
		{
			fm.beginTransaction().remove(TaskFragment.this).commitAllowingStateLoss();
			Logger.debug(String.format("TaskFragment %s has been removed from the fragment manager", this));
			onEvent(EventType.Removed, this);
		}
	}

	private TaskCallbacks _callbacks;

	private void initCallbacks()
	{
		_callbacks = getTaskCallbacks(getActivity());
		if (_callbacks != null)
		{
			if(_task.getStatus() != Status.FINISHED)
				_callbacks.onPrepare(getArguments());
			else
				try
				{
					_callbacks.onCompleted(getArguments(), _task.get());
				}
				catch (Exception ignored)
				{
				}
		}
	}

	private final AsyncTask<Void, Object, Result> _task = new AsyncTask<Void, Object, Result>()
	{
		@Override
		protected Result doInBackground(Void... ignore)
		{
			Logger.debug(String.format("TaskFragment %s: background job started", TaskFragment.this));
			try
			{
				TaskStateImpl ts = new TaskStateImpl();
				doWork(ts);
				return new Result(ts.workResult);
			}
			catch(Throwable e)
			{
				return new Result(e, false);
			}
		}

		@Override
		protected void onProgressUpdate(Object... state)
		{
			if (_callbacks != null)
				_callbacks.onUpdateUI(state[0]);

		}

		@Override
		protected void onCancelled()
		{
			Logger.debug(String.format("TaskFragment %s has been cancelled", TaskFragment.this));
			try
			{
				if (_callbacks != null)
                {
                    _callbacks.onSuspendUI(getArguments());
                    _callbacks.onCompleted(getArguments(), new Result(new CancellationException(), true));
                }
			}
            catch(Throwable e)
            {
                Logger.showAndLog(getActivity(), e);
            }
			finally
			{
				detachTask();
			}
		}

		@Override
		protected void onPostExecute(Result result)
		{
			Logger.debug(String.format("TaskFragment %s completed", TaskFragment.this));
			try
			{
				if (_callbacks != null)
				{
					_callbacks.onSuspendUI(getArguments());
					_callbacks.onCompleted(getArguments(),result);
				}
			}
            catch(Throwable e)
            {
                Logger.showAndLog(getActivity(), e);
            }
			finally
			{
				detachTask();
			}
		}

		class TaskStateImpl implements TaskState
		{
			public Object workResult;

			@Override
			public void updateUI(Object state)
			{
				if(!isTaskCancelled())
				publishProgress(state);
			}

			@Override
			public boolean isTaskCancelled()
			{
				return isCancelled();
			}

			@Override
			public void setResult(Object result)
			{
				workResult = result;
			}
		}


	};

}
