package com.sovworks.eds.fs.util;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.Directory;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.LocationsManagerBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class SrcDstRec implements SrcDstCollection, SrcDstCollection.SrcDst
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
			Location nextSrcLoc = srcLoc.copy();
			nextSrcLoc.setCurrentPath(p);
            SrcDstRec sdr = new SrcDstRec(new SrcDstSingle(nextSrcLoc, dstLoc));
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
						SrcDstRec sdr = new SrcDstRec( new SrcDstSingle(srcLoc, dstLoc));
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
	
	public SrcDstRec(SrcDstCollection.SrcDst topElement)
	{
		_topElement = topElement;
	}
	
	public void setIsDirLast(boolean val)
	{
		_dirLast = val;
	}

	@NonNull
	@Override
	public Iterator<SrcDstCollection.SrcDst> iterator()
	{
		return observeTree(this, _dirLast).
				subscribeOn(Schedulers.newThread()).
				blockingIterable(100).
				iterator();
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		try
		{
			Location srcLocation = _topElement.getSrcLocation();
			Location dstLocation = _topElement.getDstLocation();
			dest.writeParcelable(srcLocation.getLocationUri(), flags);
			dest.writeParcelable(dstLocation == null ? Uri.EMPTY : dstLocation.getLocationUri(), flags);
			dest.writeByte((byte) (_dirLast ? 1 : 0));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Location getSrcLocation() throws IOException
	{
		return _topElement.getSrcLocation();
	}

	@Override
	public Location getDstLocation() throws IOException
	{
		return _topElement.getDstLocation();
	}


	private final SrcDstCollection.SrcDst _topElement;
	private List<SrcDstRec> _subElements;
    private boolean _dirLast;

    private List<SrcDstRec> getSubElements()
    {
        if(_subElements == null)
        {
            try
            {
                _subElements = listSubElements();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return _subElements;
    }


	private static Observable<SrcDst> observeTree(SrcDstRec tree, boolean isDirLast)
	{
		return Observable.create(emitter -> {
			if(!isDirLast)
				emitter.onNext(tree);
			for(SrcDstRec sdc : tree.getSubElements())
				observeTree(sdc, isDirLast).
                        subscribe(emitter::onNext, emitter::onError);
			if(isDirLast)
				emitter.onNext(tree);
			emitter.onComplete();
		});
	}

	private List<SrcDstRec> listSubElements() throws IOException
	{
		ArrayList<SrcDstRec> res = new ArrayList<>();
		Location srcLocation = _topElement.getSrcLocation();
		Path startPath = srcLocation.getCurrentPath();
        if(startPath == null || !startPath.isDirectory())
            return res;

        Directory directory = startPath.getDirectory();
        String directoryName = directory.getName();
		com.sovworks.eds.fs.Directory.Contents dc = directory.list();
		try
		{
			for(Path subPath: dc)
			{
				Location subSrcLocation = srcLocation.copy();
				subSrcLocation.setCurrentPath(subPath);
				SrcDstCollection.SrcDst subSrcDst = new SrcDstCollection.SrcDst()
				{
					@Override
					public Location getSrcLocation() throws IOException
					{
						return subSrcLocation;
					}

					@Override
					public Location getDstLocation() throws IOException
					{
						if(_dstLocationCache == null)
							_dstLocationCache = calcSubDestLocation(_topElement, directoryName);
						return _dstLocationCache;
					}
					private Location _dstLocationCache;
				};
				res.add(new SrcDstRec(subSrcDst));
			}
		}
		finally
		{
			dc.close();
		}
		return res;
	}

	private static Location calcSubDestLocation(SrcDstCollection.SrcDst parentSrcDst, String nextDirName) throws IOException
	{
		Location loc = parentSrcDst.getDstLocation();
		if(loc == null)
			return null;
		try
		{
		    loc = loc.copy();
            Path dstSubPath = PathUtil.getDirectory(
                    loc.getCurrentPath(),
                    nextDirName
            ).getPath();
		    loc.setCurrentPath(dstSubPath);
			return loc;
		}
		catch (IOException e)
		{
			Logger.log(e);
		}
		return null;
	}
}

