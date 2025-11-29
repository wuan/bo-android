package org.blitzortung.android.dagger.module

import android.app.AlarmManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Singleton
import org.blitzortung.android.util.Period

@Module
class ServiceModule
    @Inject
    constructor() {
        @Provides
        @Singleton
        fun provideHandler(): Handler = Handler(Looper.getMainLooper())

        @Provides
        @Singleton
        fun providePeriod(): Period = Period()

        @Provides
        @Singleton
        fun provideAlarmManager(context: Context): AlarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
