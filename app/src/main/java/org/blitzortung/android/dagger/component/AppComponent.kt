package org.blitzortung.android.dagger.component

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.dagger.module.ActivityBindingModule
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AppModule::class,
        ServiceModule::class,
        ActivityBindingModule::class
    ]
)
interface AppComponent : AndroidInjector<BOApplication>

