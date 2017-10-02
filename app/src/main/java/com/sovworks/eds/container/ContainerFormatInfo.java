package com.sovworks.eds.container;

import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.FileSystemInfo;
import com.sovworks.eds.fs.RandomAccessIO;

import java.io.IOException;

public interface ContainerFormatInfo
{
	String getFormatName();
	VolumeLayout getVolumeLayout();
	boolean hasHiddenContainerSupport();
	boolean hasKeyfilesSupport();
	boolean hasCustomKDFIterationsSupport();
	int getMaxPasswordLength();
	VolumeLayout getHiddenVolumeLayout();
	void formatContainer(RandomAccessIO io, VolumeLayout layout, FileSystemInfo fsInfo) throws IOException, ApplicationException;
	int getOpeningPriority();
}
