package org.blitzortung.android.dagger.module

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.PowerManager
import android.os.Vibrator
import dagger.Module
import dagger.Provides
import org.blitzortung.android.app.BOApplication
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.powerManager
import javax.inject.Inject
import javax.inject.Named

@Module
class AppModule @Inject constructor(private val application: Application) {

    @Provides
    fun provideContext(): Context = application

    @Provides
    fun provideSharedPrefs(): SharedPreferences = application.defaultSharedPreferences

    @Provides
    fun wakeLock() : PowerManager.WakeLock = application.powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BOApplication.WAKE_LOCK_TAG)

    @Provides
    fun packageInfo(): PackageInfo = application.packageManager.getPackageInfo(application.packageName, 0)

    @Provides
    @Named("agentSuffix")
    fun agentSuffix(packageInfo: PackageInfo): String = "-${packageInfo.versionCode}"

    @Provides
    fun provideVibrator(): Vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
}