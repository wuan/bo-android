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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.util.LogUtil
import org.blitzortung.android.util.Period
import java.util.*

class AppService protected constructor(private val handler: Handler, private val updatePeriod: Period) : Service(), Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    private val binder = DataServiceBinder()

    var period: Int = 0
        private set
    var backgroundPeriod: Int = 0
        private set

    private var lastParameters: Parameters? = null
    private var updateParticipants: Boolean = false
    var isEnabled: Boolean = false
        private set

    var showHistoricData: Boolean = false
        private set

    private val dataHandler: DataHandler = BOApplication.dataHandler
    private val locationHandler: LocationHandler = BOApplication.locationHandler
    private val alertHandler: AlertHandler = BOApplication.alertHandler

    private val preferences = BOApplication.sharedPreferences

    private var alertEnabled: Boolean = false
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    private val wakeLock = BOApplication.wakeLock

    private val dataEventConsumer = { event: DataEvent ->
        if (event is ResultEvent) {
            lastParameters = event.parameters
            configureServiceMode()
        }

        releaseWakeLock()
    }

    @SuppressWarnings("UnusedDeclaration")
    constructor() : this(Handler(), Period()) {
        Log.d(Main.LOG_TAG, "AppService() created with new handler")
    }

    init {
        Log.d(Main.LOG_TAG, "AppService() create")
        AppService.instance = this
    }

    fun reloadData() {
        if (isEnabled) {
            restart()
        } else {
            dataHandler.updateData()
        }
    }

    override fun onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate() ${LogUtil.timestamp}")
        super.onCreate()

        preferences.registerOnSharedPreferenceChangeListener(this)

        dataHandler.requestInternalUpdates(dataEventConsumer)

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED)
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: $startId $intent")

        if (intent != null && RETRIEVE_DATA_ACTION == intent.action) {
            if (acquireWakeLock()) {
                Log.v(Main.LOG_TAG, "AppService.onStartCommand() ${LogUtil.timestamp} with wake lock " + wakeLock)

                disableHandler()
                handler.post(this)
            } else {
                Log.v(Main.LOG_TAG, "AppService.onStartCommand() ${LogUtil.timestamp} skip with held wake lock " + wakeLock)
            }
        }

        return Service.START_STICKY
    }

    private fun acquireWakeLock(): Boolean {
        synchronized(wakeLock) {
            if (!wakeLock.isHeld) {
                Log.v(Main.LOG_TAG, "AppService.acquireWakeLock() before: $wakeLock")
                wakeLock.acquire()
                return true
            } else {
                Log.v(Main.LOG_TAG, "AppService.acquireWakeLock() skip")
                return false
            }
        }
    }

    fun releaseWakeLock() {
        synchronized(wakeLock) {
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() after $wakeLock")
                } catch (e: Exception) {
                    Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e)
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent)

        return binder
    }

    override fun run() {
        if (dataHandler.hasConsumers) {
            if (alertEnabled && backgroundPeriod > 0) {
                Log.v(Main.LOG_TAG, "AppService.run() in background")

                dataHandler.updateDataInBackground()
            } else {
                disableHandler()
            }
        } else {
            releaseWakeLock()

            val currentTime = Period.currentTime
            val updateTargets = HashSet<DataChannel>()

            if (updatePeriod.shouldUpdate(currentTime, period)) {
                updateTargets.add(DataChannel.STRIKES)

                if (updateParticipants && updatePeriod.isNthUpdate(10)) {
                    updateTargets.add(DataChannel.PARTICIPANTS)
                }
            }

            if (!updateTargets.isEmpty()) {
                dataHandler.updateData(updateTargets)
            }

            if (!showHistoricData) {
                val statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
                dataHandler.broadcastEvent(StatusEvent(statusString))
                // Schedule the next update
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun restart() {
        Log.v(Main.LOG_TAG, "AppService.restart() ${LogUtil.timestamp}")
        configureServiceMode()
        updatePeriod.restart()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(Main.LOG_TAG, "AppService.onDestroy() ${LogUtil.timestamp}")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                alertEnabled = sharedPreferences.get(key, false)

                configureServiceMode()
            }

            PreferenceKey.QUERY_PERIOD -> period = Integer.parseInt(sharedPreferences.get(key, "60"))

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                backgroundPeriod = Integer.parseInt(sharedPreferences.get(key, "0"))

                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() backgroundPeriod=%d".format(backgroundPeriod))
                discardAlarm()
                configureServiceMode()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> updateParticipants = sharedPreferences.get(key, true)
        }
    }

    fun configureServiceMode() {
        val backgroundOperation = dataHandler.hasConsumers
        val logElements = mutableListOf<String>()
        if (backgroundOperation) {
            if (alertEnabled && backgroundPeriod > 0) {
                logElements += "enable_bg"
                locationHandler.enableBackgroundMode()
                disableHandler()
                createAlarm()
            } else {
                logElements += "disable_bg"
                alertHandler.unsetAlertListener()
                locationHandler.shutdown()
                discardAlarm()
            }
        } else {
            discardAlarm()
            if (dataHandler.isRealtime) {
                logElements += "realtime_data"
                if (!isEnabled) {
                    logElements += "restart_handler"
                    isEnabled = true
                    showHistoricData = false
                    handler.removeCallbacks(this)
                    handler.post(this)
                }
            } else {
                logElements += "historic_data"
                isEnabled = false
                disableHandler()
                if (lastParameters != null && lastParameters != dataHandler.activeParameters) {
                    logElements += "force_update"
                    dataHandler.updateData()
                }
            }
            locationHandler.start()
            locationHandler.disableBackgroundMode()
        }
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() ${logElements.joinToString(", ")}")
    }

    private fun disableHandler() {
        isEnabled = false
        handler.removeCallbacks(this)
    }

    private fun createAlarm() {
        if (alarmManager == null && dataHandler.hasConsumers && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, "AppService.createAlarm() with backgroundPeriod=%d".format(backgroundPeriod))
            val intent = Intent(this, AppService::class.java)
            intent.action = RETRIEVE_DATA_ACTION
            pendingIntent = PendingIntent.getService(this, 0, intent, 0)

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, (backgroundPeriod * 1000).toLong(), pendingIntent)
            } else {
                Log.e(Main.LOG_TAG, "AppService.createAlarm() failed")
            }
            this.alarmManager = alarmManager
        }
    }

    private fun discardAlarm() {
        alarmManager?.let { alarmManager ->
            Log.v(Main.LOG_TAG, "AppService.discardAlarm()")
            alarmManager.cancel(pendingIntent)
            pendingIntent!!.cancel()

            pendingIntent = null
            this.alarmManager = null
        }
    }

    inner class DataServiceBinder : Binder() {
        internal val service: AppService
            get() {
                Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + this@AppService)
                return this@AppService
            }
    }

    companion object {
        val RETRIEVE_DATA_ACTION = "retrieveData"

        var instance: AppService? = null
            private set
    }
}
