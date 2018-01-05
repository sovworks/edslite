package com.sovworks.eds.android.helpers;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.filemanager.records.BrowserRecord;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class ExtendedFileInfoLoader implements Closeable
{
    private static final int FB_EXTENDED_INFO_QUEUE_SIZE = 40;
    private static final int FB_NUM_CACHED_EXTENDED_INFO = 100;

    public interface ExtendedFileInfo
    {
        void attach(BrowserRecord record);
        void detach(BrowserRecord record);
        void clear();
    }

    static String getPathKey(String locationId, Path path)
	{
		return String.format("%s/%s",locationId,path.getPathString());
	}

    public static synchronized ExtendedFileInfoLoader getInstance()
    {
        if(_instance == null)
            _instance = new ExtendedFileInfoLoader();
        return _instance;
    }

    public static synchronized void closeInstance()
    {
        if(_instance!=null)
        {
            _instance.close();
            _instance = null;
        }
    }

    private static ExtendedFileInfoLoader _instance;

	public ExtendedFileInfoLoader()
	{
        _updateViewHandler = new Handler(Looper.getMainLooper());
        _loadedInfo = new LruCache<String, ExtendedFileInfo>(FB_NUM_CACHED_EXTENDED_INFO)
        {
            /*@Override
            protected int sizeOf(String key, ExtendedFileInfo ii)
            {
                return 16 + (ii.icon == null ? 0 : ii.icon.getWidth()*ii.icon.getHeight()*4);
            }*/

            @Override
            protected void entryRemoved (boolean evicted, String key, final ExtendedFileInfo oldValue, ExtendedFileInfo newValue)
            {
                if(oldValue!=null)
                {
                    _updateViewHandler.post(oldValue::clear);
                }
                super.entryRemoved(evicted, key, oldValue, newValue);
            }

        };
        _loadingQueue = new FileInfoLoadQueue(FB_EXTENDED_INFO_QUEUE_SIZE);
        _loadingTask = new LoadingTask();
        _loadingTask.start();
	}

    //Call from main thread
	public void requestExtendedInfo(String locationId, BrowserRecord rec)
	{
        InfoCache ii = new InfoCache(locationId,rec);
        ExtendedFileInfo data = _loadedInfo.get(ii.getPathKey());
        if(data != null)
            data.attach(rec);
        else
        {
            synchronized (_loadingQueue)
            {
                enqueueRequest(ii);
            }
        }
	}

    //Call from main thread
    public void detachRecord(String locationId, BrowserRecord rec)
    {
        InfoCache ii = new InfoCache(locationId,rec);
        ExtendedFileInfo data = _loadedInfo.get(ii.getPathKey());
        if(data != null)
            data.detach(rec);
        synchronized (_loadingQueue)
        {
            _loadingQueue.discard(rec);
        }
    }

    public void pauseViewUpdate()
    {
        _pause = true;
    }

    public void resumeViewUpdate()
    {
        _pause = false;
    }

    public void discardCache(Location loc, Path path)
    {
        String key = getPathKey(loc.getId(), path);
        _loadedInfo.remove(key);
    }
	
	@Override
	public void close()
	{
        _pause = true;
		_stop = true;
        synchronized (_loadingQueue)
        {
            _loadingQueue.notify();
        }
        _loadedInfo.evictAll();
        try
        {
            _loadingTask.join(5000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
	
	private class LoadingTask extends Thread
	{
		@Override
		public void run()
		{
            InfoCache nextTarget = null;
			while(!_stop)
			{
				try
				{
					synchronized(_loadingQueue)
					{
                        if(nextTarget == null)
						    _loadingQueue.wait();
						nextTarget = _loadingQueue.getLast();//_loadingQueue.poll();
					}
                    if(nextTarget != null && !nextTarget.discard)
                        processExtInfo(nextTarget);
				}
				catch(Exception e)
				{
					Logger.log(e);
				}
			}		
		}
		
		private void processExtInfo(final InfoCache ii) throws IOException
		{
            final ExtendedFileInfo data = ii.record.loadExtendedInfo();
            if (data != null)
            {
                _loadedInfo.put(ii.getPathKey(), data);
                _updateViewHandler.post(() ->
                {
                    if (!ii.discard)
                    {
                        data.attach(ii.record);
                        if (!_pause)
                            ii.record.updateView();
                    }

                });
            }
		}
	}
	
	//private final Map<String,IconInfo> _loadedInfo = new HashMap<String,IconInfo>(Preferences.MAX_CACHED_ICONS);
	private final LruCache<String, ExtendedFileInfo> _loadedInfo;
	private final FileInfoLoadQueue _loadingQueue;
    private final Handler _updateViewHandler;
	private boolean _stop;
    private boolean _pause = true;
	private final LoadingTask _loadingTask;

	private void removeOldestInfo()
	{
		_loadingQueue.poll();
	}
	
	private void enqueueRequest(InfoCache ii)
	{
        if (_loadingQueue.size() == _loadingQueue.getCapacity())
            removeOldestInfo();
		_loadingQueue.add(ii);
		_loadingQueue.notify();	
	}
	
}

class InfoCache
{
	InfoCache(String locId, BrowserRecord rec)
	{
		locationId = locId;
		record = rec;
	}
	
	String getPathKey()
	{
		return ExtendedFileInfoLoader.getPathKey(locationId, record.getPath());
	}

	public final String locationId;
	public final BrowserRecord record;
    boolean discard;
}

class FileInfoLoadQueue extends AbstractQueue<InfoCache>
{
	
	
	FileInfoLoadQueue(int capacity)
	{	
		_buf = new InfoCache[capacity];
	}
	
	int getCapacity()
	{
		return _buf.length;
	}

    void discard(BrowserRecord rec)
    {
        for(int i=0; i<_usedSlots; i++)
        {
            InfoCache tmp = _buf[(_headPosition + i) % _buf.length];
            if(tmp.record == rec)
                tmp.discard = true;
        }
    }

	@Override
	public boolean offer(InfoCache e)
	{
		if(e == null)
			throw new RuntimeException("Argument cannot be null");
		
		if(_usedSlots<_buf.length)
		{			
			_buf[(_headPosition+_usedSlots++) % _buf.length] = e;
			return true;
		}
		return false;
	}

	@Override
	public InfoCache peek()
	{
		return _usedSlots>0 ? _buf[_headPosition] : null;
	}

	@Override
	public InfoCache poll()
	{
		if(_usedSlots == 0)
            return null;

        InfoCache tmp = _buf[_headPosition];
        _buf[_headPosition]=null;
        _headPosition = ++_headPosition % _buf.length;
        _usedSlots--;
        return tmp;
	}

    public InfoCache getLast()
    {
        if(_usedSlots == 0)
            return null;

        int pos = (_headPosition + _usedSlots - 1) % _buf.length;
        InfoCache tmp = _buf[pos];
        _buf[pos]=null;
        _usedSlots--;
        return tmp;
    }
	
	@Override
	public void clear()
	{
		_headPosition=_usedSlots=0;
		Arrays.fill(_buf, null);
	}

	@NonNull
    @Override
	public Iterator<InfoCache> iterator()
	{
		return new Iterator<InfoCache>()
		{
			
			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();				
			}
			
			@Override
			public InfoCache next()
			{
				if(!hasNext())
					throw new NoSuchElementException();
				
				return _buf[(_headPosition+_proc++)%_buf.length];
			}
			
			@Override
			public boolean hasNext()
			{
				return _proc<_usedSlots;
			}
			
			private int _proc = 0;
		};
	}

	@Override
	public int size()
	{
		return _usedSlots;
	}
	
	private final InfoCache[] _buf;
	private int _usedSlots;
	private int _headPosition;
}
