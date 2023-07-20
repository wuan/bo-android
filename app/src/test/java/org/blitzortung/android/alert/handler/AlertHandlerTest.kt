package org.blitzortung.android.alert.handler

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.event.AlertCancelEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.RasterElement
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.location.LocationHandler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
public class AlertHandlerTest {
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
    public fun locationHandlerUpdatesEnabled() {
        verify { locationHandler.requestUpdates(any()) }
    }

    @Test
    fun receiveData() {
        val rasterBaselength = 5000
        val parameters = Parameters(
            intervalOffset = 0, intervalDuration = 60, region = LOCAL_REGION, rasterBaselength = rasterBaselength
        )
        uut.dataEventConsumer.invoke(
            ResultEvent(
                strikes = listOf(RasterElement(System.currentTimeMillis(), 11.0, 45.0, 5)),
                rasterParameters = RasterParameters(10.0, 40.0, 10.0, 10.0, 20, 20, rasterBaselength),
                flags = Flags(),
                parameters = parameters
            )
        )

        assertThat(uut.alertEvent).isInstanceOf(AlertCancelEvent::class.java)
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

        val rasterBaselength = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(RasterElement(currentTime, 11.0, 45.0, 5))
        val rasterParameters = RasterParameters(10.0, 40.0, 10.0, 10.0, 20, 20, rasterBaselength)

        mockAlertResult(currentTime, 0.0f)

        every { alertDataHandler.getLatestTimstampWithin(any(), any()) } returns currentTime

        val parameters = Parameters(
            intervalOffset = 0, intervalDuration = 60, region = LOCAL_REGION, rasterBaselength = rasterBaselength
        )
        uut.dataEventConsumer.invoke(
            ResultEvent(
                strikes = strikeList,
                rasterParameters = rasterParameters,
                flags = Flags(),
                parameters = parameters
            )
        )

        assertThat(uut.alertEvent).isInstanceOf(AlertResultEvent::class.java)
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

        val rasterBaselength = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(RasterElement(currentTime, 11.0, 45.0, 5))
        val rasterParameters = RasterParameters(10.0, 40.0, 10.0, 10.0, 20, 20, rasterBaselength)

        mockAlertResult(currentTime, 0.0f)

        every { alertDataHandler.getLatestTimstampWithin(any(), any()) } returns currentTime

        val parameters = Parameters(
            intervalOffset = 0, intervalDuration = 60, region = LOCAL_REGION, rasterBaselength = rasterBaselength
        )
        uut.dataEventConsumer.invoke(
            ResultEvent(
                strikes = strikeList,
                rasterParameters = rasterParameters,
                flags = Flags(),
                parameters = parameters
            )
        )

        assertThat(uut.alertEvent).isInstanceOf(AlertResultEvent::class.java)
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

        val rasterBaselength = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(RasterElement(currentTime, 11.0, 45.0, 5))
        val rasterParameters = RasterParameters(10.0, 40.0, 10.0, 10.0, 20, 20, rasterBaselength)

        mockAlertResult(currentTime, 0.0f)

        val parameters = Parameters(
            intervalOffset = 0, intervalDuration = 60, region = LOCAL_REGION, rasterBaselength = rasterBaselength
        )
        uut.dataEventConsumer.invoke(
            ResultEvent(
                strikes = strikeList,
                rasterParameters = rasterParameters,
                flags = Flags(),
                parameters = parameters
            )
        )

        assertThat(uut.alertEvent).isInstanceOf(AlertResultEvent::class.java)
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

        val rasterBaselength = 5000
        val currentTime = System.currentTimeMillis()
        val strikeList = listOf(RasterElement(currentTime, 11.0, 45.0, 5))
        val rasterParameters = RasterParameters(10.0, 40.0, 10.0, 10.0, 20, 20, rasterBaselength)

        val parameters = Parameters(
            intervalOffset = 10, intervalDuration = 60, region = LOCAL_REGION, rasterBaselength = rasterBaselength
        )
        uut.dataEventConsumer.invoke(
            ResultEvent(
                strikes = strikeList,
                rasterParameters = rasterParameters,
                flags = Flags(),
                parameters = parameters
            )
        )

        assertThat(uut.alertEvent).isInstanceOf(AlertCancelEvent::class.java)
    }

    private fun mockAlertResult(currentTime: Long, closestDistance: Float) {
        every { alertDataHandler.checkStrikes(any(), any(), any(), any()) } returns AlertResult(
            sectors = listOf(AlertSector("foo", 0.0f, 1.0f, emptyList(), closestDistance)),
            uut.alertParameters,
            currentTime
        )
    }

}