package org.blitzortung.android.app

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import org.blitzortung.android.dagger.component.AppComponent
import org.blitzortung.android.dagger.component.DaggerAppComponent
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule

class BOApplication : Application(), HasAndroidInjector {
    @set:Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    lateinit var component: AppComponent
        private set

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()

        component = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .serviceModule(ServiceModule())
            .build()

        component.inject(this)
    }

    companion object {
        const val WAKE_LOCK_TAG = "boAndroid:WakeLock"
    }
}
