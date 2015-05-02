package org.blitzortung.android.app.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class VersionComponent {

    private State state;
    private String versionName;

    public enum State {
        FIRST_RUN, FIRST_RUN_AFTER_UPDATE, NO_UPDATE
    }

    private static final String CONFIGURED_VERSION_CODE = "configured_version_code";

    private int configuredVersionCode;
    private int currentVersionCode;

    public VersionComponent(final Context context) {
        updatePackageInfo(context);
        updateVersionStatus(context);
    }

    private void updateVersionStatus(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        configuredVersionCode = preferences.getInt(CONFIGURED_VERSION_CODE, -1);

        if (configuredVersionCode == -1) {
            state = State.FIRST_RUN;
        } else {
            if (configuredVersionCode < currentVersionCode) {
                state = State.FIRST_RUN_AFTER_UPDATE;
            } else {
                state = State.NO_UPDATE;
            }
        }
    }

    private void updatePackageInfo(final Context context) {
        final PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }

        currentVersionCode = pInfo.versionCode;
        versionName = pInfo.versionName;
    }

    public State getState() {
        return state;
    }

    public int getConfiguredVersionCode() {
        return configuredVersionCode;
    }

    public int getVersionCode() {
        return currentVersionCode;
    }

    public String getVersionName() {
        return versionName;
    }
}
