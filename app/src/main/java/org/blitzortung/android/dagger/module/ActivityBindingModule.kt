package org.blitzortung.android.dagger.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.WidgetProvider
import org.blitzortung.android.map.MapFragment

@Module
interface ActivityBindingModule {

    @ContributesAndroidInjector
    fun contributeMainActivityInjector(): Main

    @ContributesAndroidInjector
    fun contributesAppServiceInjector(): AppService

    @ContributesAndroidInjector
    fun contributesWidgetProvider(): WidgetProvider

    @ContributesAndroidInjector
    fun contributesMapFragment(): MapFragment
}
