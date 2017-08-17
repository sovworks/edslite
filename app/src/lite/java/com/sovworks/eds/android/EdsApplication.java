package com.sovworks.eds.android;

import android.content.Context;

public class EdsApplication extends EdsApplicationBase
{

    public static void stopProgram(Context context)
    {
        stopProgramBase(context);
        exitProcess();
    }

}
