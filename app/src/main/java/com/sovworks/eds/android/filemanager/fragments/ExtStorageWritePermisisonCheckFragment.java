package com.sovworks.eds.android.filemanager.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.sovworks.eds.android.dialogs.AskPrimaryStoragePermissionDialog;
import com.trello.rxlifecycle2.android.FragmentEvent;
import com.trello.rxlifecycle2.components.RxActivity;
import com.trello.rxlifecycle2.components.RxFragment;

import io.reactivex.Completable;
import io.reactivex.subjects.CompletableSubject;


public class ExtStorageWritePermisisonCheckFragment extends RxFragment
{
    public static final String TAG = "com.sovworks.eds.android.filemanager.fragments.ExtStorageWritePermisisonCheckFragment";

    public static Completable getObservable(RxActivity activity)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
        )
            return Completable.complete();

        FragmentManager fm = activity.getFragmentManager();
        ExtStorageWritePermisisonCheckFragment f = (ExtStorageWritePermisisonCheckFragment) fm.findFragmentByTag(TAG);
        if(f == null)
        {
            f = new ExtStorageWritePermisisonCheckFragment();
            activity.getFragmentManager().beginTransaction().add(f, TAG).commit();
        }
        return f._extStoragePermissionCheckSubject;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        lifecycle().
                filter(event -> event == FragmentEvent.RESUME).
                firstElement().
                subscribe((event) -> requestExtStoragePermission());
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestExtStoragePermission()
    {
        requestPermissions(
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_EXT_STORAGE_PERMISSIONS);
    }

    public void cancelExtStoragePermissionRequest()
    {
        _extStoragePermissionCheckSubject.onComplete();
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_EXT_STORAGE_PERMISSIONS)
        {
            if((grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) ||
                    !requestExtStoragePermissionWithRationale())
            {
                _extStoragePermissionCheckSubject.onComplete();
                getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            }

        }
    }

    private static final int REQUEST_EXT_STORAGE_PERMISSIONS = 1;
    private final CompletableSubject _extStoragePermissionCheckSubject = CompletableSubject.create();


    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestExtStoragePermissionWithRationale()
    {
        if (shouldShowRequestPermissionRationale(
                Manifest.permission.READ_EXTERNAL_STORAGE)
                || shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            AskPrimaryStoragePermissionDialog.showDialog(getFragmentManager());
            return true;
        }
        return false;
    }
}
