package com.sovworks.eds.android.providers;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class ContainersDocumentProvider extends ContainersDocumentProviderBase
{
    public static final String AUTHORITY = "com.sovworks.eds.android.providers.documents.lite";
}
