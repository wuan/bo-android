package org.blitzortung.android.dagger.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.Main

@Module
interface ActivityBindingModule {

    @ContributesAndroidInjector
    fun contributeMainActivityInjector(): Main

    @ContributesAndroidInjector
    fun contributesAppServiceInjector(): AppService
}