package org.blitzortung.android.app

import android.content.SharedPreferences
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

    @Test
    fun formatLocationInfo_returnsGpsLocation() {
        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val location = createLocation(51.0, 7.0, 10f).apply { provider = LocationManager.GPS_PROVIDER }

        val result = worker.testFormatLocationInfo(location)

        assertThat(result).isNotNull
        assertThat(result).contains("gps")
        assertThat(result).contains("51.000")
        assertThat(result).contains("7.000")
    }

    @Test
    fun formatLocationInfo_returnsNetworkLocation() {
        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val location = createLocation(52.0, 8.0, 50f).apply { provider = LocationManager.NETWORK_PROVIDER }

        val result = worker.testFormatLocationInfo(location)

        assertThat(result).isNotNull
        assertThat(result).contains("network")
    }

    @Test
    fun formatLocationInfo_returnsNullForNullLocation() {
        val worker = TestableWidgetUpdateWorker(context, workerParams)

        val result = worker.testFormatLocationInfo(null)

        assertThat(result).isNull()
    }

    @Test
    fun getLastKnownLocationFromProvider_returnsLocation() {
        val location = createLocation(51.0, 7.0, 10f)
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns location

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val result = worker.testGetLastKnownLocationFromProvider(LocationManager.GPS_PROVIDER)

        assertThat(result).isNotNull
        assertThat(result!!.latitude).isEqualTo(51.0)
    }

    @Test
    fun getLastKnownLocationFromProvider_returnsNullOnSecurityException() {
        val mockLocationManager = mockk<LocationManager>()
        every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } throws SecurityException()

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.setLocationManager(mockLocationManager)

        val result = worker.testGetLastKnownLocationFromProvider(LocationManager.GPS_PROVIDER)

        assertThat(result).isNull()
    }

    @Test
    fun getManualLocation_returnsLocationWhenConfigured() {
        val mockPreferences = mockk<SharedPreferences>()
        every { mockPreferences.getString("location_longitude", null) } returns "7.0"
        every { mockPreferences.getString("location_latitude", null) } returns "51.0"

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val location = worker.testGetManualLocation(mockPreferences)

        assertThat(location).isNotNull
        assertThat(location!!.latitude).isEqualTo(51.0)
        assertThat(location.longitude).isEqualTo(7.0)
        assertThat(location.accuracy).isEqualTo(0f)
    }

    @Test
    fun getManualLocation_returnsNullWhenNotConfigured() {
        val mockPreferences = mockk<SharedPreferences>()
        every { mockPreferences.getString("location_longitude", null) } returns null
        every { mockPreferences.getString("location_latitude", null) } returns null

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val location = worker.testGetManualLocation(mockPreferences)

        assertThat(location).isNull()
    }

    @Test
    fun getManualLocation_returnsNullOnInvalidFormat() {
        val mockPreferences = mockk<SharedPreferences>()
        every { mockPreferences.getString("location_longitude", null) } returns "invalid"
        every { mockPreferences.getString("location_latitude", null) } returns "51.0"

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val location = worker.testGetManualLocation(mockPreferences)

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

        fun testFormatLocationInfo(location: Location?): String? {
            return formatLocationInfo(location)
        }

        fun testGetLastKnownLocationFromProvider(provider: String): Location? {
            return getLastKnownLocationFromProvider(mockLocationManager!!, provider)
        }

        fun testGetManualLocation(preferences: SharedPreferences): Location? {
            return getManualLocation(preferences)
        }

        override fun getAppWidgetManager(): android.appwidget.AppWidgetManager {
            return mockk(relaxed = true)
        }
    }
}
