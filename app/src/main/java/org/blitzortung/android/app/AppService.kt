/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import dagger.android.AndroidInjection
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.ServiceDataHandler
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.util.LogUtil
import org.blitzortung.android.util.isAtLeast
import javax.inject.Inject


class AppService : Service(), OnSharedPreferenceChangeListener {

    init {
        Log.d(Main.LOG_TAG, "AppService() create")
    }

    @Volatile
    private var isEnabled = false

    @Volatile
    private var backgroundPeriod: Int = 0

    @set:Inject
    internal lateinit var dataHandler: ServiceDataHandler

    @set:Inject
    internal lateinit var locationHandler: LocationHandler

    @set:Inject
    internal lateinit var alertHandler: AlertHandler

    @set:Inject
    internal lateinit var preferences: SharedPreferences

    @set:Inject
    internal lateinit var alarmManager: AlarmManager

    @Volatile
    private var alertEnabled: Boolean = false

    @Volatile
    private var pendingIntent: PendingIntent? = null

    @Volatile
    private var lastUpdateTime: Long? = null

    private val dataEventConsumer = { _: DataEvent ->
        //releaseWakeLock()
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        Log.i(Main.LOG_TAG, "AppService.onCreate() ${LogUtil.timestamp}")

        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED, PreferenceKey.BACKGROUND_QUERY_PERIOD)

        dataHandler.requestUpdates(dataEventConsumer)
        dataHandler.requestUpdates(alertHandler.dataEventConsumer)
        locationHandler.requestUpdates(dataHandler.locationEventConsumer)

        isEnabled = true

        configureServiceMode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: $startId $intent")

        if (!locationHandler.backgroundMode) {
            locationHandler.shutdown()
            locationHandler.enableBackgroundMode()
            locationHandler.start()
        }

        if (intent != null && RETRIEVE_DATA_ACTION == intent.action) {
            val currentTimeSeconds = System.currentTimeMillis() / 1000

            val timeDifference = lastUpdateTime?.let {
                currentTimeSeconds - it
            }
            if (timeDifference == null || timeDifference > 0.6 * backgroundPeriod) {
                Log.v(Main.LOG_TAG, "AppService.onStartCommand() with time difference ${timeDifference ?: 0} s")
                lastUpdateTime = currentTimeSeconds
                dataHandler.updateData()
            } else {
                Log.v(
                    Main.LOG_TAG,
                    "AppService.onStartCommand() skip with insufficient time passed: ${currentTimeSeconds - lastUpdateTime!!} s < $backgroundPeriod s"
                )
            }
        } else {
            Log.v(Main.LOG_TAG, "AppService.onStartCommand() intent ${intent?.action}")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(Main.LOG_TAG, "AppService.onBind() $intent")

        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        preferences.unregisterOnSharedPreferenceChangeListener(this)
        isEnabled = false

        discardAlarm()

        locationHandler.removeUpdates(dataHandler.locationEventConsumer)
        dataHandler.removeUpdates(dataEventConsumer)
        dataHandler.removeUpdates(alertHandler.dataEventConsumer)

        Log.v(Main.LOG_TAG, "AppService.onDestroy() ${LogUtil.timestamp}")
    }

    private fun configureBootReceiver(enable: Boolean) {
        val receiver = ComponentName(this, BootReceiver::class.java)

        packageManager.setComponentEnabledSetting(
            receiver,
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun configureServiceMode(forceUpdate: Boolean = false) {
        if (isEnabled) {
            val logElements = mutableListOf<String>()
            if (alertEnabled && backgroundPeriod > 0) {
                logElements += "enable_bg"
                if (!locationHandler.backgroundMode) {
                    locationHandler.shutdown()
                    locationHandler.enableBackgroundMode()
                }
                locationHandler.start()
                if (forceUpdate) {
                    discardAlarm()
                }
                createAlarm()
                configureBootReceiver(backgroundPeriod > 0)
            } else {
                logElements += "disable_bg"
                locationHandler.shutdown()
                discardAlarm()
            }
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() ${logElements.joinToString(", ")}")
        } else {
            discardAlarm()
        }
    }

    private fun createAlarm() {
        synchronized(alarmManager) {
            if (backgroundPeriod > 0 && pendingIntent == null) {
                Log.v(Main.LOG_TAG, "AppService.createAlarm() with backgroundPeriod=%d".format(backgroundPeriod))
                val intent = Intent(this, AppService::class.java)
                intent.action = RETRIEVE_DATA_ACTION
                val flags = if (isAtLeast(Build.VERSION_CODES.M)) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
                val pendingIntent = PendingIntent.getService(this, 0, intent, flags)
                this.pendingIntent = pendingIntent

                val period = (backgroundPeriod * 1000).toLong()
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, period, period, pendingIntent)
            }
        }
    }

    private fun discardAlarm() {
        synchronized(alarmManager) {
            pendingIntent?.let { pendingIntent ->
                Log.v(Main.LOG_TAG, "AppService.discardAlarm()")
                try {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                } finally {
                    this.pendingIntent = null
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                alertEnabled = sharedPreferences.get(key, false)
                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() alertEnabled=$alertEnabled")

                configureServiceMode()
            }

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                backgroundPeriod = Integer.parseInt(sharedPreferences.get(key, "0"))
                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() backgroundPeriod=$backgroundPeriod")

                configureServiceMode(true)
            }
            else -> {}
        }
    }

    companion object {
        const val RETRIEVE_DATA_ACTION = "retrieveData"
    }
}
