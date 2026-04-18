package org.blitzortung.android.app

import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerParameters
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class WidgetUpdateWorkerTest {

    @get:Rule
    val mockKRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var workerParams: WorkerParameters

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun workerClassIsOpen() {
        val workerClass = WidgetUpdateWorker::class.java
        val modifiers = workerClass.modifiers
        val isPublic = java.lang.reflect.Modifier.isPublic(modifiers)
        assertThat(isPublic).isTrue()
    }

    @Test
    fun updateIntervalIsFifteenMinutes() {
        assertThat(WidgetProvider.UPDATE_INTERVAL_MINUTES).isEqualTo(15L)
    }

    @Test
    fun widgetProviderIsOpen() {
        val providerClass = WidgetProvider::class.java
        val modifiers = providerClass.modifiers
        val isPublic = java.lang.reflect.Modifier.isPublic(modifiers)
        assertThat(isPublic).isTrue()
    }

    @Test
    fun getLastKnownLocation_returnsNullWhenNoProviders() {
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns null
        every { mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns null
        every { mockLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns null

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val location = worker.testGetLastKnownLocation()

        assertThat(location).isNull()
    }

    @Test
    fun getLastKnownLocation_returnsLocationFromGpsProvider() {
        val gpsLocation = createLocation(51.0, 7.0, 10f)
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns gpsLocation
        every { mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns null
        every { mockLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns null

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val location = worker.testGetLastKnownLocation()

        assertThat(location).isNotNull
        assertThat(location!!.latitude).isEqualTo(51.0)
        assertThat(location.longitude).isEqualTo(7.0)
    }

    @Test
    fun getLastKnownLocation_returnsMostAccurateLocation() {
        val gpsLocation = createLocation(51.0, 7.0, 100f) // less accurate
        val networkLocation = createLocation(51.0, 7.0, 50f) // more accurate
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns gpsLocation
        every { mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns networkLocation
        every { mockLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns null

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val location = worker.testGetLastKnownLocation()

        assertThat(location).isNotNull
        assertThat(location!!.latitude).isEqualTo(51.0)
        assertThat(location.longitude).isEqualTo(7.0)
        assertThat(location.accuracy).isEqualTo(50f)
    }

    @Test
    fun getLastKnownLocation_returnsNullWhenSecurityException() {
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(any()) } throws SecurityException()

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val location = worker.testGetLastKnownLocation()

        assertThat(location).isNull()
    }

    private fun createLocation(latitude: Double, longitude: Double, accuracy: Float): Location {
        return Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            time = System.currentTimeMillis()
        }
    }

    private inner class TestableWidgetUpdateWorker(
        appContext: android.content.Context,
        workerParams: WorkerParameters
    ) : WidgetUpdateWorker(appContext, workerParams) {

        private var mockLocationManager: LocationManager? = null

        fun setLocationManager(manager: LocationManager) {
            mockLocationManager = manager
        }

        fun testGetLastKnownLocation(): Location? {
            return getLastKnownLocation(mockLocationManager!!)
        }

        override fun getAppWidgetManager(): android.appwidget.AppWidgetManager {
            return mockk(relaxed = true)
        }
    }
}
