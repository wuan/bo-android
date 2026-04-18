package org.blitzortung.android.dagger.component

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton
import org.blitzortung.android.alert.handler.AlertDataHandler
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.dagger.module.ActivityBindingModule
import org.blitzortung.android.dagger.module.AppModule
import org.blitzortung.android.dagger.module.ServiceModule
import org.blitzortung.android.dagger.module.SettingsModule
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ServiceModule::class,
        SettingsModule::class,
        ActivityBindingModule::class,
    ],
)
interface AppComponent : AndroidInjector<BOApplication> {
    fun strikeColorHandler(): StrikeColorHandler
    fun alertHandler(): AlertHandler
    fun alertDataHandler(): AlertDataHandler
    fun locationHandler(): LocationHandler
    fun jsonRpcDataProvider(): JsonRpcDataProvider
}
