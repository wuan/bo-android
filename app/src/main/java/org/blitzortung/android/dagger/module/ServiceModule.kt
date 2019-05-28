package org.blitzortung.android.dagger.module

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.PowerManager
import android.os.Vibrator
import dagger.Module
import dagger.Provides
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.util.Period
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.powerManager
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
class ServiceModule @Inject constructor(
        private val application: Application
) {

    @Provides
    @Singleton
    fun provideHandler(): Handler = Handler()

    @Provides
    @Singleton
    fun providePeriod(): Period = Period()
}