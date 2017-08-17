package com.sovworks.eds.android.filemanager.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.filemanager.FileManagerFragment;
import com.sovworks.eds.android.helpers.CachedPathInfo;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.views.GestureImageView;
import com.sovworks.eds.android.views.GestureImageView.NavigListener;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.sovworks.eds.android.settings.UserSettingsCommon.IMAGE_VIEWER_AUTO_ZOOM_ENABLED;

public class PreviewFragment extends Fragment implements FileManagerFragment
{
	public interface Host
	{
		List<? extends CachedPathInfo> getCurrentFiles();
		Location getLocation();
	}
	
	public static final String TAG = "PreviewFragment";

    public static final String STATE_CURRENT_PATH = "com.sovworks.eds.android.CURRENT_PATH";
	
	public static PreviewFragment newInstance(Path currentImagePath)
	{
		Bundle b = new Bundle();
        if(currentImagePath!=null)
		    b.putString(STATE_CURRENT_PATH,currentImagePath.getPathString());
		PreviewFragment pf = new PreviewFragment();
		pf.setArguments(b);
		return pf;	
	}
	
	public static PreviewFragment newInstance(String currentImagePathString)
	{
		Bundle b = new Bundle();
		b.putString(STATE_CURRENT_PATH,currentImagePathString);
		PreviewFragment pf = new PreviewFragment();
		pf.setArguments(b);
		return pf;	
	}
	
	public static Bitmap loadDownsampledImage(Path path,int sampleSize) throws IOException
	{
		BitmapFactory.Options options = new BitmapFactory.Options();			
	    options.inSampleSize = sampleSize;
	    InputStream data = path.getFile().getInputStream();
		try
		{
			return BitmapFactory.decodeStream(data, null, options);
		}
		finally
		{
			data.close();
		}
	}
	
