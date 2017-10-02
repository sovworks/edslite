package com.sovworks.eds.locations;

import com.sovworks.eds.android.helpers.ProgressReporter;
import com.sovworks.eds.crypto.SecureBuffer;

import java.io.IOException;

public interface Openable extends Location
{
	String PARAM_PASSWORD = "com.sovworks.eds.android.PASSWORD";
	String PARAM_KDF_ITERATIONS = "com.sovworks.eds.android.KDF_ITERATIONS";

	void setPassword(SecureBuffer pass);
	boolean hasPassword();
	boolean requirePassword();
	boolean hasCustomKDFIterations();
	boolean requireCustomKDFIterations();
	void setNumKDFIterations(int num);
	void setOpenReadOnly(boolean readOnly);
	boolean isOpen();
	void open() throws Exception;	
	void close(boolean force) throws IOException;
	void setOpeningProgressReporter(ProgressReporter pr);
}
