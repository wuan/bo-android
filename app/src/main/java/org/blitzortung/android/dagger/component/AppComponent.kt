package org.blitzortung.android.dagger.component

import org.blitzortung.android.dagger.module.ActivityBindingModule
import org.blitzortung.android.dagger.module.AppModule
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import org.blitzortung.android.app.BOApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, AppModule::class, ActivityBindingModule::class])
interface AppComponent : AndroidInjector<BOApplication>

