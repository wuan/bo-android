package org.blitzortung.android.dagger.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.Main

@Module
abstract class ActivityBindingModule {

    @ContributesAndroidInjector
    abstract fun contributeMainActivityInjector(): Main

    @ContributesAndroidInjector
    abstract fun contributesAppServiceInjector(): AppService
}