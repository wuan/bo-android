package org.blitzortung.android.dagger.module

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import org.blitzortung.android.app.BOApplication

@Module
class AppModule
    @Inject
    constructor(
        private val application: Application,
    ) {
        @Provides
        fun provideContext(): Context = application

        @Provides
        fun provideSharedPrefs(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

        @Provides
        fun wakeLock(): PowerManager.WakeLock =
            (application.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                BOApplication.WAKE_LOCK_TAG,
            )

        @Provides
        @Singleton
        fun packageInfo(): PackageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

        @Provides
        @Named("agentSuffix")
        @Singleton
        fun agentSuffix(packageInfo: PackageInfo): String = "-${PackageInfoCompat.getLongVersionCode(packageInfo)}"

        @Provides
        fun notificationManager(): NotificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        @Suppress("DEPRECATION", "kotlin:S1874")
        @Provides
        fun provideVibrator(): Vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
    }
