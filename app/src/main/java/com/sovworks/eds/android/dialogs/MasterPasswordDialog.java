package com.sovworks.eds.android.dialogs;

import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import com.sovworks.eds.android.EdsApplication;
import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.settings.GlobalConfig;
import com.sovworks.eds.settings.Settings;
import com.trello.rxlifecycle2.components.RxActivity;

import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static com.sovworks.eds.android.settings.UserSettingsCommon.SETTINGS_PROTECTION_KEY_CHECK;

public class MasterPasswordDialog extends PasswordDialog
{
    public static final String TAG = "com.sovworks.eds.android.dialogs.MasterPasswordDialog";
    public static final String ARG_IS_OBSERVABLE = "com.sovworks.eds.android.IS_OBSERVABLE";

    public static Single<Boolean> getObservable(RxActivity activity)
    {
        UserSettings s = UserSettings.getSettings(activity);
        long curTime = SystemClock.elapsedRealtime();
        long lastActTime = EdsApplication.getLastActivityTime();
        if(curTime - lastActTime > GlobalConfig.CLEAR_MASTER_PASS_INACTIVITY_TIMEOUT)
        {
            Logger.debug("Clearing settings protection key");
            EdsApplication.clearMasterPassword();
            s.clearSettingsProtectionKey();
        }
        EdsApplication.updateLastActivityTime();
        try
        {
            s.getSettingsProtectionKey();
        }
        catch(Settings.InvalidSettingsPassword e)
        {
            FragmentManager fm = activity.getFragmentManager();
            MasterPasswordDialog mpd = (MasterPasswordDialog) fm.findFragmentByTag(TAG);
            if(mpd == null)
            {
                MasterPasswordDialog masterPasswordDialog = new MasterPasswordDialog();
                Bundle args = new Bundle();
                args.putBoolean(ARG_IS_OBSERVABLE, true);
                masterPasswordDialog.setArguments(args);
                return masterPasswordDialog.
                        _passwordCheckSubject.
                        doOnSubscribe(subscription ->
                                masterPasswordDialog.show(fm, TAG)).
                        firstOrError();
            }
            return mpd.
                    _passwordCheckSubject.
                    firstOrError();
        }
        return Single.just(true);
    }

    public static boolean checkSettingsKey(Context context)
    {
        UserSettings settings = UserSettings.getSettings(context);
        try
        {
            String check = settings.getProtectedString(SETTINGS_PROTECTION_KEY_CHECK);
            if(check == null)
                settings.saveSettingsProtectionKey();
            EdsApplication.updateLastActivityTime();
            return true;
        }
        catch (Settings.InvalidSettingsPassword ignored)
        {
            settings.clearSettingsProtectionKey();
            Toast.makeText(context, R.string.invalid_master_password, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public static boolean checkMasterPasswordIsSet(Context context, FragmentManager fm, String receiverFragmentTag)
    {
        UserSettings s = UserSettings.getSettings(context);
        long curTime = SystemClock.elapsedRealtime();
        long lastActTime = EdsApplication.getLastActivityTime();
        if(curTime - lastActTime > GlobalConfig.CLEAR_MASTER_PASS_INACTIVITY_TIMEOUT)
        {
            Logger.debug("Clearing settings protection key");
            EdsApplication.clearMasterPassword();
            s.clearSettingsProtectionKey();
        }
        EdsApplication.updateLastActivityTime();
        try
        {
            s.getSettingsProtectionKey();
        }
        catch(Settings.InvalidSettingsPassword e)
        {
            MasterPasswordDialog mpd = new MasterPasswordDialog();
            if(receiverFragmentTag != null)
            {
                Bundle args = new Bundle();
                args.putString(ARG_RECEIVER_FRAGMENT_TAG, receiverFragmentTag);
                mpd.setArguments(args);
            }
            mpd.show(fm, TAG);
            return false;
        }
        return true;
    }

    @Override
    protected String loadLabel()
    {
        return getString(R.string.enter_master_password);
    }

    @Override
    public boolean hasPassword()
    {
        return true;
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        EdsApplication.clearMasterPassword();
        super.onCancel(dialog);
    }

    @Override
    protected void onPasswordEntered()
    {
        EdsApplication.setMasterPassword(new SecureBuffer(getPassword()));
        if(checkSettingsKey(getActivity()))
        {
            Bundle args = getArguments();
            if(args!=null && args.getBoolean(ARG_IS_OBSERVABLE))
                _passwordCheckSubject.onNext(true);
            else
                super.onPasswordEntered();
        }
        else
            onPasswordNotEntered();
    }

    @Override
    protected void onPasswordNotEntered()
    {
        Bundle args = getArguments();
        if(args!=null && args.getBoolean(ARG_IS_OBSERVABLE))
            _passwordCheckSubject.onNext(false);
        else
            super.onPasswordNotEntered();
    }

    private final Subject<Boolean> _passwordCheckSubject = BehaviorSubject.create();
}
