package com.sovworks.eds.fs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import android.os.Parcel;
import android.os.Parcelable;

import com.sovworks.eds.android.Logger;


public class SrcDstGroup implements SrcDstCollection
{	
	public static final Parcelable.Creator<SrcDstGroup> CREATOR = new Parcelable.Creator<SrcDstGroup>() 
	{
			public SrcDstGroup createFromParcel(Parcel in) 
			{			
				try
				{
                    ArrayList<SrcDstCollection> cols = new ArrayList<>();
                    int size = in.readInt();
                    for(int i=0;i<size;i++)
						cols.add((SrcDstCollection) in.readParcelable(getClass().getClassLoader()));
					return new SrcDstGroup(cols);
				}
				catch(Exception e)
				{
					Logger.log(e);
					return null;
				}						
			}
			
			public SrcDstGroup[] newArray(int size) 
			{
			    return new SrcDstGroup[size];
			}
	};


	public SrcDstGroup(Collection<? extends SrcDstCollection> srcDsts)
	{
		_srcDsts = srcDsts;
	}	
	
	@Override
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
				if(!_hasNext)
					throw new NoSuchElementException();
				_hasNext = false;
				return _next;
			}
			
			public boolean hasNext()
			{
				if(_hasNext)
					return true;
				
				while(_curCol == null || !_curCol.hasNext())
				{
					if(!_cols.hasNext())
						return false;
					_curCol = _cols.next().iterator();
				}
				_next = _curCol.next();
				_hasNext = true;
				return true;
			}
			
			private boolean _hasNext;
			private SrcDst _next;
			private Iterator<SrcDst> _curCol;
			private final Iterator<? extends SrcDstCollection> _cols = _srcDsts.iterator();
		};
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
        int size = _srcDsts.size();
        dest.writeInt(size);
		for(SrcDstCollection c: _srcDsts)
			dest.writeParcelable(c, flags);
	}
		
	private final Collection<? extends SrcDstCollection> _srcDsts;
	
}
