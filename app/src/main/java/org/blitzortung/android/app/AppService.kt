package org.blitzortung.android.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.util.Period
import java.util.*

class AppService protected constructor(private val handler: Handler, private val updatePeriod: Period) : Service(), Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    private val binder = DataServiceBinder()

    internal var alertConsumerContainer: ConsumerContainer<AlertEvent> = object : ConsumerContainer<AlertEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "added first alert consumer")
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "removed last alert consumer")
        }
    }

    private val alertEventConsumer = { event: AlertEvent -> alertConsumerContainer.storeAndBroadcast(event) }

    var period: Int = 0
        private set
    var backgroundPeriod: Int = 0
        private set

    private var lastParameters: Parameters? = null
    private var updateParticipants: Boolean = false
    var isEnabled: Boolean = false
        private set

    private lateinit var dataHandler: DataHandler
    private lateinit var locationHandler: LocationHandler
    public lateinit var alertHandler: AlertHandler

    private var alertEnabled: Boolean = false
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null

    internal var dataConsumerContainer: ConsumerContainer<DataEvent> = object : ConsumerContainer<DataEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "added first data consumer")
            configureServiceMode()
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "removed last data consumer")
            configureServiceMode()
        }
    }
    private var wakeLock: PowerManager.WakeLock? = null

    private val dataEventConsumer = { event: DataEvent ->
        if (!dataConsumerContainer.isEmpty) {
            dataConsumerContainer.storeAndBroadcast(event)
        }

        if (alertEnabled) {
            alertHandler.dataEventConsumer.invoke(event)
        }

        if (event is ClearDataEvent) {
            restart()
        } else if (event is ResultEvent) {
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
    }

    val lastUpdate: Long
        get() = updatePeriod.lastUpdateTime

    fun reloadData() {
        if (isEnabled) {
            restart()
        } else {
            dataHandler.updateData(setOf(DataChannel.STRIKES))
        }
    }

    fun dataHandler(): DataHandler {
        return dataHandler
    }

    fun addDataConsumer(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeDataConsumer(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    fun addAlertConsumer(alertConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.addConsumer(alertConsumer)
    }

    fun removeAlertListener(alertConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.removeConsumer(alertConsumer)
    }

    fun removeLocationConsumer(locationConsumer: (LocationEvent) -> Unit) {
        locationHandler.removeUpdates(locationConsumer)
    }

    fun addLocationConsumer(locationListener: (LocationEvent) -> Unit) {
        locationHandler.requestUpdates(locationListener)
    }

    fun updateLocationHandler(preferences: SharedPreferences) {
        locationHandler.update(preferences)
    }

    override fun onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()")
        super.onCreate()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        if (wakeLock == null) {
            Log.d(Main.LOG_TAG, "AppService.onCreate() create wakelock")
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }

        dataHandler = DataHandler(wakeLock!!, preferences, packageInfo)
        dataHandler.setDataConsumer(dataEventConsumer)

        locationHandler = LocationHandler(this, preferences)
        alertHandler = AlertHandler(locationHandler, preferences, this)

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED)
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: $startId $intent")

        if (intent != null && RETRIEVE_DATA_ACTION == intent.action) {
            acquireWakeLock()

            Log.v(Main.LOG_TAG, "AppService.onStartCommand() acquired wake lock " + wakeLock!!)

            isEnabled = false
            handler.removeCallbacks(this)
            handler.post(this)
        }

        return Service.START_STICKY
    }

    private fun acquireWakeLock() {
        wakeLock!!.acquire()
    }

    fun releaseWakeLock() {
        if (wakeLock!!.isHeld) {
            try {
                wakeLock!!.release()
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() " + wakeLock!!)
            } catch (e: RuntimeException) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e)
            }

        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent)

        return binder
    }

    override fun run() {
        if (dataConsumerContainer.isEmpty) {
            if (alertEnabled && backgroundPeriod > 0) {
                Log.v(Main.LOG_TAG, "AppService.run() in background")

                dataHandler.updateDatainBackground()
            } else {
                isEnabled = false
                handler.removeCallbacks(this)
            }
        } else {
            releaseWakeLock()

            val currentTime = Period.currentTime
            val updateTargets = HashSet<DataChannel>()

            if (updatePeriod.shouldUpdate(currentTime, period)) {
                updatePeriod.lastUpdateTime = currentTime
                updateTargets.add(DataChannel.STRIKES)

                if (updateParticipants && updatePeriod.isNthUpdate(10)) {
                    updateTargets.add(DataChannel.PARTICIPANTS)
                }
            }

            if (!updateTargets.isEmpty()) {
                dataHandler.updateData(updateTargets)
            }

            val statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
            dataConsumerContainer.broadcast(StatusEvent(statusString))
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
        Log.v(Main.LOG_TAG, "AppService.onDestroy()")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                alertEnabled = sharedPreferences.getBoolean(key.toString(), false)

                configureServiceMode()
            }

            PreferenceKey.QUERY_PERIOD -> period = Integer.parseInt(sharedPreferences.getString(key.toString(), "60"))

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                backgroundPeriod = Integer.parseInt(sharedPreferences.getString(key.toString(), "0"))

                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() backgroundPeriod=%d".format(backgroundPeriod))
                discardAlarm()
                configureServiceMode()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> updateParticipants = sharedPreferences.getBoolean(key.toString(), true)
        }
    }

    private fun configureServiceMode() {
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() entered")
        val backgroundOperation = dataConsumerContainer.isEmpty
        if (backgroundOperation) {
            if (alertEnabled && backgroundPeriod > 0) {
                locationHandler.enableBackgroundMode()
                alertHandler.alertEventConsumer = alertEventConsumer
                alertHandler.reconfigureLocationHandler()
                createAlarm()
            } else {
                alertHandler.unsetAlertListener()
                discardAlarm()
            }
        } else {
            discardAlarm()
            if (dataHandler.isRealtime) {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() realtime data")
                if (!isEnabled) {
                    isEnabled = true
                    handler.removeCallbacks(this)
                    handler.post(this)
                }
            } else {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() historic data")
                isEnabled = false
                handler.removeCallbacks(this)
                if (lastParameters != null && lastParameters != dataHandler.activeParameters) {
                    dataHandler.updateData()
                }
            }
            locationHandler.disableBackgroundMode()
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() set alert event consumer")
            alertHandler.alertEventConsumer = alertEventConsumer
        }
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() done")
    }

    private fun createAlarm() {
        if (alarmManager == null && dataConsumerContainer.isEmpty && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, "AppService.createAlarm() with backgroundPeriod=%d".format(backgroundPeriod))
            val intent = Intent(this, AppService::class.java)
            intent.setAction(RETRIEVE_DATA_ACTION)
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
        val alarmManager = alarmManager
        if (alarmManager != null) {
            Log.v(Main.LOG_TAG, "AppService.discardAlarm()")
            alarmManager.cancel(pendingIntent)
            pendingIntent!!.cancel()

            pendingIntent = null
            this.alarmManager = null
        }
    }

    public fun alertEvent(): AlertEvent? {
        return alertHandler.alertEvent()
    }

    private val packageInfo: PackageInfo
        get() {
            try {
                return packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                throw IllegalStateException(e)
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
        val WAKE_LOCK_TAG = "boAndroidWakeLock"
    }
}
