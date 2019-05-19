package org.blitzortung.android.app

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.pm.PackageInfo
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.blitzortung.android.dagger.component.DaggerAppComponent
import org.blitzortung.android.dagger.module.AppModule
import javax.inject.Inject

class BOApplication : Application(), HasActivityInjector, HasServiceInjector {

    @Inject
    lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector

    @Inject
    lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
                .inject(this)
    }

    companion object {
        val WAKE_LOCK_TAG = "boAndroid:WakeLock"
    }
}