	public static BitmapFactory.Options loadImageParams(Path path) throws IOException
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		InputStream data = path.getFile().getInputStream();
		try
		{
			BitmapFactory.decodeStream(data, null, options);
		}
		finally
		{
			data.close();
		}
		return options;
	}
	
	public static int calcSampleSize(Rect viewRect,Rect regionRect)
	{
	    int inSampleSize = 1;

	    if (regionRect.height() > viewRect.height() || regionRect.width() > viewRect.width()) 
	    { 
	        inSampleSize = Math.max(
		        				Math.round((float)regionRect.height() / (float)viewRect.height()),
		        				Math.round((float)regionRect.width() / (float)viewRect.width())
	        				);
	        if(inSampleSize>1)
	        {
		        for(int i=1;i<10;i++)
		        {
		        	if(Math.pow(2, i)>inSampleSize)
		        	{
		        		inSampleSize = (int) Math.pow(2, i-1);
		        		break;
		        	}
		        }
	        }
	    }
	    return inSampleSize;
		
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
        initParams(savedInstanceState != null && savedInstanceState.containsKey(STATE_CURRENT_PATH) ?
                   savedInstanceState
                                                                                                    :
                   getArguments()
        );

		setHasOptionsMenu(true);
	}

	@Override
	public boolean onBackPressed()
	{
		return false;
	}
		
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) 
	{
		View view = inflater.inflate(R.layout.preview_fragment, container, false);
		_mainImageView = (GestureImageView)view.findViewById(R.id.imageView);
		_mainImageView.setAutoZoom(UserSettings.getSettings(getActivity()).isImageViewerAutoZoomEnabled());
		_mainImageView.setNavigListener(new NavigListener()
		{			
			@Override
			public void onPrev()
			{
				moveLeft();
				
			}
			
			@Override
			public void onNext()
			{
				moveRight();
				
			}
		});
		_mainImageView.setOnLoadOptimImageListener(new GestureImageView.OptimImageRequiredListener()
		{			
			@Override
			public void onOptimImageRequired(Rect srcImageRect)
			{
				if(_isOptimSupported)
					loadImage(_currentImagePath, srcImageRect);				
			}
		});
        _mainImageView.setOnSizeChangedListener(new Runnable()
        {
            @Override
            public void run()
            {
                _mainImageView.getViewRect().round(_viewRect);
                startLoad();
            }
        });
		_viewSwitcher = (ViewSwitcher)view.findViewById(R.id.viewSwitcher);
		return view;
	}


	@Override
	public void onDestroyView()
	{		
		cancelTask();
		_mainImageView.clearImage();
		_mainImageView = null;
		super.onDestroyView();		
	}	
	
	@Override
	public void onSaveInstanceState (Bundle outState)
	{	
		super.onSaveInstanceState(outState);
		if(_currentImagePath!=null)					
			outState.putString(STATE_CURRENT_PATH, _currentImagePath.getPathString());		
	}
	
	@Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater menuInflater)
	{		
		menuInflater.inflate(R.menu.image_viewer_menu, menu);
		super.onCreateOptionsMenu(menu, menuInflater);
	}
	
	public void onPrepareOptionsMenu (Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		MenuItem mi = menu.findItem(R.id.prevMenuItem);
		mi.setEnabled(getPrevImagePath()!=null);
		mi = menu.findItem(R.id.nextMenuItem);
		mi.setEnabled(getNextImagePath()!=null);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{
		try
		{
			switch (menuItem.getItemId())
			{
			case R.id.prevMenuItem:
				moveLeft();
				return true;
			case R.id.nextMenuItem:
				moveRight();
				return true;		
			case R.id.zoomInMenuItem:
				_mainImageView.zoomIn();
				return true;
			case R.id.zoomOutMenuItem:
				_mainImageView.zoomOut();
				return true;
			case R.id.rotateLeftMenuItem:
				_mainImageView.rotateLeft();
				return true;
			case R.id.rotateRightMenuItem:
				_mainImageView.rotateRight();
				return true;
			case R.id.toggleAutoZoom:
				toggleAutoZoom();
				return true;
			}
		}
		catch (Exception e)
		{
			Logger.showAndLog(getActivity(), e);
		}
		return super.onOptionsItemSelected(menuItem);
	}

    /*
	public float convertPixelsToDp(float px)
	{		    
	    return  px / (getResources().getDisplayMetrics().densityDpi / 160f);	    
	    //return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, getResources().getDisplayMetrics());
	}
	*/

	public float convertDpToPixel(float dp)
	{		    
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());	    
	}
	
	public void waitTask()	
	{
		synchronized (_taskSync)
		{
			if(_loader!=null)
				_loader.waitIdle();		
		}		
	}

	public boolean isLoading()
	{
		return _isLoading;
	}

	public void setOnLoadingCompletedReference(Runnable r)
    {
        _onLoadingCompleted = r;
    }

	private Runnable _onLoadingCompleted;
	
	public Path getCurrentImagePath()
	{
		return _currentImagePath;
	}
	
	private static class ImageLoaderTask
	{
		public Path imagePath;
		public Rect viewRect;
		public Rect regionRect;
	}
	
	private class ImageLoader extends Thread
	{		
		public ImageLoader()
		{
			_uiHandler = new Handler();
			PowerManager pm = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);
			_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LoadImageTask");
		}
		
		@Override
		public void run()
		{
			while(!_stop)
			{
				ImageLoaderTask curTask;
				synchronized (_currentTaskSync)
				{
					curTask = _nextTask;
					_nextTask = null;
					_currentTaskSync.notify();
				}				
				if(curTask == null)
				{
					try 
					{
						Thread.sleep(POLLING_INTERVAL);
					} 
					catch (InterruptedException ignored)
					{				
					}
					continue;
				}
				_wakeLock.acquire();
				try
				{
					loadTask(curTask);
				}
				finally
				{
					_wakeLock.release();
				}
				
			}			
		}
		
		public void setNextTask(Path imagePath, Rect viewRect, Rect regionRect)
		{
			ImageLoaderTask t = new ImageLoaderTask();
			t.imagePath = imagePath;
			t.viewRect = viewRect;
			t.regionRect = regionRect;
			synchronized (_currentTaskSync)
			{
				_nextTask = t;				
			}			
		}
		
		public void cancel()
		{
			_stop=true;
		}
		
		public void waitIdle()
		{
			synchronized (_currentTaskSync)
			{
				try
				{
					while(_nextTask!=null)
						_currentTaskSync.wait();
				}
				catch (InterruptedException ignored)
				{					
				}
			}
		}
		
		private static final int POLLING_INTERVAL = 200;
		
		private final Handler _uiHandler;
		private final WakeLock _wakeLock;
		private final Object _currentTaskSync = new Object();
		private boolean _stop, _flipX, _flipY;
		private ImageLoaderTask _nextTask;
		private Bitmap _image;
		private int _sampleSize, _rotation;
		
		private void loadTask(final ImageLoaderTask task)
		{
			_isLoading = true;
			if(task.regionRect == null)
			{
				_uiHandler.post(new Runnable()
				{
					
					@Override
					public void run()
					{
						showLoading();
						_mainImageView.setImageBitmap(null);						
					}
				});							
			}
			
			try
			{
				loadImage(task.imagePath, task.viewRect,task.regionRect);
				synchronized (_currentTaskSync)
				{
					if(_nextTask == null && !_stop)
						_uiHandler.post(new Runnable()
						{			
							private Bitmap _result = _image;
							private int _curSampleSize = _sampleSize;
							private boolean _curFlipX = _flipX;
							private boolean _curFlipY = _flipY;
							private int _curRotation = _rotation;
							@Override
							public void run()							
							{
								if(_stop)
									return;
								if(task.regionRect == null)
								{
									_mainImageView.setImage(_result,_curSampleSize, _curRotation, _curFlipX, _curFlipY);
									_currentImagePath = task.imagePath;
									showImage();
								}
								else 
									_mainImageView.setOptimImage(_result,_curSampleSize);		
								
							}
						});					
				}
				
			}
			catch(final Throwable e)
			{
				_uiHandler.post(new Runnable()
				{				
					@Override
					public void run()
					{
						Logger.showAndLog(getActivity(), e);
					}
				});
			}
            _isLoading = false;
			if(_onLoadingCompleted!=null)
			    _onLoadingCompleted.run();
		}
		
		private void loadImage(Path imagePath, Rect viewRect, Rect regionRect) throws IOException
		{
			BitmapFactory.Options params = loadImageParams(imagePath);			
			boolean loadFull;
			boolean isJpg = "image/jpeg".equalsIgnoreCase(params.outMimeType);
			if(regionRect == null)
			{
				regionRect = new Rect(0,0,params.outWidth,params.outHeight);
				_isOptimSupported = android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.GINGERBREAD_MR1 && (isJpg || "image/png".equalsIgnoreCase(params.outMimeType));
				loadFull = true;
			}
			else
			{
				if(regionRect.top<0)
					regionRect.top = 0;
				if(regionRect.left<0)
					regionRect.left=0;
				if(regionRect.width()>params.outWidth)
					regionRect.right -= (regionRect.width()-params.outWidth);
				if(regionRect.height()>params.outHeight)
					regionRect.bottom -= (regionRect.height()-params.outHeight);
				loadFull = false;
			}
			_sampleSize = calcSampleSize(viewRect, regionRect);			
			for(int i=0;i<5;i++,_sampleSize*=2)
			{
				try
				{
					    if(loadFull)
						_image =  loadDownsampledImage(imagePath, _sampleSize);
					else
						_image = CompatHelper.loadBitmapRegion(imagePath, _sampleSize, regionRect);

					_flipX                                                                                                                                                                      = _flipY = false;
					_rotation = 0;
					if(isJpg)
						loadInitOrientation(imagePath);
					return;
				}
				catch(OutOfMemoryError e)
				{
					System.gc();					
				}				
				try
				{
					Thread.sleep(3000);
				}
				catch (InterruptedException ignored)
				{					
				}
			}
			throw new OutOfMemoryError();
		}

		private void loadInitOrientation(Path imagePath)
		{
			try
			{
				InputStream s = imagePath.getFile().getInputStream();
				try
				{
					Metadata m = ImageMetadataReader.readMetadata(s);

					for(Directory directory: m.getDirectories())
                        if(directory.containsTag(ExifSubIFDDirectory.TAG_ORIENTATION))
                        {
                            int orientation = directory.getInt(ExifSubIFDDirectory.TAG_ORIENTATION);
                            switch (orientation)
                            {
                                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                                    _flipX = true;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    _rotation = 180;
                                    break;
                                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                                    _rotation = 180;
                                    _flipX = true;
                                    break;
                                case ExifInterface.ORIENTATION_TRANSPOSE:
                                    _rotation = 90;
                                    _flipX = true;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    _rotation = 90;
                                    break;
                                case ExifInterface.ORIENTATION_TRANSVERSE:
                                    _rotation = -90;
                                    _flipX = true;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    _rotation = -90;
                                    break;
                            }
                            break;
                        }
				}
				finally
				{
					s.close();
				}
			}
			catch (Exception e)
            {
                if(GlobalConfig.isDebug())
                    Logger.log(e);
            }
		}
		
	}	
	
	private GestureImageView _mainImageView;
	private ViewSwitcher _viewSwitcher;
	private Path _currentImagePath;
	private final Rect _viewRect = new Rect();
	private final Object _taskSync = new Object();
	private ImageLoader _loader;
	private boolean _isOptimSupported;
	private boolean _isLoading;
	
	private void initParams(Bundle args)
	{
		Host act = getPreviewFragmentHost();
		if(act == null)
			return;
		Location loc = act.getLocation();
		try
		{
			_currentImagePath = args.containsKey(STATE_CURRENT_PATH) ? loc.getFS().getPath(args.getString(STATE_CURRENT_PATH)) : null;
		}
		catch(IOException e)
		{
			Logger.showAndLog(getActivity(), e);
		}
		if(_currentImagePath == null)
			_currentImagePath = getNextImagePath(null, true);						
	}
	
	private void startLoad()
	{
		if(_loader!=null)
			return;
		_loader = new ImageLoader();
		_loader.start();
		if(!loadImage(_currentImagePath,null))
			cancelTask();
	}
	
	
	private Host getPreviewFragmentHost()
	{
		return (Host)getActivity();
	}
		
	private void showLoading()
	{
		if(_viewSwitcher.getCurrentView()==_mainImageView)
			_viewSwitcher.showNext();		
	}
	
	private void showImage()
	{
		if(_viewSwitcher.getCurrentView()!=_mainImageView)
			_viewSwitcher.showPrevious();
		getActivity().invalidateOptionsMenu();
	}

	private void toggleAutoZoom()
	{
		UserSettings settings = UserSettings.getSettings(getActivity());
		boolean val = !settings.isImageViewerAutoZoomEnabled();
		settings.getSharedPreferences().edit().putBoolean(IMAGE_VIEWER_AUTO_ZOOM_ENABLED, val).commit();
		_mainImageView.setAutoZoom(val);
	}

	private void moveLeft()
	{
		loadImage(getPrevImagePath(),null);		
	}
	
	private void moveRight()
	{
		loadImage(getNextImagePath(),null);
	}
	
	private int getImageIndexByPath(Path imagePath)
	{			
		List<? extends CachedPathInfo> paths = getPreviewFragmentHost().getCurrentFiles();
		int count = paths.size();
		for(int i=0;i<count;i++)		
			if(imagePath.equals(paths.get(i).getPath()))
				return i;			
		
		return -1;
	}
	
	private Path getNextImagePath()
	{
		return getNextImagePath(_currentImagePath, true);
	}
	
	private Path getPrevImagePath()
	{
		return getNextImagePath(_currentImagePath, false);
	}
	
	private Path getNextImagePath(Path curPath, boolean forward)
	{
		List<? extends CachedPathInfo> paths = getPreviewFragmentHost().getCurrentFiles();
		int count = paths.size();
		if(count == 0)
			return null;
		
		int curIdx = 0;
		if(curPath!=null)
		{
			curIdx = getImageIndexByPath(curPath);
			if(curIdx<0)
				curIdx = forward ? -1 : count;
		}				
		int inc = forward ? 1 : -1;
		Context ctx = getActivity();
		for(int i= curIdx + inc; (forward && i<count) || (!forward && i>=0);i+=inc)
		{
			CachedPathInfo rec = paths.get(i);
			if(rec.isFile())
			{
				String mime = FileOpsService.getMimeTypeFromExtension(ctx, new StringPathUtil(rec.getName()).getFileExtension());
				if (mime.startsWith("image/"))				
					return rec.getPath();
			}
		}
		return null;
	}
	
	private void cancelTask()
	{
		synchronized (_taskSync)
		{
			if(_loader!=null)
			{
				_loader.cancel();
				try
				{
					_loader.join();
				}
				catch (InterruptedException ignored)
				{
				}
				_loader = null;
			}			
		}	
	}
	
	private boolean loadImage(Path imagePath, Rect regionRect)
	{		
		if(imagePath == null || getActivity() == null || _viewRect.width() == 0 || _viewRect.height() == 0 || _viewSwitcher == null)			
			return false;
		
		synchronized (_taskSync)
		{
			if(_loader!=null)			
				_loader.setNextTask(imagePath, new Rect(_viewRect), regionRect);				
		}		
		return true;
	}
}
