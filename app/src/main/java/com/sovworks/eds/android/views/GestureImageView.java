package com.sovworks.eds.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class GestureImageView extends ImageView
{
	//private static final float MIN_NAVIG_VELOCITY = 300;
	private static final float MIN_SCALE = 0.1f;
	private static final float MAX_SCALE = 10f;
	
	public interface OptimImageRequiredListener
	{
		void onOptimImageRequired(Rect srcImageRect);
	}
	
	
	public interface NavigListener
	{
		void onNext();
		void onPrev();
	}

	public GestureImageView(Context context, AttributeSet attr)
	{
		super(context, attr);
		_scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener()
		{
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector)
			{
				Matrix m = new Matrix();
				_imageMatrix.invert(m);
				float[] points = new float[]{detector.getFocusX(), detector.getFocusY()};
				m.mapPoints(points);
				_scaleX = points[0];
				_scaleY = points[1];
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector)
			{
				float sf = Math.abs(_scaleFactorX) * detector.getScaleFactor();
				// Don't let the object get too small or too large.
				sf = Math.max(MIN_SCALE, Math.min(sf, MAX_SCALE));

				_scaleFactorX = sf;
				_scaleFactorY = sf;
				moveAndScale();
				return true;
			}
		});
		_flingDetector = new GestureDetector(context,new GestureDetector.SimpleOnGestureListener()
		{			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY)
			{
				if(_navigListener!=null && _allowNavig )//&& Math.abs(velocityX) > MIN_NAVIG_VELOCITY)
				{
					if(velocityX<0)
						_navigListener.onNext();
					else
						_navigListener.onPrev();					
						
					return true;
				}
				return false;
			}

		});
		setScaleType(ScaleType.MATRIX);
	}
	
	public void setNavigListener(NavigListener listener)
	{
		_navigListener = listener;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent ev)
	{
		// Let the ScaleGestureDetector inspect all events.
		_scaleDetector.onTouchEvent(ev);
		_flingDetector.onTouchEvent(ev);

		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN:
			{
				final float x = ev.getX();
				final float y = ev.getY();
	
				_lastTouchX = x;
				_lastTouchY = y;
				_activePointerId = ev.getPointerId(0);
				break;
			}
	
			case MotionEvent.ACTION_MOVE:
			{
				final int pointerIndex = ev.findPointerIndex(_activePointerId);
				final float x = ev.getX(pointerIndex);
				final float y = ev.getY(pointerIndex);		
				
	
				// Only move if the ScaleGestureDetector isn't processing a gesture.
				if (!_scaleDetector.isInProgress())
				{
					if(getDrawable()==null)
						break;				
					
					final float dx = x - _lastTouchX;
					final float dy = y - _lastTouchY;
					_posX += dx;
					_posY += dy;
					moveAndScale();		
				}
	
				_lastTouchX = x;
				_lastTouchY = y;
	
				break;
			}
	
			case MotionEvent.ACTION_UP:
			{
				_activePointerId = INVALID_POINTER_ID;
				onTouchUp();
				break;
			}
	
			case MotionEvent.ACTION_CANCEL:
			{
				_activePointerId = INVALID_POINTER_ID;
				break;
			}
	
			case MotionEvent.ACTION_POINTER_UP:
			{
				final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				final int pointerId = ev.getPointerId(pointerIndex);
				if (pointerId == _activePointerId)
				{
					// This was our active pointer going up. Choose a new
					// active pointer and adjust accordingly.
					final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
					_lastTouchX = ev.getX(newPointerIndex);
					_lastTouchY = ev.getY(newPointerIndex);
					_activePointerId = ev.getPointerId(newPointerIndex);
				}
				break;
			}
		}

		return true;
	}
	
	public RectF getViewRect()
	{
		return _viewRect;
	}
	
	public Matrix getOriginalImageMatrix()
	{
		return _imageMatrix;
	}
	
	public void setOptimImage(Bitmap b,int sampleSize)
	{	
		if(_optimImage != null)		
			_optimImage.recycle();
		float sfX = (float)sampleSize/(float)_originalSampleSize*_scaleFactorX;
		float sfy = (float)sampleSize/(float)_originalSampleSize*_scaleFactorY;
		_optimImage = b;		
		_optimImageMatrix.reset();
		_optimImageMatrix.postRotate(_rotation);
		_optimImageMatrix.postScale(sfX, sfy,0,0);
		RectF imageRect = new RectF(0, 0, b.getWidth(), b.getHeight());
		PointF delta = new PointF();
		validate(imageRect, _optimImageMatrix, delta);
		_optimImageMatrix.postTranslate(delta.x, delta.y);
		setImageBitmap(b);
		Matrix m = new Matrix(_optimImageMatrix);
		setImageMatrix(m);
	}

	public void clearImage()
	{
		setImage(null, 0);
	}

	public void setImage(Bitmap bm, int sampleSize)
	{
		setImage(bm, sampleSize, 0, false, false);
	}
		
	public void setImage(Bitmap bm, int sampleSize, int rotation, boolean flixpX, boolean flipY)
	{
		_inited = false;
		if(_optimImage!=null)
		{
			_optimImage.recycle();
			_optimImage = null;
		}
		
		if(_originalImage!=null)		
			_originalImage.recycle();
		_originalImage = bm;
		_originalSampleSize = sampleSize;
		_rotation = rotation;
		_flipX = flixpX;
		_flipY = flipY;
		setImageBitmap(bm);
		if(bm!=null)
		{
			_imageRect.set(0,0,
					bm.getWidth(),
					bm.getHeight());
			calcInitParams();
		}
	}
	
	public Bitmap getOriginalImage()
	{
		return _originalImage;
	}
	
	
	public void rotateLeft()
	{
		_rotation -= 90;
		while(_rotation<0)
			_rotation += 360 ;
		moveAndScale();
		startOptimImageLoad();
	}
	
	public void rotateRight()
	{
		_rotation += 90;		
		while(_rotation>360)
			_rotation -= 360;
		moveAndScale();
		startOptimImageLoad();
	}
	
	public void zoomIn()
	{
		int sci = findClosestScaleIndex();
		if(++sci<SCALE_FACTORS.length)
		{
			_scaleFactorX = SCALE_FACTORS[sci] * (_flipX ? -1 : 1);
			_scaleFactorY = SCALE_FACTORS[sci] * (_flipY ? -1 : 1);
			moveAndScale();
			startOptimImageLoad();
		}
	}
	
	public void zoomOut()
	{
		int sci = findClosestScaleIndex();
		if(--sci>=0)
		{
			_scaleFactorX = SCALE_FACTORS[sci] * (_flipX ? -1 : 1);
			_scaleFactorY = SCALE_FACTORS[sci] * (_flipY ? -1 : 1);
			moveAndScale();
			startOptimImageLoad();
		}		
	}
	
	public void rotate(int angle)
	{
		_rotation = angle;
		moveAndScale();
		startOptimImageLoad();
	}
	
	public void setOnLoadOptimImageListener(OptimImageRequiredListener listener)
	{
		_onLoadOptimImageListener = listener;
	}

    public void setOnSizeChangedListener(Runnable listener)
    {
        _onSizeChangedListener = listener;
    }

	public void setAutoZoom(boolean val)
	{
		_autoZoom = val;
		calcInitParams();
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		_viewRect.set(0, 0, w, h);
		if(!_inited)
			calcInitParams();
        if(_onSizeChangedListener!=null)
            _onSizeChangedListener.run();
	}
	
	
	protected void onTouchUp()
	{
		startOptimImageLoad();
	}
	
	private static final int INVALID_POINTER_ID = -1;
	
	private static final float[] SCALE_FACTORS = new float[] {0.1f,0.2f,0.4f,0.8f,1f,2f,4f,8f,10f};
	
	
	private NavigListener _navigListener;
	private final RectF _imageRect = new RectF(), _viewRect = new RectF();
	private final ScaleGestureDetector _scaleDetector;
	private final GestureDetector _flingDetector;
	private final Matrix _imageMatrix = new Matrix(), _optimImageMatrix = new Matrix();
	private float _scaleFactorX, _scaleFactorY,_posX,_posY,_lastTouchX,_lastTouchY,_scaleX,_scaleY;
	private int _activePointerId = INVALID_POINTER_ID;
	private boolean  _allowNavig, _autoZoom, _flipX, _flipY, _inited;
	private int _rotation;
	private Bitmap _originalImage, _optimImage;
	private int _originalSampleSize;
    private Runnable _onSizeChangedListener;
	
	
	private OptimImageRequiredListener _onLoadOptimImageListener;
	
	private void showOriginalImage()	
	{
		super.setImageBitmap(_originalImage);		
		if(_optimImage!=null)
		{
			_optimImage.recycle();
			_optimImage = null;
		}		
	}
	
	private int findClosestScaleIndex()
	{
		int idx = 0;
		float delta = 100;
		for(int i=0;i<SCALE_FACTORS.length;i++)
		{
			float c = Math.abs(SCALE_FACTORS[i]- Math.abs(_scaleFactorX));
			if(c<delta)
			{
				delta = c;
				idx = i;
			}
		}
		return idx;
	}
	
	private void centerImage()
	{
		validate();
	}
	
	private void moveAndScale()
	{
		showOriginalImage();
		applyTrans();
		validate();
		Matrix m = new Matrix(_imageMatrix);
		setImageMatrix(m);
		RectF rf = new RectF(_imageRect);
		_imageMatrix.mapRect(rf);
		_allowNavig = rf.width()<=_viewRect.width(); //_imageRect.width()*_scaleFactor<=_viewRect.width();			
	}
	
	private void startOptimImageLoad()
	{
		if(_onLoadOptimImageListener == null)
			return;
		
		if(_originalSampleSize>1)
		{		
			RectF rf = new RectF(_imageRect);
			_imageMatrix.mapRect(rf);
			if(rf.width()>_viewRect.width() || rf.height()>_viewRect.height())
			{
				Matrix m = new Matrix();	
				//m.postScale(_scaleFactor, _scaleFactor,_scaleX,_scaleY);		
				//m.postTranslate(_posX, _posY);
				if(_imageMatrix.invert(m))
				{	
					m.postScale(_originalSampleSize,_originalSampleSize);				
					rf = new RectF(_viewRect);
					m.mapRect(rf);
					Rect r = new Rect();
					rf.round(r);
					_onLoadOptimImageListener.onOptimImageRequired(r);
				}
			}
		}
	}
	
	private void calcInitParams()
	{
		if(_imageRect.width()==0 || _imageRect.height()==0 || _viewRect.width() == 0 || _viewRect.height() == 0)
			return;

		_scaleX = _scaleY = 0;
		_posX = _posY = 0;
		_scaleFactorX = _scaleFactorY = 1.f;
		RectF imageRect = new RectF(_imageRect);
		if(_rotation != 0)
		{
			applyTrans();
			_imageMatrix.mapRect(imageRect);
		}

		float scaleFactor;
		if(imageRect.width()<=_viewRect.width() && imageRect.height()<= _viewRect.height() && !_autoZoom)
			scaleFactor = 1.f;
		else	
			scaleFactor = Math.min(
					_viewRect.height()/imageRect.height(),
					_viewRect.width()/imageRect.width()
			);


		_scaleFactorX = _flipX ? -scaleFactor : scaleFactor;
		_scaleFactorY = _flipY ? -scaleFactor : scaleFactor;
		_imageMatrix.reset();
		_imageMatrix.postRotate(_rotation);
		_imageMatrix.postScale(_scaleFactorX, _scaleFactorY);
		validate();
		Matrix m = new Matrix(_imageMatrix);
		setImageMatrix(m);
		_inited = true;
	}
	
	private void applyTrans()
	{
		_imageMatrix.reset();
		_imageMatrix.postScale(_scaleFactorX, _scaleFactorY,_scaleX,_scaleY);
		_imageMatrix.postRotate(_rotation);
		_imageMatrix.postTranslate(_posX, _posY);
	}
	
	private void validate()
	{	    
		PointF delta = new PointF();
		validate(_imageRect,_imageMatrix,delta);
		
		if(delta.x!=0 || delta.y!=0)
		{
			_posX += delta.x;
			_posY += delta.y;
			applyTrans();
		}
	}	
	
	private void validate(RectF curImageRect,Matrix curImageMatrix, PointF outDelta)
	{	
		float  deltaX= 0, deltaY = 0;
		RectF imageRect = new RectF(curImageRect);
		curImageMatrix.mapRect(imageRect);		
		
		if (imageRect.height() <= _viewRect.height())
			deltaY = (_viewRect.height() - imageRect.height()) / 2 - imageRect.top;
	    else if (imageRect.top > 0) 
	    	deltaY = -imageRect.top;
	    else if (imageRect.bottom < _viewRect.height()) 
	    	deltaY = _viewRect.height() - imageRect.bottom;		
		
		
		if (imageRect.width() <= _viewRect.width())	
			deltaX = (_viewRect.width() - imageRect.width()) / 2 - imageRect.left;		
	    else if (imageRect.left > 0)	    
	    	deltaX = -imageRect.left;
	    else if (imageRect.right < _viewRect.width())	    
	    	deltaX = _viewRect.width() - imageRect.right;
	    
		outDelta.offset(deltaX, deltaY);
	}
}
