package com.sovworks.eds.android.filemanager.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;

import com.sovworks.eds.android.R;

import java.util.concurrent.CancellationException;

import io.reactivex.Completable;

public class LoadingProgressDialog
{
    public static Completable createObservable(Context context, boolean isCancellable)
    {
        return Completable.create(emitter -> {
            Dialog dialog = makeProgressDialog(context);
            dialog.setCancelable(isCancellable);
            if(isCancellable)
                dialog.setOnCancelListener((dialogInterface) -> {
                    throw new CancellationException();
                });
            emitter.setCancellable(dialog::dismiss);
            if(!emitter.isDisposed())
                dialog.show();
        });
    }

    private static Dialog makeProgressDialog(Context context)
    {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.loading));
        dialog.setIndeterminate(true);
        return dialog;
    }
}
