package com.sovworks.eds.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.fragments.TaskFragment;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.fs.util.SrcDstPlain;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;

import org.json.JSONException;

import java.io.IOException;
import java.util.Iterator;

public class AskOverwriteActivity extends Activity
{

	public static class LoadFileNameTask extends TaskFragment
	{
		public static final String TAG = "LoadFileNameTask";

		public static LoadFileNameTask newInstance(Location srcLoc, Location dstLoc)
		{
			Bundle args = new Bundle();
			LocationsManager.storePathsInBundle(args, srcLoc, null);
			args.putParcelable(ARG_DST_LOC, dstLoc.getLocationUri());
			LoadFileNameTask task = new LoadFileNameTask();
			task.setArguments(args);
			return task;
		}

		private static final String ARG_DST_LOC = "com.sovworks.eds.android.DST_LOCATION_URI";

		private static class Names
		{
			String srcName, dstName;
		}

		@Override
		protected void initTask(Activity activity)
		{
			super.initTask(activity);
			_context = activity.getApplicationContext();
		}

		private Context _context;

		@Override
		protected void doWork(TaskState state) throws Throwable
		{
			LocationsManager lm = LocationsManagerBase.getLocationsManager(_context);
			Location srcLoc = lm.getFromBundle(getArguments(), null);
			Location dstLoc = lm.getLocation((Uri) getArguments().getParcelable(ARG_DST_LOC));
			Names res = new Names();
			res.srcName = PathUtil.getNameFromPath(srcLoc.getCurrentPath());
			res.dstName = dstLoc.getCurrentPath().getPathDesc();
			state.setResult(res);
		}

		@Override
		protected TaskCallbacks getTaskCallbacks(final Activity activity)
		{
			return new TaskCallbacks()
			{
				@Override
				public void onPrepare(Bundle args)
				{

				}

				@Override
				public void onUpdateUI(Object state)
				{

				}

				@Override
				public void onResumeUI(Bundle args)
				{

				}

				@Override
				public void onSuspendUI(Bundle args)
				{

				}

				@Override
				public void onCompleted(Bundle args, Result result)
				{
					try
					{
						if(!result.isCancelled())
						{
							AskOverwriteActivity act = (AskOverwriteActivity)activity;
							if(act._next != null)
							{
								Names names = (Names) result.getResult();
								act.setText(names.srcName, names.dstName);
							}
						}

					}
					catch (Throwable e)
					{
						Logger.showAndLog(activity, e);
					}
				}
			};
		}
	}
	
	public static Intent getOverwriteActivityIntent(
			Context context,
			boolean move,
			SrcDstCollection records) throws IOException, JSONException
	{
		Intent i = new Intent(context,AskOverwriteActivity.class);		
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
		i.putExtra(ARG_MOVE, move);		
		i.putExtra(ARG_PATHS, records);
		return i;
	}
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		Util.setDialogStyle(this);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ask_overwrite_activity);
		
		_textView = ((TextView)findViewById(R.id.askOverwriteDialogText));
		findViewById(R.id.askOverwriteDialogSkipButton).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                skipRecord();
            }
        });
		findViewById(R.id.askOverwriteDialogOverwriteButton).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View arg0)
            {
                overwriteRecord();
            }
        });
	
		((CheckBox)findViewById(R.id.applyToAllCheckBox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
			{
				_applyToAll = isChecked;				
			}
		});	
		
		_selectedPaths = savedInstanceState == null ?
				new SrcDstPlain()
			:
				(SrcDstPlain) savedInstanceState.getParcelable(ARG_SELECTED_PATHS);
		SrcDstCollection paths = getIntent().getParcelableExtra(ARG_PATHS);
		_applyToAll = savedInstanceState != null && savedInstanceState.getBoolean(ARG_APPLY_TO_ALL);
		_numProc = savedInstanceState == null ? 0 : savedInstanceState.getInt(ARG_NUM_PROC);
		_pathsIter = paths.iterator();
		for(int i=0;i<_numProc;i++)
			_next = _pathsIter.next();
		askNextRecord();	
	}
	
	@Override
	protected void onSaveInstanceState (@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);		
		try
		{
			outState.putParcelable(ARG_SELECTED_PATHS, _selectedPaths);
			outState.putBoolean(ARG_APPLY_TO_ALL, _applyToAll);
			outState.putInt(ARG_NUM_PROC, _numProc);
		}
		catch (Exception e)
		{
			Logger.showAndLog(this, e);
		}	
	}
	
	private static final String ARG_MOVE = "move";
	private static final String ARG_PATHS = "paths";	
	private static final String ARG_SELECTED_PATHS = "selected_paths";
	private static final String ARG_APPLY_TO_ALL = "apply_to_all";
	private static final String ARG_NUM_PROC = "num_proc";
	
	private SrcDstPlain _selectedPaths;
	private Iterator<SrcDst> _pathsIter;
	private int _numProc;	
	private boolean _applyToAll;
	private TextView _textView;	
	private SrcDst _next;
	
	private void overwriteRecord()
	{
		_selectedPaths.add(_next);		
		while(_applyToAll && _pathsIter.hasNext())
		{			
			_selectedPaths.add(_pathsIter.next());
			_numProc++;			
		}	
		askNextRecord();
	}
	
	private void skipRecord()
	{		
		while(_applyToAll && _pathsIter.hasNext())
        {
            _pathsIter.next();
            _numProc++;
        }
		askNextRecord();		
	}
	
	private void askNextRecord()
	{	
		try
		{			
			if(!_pathsIter.hasNext())
			{
				if(getIntent().getBooleanExtra(ARG_MOVE, false))
					FileOpsService.moveFiles(this, _selectedPaths, true);
				else
					FileOpsService.copyFiles(this, _selectedPaths, true);			
							
				finish();
            }
			else
			{
				cancelLoadTask();
				_next = _pathsIter.next();
				_numProc++;
				loadFileName(_next.getSrcLocation(), _next.getDstLocation());
			}
		}
		catch (IOException e)
		{
			Logger.showAndLog(this, e);
		}	
	}

	private void setText(String srcName, String dstName)
	{
		_textView.setText(getString(
				R.string.file_already_exists,
				srcName,
				dstName)
		);
	}

	private void cancelLoadTask()
	{
		LoadFileNameTask task = (LoadFileNameTask) getFragmentManager().findFragmentByTag(LoadFileNameTask.TAG);
		if(task!=null)
			task.cancel();
	}
	private void loadFileName(Location srcLoc, Location dstLoc)
	{
		getFragmentManager().
				beginTransaction().
				add(LoadFileNameTask.newInstance(srcLoc, dstLoc), LoadFileNameTask.TAG).
				commit();
	}
}
