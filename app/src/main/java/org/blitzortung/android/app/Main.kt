/*

   Copyright 2015, 2016 Andreas Würl

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

import android.Manifest
import android.annotation.TargetApi
import android.app.Dialog
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.maps.GeoPoint
import kotlinx.android.synthetic.main.map_overlay.*
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.app.controller.ButtonColumnHandler
import org.blitzortung.android.app.controller.HistoryController
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.components.StatusComponent
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.event.status.*
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.dialogs.*
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.OwnMapActivity
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.map.overlay.FadeOverlay
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.ParticipantsOverlay
import org.blitzortung.android.map.overlay.StrikesOverlay
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.TabletAwareView
import org.blitzortung.android.util.isAtLeast
import org.jetbrains.anko.startActivity
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.main.mapview as prod_mapview
import kotlinx.android.synthetic.main.main_debug.mapview as debug_mapview

class Main : OwnMapActivity(), OnSharedPreferenceChangeListener {

    private lateinit var statusComponent: StatusComponent
    private lateinit var versionComponent: VersionComponent
    private lateinit var parametersComponent: ParametersComponent

    private lateinit var strikesOverlay: StrikesOverlay
    private lateinit var participantsOverlay: ParticipantsOverlay
    private lateinit var ownLocationOverlay: OwnLocationOverlay
    private lateinit var fadeOverlay: FadeOverlay

    private var clearData: Boolean = false
    private lateinit var buttonColumnHandler: ButtonColumnHandler<ImageButton, ButtonGroup>

    private lateinit var historyController: HistoryController

    private lateinit var stateFragment: StateFragment

    private var updateInterval = 30000L
    private val errorUpdateInterval: Long = 10000L

    private var timerSubscription: Subscription? = null
    private var statusSubscription: Subscription? = null
    private var resultSubscription: Subscription? = null
    private var parametersSubscription: Subscription? = null

    val dataEventConsumer: (DataEvent) -> Unit = { event ->
        when (event) {
            is DataEvent -> {
                statusComponent.indicateError(event.failed)
                if (!event.failed) {

                    Log.d(Main.LOG_TAG, "Main.onDataUpdate() " + event)

                    val resultParameters = event.parameters

                    clearDataIfRequested()

                    val countStrikes: (Int, Strike) -> Int = { count, strike -> count + strike.multiplicity }
                    val numberOfStrikes = event.totalStrikes?.fold(0, countStrikes) ?: event.strikes?.fold(0, countStrikes) ?: 0
                    val intervalDuration = event.parameters.intervalDuration
                    var statusText = """${resources.getQuantityString(R.plurals.strike, numberOfStrikes, numberOfStrikes)}/${resources.getQuantityString(R.plurals.minute, intervalDuration, intervalDuration)}"""
                    statusObservable.onNext(DataStatusUpdateEvent(statusText))
                    if (!event.containsRealtimeData()) {
                        val referenceTime = event.referenceTime + event.parameters.intervalOffset * 60 * 1000
                        val timeString = DateFormat.format("@ kk:mm", referenceTime) as String
                        statusObservable.onNext(TimeStatusUpdateEvent(timeString))
                    }

                    val initializeOverlay = strikesOverlay.parameters != resultParameters
                    with(strikesOverlay) {
                        parameters = resultParameters
                        rasterParameters = event.rasterParameters
                        referenceTime = event.referenceTime
                    }

                    if (event.incrementalData && !initializeOverlay) {
                        Log.v(LOG_TAG, "expire strikes")
                        strikesOverlay.expireStrikes()
                    } else {
                        Log.v(LOG_TAG, "clear strikes")
                        strikesOverlay.clear()
                    }

                    if (initializeOverlay && event.totalStrikes != null) {
                        Log.v(LOG_TAG, "add total strikes")
                        strikesOverlay.addStrikes(event.totalStrikes)
                    } else if (event.strikes != null) {
                        Log.v(LOG_TAG, "add strikes")
                        strikesOverlay.addStrikes(event.strikes)
                    }

                    alert_view.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)

                    strikesOverlay.refresh()

                    legend_view.requestLayout()

                    event.stations?.run {
                        participantsOverlay.setParticipants(this)
                        participantsOverlay.refresh()
                    }
                }

                mapView.invalidate()
                legend_view.invalidate()
                statusObservable.onNext(StatusProgressUpdateEvent(running =false))
            }
        }
    }

    private val statusObservable: PublishSubject<StatusEvent> by lazy {
        PublishSubject.create<StatusEvent>()
    }

    private val timerObservable: Observable<Long> by lazy {
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
        } catch (e: NoClassDefFoundError) {
            Log.e(Main.LOG_TAG, e.toString())
            Toast.makeText(baseContext, "bad android version", Toast.LENGTH_LONG).show()
        }

        Log.v(LOG_TAG, "Main.onCreate()")

        versionComponent = VersionComponent(this.applicationContext)

        setContentView(if (isDebugBuild) R.layout.main_debug else R.layout.main)

        val mapView = findViewById(R.id.mapview) as OwnMapView
        mapView.setBuiltInZoomControls(true)
        this.mapView = mapView

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        initializeStateFragment(preferences)

        val value: DataEvent? = stateFragment.dataObservable.value
        parametersComponent = ParametersComponent(preferences, value?.parameters ?: Parameters())

        strikesOverlay = StrikesOverlay(this, StrikeColorHandler(preferences))
        participantsOverlay = ParticipantsOverlay(this, ParticipantColorHandler(preferences))

        mapView.addZoomListener { zoomLevel ->
            strikesOverlay.updateZoomLevel(zoomLevel)
            participantsOverlay.updateZoomLevel(zoomLevel)
        }

        fadeOverlay = FadeOverlay(strikesOverlay.getColorHandler())
        ownLocationOverlay = OwnLocationOverlay(this, mapView)

        addOverlay(fadeOverlay)
        addOverlay(strikesOverlay)
        addOverlay(participantsOverlay)
        addOverlay(ownLocationOverlay)
        updateOverlays()

        statusComponent = StatusComponent(this)

        hideActionBar()

        buttonColumnHandler = ButtonColumnHandler<ImageButton, ButtonGroup>(if (TabletAwareView.isTablet(this)) 75f else 55f)
        configureMenuAccess()
        historyController = HistoryController(this, buttonColumnHandler, parametersComponent)


        buttonColumnHandler.addAllElements(historyController.getButtons(), ButtonGroup.DATA_UPDATING)

        buttonColumnHandler.lockButtonColumn(ButtonGroup.DATA_UPDATING)
        buttonColumnHandler.updateButtonColumn()

        setupCustomViews()

        onSharedPreferencesChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP, PreferenceKey.SHOW_PARTICIPANTS, PreferenceKey.QUERY_PERIOD)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(preferences)
        }

        if (versionComponent.state == VersionComponent.State.FIRST_RUN) {
            openQuickSettingsDialog()
        }

    }

    private fun initializeStateFragment(preferences: SharedPreferences) {
        val persistedStateFragment = fragmentManager.findFragmentByTag(StateFragment.TAG)
        stateFragment = if (persistedStateFragment != null && persistedStateFragment is StateFragment) persistedStateFragment else
            createStateFragment(preferences)

        Log.v(LOG_TAG, "state fragment: " + stateFragment + (if (persistedStateFragment == null) "CREATED" else ""))
    }

    private fun getTimeUntilUpdate(currentUpdateInterval: Long, currentTime: Long): Long {
        return stateFragment.data.referenceTime + currentUpdateInterval - currentTime
    }

    private fun createStateFragment(preferences: SharedPreferences): StateFragment {
        val locationHandler = LocationHandler(this, preferences)
        val stateFragment = StateFragment(locationHandler, AlertHandler(locationHandler, preferences, this))
        fragmentManager.beginTransaction().add(stateFragment, StateFragment.TAG).commit()
        return stateFragment
    }

    private fun setupCustomViews() {
        with(legend_view) {
            strikesOverlay = this@Main.strikesOverlay
            setAlpha(150)
            setOnClickListener { v -> openQuickSettingsDialog() }
        }

        with(alert_view) {
            setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.parameters.intervalDuration)
            setBackgroundColor(Color.TRANSPARENT)
            setAlpha(200)
            setOnClickListener { view ->
                val alertHandler = stateFragment.alertHandler
                if (alertHandler!!.isAlertEnabled) {
                    val currentLocation = alertHandler!!.currentLocation
                    if (currentLocation != null) {
                        var radius = determineTargetZoomRadius(alertHandler)

                        val diameter = 1.5f * 2f * radius
                        animateToLocationAndVisibleSize(currentLocation.longitude, currentLocation.latitude, diameter)
                    }
                }
            }
        }

        with(histogram_view) {
            setStrikesOverlay(strikesOverlay)
            setOnClickListener { view ->
                val currentResult = stateFragment.data
                if (currentResult != null) {
                    val rasterParameters = currentResult.rasterParameters
                    if (rasterParameters != null) {
                        animateToLocationAndVisibleSize(rasterParameters.rectCenterLongitude.toDouble(), rasterParameters.rectCenterLatitude.toDouble(), 5000f)
                    } else {
                        animateToLocationAndVisibleSize(0.0, 0.0, 20000f)
                    }
                }
            }
        }
    }

    private fun determineTargetZoomRadius(alertHandler: AlertHandler): Float {
        var radius = alertHandler.maxDistance

        val alertEvent = alertHandler.alertEvent
        if (alertEvent is AlertResultEvent) {
            val alertResult = alertEvent.alertResult
            if (alertResult != null) {
                radius = Math.max(Math.min(alertResult.closestStrikeDistance * 1.2f, radius), 50f)
            }
        }
        return radius
    }

    private fun openQuickSettingsDialog() {
        if (isAtLeast(Build.VERSION_CODES.HONEYCOMB)) {
            val dialog = QuickSettingsDialog()
            dialog.show(fragmentManager, "QuickSettingsDialog")
        }
    }

    private fun animateToLocationAndVisibleSize(longitude: Double, latitude: Double, diameter: Float) {
        Log.d(Main.LOG_TAG, "Main.animateAndZoomTo() %.4f, %.4f, %.0fkm".format(longitude, latitude, diameter))

        val mapView = mapView
        val controller = mapView.controller

        val startZoomLevel = mapView.zoomLevel
        val targetZoomLevel = mapView.calculateTargetZoomLevel(diameter * 1000f)

        controller.animateTo(GeoPoint((latitude * 1e6).toInt(), (longitude * 1e6).toInt()), {
            if (startZoomLevel != targetZoomLevel) {
                val zoomOut = targetZoomLevel - startZoomLevel < 0
                val zoomCount = Math.abs(targetZoomLevel - startZoomLevel)
                val handler = Handler()
                var delay: Long = 0
                for (i in 0..zoomCount - 1) {
                    handler.postDelayed({
                        if (zoomOut) {
                            controller.zoomOut()
                        } else {
                            controller.zoomIn()
                        }
                    }, delay)
                    delay += 150
                }
            }
        })
    }

    val isDebugBuild: Boolean
        get() {
            var dbg = false
            try {
                val pm = packageManager
                val pi = pm.getPackageInfo(packageName, 0)

                dbg = ((pi.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            } catch (ignored: Exception) {
            }

            return dbg
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_info -> showDialog(R.id.info_dialog)

            R.id.menu_alarms -> showDialog(R.id.alarm_dialog)

            R.id.menu_log -> showDialog(R.id.log_dialog)

            R.id.menu_preferences -> startActivity<Preferences>()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        Log.d(Main.LOG_TAG, "Main.onStart()")
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(Main.LOG_TAG, "Main.onStart()")
    }

    override fun onResume() {
        super.onResume()

        //initializeStateFragment(PreferenceManager.getDefaultSharedPreferences(this))

        Log.v(Main.LOG_TAG, "on resume with state fragment " + stateFragment)

        val statusSubscriber = { event: StatusEvent ->
            Log.v(LOG_TAG, "statusSubscriber($event)")
            when (event) {
                is TimeStatusUpdateEvent -> statusComponent.setTimeStatus(event.status)
                is DataStatusUpdateEvent -> statusComponent.setDataStatus(event.status)
                is StatusProgressUpdateEvent ->
                    if (event.running) {
                        statusComponent.startProgress()
                        buttonColumnHandler.lockButtonColumn(ButtonGroup.DATA_UPDATING)
                    } else {
                        statusComponent.stopProgress()
                        buttonColumnHandler.unlockButtonColumn(ButtonGroup.DATA_UPDATING)
                    }
                is StatusFailureUpdateEvent -> statusComponent.indicateError(event.failed)
            }
        }

        statusSubscription = statusObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(statusSubscriber)

        val parameterSubject = PublishSubject.create<Parameters>()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (stateFragment.dataObservable.hasValue()) {
            Log.v(LOG_TAG, "stored parameters: " + stateFragment.dataObservable.value)
        }

        resultSubscription = stateFragment.dataObservable.subscribe(dataEventConsumer)
        stateFragment.dataObservable.subscribe(histogram_view.dataConsumer)

        timerObservable.doOnUnsubscribe { Log.v(LOG_TAG, "timer observable unsubscribed") }

        parametersSubscription = stateFragment.parametersObservable.subscribe(parameterSubject)

        val timerSubscriber = { index: Long ->
            Log.v(LOG_TAG, "timerSubscriber($index)")
            val currentTime = System.currentTimeMillis()
            val currentUpdateInterval = if (stateFragment.data.failed) errorUpdateInterval else updateInterval
            val timeUntilUpdate = getTimeUntilUpdate(currentUpdateInterval, currentTime) + 500
            statusObservable.onNext(TimeStatusUpdateEvent("${if (timeUntilUpdate < 0) "-" else timeUntilUpdate / 1000}/${currentUpdateInterval / 1000}s"))
            if (timeUntilUpdate <= 0) {
                statusObservable.onNext(StatusProgressUpdateEvent(true))
                parametersComponent.trigger()
            }
        }

        parameterSubject
                .onBackpressureDrop()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .asObservable()
                .doOnNext { parameters ->
                    Log.v(LOG_TAG, "triggered with $parameters")
                    val isRealtime = parameters.isRealtime()
                    updateTimerState(isRealtime, timerSubscriber)
                    statusObservable.onNext(StatusProgressUpdateEvent(running=true))
                }
                .map(DataMapper(preferences, "-" + versionComponent.versionCode))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stateFragment.dataObservable)

        parametersComponent.observable.subscribe(parameterSubject)
        updateTimerState(parametersComponent.isRealtime, timerSubscriber)



        Log.d(Main.LOG_TAG, "Main.onResume() ${parametersComponent.observable.value} ${parametersComponent.observable.hasValue()}")
    }

    private @Synchronized fun updateTimerState(isRealtime: Boolean, timerSubscriber: (Long) -> Unit) {
        if (isRealtime) {
            Log.v(LOG_TAG, "start timer $timerSubscription")
            if (timerSubscription == null) {
                timerSubscription = timerObservable.subscribe(timerSubscriber)
            } else {
                Log.v(LOG_TAG, "not started")
            }
        } else {
            Log.v(LOG_TAG, "stop timer $timerSubscription")
            timerSubscription?.unsubscribe()
            timerSubscription = null
        }
    }

    override fun onPause() {
        super.onPause()

        timerSubscription?.unsubscribe()
        timerSubscription = null

        statusSubscription?.unsubscribe()
        statusSubscription = null

        parametersSubscription?.unsubscribe()
        parametersSubscription = null

        resultSubscription?.unsubscribe()
        resultSubscription = null

        Log.v(Main.LOG_TAG, "Main.onPause()")
    }

    override fun onStop() {
        super.onStop()

        Log.i(LOG_TAG, "Main.onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(LOG_TAG, "Main: onDestroy() unbind service")
    }

    override fun isRouteDisplayed(): Boolean {
        return false
    }

    private fun clearDataIfRequested() {
        if (clearData) {
            clearData()
        }
    }

    private fun clearData() {
        Log.v(Main.LOG_TAG, "Main.clearData()")
        clearData = false

        strikesOverlay.clear()
        participantsOverlay.clear()
    }

    override fun onCreateDialog(id: Int, args: Bundle?): Dialog? {
        return when (id) {
            R.id.info_dialog -> InfoDialog(this, versionComponent)

            R.id.alarm_dialog ->
                AlertDialog(this, stateFragment.alertHandler, AlertDialogColorHandler(PreferenceManager.getDefaultSharedPreferences(this)))

            R.id.log_dialog -> LogDialog(this)

            else -> throw RuntimeException("unhandled dialog with id $id")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.v(LOG_TAG, "Main.onRequestPermissionsResult() $requestCode - $permissions - $grantResults")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val providerRelation = LocationProviderRelation.byOrdinal[requestCode]
        if (providerRelation != null) {
            val providerName = providerRelation.providerName
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "$providerName permission has now been granted.")
                val editor = sharedPreferences.edit()
                editor.put(PreferenceKey.LOCATION_MODE, providerName)
                editor.commit()
            } else {
                Log.i(LOG_TAG, "$providerName permission was NOT granted.")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions(sharedPreferences: SharedPreferences) {
        val locationProviderName = sharedPreferences.get(PreferenceKey.LOCATION_MODE, LocationManager.PASSIVE_PROVIDER)
        val permission = when (locationProviderName) {
            LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER -> Manifest.permission.ACCESS_FINE_LOCATION
            LocationManager.NETWORK_PROVIDER -> Manifest.permission.ACCESS_COARSE_LOCATION
            else -> null
        }

        if (permission is String && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), LocationProviderRelation.byProviderName[locationProviderName]?.ordinal ?: Int.MIN_VALUE)
        }
    }

    private enum class LocationProviderRelation(val providerName : String) {
        GPS(LocationManager.GPS_PROVIDER), PASSIVE(LocationManager.PASSIVE_PROVIDER), NETWORK(LocationManager.NETWORK_PROVIDER);

        companion object {
            val byProviderName: Map<String, LocationProviderRelation> = LocationProviderRelation.values().groupBy {it.providerName}.mapValues { it.value.first() }
            val byOrdinal : Map<Int, LocationProviderRelation> = LocationProviderRelation.values().groupBy {it.ordinal}.mapValues { it.value.first() }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.MAP_TYPE -> {
                val mapTypeString = sharedPreferences.get(key, "SATELLITE")
                mapView.isSatellite = mapTypeString == "SATELLITE"
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> {
                val showParticipants = sharedPreferences.get(key, true)
                participantsOverlay.enabled = showParticipants
                updateOverlays()
            }

            PreferenceKey.COLOR_SCHEME -> {
                strikesOverlay.refresh()
                participantsOverlay.refresh()
            }

            PreferenceKey.QUERY_PERIOD -> {
                updateInterval = sharedPreferences.get(key, "30").toLong() * 1000
            }

            PreferenceKey.MAP_FADE -> {
                val alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.get(key, 40))
                fadeOverlay.setAlpha(alphaValue)
            }

            PreferenceKey.DO_NOT_SLEEP -> {
                val doNotSleep = sharedPreferences.get(key, false)
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

                if (doNotSleep) {
                    window.addFlags(flag)
                } else {
                    window.clearFlags(flag)
                }
            }
        }
    }

    protected fun setHistoricStatusString() {
        if (!strikesOverlay.hasRealtimeData()) {
            val referenceTime = strikesOverlay.referenceTime + strikesOverlay.parameters.intervalOffset * 60 * 1000
            val timeString = DateFormat.format("@ kk:mm", referenceTime) as String
            setStatusString(timeString)
        }
    }

    protected fun setStatusString(runStatus: String) {
        val numberOfStrikes = strikesOverlay.totalNumberOfStrikes
        var statusText = resources.getQuantityString(R.plurals.strike, numberOfStrikes, numberOfStrikes)
        statusText += "/"
        val intervalDuration = strikesOverlay.parameters.intervalDuration
        statusText += resources.getQuantityString(R.plurals.minute, intervalDuration, intervalDuration)
        statusText += " " + runStatus

        statusComponent.setDataStatus(statusText)
    }

    private fun configureMenuAccess() {
        val config = ViewConfiguration.get(this)

        if (isAtLeast(Build.VERSION_CODES.LOLLIPOP) ||
                isAtLeast(Build.VERSION_CODES.ICE_CREAM_SANDWICH) &&
                        !config.hasPermanentMenuKey()) {
            menu.visibility = View.VISIBLE
            menu.setOnClickListener { v -> openOptionsMenu() }
            buttonColumnHandler.addElement(menu)
        }
    }

    private fun hideActionBar() {
        if (isAtLeast(Build.VERSION_CODES.HONEYCOMB)) {
            actionBar?.hide()
        }
    }

    companion object {
        val LOG_TAG = "BO_ANDROID"

        val REQUEST_GPS = 1
    }
}

