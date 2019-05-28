package org.blitzortung.android.app

import android.app.Activity
import android.app.Application
import android.app.Service
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.blitzortung.android.dagger.component.DaggerAppComponent
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import javax.inject.Inject

class BOApplication : Application(), HasActivityInjector, HasServiceInjector {

    @set:Inject
    lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector

    @set:Inject
    lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .serviceModule(ServiceModule(this))
                .build()
                .inject(this)
    }

    companion object {
        val WAKE_LOCK_TAG = "boAndroid:WakeLock"
    }
}