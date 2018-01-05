package com.sovworks.eds.android.helpers;


import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.dialogs.MasterPasswordDialog;
import com.sovworks.eds.android.errors.UserException;
import com.sovworks.eds.android.filemanager.fragments.ExtStorageWritePermisisonCheckFragment;
import com.trello.rxlifecycle2.components.RxActivity;

import java.util.concurrent.CancellationException;

import io.reactivex.CompletableEmitter;

public class AppInitHelper extends AppInitHelperBase
{
    AppInitHelper(RxActivity activity, CompletableEmitter emitter)
    {
        super(activity, emitter);
    }

    void startInitSequence()
    {
        MasterPasswordDialog.getObservable(_activity).
                flatMapCompletable(isValidPassword ->
                {
                    if (isValidPassword)
                        return ExtStorageWritePermisisonCheckFragment.getObservable(_activity);
                    throw new UserException(_activity, R.string.invalid_master_password);
                }).

                compose(_activity.bindToLifecycle()).
                subscribe(() -> {
                    convertLegacySettings();
                    _initFinished.onComplete();
                }, err ->
                {
                    if(!(err instanceof CancellationException))
                        Logger.log(err);
                });
    }
}
