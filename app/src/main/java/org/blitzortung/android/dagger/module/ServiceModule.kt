package org.blitzortung.android.dagger.module

import android.app.AlarmManager
import android.content.Context
import android.os.Handler
import dagger.Module
import dagger.Provides
import org.blitzortung.android.util.Period
import javax.inject.Inject
import javax.inject.Singleton

@Module
class ServiceModule @Inject constructor(
) {
    @Provides
    @Singleton
    fun provideHandler(): Handler = Handler()

    @Provides
    @Singleton
    fun providePeriod(): Period = Period()

    @Provides
    @Singleton
    fun provideAlarmManager(context: Context): AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}