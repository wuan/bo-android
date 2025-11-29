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

package org.blitzortung.android.data

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.data.cache.DataCache
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.LocalData
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.NoData
import org.blitzortung.android.data.provider.result.RequestStarted
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.result.StatusUpdate
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.util.Period
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox

@Singleton
class MainDataHandler
@Inject
constructor(
    private val context: Context,
    private val dataProviderFactory: DataProviderFactory,
    private val preferences: SharedPreferences,
    private val handler: Handler,
    private val cache: DataCache,
    private val localData: LocalData,
    private val updatePeriod: Period,
) : OnSharedPreferenceChangeListener, Runnable, MapListener {
    private var location: Location? = null

    @Volatile
    private var updatesEnabled = false

    private var animationSleepDuration by Delegates.notNull<Long>()
    private var animationCycleSleepDuration by Delegates.notNull<Long>()

    var mode = Mode.DATA
        private set

    private var period: Int = 0
    private var sequenceNumber = AtomicLong()

    private var dataProvider: DataProvider? = null

    var parameters = Parameters()
        private set

    var history = History()
        private set

    private var animationHistory: History? = null

    var autoGridSize = false
        private set

    private val dataConsumerContainer =
        object : ConsumerContainer<DataEvent>(NoData) {
            override fun addedFirstConsumer() {
                Log.d(LOG_TAG, "MainDataHandler: added first data consumer")
            }

            override fun removedLastConsumer() {
                Log.d(LOG_TAG, "MainDataHandler: removed last data consumer")
            }
        }

    val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
        Log.v(LOG_TAG, "AlertView received location ${locationEvent}")
        location = locationEvent.location()
        if (location != null) {
            updateData()
        }
    }

    private var dataMode = DataMode()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)

        updatesEnabled = false
        onSharedPreferenceChanged(
            this.preferences,
            PreferenceKey.DATA_SOURCE,
            PreferenceKey.USERNAME,
            PreferenceKey.PASSWORD,
            PreferenceKey.GRID_SIZE,
            PreferenceKey.COUNT_THRESHOLD,
            PreferenceKey.REGION,
            PreferenceKey.INTERVAL_DURATION,
            PreferenceKey.HISTORIC_TIMESTEP,
            PreferenceKey.QUERY_PERIOD,
            PreferenceKey.ANIMATION_INTERVAL_DURATION,
            PreferenceKey.ANIMATION_SLEEP_DURATION,
            PreferenceKey.ANIMATION_CYCLE_SLEEP_DURATION,
        )
        updatesEnabled = true
    }

    fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    val hasConsumers: Boolean
        get() = dataConsumerContainer.isEmpty

    fun updateData() {
        if (updatesEnabled) {
            sendEvent(REQUEST_STARTED_EVENT)

            updateUsingCache()
        }
    }

    private fun updateUsingCache() {
        var flags = Flags(mode = mode)
        val sequenceNumber = sequenceNumber.incrementAndGet()

        val parameters = activeParameters
        val cachedResult = cache.get(parameters)
        if (cachedResult != null) {
            Log.d(LOG_TAG, "MainDataHandler.updateData() cached $parameters")
            sendEvent(cachedResult.copy(sequenceNumber = sequenceNumber))
        } else {
            Log.d(LOG_TAG, "MainDataHandler.updateData() fetch $parameters")
            FetchDataTask(dataMode, dataProvider!!) {
                if (mode == Mode.ANIMATION) {
                    flags = flags.copy(storeResult = false)
                    if (!it.containsRealtimeData()) {
                        flags = flags.copy(ignoreForAlerting = true)
                    }
                }
                val event = it.copy(flags = flags)
                if (!it.containsRealtimeData()) {
                    cache.put(event.parameters, event)
                }
                sendEvent(event.copy(sequenceNumber = sequenceNumber))
            }.execute(parameters = parameters, history = history)
        }
    }

    private val activeParameters: Parameters
        get() {
            return if (dataMode.grid) {
                localData.updateParameters(parameters, location)
            } else {
                var parameters = parameters
                if (!dataMode.region) {
                    parameters = parameters.copy(region = 0)
                }
                parameters.copy(gridSize = 0, countThreshold = 0)
            }
        }

    private fun sendEvent(dataEvent: DataEvent) {
        if (dataEvent is DataReceived) {
            localData.storeResult(dataEvent.gridParameters)
        }
        if (dataEvent is DataReceived && dataEvent.flags.storeResult) {
            dataConsumerContainer.storeAndBroadcast(dataEvent)
        } else {
            dataConsumerContainer.broadcast(dataEvent)
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: PreferenceKey,
    ) {
        when (key) {
            PreferenceKey.DATA_SOURCE, PreferenceKey.SERVICE_URL -> {
                val providerTypeString =
                    sharedPreferences.get(
                        PreferenceKey.DATA_SOURCE,
                        DataProviderType.RPC.toString(),
                    )
                val providerType =
                    DataProviderType.valueOf(providerTypeString.uppercase(Locale.getDefault()))
                val dataProvider = dataProviderFactory.getDataProviderForType(providerType)
                this.dataProvider = dataProvider

                updateProviderSpecifics()

                if (providerTypeString == DataProviderType.HTTP.toString()) {
                    showBlitzortungProviderWarning()
                }

                Log.v(LOG_TAG, "MainDataHandler update data source: $providerType")

                updateData()
            }

            PreferenceKey.GRID_SIZE -> {
                val gridSizeString = sharedPreferences.get(key, AUTO_GRID_SIZE_VALUE)
                if (gridSizeString == AUTO_GRID_SIZE_VALUE) {
                    autoGridSize = true
                    parameters = parameters.copy(gridSize = DEFAULT_GRID_SIZE)
                } else {
                    val gridSize = Integer.parseInt(gridSizeString)
                    autoGridSize = false
                    parameters = parameters.copy(gridSize = gridSize)
                }
                updateData()
            }

            PreferenceKey.COUNT_THRESHOLD -> {
                val countThreshold = Integer.parseInt(sharedPreferences.get(key, "0"))
                parameters = parameters.copy(countThreshold = countThreshold)
                updateData()
            }

            PreferenceKey.INTERVAL_DURATION -> {
                val intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60"))
                parameters = parameters.withIntervalDuration(intervalDuration)
                updateData()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> {
                history =
                    history.copy(
                        timeIncrement =
                            sharedPreferences.get(key, "30").toInt(),
                    )
            }

            PreferenceKey.REGION -> {
                val region = Integer.parseInt(sharedPreferences.get(key, "0"))
                parameters = parameters.copy(region = region)
                updateData()
            }

            PreferenceKey.QUERY_PERIOD -> {
                period = Integer.parseInt(sharedPreferences.get(key, "60"))
                Log.v(LOG_TAG, "MainDataHandler query $period")
            }

            PreferenceKey.ANIMATION_INTERVAL_DURATION -> {
                val value = Integer.parseInt(sharedPreferences.get(key, "4"))
                animationHistory =
                    when (value) {
                        2 -> History(5, 120, false)
                        4 -> History(10, 240, false)
                        6 -> History(10, 360, false)
                        12 -> History(20, 720, false)
                        24 -> History(30, 1440, true)
                        else -> History(10, 240, false)
                    }
                if (mode == Mode.ANIMATION) {
                    history = animationHistory!!
                    cache.clear()
                }
            }

            PreferenceKey.ANIMATION_SLEEP_DURATION -> {
                animationSleepDuration = sharedPreferences.getInt(key.key, 200).toLong()
            }

            PreferenceKey.ANIMATION_CYCLE_SLEEP_DURATION -> {
                animationCycleSleepDuration = sharedPreferences.getInt(key.key, 3000).toLong()
            }

            else -> {
            }
        }
    }

    private fun showBlitzortungProviderWarning() =
        CustomToast.makeText(context, R.string.provider_warning, Toast.LENGTH_LONG).show()

    private fun updateProviderSpecifics() {
        val providerType = dataProvider!!.type

        dataMode =
            when (providerType) {
                DataProviderType.RPC -> DataMode(grid = true, region = false)
                DataProviderType.HTTP -> DataMode(grid = false, region = true)
            }
    }

    fun toggleExtendedMode() {
        dataMode = dataMode.copy(grid = dataMode.grid.xor(true))

        if (!isRealtime) {
            val dataChannels = HashSet<DataChannel>()
            dataChannels.add(DataChannel.STRIKES)
            updateData()
        }
    }

    val intervalDuration: Int
        get() = parameters.intervalDuration

    fun goRealtime(): Boolean {
        return updateParameters { it.goRealtime() }
    }

    fun setPosition(position: Int): Boolean {
        return updateParameters { it.withPosition(position, history) }
    }

    fun historySteps(): Int {
        return parameters.intervalMaxPosition(history)
    }

    private fun updateParameters(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    val isRealtime: Boolean
        get() = parameters.isRealtime()

    private fun broadcastEvent(event: DataEvent) {
        dataConsumerContainer.broadcast(event)
    }

    override fun run() {
        when (mode) {
            Mode.DATA -> {
                val currentTime = Period.currentTime
                val updateTargets = HashSet<DataChannel>()

                if (updatePeriod.shouldUpdate(currentTime, period)) {
                    updateTargets.add(DataChannel.STRIKES)
                }

                if (updateTargets.isNotEmpty()) {
                    updateData()
                }

                if (parameters.isRealtime()) {
                    val statusString =
                        "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
                    broadcastEvent(StatusUpdate(statusString))
                    // Schedule the next update
                    handler.postDelayed(this, 1000)
                }
            }

            Mode.ANIMATION -> {
                parameters = parameters.animationStep(history)
                val delay =
                    if (parameters.isRealtime()) {
                        if (animationCycleSleepDuration > 0) animationCycleSleepDuration else animationSleepDuration
                    } else {
                        animationSleepDuration
                    }
                handler.postDelayed(this, delay)
                updateUsingCache()
            }
        }
    }

    fun start() {
        if (isRealtime || mode == Mode.ANIMATION) {
            handler.post(this)
        }
    }

    fun startAnimation() {
        cache.clear()
        this.history = animationHistory!!
        mode = Mode.ANIMATION
        handler.post(this)
    }

    fun restart() {
        updatePeriod.restart()
        if (mode == Mode.ANIMATION) {
            cache.clear()
        }
        history = History()
        mode = Mode.DATA
        start()
    }

    fun stop() {
        handler.removeCallbacks(this)
    }

    companion object {
        val REQUEST_STARTED_EVENT = RequestStarted()
        val DEFAULT_DATA_CHANNELS = setOf(DataChannel.STRIKES)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return event?.let { updateGrid(event.source as OwnMapView, this.autoGridSize) } ?: false
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return event?.let { updateGrid(event.source as OwnMapView, this.autoGridSize) } ?: false
    }

    fun updateGrid(
        mapView: OwnMapView,
        autoGridSize: Boolean,
    ): Boolean {
        val updatedAutoGridSize = updateAutoGridSize(mapView.zoomLevelDouble, autoGridSize)
        val updatedLocation = mapView.boundingBox?.let { updateLocation(it, updatedAutoGridSize) } ?: false

        if (updatedLocation || updatedAutoGridSize) {
            Log.v(LOG_TAG, "MainDataHandler.updateGrid() location: $updatedLocation gridSize: $updatedAutoGridSize")
            if (mapView.isAnimating) {
                addUpdateAfterAnimationListener(mapView)
            } else {
                Log.v(LOG_TAG, "MainDataHandler.updateGrid() call updateData()")
                updateData()
            }
        }
        return updatedLocation || updatedAutoGridSize
    }

    fun updateLocation(
        boundingBox: BoundingBox,
        force: Boolean = false,
    ): Boolean = localData.update(boundingBox, force)

    fun updateAutoGridSize(
        zoomLevel: Double,
        autoGridSize: Boolean,
    ): Boolean =
        if (autoGridSize) {
            val gridSize =
                when {
                    zoomLevel >= 7.5f -> 5000
                    zoomLevel in 5f..7.5f -> 10000
                    zoomLevel in 3.5f..5f -> 25000
                    zoomLevel in 2.5f..3.5f -> 50000
                    else -> 100000
                }
            if (parameters.gridSize != gridSize) {
                Log.v(
                    LOG_TAG,
                    "MainDataHandler.updateAutoGridSize() $zoomLevel : ${parameters.gridSize} -> $gridSize",
                )
                parameters = parameters.copy(gridSize = gridSize)
                true
            } else {
                false
            }
        } else {
            false
        }

    private fun addUpdateAfterAnimationListener(mapView: OwnMapView) {
        val animator = mapView.animator()

        if (animator != null && !animator.listeners.contains(animatorListener)) {
            animator.addListener(animatorListener)
        }
    }

    private val animatorListener =
        object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                this@MainDataHandler.updateData()
            }

            override fun onAnimationCancel(animation: Animator) {
                this@MainDataHandler.updateData()
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        }

    fun calculateTotalCacheSize(): CacheSize = cache.calculateTotalSize()
}

internal const val DEFAULT_GRID_SIZE = 10000
internal const val AUTO_GRID_SIZE_VALUE = "auto"
