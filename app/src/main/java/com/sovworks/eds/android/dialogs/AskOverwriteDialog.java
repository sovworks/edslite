package com.sovworks.eds.android.dialogs;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.fs.util.PathUtil;
import com.sovworks.eds.fs.util.SrcDstCollection;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.fs.util.SrcDstPlain;
import com.sovworks.eds.locations.Location;
import com.trello.rxlifecycle2.android.FragmentEvent;
import com.trello.rxlifecycle2.components.RxDialogFragment;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CancellationException;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AskOverwriteDialog extends RxDialogFragment
{
	public static void showDialog(
			FragmentManager fm,
			boolean move,
			SrcDstCollection records)
	{

		Bundle args = new Bundle();
		args.putBoolean(ARG_MOVE, move);
		args.putParcelable(ARG_PATHS, records);
		showDialog(fm, args);
	}

	public static void showDialog(
			FragmentManager fm,
			Bundle args)
	{
		AskOverwriteDialog d = new AskOverwriteDialog();
		d.setArguments(args);
		d.show(fm, TAG);
	}

	public static final String TAG = "com.sovworks.eds.android.dialogs.AskOverwriteDialog";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Util.setDialogStyle(this);
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.ask_overwrite_dialog, container);
		_textView = v.findViewById(R.id.askOverwriteDialogText);
		v.findViewById(R.id.askOverwriteDialogSkipButton).setOnClickListener(arg0 -> skipRecord());
		v.findViewById(R.id.askOverwriteDialogOverwriteButton).setOnClickListener(arg0 -> overwriteRecord());
		((CheckBox)v.findViewById(R.id.applyToAllCheckBox)).setOnCheckedChangeListener((buttonView, isChecked) -> _applyToAll = isChecked);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		_selectedPaths = savedInstanceState == null ?
				new SrcDstPlain()
				:
				(SrcDstPlain) savedInstanceState.getParcelable(ARG_SELECTED_PATHS);
		SrcDstCollection paths = getArguments().getParcelable(ARG_PATHS);
		if(paths == null)
			paths = new SrcDstPlain();
		_applyToAll = savedInstanceState != null && savedInstanceState.getBoolean(ARG_APPLY_TO_ALL);
		_numProc = savedInstanceState == null ? 0 : savedInstanceState.getInt(ARG_NUM_PROC);
		_pathsIter = paths.iterator();
		for(int i=0;i<_numProc;i++)
			_next = _pathsIter.next();

		lifecycle().
				filter(event -> event == FragmentEvent.RESUME).
				firstElement().
				subscribe(res -> askNextRecord(), err ->
				{
					if(!(err instanceof CancellationException))
						Logger.log(err);
				});
	}

	@Override
	public void onSaveInstanceState (@NonNull Bundle outState)
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
			Logger.showAndLog(getActivity(), e);
		}	
	}

	public static final String ARG_MOVE = "move";
	public static final String ARG_PATHS = "paths";
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
				if(getArguments().getBoolean(ARG_MOVE, false))
					FileOpsService.moveFiles(getActivity(), _selectedPaths, true);
				else
					FileOpsService.copyFiles(getActivity(), _selectedPaths, true);
							
				dismiss();
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
			Logger.showAndLog(getActivity(), e);
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

	private static class Names
	{

		String srcName, dstName;
	}

	private Disposable _observer;

	private synchronized void cancelLoadTask()
	{
		if(_observer != null)
		{
			_observer.dispose();
			_observer = null;
		}
	}

	private synchronized void loadFileName(Location srcLoc, Location dstLoc)
	{
		Context context = getActivity().getApplicationContext();
		_observer = Single.<Names>create(emitter -> {
			Names res = new Names();
			res.srcName = PathUtil.getNameFromPath(srcLoc.getCurrentPath());
			res.dstName = dstLoc.getCurrentPath().getPathDesc();
			emitter.onSuccess(res);
		}).
				subscribeOn(Schedulers.io()).
				observeOn(AndroidSchedulers.mainThread()).
				compose(bindToLifecycle()).
				subscribe(res -> setText(res.srcName, res.dstName), err -> {
					if(!(err instanceof CancellationException))
						Logger.showAndLog(context, err);
				});
	}
}
