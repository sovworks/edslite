package com.sovworks.eds.android.filemanager.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.PowerManager;
import android.support.media.ExifInterface;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.helpers.CompatHelper;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Single;

import static com.sovworks.eds.android.filemanager.fragments.PreviewFragment.calcSampleSize;
import static com.sovworks.eds.android.filemanager.fragments.PreviewFragment.loadDownsampledImage;
import static com.sovworks.eds.android.filemanager.fragments.PreviewFragment.loadImageParams;

public class LoadedImage
{
    public static Single<LoadedImage> createObservable(Context context, Path imagePath, Rect viewRect, Rect regionRect)
    {
        return Single.create(emitter -> {
            PowerManager pm = (PowerManager)context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm == null ? null : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LoadImageTask");
            if(wl!=null)
                wl.acquire(10000);
            try
            {
                LoadedImage loadedImage = new LoadedImage(imagePath, viewRect, regionRect);
                loadedImage.loadImage();
                emitter.onSuccess(loadedImage);
            }
            finally
            {
                if(wl!=null)
                    wl.release();
            }

        });
    }

    public Rect getRegionRect()
    {
        return _regionRect;
    }

    public Bitmap getImageData()
    {
        return _image;
    }

    public int getSampleSize()
    {
        return _sampleSize;
    }

    public int getRotation()
    {
        return _rotation;
    }

    public boolean getFlipX()
    {
        return _flipX;
    }

    public boolean getFlipY()
    {
        return _flipY;
    }

    public boolean isOptimSupported()
    {
        return _isOptimSupported;
    }

    private LoadedImage(Path imagePath, Rect viewRect, Rect regionRect)
    {
        _imagePath = imagePath;
        _viewRect = viewRect;
        _regionRect = regionRect;
    }

    private final Path _imagePath;
    private final Rect _viewRect;
    private Rect _regionRect;
    private boolean _isOptimSupported;
    private Bitmap _image;
    private boolean _flipX, _flipY;
    private int _sampleSize, _rotation;

    private void loadImage() throws IOException
    {
        BitmapFactory.Options params = loadImageParams(_imagePath);
        boolean loadFull;
        boolean isJpg = "image/jpeg".equalsIgnoreCase(params.outMimeType);
        if(_regionRect == null)
        {
            _regionRect = new Rect(0,0,params.outWidth,params.outHeight);
            _isOptimSupported = isJpg || "image/png".equalsIgnoreCase(params.outMimeType);
            loadFull = true;
        }
        else
        {
            if(_regionRect.top<0)
                _regionRect.top = 0;
            if(_regionRect.left<0)
                _regionRect.left=0;
            if(_regionRect.width()>params.outWidth)
                _regionRect.right -= (_regionRect.width()-params.outWidth);
            if(_regionRect.height()>params.outHeight)
                _regionRect.bottom -= (_regionRect.height()-params.outHeight);
            loadFull = false;
        }
        _sampleSize = calcSampleSize(_viewRect, _regionRect);
        for(int i=0;i<5;i++,_sampleSize*=2)
        {
            try
            {
                if(loadFull)
                    _image = loadDownsampledImage(_imagePath, _sampleSize);
                else
                    _image = CompatHelper.loadBitmapRegion(_imagePath, _sampleSize, _regionRect);

                _flipX = _flipY = false;
                _rotation = 0;
                if(isJpg)
                    loadInitOrientation(_imagePath);
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
