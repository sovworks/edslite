package com.sovworks.eds.container;

import com.sovworks.eds.fs.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdsContainer extends EdsContainerBase
{
	public static List<ContainerFormatInfo> getSupportedFormats()
	{
		ArrayList<ContainerFormatInfo> al = new ArrayList<>();
		Collections.addAll(al, SUPPORTED_FORMATS);
		return al;
	}

	private static final ContainerFormatInfo[] SUPPORTED_FORMATS =
			new ContainerFormatInfo[]{
					new com.sovworks.eds.truecrypt.FormatInfo(),
					new com.sovworks.eds.veracrypt.FormatInfo(),
					new com.sovworks.eds.luks.FormatInfo()
			};
	
	public static ContainerFormatInfo findFormatByName(String name)
	{
		return findFormatByName(getSupportedFormats(), name);
	}
	
	public EdsContainer(Path path)
	{			
		this(path, null, null);							
	}
	
	public EdsContainer(Path path, ContainerFormatInfo containerFormat,VolumeLayout layout)
	{
		super(path, containerFormat, layout);
	}

	@Override
	protected List<ContainerFormatInfo> getFormats()
	{
		return getSupportedFormats();
	}

}



