package org.blitzortung.android.dagger.module

import android.app.Application
import android.os.Handler
import dagger.Module
import dagger.Provides
import org.blitzortung.android.util.Period
import javax.inject.Inject
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