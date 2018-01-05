package com.sovworks.eds.android.helpers;

import android.annotation.SuppressLint;

import com.sovworks.eds.android.locations.ContainerBasedLocation;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.locations.Location;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.settings.Settings;
import com.trello.rxlifecycle2.components.RxActivity;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;

import static com.sovworks.eds.android.settings.UserSettingsCommon.CURRENT_SETTINGS_VERSION;

public abstract class AppInitHelperBase
{
    public static Completable createObservable(RxActivity activity)
    {
        return Completable.create(emitter -> {
            AppInitHelper initHelper = new AppInitHelper(activity, emitter);
            initHelper.startInitSequence();
        });
    }

    AppInitHelperBase(RxActivity activity, CompletableEmitter emitter)
    {
        _activity = activity;
        _settings = UserSettings.getSettings(activity);
        _initFinished = emitter;
    }

    final RxActivity _activity;
    protected final UserSettings _settings;
    final CompletableEmitter _initFinished;

    @SuppressLint("ApplySharedPref")
    void convertLegacySettings()
    {
        int curSettingsVersion = _settings.getCurrentSettingsVersion();
        if(curSettingsVersion >= Settings.VERSION)
            return;

        if(curSettingsVersion < 0)
        {
            if(_settings.getLastViewedPromoVersion() > 160)
                _settings.getSharedPreferences().edit().putInt(CURRENT_SETTINGS_VERSION, Settings.VERSION).commit();
            else
                curSettingsVersion = 1;
        }

        if(curSettingsVersion < 2)
            updateSettingsV2();
        if(curSettingsVersion < 3)
            updateSettingsV3();
        _settings.
                getSharedPreferences().
                edit().
                putInt(UserSettings.CURRENT_SETTINGS_VERSION, Settings.VERSION).
                commit();
    }

    protected void updateSettingsV2()
    {
        makeContainersVisible();
    }

    private void updateSettingsV3()
    {
        convertEncAlgName();
    }

    private void convertEncAlgName()
    {
        LocationsManager lm = LocationsManager.getLocationsManager(_activity);
        for (Location l : lm.getLoadedLocations(false))
            if (l instanceof ContainerBasedLocation)
            {
                ContainerBasedLocation.ExternalSettings externalSettings = ((ContainerBasedLocation)l).getExternalSettings();
                String encAlg = externalSettings.getEncEngineName();
                if(encAlg == null)
                    continue;
                switch (encAlg)
                {
                    case "aes-twofish-serpent-xts-plain64":
                        externalSettings.setEncEngineName("serpent-twofish-aes-xts-plain64");
                        l.saveExternalSettings();
                        break;
                    case "serpent-twofish-aes-xts-plain64":
                        externalSettings.setEncEngineName("aes-twofish-serpent-xts-plain64");
                        l.saveExternalSettings();
                        break;
                    case "twofish-aes-xts-plain64":
                        externalSettings.setEncEngineName("aes-twofish-xts-plain64");
                        l.saveExternalSettings();
                        break;
                    case "aes-serpent-xts-plain64":
                        externalSettings.setEncEngineName("serpent-aes-xts-plain64");
                        l.saveExternalSettings();
                        break;
                    case "serpent-twofish-xts-plain64":
                        externalSettings.setEncEngineName("twofish-serpent-xts-plain64");
                        l.saveExternalSettings();
                        break;
                }
            }
    }


    private void makeContainersVisible()
    {
        LocationsManager lm = LocationsManager.getLocationsManager(_activity);
        for (Location l : lm.getLoadedLocations(false))
            if (l instanceof ContainerBasedLocation && !l.getExternalSettings().isVisibleToUser())
            {
                l.getExternalSettings().setVisibleToUser(true);
                l.saveExternalSettings();
            }
    }
}
