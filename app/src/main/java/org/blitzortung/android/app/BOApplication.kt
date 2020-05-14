package org.blitzortung.android.app

import androidx.multidex.MultiDexApplication
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.blitzortung.android.dagger.component.DaggerAppComponent
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import javax.inject.Inject

class BOApplication : MultiDexApplication(), HasAndroidInjector {

    @set:Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .serviceModule(ServiceModule())
                .build()
                .inject(this)
    }

    companion object {
        const val WAKE_LOCK_TAG = "boAndroid:WakeLock"
    }
}