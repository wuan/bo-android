package org.blitzortung.android.alert.handler

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.alert.NoData
import org.blitzortung.android.alert.NoLocation
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationHandler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AlertHandlerTest {
    private lateinit var uut: AlertHandler

    @MockK
    private lateinit var locationHandler: LocationHandler

    @MockK
    private lateinit var notificationHandler: NotificationHandler

    @MockK
    private lateinit var alertDataHandler: AlertDataHandler

    @MockK
    private lateinit var alertSignal: AlertSignal

    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        uut = AlertHandler(locationHandler, preferences, context, notificationHandler, alertDataHandler, alertSignal)
    }

    @Test
    fun locationHandlerUpdatesEnabled() {
        verify { locationHandler.requestUpdates(any()) }
    }

    @Test
    fun receiveDataShouldProduceAlertResultButNotTriggerAnyNotification() {
        val gridSize = 5000
        val parameters =
            Parameters(interval = TimeInterval(offset = 0, duration = 60), region = LOCAL_REGION, gridSize = gridSize)
        val dataReceived =
            DataReceived(
                strikes = listOf(GridElement(System.currentTimeMillis(), 11.0, 45.0, 5)),
                gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize),
                flags = Flags(),
                parameters = parameters,
            )
        mockAlarm(System.currentTimeMillis(), 5.0f)

        uut.dataEventConsumer.invoke(dataReceived)

        assertThat(uut.alertEvent).isInstanceOf(Warning::class.java)
        val alertResult = uut.alertEvent as LocalActivity
        assertThat(alertResult.closestStrikeDistance).isEqualTo(5.0f)

        verify(exactly = 0) { alertSignal.emitSignal() }
    }

    @Test
    fun receiveDataWithLocationAndAlertEnabled() {
        val edit = preferences.edit()
        edit.put(PreferenceKey.ALERT_ENABLED, true)
        edit.apply()

        val location = Location("manual")
        location.longitude = 11.0
        location.latitude = 45.0
        every { locationHandler.location } returns location

        val gridSize = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(GridElement(currentTime, 11.0, 45.0, 5))
        val gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize)

        mockAlarm(currentTime, 0.0f)

        every { alertDataHandler.getLatestTimstampWithin(any(), any()) } returns currentTime

        val parameters =
            Parameters(
                interval = TimeInterval(offset = 0, duration = 60),
                region = LOCAL_REGION,
                gridSize = gridSize,
            )
        uut.dataEventConsumer.invoke(
            DataReceived(
                strikes = strikeList,
                gridParameters = gridParameters,
                flags = Flags(),
                parameters = parameters,
            ),
        )

        assertThat(uut.alertEvent).isInstanceOf(Warning::class.java)
        verify { alertSignal.emitSignal() }
        verify { notificationHandler.sendNotification(any()) }
    }

    @Test
    fun receiveDataWithOffLocationAndAlertEnabled() {
        val edit = preferences.edit()
        edit.put(PreferenceKey.ALERT_ENABLED, true)
        edit.apply()

        val location = Location("manual")
        location.longitude = 11.0
        location.latitude = 45.0
        every { locationHandler.location } returns location

        val gridSize = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(GridElement(currentTime, 11.0, 45.0, 5))
        val gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize)

        mockAlarm(currentTime, 0.0f)

        every { alertDataHandler.getLatestTimstampWithin(any(), any()) } returns currentTime

        val parameters =
            Parameters(
                interval = TimeInterval(offset = 0, duration = 60),
                region = LOCAL_REGION,
                gridSize = gridSize,
            )
        uut.dataEventConsumer.invoke(
            DataReceived(
                strikes = strikeList,
                gridParameters = gridParameters,
                flags = Flags(),
                parameters = parameters,
            ),
        )

        assertThat(uut.alertEvent).isInstanceOf(LocalActivity::class.java)
        verify { alertSignal.emitSignal() }
    }

    @Test
    fun receiveDataWithLocationWithoutSignal() {
        val edit = preferences.edit()
        edit.put(PreferenceKey.ALERT_ENABLED, true)
        edit.apply()

        val location = Location("manual")
        location.longitude = 11.0
        location.latitude = 45.0
        every { locationHandler.location } returns location

        val gridSize = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(GridElement(currentTime, 11.0, 45.0, 5))
        val gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize)

        mockAlarm(currentTime, 0.0f)

        val parameters =
            Parameters(
                interval = TimeInterval(offset = 0, duration = 60),
                region = LOCAL_REGION,
                gridSize = gridSize,
            )
        uut.dataEventConsumer.invoke(
            DataReceived(
                strikes = strikeList,
                gridParameters = gridParameters,
                flags = Flags(),
                parameters = parameters,
            ),
        )

        assertThat(uut.alertEvent).isInstanceOf(LocalActivity::class.java)
        verify(exactly = 0) { alertSignal.emitSignal() }
    }

    @Test
    fun receiveNonRealtimeDataWithLocationAndAlertsEnabled() {
        val edit = preferences.edit()
        edit.put(PreferenceKey.ALERT_ENABLED, true)
        edit.apply()

        val location = Location("manual")
        location.longitude = 11.0
        location.latitude = 45.0
        every { locationHandler.location } returns location

        val gridSize = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(GridElement(currentTime, 11.0, 45.0, 5))
        val gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize)

        val parameters =
            Parameters(
                interval = TimeInterval(offset = -30, duration = 60),
                region = LOCAL_REGION,
                gridSize = gridSize,
            )

        var alertEvent: Warning = NoLocation
        uut.requestUpdates { event -> alertEvent = event }

        uut.dataEventConsumer.invoke(
            DataReceived(
                strikes = strikeList,
                gridParameters = gridParameters,
                flags = Flags(),
                parameters = parameters,
            ),
        )

        assertThat(uut.alertEvent).isEqualTo(NoData)
        assertThat(alertEvent).isEqualTo(NoData)
    }

    @Test
    fun receiveNonRealtimeDataWithLocationAndAlertsEnabledShouldDoNothingWhenIgnoreFlagIsSet() {
        val edit = preferences.edit()
        edit.put(PreferenceKey.ALERT_ENABLED, true)
        edit.apply()

        val location = Location("manual")
        location.longitude = 11.0
        location.latitude = 45.0
        every { locationHandler.location } returns location

        val gridSize = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(GridElement(currentTime, 11.0, 45.0, 5))
        val gridParameters = GridParameters(10.0, 40.0, 10.0, 10.0, 20, 20, gridSize)

        val parameters =
            Parameters(
                interval = TimeInterval(offset = -30, duration = 60),
                region = LOCAL_REGION,
                gridSize = gridSize,
            )

        var warning: Warning = NoData
        uut.requestUpdates { event -> warning = event }

        uut.dataEventConsumer.invoke(
            DataReceived(
                strikes = strikeList,
                gridParameters = gridParameters,
                flags = Flags(ignoreForAlerting = true),
                parameters = parameters,
            ),
        )

        assertThat(uut.alertEvent).isEqualTo(NoData)
        assertThat(warning).isEqualTo(NoData)
    }

    private fun mockAlarm(
        currentTime: Long,
        closestDistance: Float,
    ) {
        every { alertDataHandler.checkStrikes(any(), any(), any(), any()) } returns
            LocalActivity(
                sectors = listOf(AlertSector("foo", 0.0f, 1.0f, emptyList(), closestDistance)),
                uut.alertParameters,
                currentTime,
            )
    }
}
