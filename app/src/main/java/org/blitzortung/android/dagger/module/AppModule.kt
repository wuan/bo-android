package org.blitzortung.android.dagger.module

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import org.blitzortung.android.app.BOApplication
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule @Inject constructor(
    private val application: Application
) {

    @Provides
    fun provideContext(): Context = application

    @Provides
    fun provideSharedPrefs(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    @Provides
    fun wakeLock(): PowerManager.WakeLock =
        (application.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            BOApplication.WAKE_LOCK_TAG
        )

    @Provides
    @Singleton
    fun packageInfo(): PackageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

    @Provides
    @Named("agentSuffix")
    @Singleton
    fun agentSuffix(packageInfo: PackageInfo): String = "-${packageInfo.versionCode}"

    @Provides
    fun notificationManager(): NotificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun provideVibrator(): Vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
}