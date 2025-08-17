package org.blitzortung.android.dagger.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.blitzortung.android.settings.SettingsFragment

@Module
abstract class SettingsModule {
    @ContributesAndroidInjector
    abstract fun contributeSettingsFragment(): SettingsFragment
}