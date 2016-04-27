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
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.event.BackgroundModeEvent
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.service.BackgroundDownloadReceiver
import org.blitzortung.android.util.Period
import org.jetbrains.anko.intentFor
import java.util.*

class AppService protected constructor(private val handler: Handler, private val updatePeriod: Period) : Service(), Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    private val binder = DataServiceBinder()

    private val period: Int
        get() = preferences.get(PreferenceKey.QUERY_PERIOD, "60").toInt()

    private val backgroundPeriod: Int
        get() = preferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0").toInt()

    private var lastParameters: Parameters? = null
    private val updateParticipants: Boolean
        get() = preferences.get(PreferenceKey.SHOW_PARTICIPANTS, true)

    var isEnabled: Boolean = false
        private set

    private val dataHandler: DataHandler = BOApplication.dataHandler
    private val alertHandler: AlertHandler = BOApplication.alertHandler
    private val backgroundModeHandler = BOApplication.backgroundModeHandler
    private var isInBackground = true

    private val preferences = BOApplication.sharedPreferences

    private val alertEnabled: Boolean
        get() = preferences.get(PreferenceKey.ALERT_ENABLED, false)

    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    private val wakeLock = BOApplication.wakeLock

    private val dataEventConsumer = { event: DataEvent ->
        if (event is ClearDataEvent) {
            restart()
        } else if (event is ResultEvent) {
            lastParameters = event.parameters
            configureServiceMode()
        }

        releaseWakeLock()
    }

    private val backgroundModeConsumer = {backgroundModeEvent: BackgroundModeEvent ->
        this.isInBackground = backgroundModeEvent.isInBackground

        configureServiceMode()
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
            dataHandler.updateData(setOf(DataChannel.STRIKES))
        }
    }

    override fun onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()")
        super.onCreate()

        backgroundModeHandler.requestUpdates(backgroundModeConsumer)

        preferences.registerOnSharedPreferenceChangeListener(this)

        dataHandler.requestUpdates(dataEventConsumer)
    }

    private var isStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: $startId $intent")

        if(!isStarted) {
            //We won't get Intents to download data anymore,
            //So if we receive an Intent, all we have to do is start the appropriate alarm or start updating the ui
            configureServiceMode()

            isStarted = true
        }

        return Service.START_STICKY
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() after $wakeLock")
            } catch (e: Exception) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent)

        return binder
    }

    override fun run() {
        //Shouldn't be called anymore when we run in background
        if (!isInBackground) {
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

            val statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
            dataHandler.broadcastEvent(StatusEvent(statusString))
            // Schedule the next update
            handler.postDelayed(this, 1000)
        }
    }

    fun restart() {
        configureServiceMode()
        updatePeriod.restart()
    }

    override fun onDestroy() {
        super.onDestroy()

        backgroundModeHandler.removeUpdates(backgroundModeConsumer)

        Log.v(Main.LOG_TAG, "AppService.onDestroy()")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                //Only if we are running in the background, we have to reconfigure our service
                if(isInBackground)
                    configureServiceMode()
            }

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() backgroundPeriod=%d".format(backgroundPeriod))
                discardAlarm()

                configureServiceMode()
            }
        }
    }

    fun configureServiceMode() {
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() entered")
        val logElements = mutableListOf<String>()
        if (isInBackground) {
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() in background")
            if (alertEnabled && backgroundPeriod > 0) {
                logElements += "enable_bg"
                createAlarm()
            } else {
                logElements += "disable_bg"
                alertHandler.unsetAlertListener()
                discardAlarm()
            }
        } else {
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() in foreground")
            discardAlarm()
            if (dataHandler.isRealtime) {
                logElements += "realtime_data"
                if (!isEnabled) {
                    logElements += "restart_handler"
                    isEnabled = true
                    handler.removeCallbacks(this)
                    handler.post(this)
                }
            } else {
                logElements += "historic_data"
                isEnabled = false
                handler.removeCallbacks(this)
                if (lastParameters != null && lastParameters != dataHandler.activeParameters) {
                    logElements += "force_update"
                    dataHandler.updateData()
                }
            }
        }
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() ${logElements.joinToString(", ")}")
    }

    private fun createAlarm() {
        if (alarmManager == null && isInBackground && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, "AppService.createAlarm() with backgroundPeriod=%d".format(backgroundPeriod))
            val intent = intentFor<BackgroundDownloadReceiver>()
            intent.action = RETRIEVE_DATA_ACTION
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

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

    fun alertEvent(): AlertEvent {
        return alertHandler.alertEvent
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
