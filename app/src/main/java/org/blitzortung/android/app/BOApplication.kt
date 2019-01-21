package org.blitzortung.android.app

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.PowerManager
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.powerManager

class BOApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        sharedPreferences = applicationContext.defaultSharedPreferences

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

        dataHandler = DataHandler(applicationContext, wakeLock, "-${getPackageInfo().versionCode}")

        locationHandler = LocationHandler(applicationContext, sharedPreferences)

        alertHandler = AlertHandler(locationHandler, sharedPreferences, this)
    }

    private fun getPackageInfo(): PackageInfo = packageManager.getPackageInfo(packageName, 0)

    companion object {
        lateinit var locationHandler: LocationHandler
            private set

        lateinit var sharedPreferences: SharedPreferences
            private set

        lateinit var alertHandler: AlertHandler
            private set

        lateinit var dataHandler: DataHandler
            private set

        lateinit var wakeLock: PowerManager.WakeLock


        val WAKE_LOCK_TAG = "boAndroid:WakeLock"
    }
}