package org.blitzortung.android.dagger.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.blitzortung.android.settings.SettingsFragment

@Module
interface SettingsModule {
    @ContributesAndroidInjector
    fun contributeSettingsFragment(): SettingsFragment
}
