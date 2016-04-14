package org.blitzortung.android

import android.app.Application
import android.content.SharedPreferences
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences

class BOApplication: Application() {
    lateinit var locationHandler: LocationHandler
        private set

    lateinit var sharedPreferences: SharedPreferences
        private set

    lateinit var alertHandler: AlertHandler
        private set

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = applicationContext.defaultSharedPreferences

        locationHandler = LocationHandler(applicationContext, sharedPreferences)
    }
}