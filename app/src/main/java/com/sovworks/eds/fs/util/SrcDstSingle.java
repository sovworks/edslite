package com.sovworks.eds.fs.util;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.fs.util.SrcDstCollection.SrcDst;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.LocationsManagerBase;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class SrcDstSingle implements SrcDstCollection, SrcDst
{	
	
	public static final Parcelable.Creator<SrcDstSingle> CREATOR = new Parcelable.Creator<SrcDstSingle>() 
	{
			public SrcDstSingle createFromParcel(Parcel in) 
			{			
				try
				{
					LocationsManager lm = LocationsManagerBase.getLocationsManager(null, false);
					Uri u = in.readParcelable(getClass().getClassLoader());
					Location srcLoc = lm != null ? lm.getLocation(u) : null;
					u = in.readParcelable(getClass().getClassLoader());
					Location dstLoc = Uri.EMPTY.equals(u) ? null : lm != null ? lm.getLocation(u) : null;
				    return srcLoc != null ? new SrcDstSingle(srcLoc, dstLoc) : null;
				}
				catch(Exception e)
				{
					Logger.log(e);
					return null;
				}						
			}
			
			public SrcDstSingle[] newArray(int size) 
			{
			    return new SrcDstSingle[size];
			}
	};


	public SrcDstSingle(Location srcLoc, Location dstLoc)
	{
		_srcLocation = srcLoc;
		_dstLocation = dstLoc;
	}

	@NonNull
	public Iterator<SrcDst> iterator()
	{
		return new Iterator<SrcDst>()
		{			
			public void remove()
			{
				throw new UnsupportedOperationException();				
			}
			
			public SrcDst next()
			{
				if(_shown)
					throw new NoSuchElementException();
				_shown = true;
				return SrcDstSingle.this;
			}
			
			public boolean hasNext()
			{
				return !_shown;
			}
			
			private boolean _shown;
		};
	}
	
	@Override
	public Location getSrcLocation() throws IOException
	{
		return _srcLocation;
	}

	@Override
	public Location getDstLocation()
	{
		return _dstLocation;
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
	}
	
	private final Location _srcLocation,_dstLocation;

	
	
}
