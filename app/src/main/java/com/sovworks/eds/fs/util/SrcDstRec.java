package com.sovworks.eds.fs.util;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManagerBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SrcDstRec implements SrcDstCollection
{
	public static SrcDstCollection fromPathsNoDest(Location srcLoc, boolean dirLast, Path... srcPaths)
	{
		return fromPaths(srcLoc, null, dirLast, srcPaths);
	}
	
	public static SrcDstCollection fromPathsNoDest(Location srcLoc, boolean dirLast, Collection<? extends Path> srcPaths)
	{
		return fromPaths(srcLoc, null, dirLast, srcPaths);
	}

    public static SrcDstCollection fromPaths(Location srcLoc, Location dstLoc, Path... srcPaths)
    {
        return fromPaths(srcLoc, dstLoc, false, Arrays.asList(srcPaths));
    }
	
	public static SrcDstCollection fromPaths(Location srcLoc, Location dstLoc, boolean dirLast, Path... srcPaths)
	{
		return fromPaths(srcLoc, dstLoc, dirLast, Arrays.asList(srcPaths));
	}
	
	public static SrcDstCollection fromPaths(Location srcLoc, Location dstLoc, boolean dirLast, Collection<? extends Path> srcPaths)
	{
		if(srcPaths == null)
			return new SrcDstSingle(srcLoc, dstLoc);
		ArrayList<SrcDstCollection> res = new ArrayList<>(srcPaths.size());
		for(Path p: srcPaths)
		{
			Location l = srcLoc.copy();
			l.setCurrentPath(p);
            SrcDstRec sdr = new SrcDstRec(l, dstLoc);
            sdr.setIsDirLast(dirLast);
			res.add(sdr);
		}
		return new SrcDstGroup(res);		
	}
	
	public static final Parcelable.Creator<SrcDstRec> CREATOR = new Parcelable.Creator<SrcDstRec>() 
	{
			public SrcDstRec createFromParcel(Parcel in) 
			{			
				try
				{
					LocationsManager lm = LocationsManagerBase.getLocationsManager(null, false);
					Uri u = in.readParcelable(getClass().getClassLoader());
					Location srcLoc = lm!=null ? lm.getLocation(u) : null;
					u = in.readParcelable(getClass().getClassLoader());
					Location dstLoc = Uri.EMPTY.equals(u) ? null : lm != null ? lm.getLocation(u) : null;
					boolean dirLast = in.readByte() == 1;
					if(srcLoc != null)
					{
						SrcDstRec sdr = new SrcDstRec(srcLoc, dstLoc);
						sdr.setIsDirLast(dirLast);
						return sdr;
					}
					return null;
				}
				catch(Exception e)
				{
					Logger.log(e);
					return null;
				}						
			}
			
			public SrcDstRec[] newArray(int size) 
			{
			    return new SrcDstRec[size];
			}
	};
	
	public SrcDstRec(Location srcLoc,Location dstLoc)
	{
		_srcLocation = srcLoc;
		_dstLocation = dstLoc;				
	}
	
	public void setIsDirLast(boolean val)
	{
		_dirLast = val;
	}

	@Override
	public Iterator<SrcDstCollection.SrcDst> iterator()
	{
		SrcDst list = getSrcTree();
        if(list == null)
            throw new IllegalStateException("Source path doesn't exist");
		return new SrcTreeIterator(list);
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeParcelable(_srcLocation.getLocationUri(), flags);
		dest.writeParcelable(_dstLocation == null ? Uri.EMPTY : _dstLocation.getLocationUri(), flags);
        dest.writeByte((byte) (_dirLast ? 1 : 0));
	}

	private Location _srcLocation,_dstLocation;

    private SrcDst _srcTree;
    private boolean _dirLast;

    private SrcDst getSrcTree()
    {
        if(_srcTree == null)
        {
            try
            {
                if(_srcLocation.getCurrentPath().exists())
                {
                    _srcTree = buildSrcSubTree(_srcLocation.getCurrentPath());
                    _srcTree._curDstLocation = _dstLocation;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return _srcTree;
    }

	private class SrcTreeIterator implements Iterator<SrcDstCollection.SrcDst>
	{
        SrcTreeIterator(SrcDst start)
		{
			_start = _first = start;
		}

		@Override
		public boolean hasNext()
		{
			if(!_inited)
				getNext();
			return _next!=null;
		}

		@Override
		public SrcDstCollection.SrcDst next()
		{
			if(!_inited)
				getNext();
			if(_next == null)
				throw new IllegalStateException("No more elements");
			SrcDstCollection.SrcDst next = _next;
			getNext();
			return next;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private final SrcDst _start;
		private Iterator<SrcDst> _currentFolderIterator;
        private Location _subDstLocation;
		private SrcTreeIterator _recursiveIterator;
		private SrcDstCollection.SrcDst _next;
		private SrcDst _first, _last;
		private boolean _inited;

		private synchronized boolean getNext()
		{
			_inited = true;
			_next = null;
			return checkFirst() || checkRecursive() || checkCurrentFolder() || checkLast();
		}

		private boolean checkFirst()
		{
			if(_first!=null)
			{
				if(_first._subTree != null)
				{
					_currentFolderIterator = _first._subTree.iterator();
					if(_dirLast)
					{
						_last = _first;
						_first = null;
					}
					else
					{
						_next = _first;
						_first = null;
						return true;
					}
				}
				else
				{
					_next = _first;
					_first = null;
					return true;
				}
			}
			return false;
		}

		private boolean checkRecursive()
		{
			if(_recursiveIterator!=null && _recursiveIterator.hasNext())
			{
				_next = _recursiveIterator.next();
				return true;
			}
			return false;
		}

		private boolean checkCurrentFolder()
		{
			if(_currentFolderIterator != null && _currentFolderIterator.hasNext())
			{
				SrcDst sub = _currentFolderIterator.next();
                sub._curDstLocation = getSubDstLocation();
				_recursiveIterator = new SrcTreeIterator(sub);
				return getNext();
			}
			return false;
		}

        private Location getSubDstLocation()
        {
            if(_subDstLocation == null)
                _subDstLocation = calcSubDestLocation();
            return _subDstLocation;
        }

        private Location calcSubDestLocation()
        {
            if(_start._curDstLocation!=null)
            {
                Location loc = _start._curDstLocation.copy();
                try
                {
                    loc.setCurrentPath(PathUtil.getDirectory(_start.
								_curDstLocation.
								getCurrentPath(),
								_start.
										_curSrcLocation.
										getCurrentPath().
										getDirectory().
										getName()
							).
							getPath()

                    );
                    return loc;
                }
                catch (IOException e)
                {
                    Logger.log(e);
                }
            }
            return null;
        }

		private boolean checkLast()
		{
			if(_last != null)
			{
				_next = _last;
				_last = null;
				return true;
			}
			return false;
		}
	}

	private class SrcDst implements SrcDstCollection.SrcDst
	{
		SrcDst(Location srcLocation)
		{
			_curSrcLocation = srcLocation;
		}

		@Override
		public Location getSrcLocation() throws IOException
		{
			return _curSrcLocation;
		}

		@Override
		public Location getDstLocation() throws IOException
		{
			return _curDstLocation;
		}

		private final Location _curSrcLocation;
		private Location _curDstLocation;
		private List<SrcDst> _subTree;
	}

	private SrcDst buildSrcSubTree(Path srcPath) throws IOException
	{
        if(srcPath == null)
            return null;

        Location srcLoc = _srcLocation.copy();
        srcLoc.setCurrentPath(srcPath);
		SrcDst res = new SrcDst(srcLoc);
		if(!srcPath.isDirectory())
			return res;

		ArrayList<SrcDst> sub = new ArrayList<>();
		com.sovworks.eds.fs.Directory.Contents dc = srcPath.getDirectory().list();
		try
		{
			for(Path subPath: dc)
				sub.add(buildSrcSubTree(subPath));
		}
		finally
		{
			dc.close();
		}
		res._subTree = sub;
		return res;
	}
}

