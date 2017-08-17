package com.sovworks.eds.fs.util;

import java.io.IOException;

import android.os.Parcelable;

public interface SrcDstCollection extends Iterable<SrcDstCollection.SrcDst>, Parcelable
{
	interface SrcDst
	{
		com.sovworks.eds.locations.Location getSrcLocation() throws IOException;
		com.sovworks.eds.locations.Location getDstLocation() throws IOException;
	}

}
