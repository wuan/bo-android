package org.blitzortung.android.dagger.component

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.dagger.module.ActivityBindingModule
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import org.blitzortung.android.dagger.module.SettingsModule
import org.blitzortung.android.dagger.module.ViewModelModule

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ServiceModule::class,
        SettingsModule::class,
        ActivityBindingModule::class,
        ViewModelModule::class,
    ],
)
interface AppComponent : AndroidInjector<BOApplication>
