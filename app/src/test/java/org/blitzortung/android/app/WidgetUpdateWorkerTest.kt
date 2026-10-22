package org.blitzortung.android.app

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerParameters
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoData
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.alert.handler.AlertDataHandler
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import org.blitzortung.android.app.view.AlarmView
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
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

    @Test
    fun fetchStrikeData_returnsNoStrikeDataWhenNoStrikes() {
        val mockAlertHandler = mockk<AlertHandler>()
        val mockAlertDataHandler = mockk<AlertDataHandler>()
        val mockColorHandler = mockk<StrikeColorHandler>()

        val location = createLocation(51.0, 7.0, 10f)
        val mockAlarmView: AlarmView = mockk(relaxed = true)

        val mockDataProvider = mockk<JsonRpcDataProvider>()
        every {
            mockDataProvider.retrieveData(any<DataProvider.DataRetriever.() -> DataReceived>())
        } returns DataReceived(
            strikes = null,
            gridParameters = null,
            referenceTime = System.currentTimeMillis(),
            parameters = mockk(relaxed = true),
            flags = mockk(relaxed = true)
        )

        val appComponents = createAppComponents(
            mockColorHandler,
            mockAlertHandler,
            mockAlertDataHandler,
            mockDataProvider
        )

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val (statusText, _) = worker.testFetchStrikeData(appComponents, location, mockAlarmView)

        assertThat(statusText).isEqualTo("no strike data")
    }

    @Test
    fun fetchStrikeData_returnsLocalActivityStatusForLocalWarning() {
        val mockAlertHandler = mockk<AlertHandler>()
        every { mockAlertHandler.alertParameters } returns createAlertParameters()
        val mockAlertDataHandler = mockk<AlertDataHandler>()
        val mockColorHandler = mockk<StrikeColorHandler>()
        val location = createLocation(51.0, 7.0, 10f)
        val mockAlarmView: AlarmView = mockk(relaxed = true)

        val localActivity = createLocalActivity()
        every { mockAlertDataHandler.checkStrikes(any(), any(), any(), any()) } returns localActivity

        val strike = mockk<org.blitzortung.android.data.beans.Strike>()
        val mockDataProvider = mockk<JsonRpcDataProvider>()
        every {
            mockDataProvider.retrieveData(any<DataProvider.DataRetriever.() -> DataReceived>())
        } returns DataReceived(
            strikes = listOf(strike),
            gridParameters = null,
            referenceTime = System.currentTimeMillis(),
            parameters = mockk(relaxed = true),
            flags = mockk(relaxed = true)
        )

        val appComponents = createAppComponents(
            mockColorHandler,
            mockAlertHandler,
            mockAlertDataHandler,
            mockDataProvider
        )

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.testFetchStrikeData(appComponents, location, mockAlarmView)

        verify { mockAlarmView.alertEventConsumer.invoke(localActivity) }
    }

    @Test
    fun fetchStrikeData_usesGreenWhenWarningIsNotLocal() {
        val mockAlertHandler = mockk<AlertHandler>()
        every { mockAlertHandler.alertParameters } returns createAlertParameters()
        val mockAlertDataHandler = mockk<AlertDataHandler>()
        val mockColorHandler = mockk<StrikeColorHandler>()
        val location = createLocation(51.0, 7.0, 10f)
        val mockAlarmView: AlarmView = mockk(relaxed = true)

        every { mockAlertDataHandler.checkStrikes(any(), any(), any(), any()) } returns NoData

        val strike = mockk<org.blitzortung.android.data.beans.Strike>()
        val mockDataProvider = mockk<JsonRpcDataProvider>()
        every {
            mockDataProvider.retrieveData(any<DataProvider.DataRetriever.() -> DataReceived>())
        } returns DataReceived(
            strikes = listOf(strike),
            gridParameters = null,
            referenceTime = System.currentTimeMillis(),
            parameters = mockk(relaxed = true),
            flags = mockk(relaxed = true)
        )

        val appComponents = createAppComponents(
            mockColorHandler,
            mockAlertHandler,
            mockAlertDataHandler,
            mockDataProvider
        )

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val (statusText, statusColor) = worker.testFetchStrikeData(appComponents, location, mockAlarmView)

        assertThat(statusText).isNull()
        assertThat(statusColor).isEqualTo(context.getColor(org.blitzortung.android.app.R.color.Green))
    }

    private fun createAlertParameters(): AlertParameters =
        AlertParameters(
            alarmInterval = 600000L,
            rangeSteps = listOf(10f, 25f, 50f),
            sectorLabels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
            measurementSystem = MeasurementSystem.METRIC
        )

    private fun createLocalActivity(): LocalActivity {
        val ranges =
            listOf(
                AlertSectorRange(
                    rangeMinimum = 0.0f,
                    rangeMaximum = 50.0f,
                    strikeCount = 5,
                    latestStrikeTimestamp = System.currentTimeMillis(),
                ),
            )
        val sector =
            AlertSector(
                label = "N",
                minimumSectorBearing = 0f,
                maximumSectorBearing = 45f,
                ranges = ranges,
                closestStrikeDistance = 10.0f,
            )
        return LocalActivity(
            sectors = listOf(sector),
            parameters = createAlertParameters(),
            referenceTime = System.currentTimeMillis(),
        )
    }

    @Test
    fun fetchStrikeData_returnsLocationNotAvailableWhenLocationIsNull() {
        val mockDataProvider = mockk<JsonRpcDataProvider>()
        val mockAlertHandler = mockk<AlertHandler>()
        val mockAlertDataHandler = mockk<AlertDataHandler>()
        val mockColorHandler = mockk<StrikeColorHandler>()
        val mockAlarmView: AlarmView = mockk(relaxed = true)

        val appComponents = createAppComponents(
            mockColorHandler,
            mockAlertHandler,
            mockAlertDataHandler,
            mockDataProvider
        )

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        val (statusText, _) = worker.testFetchStrikeData(appComponents, null, mockAlarmView)

        assertThat(statusText).isEqualTo("location not available")
    }

    @Test
    fun fetchStrikeData_usesCorrectParameters() {
        val mockAlertHandler = mockk<AlertHandler>()
        val mockAlertDataHandler = mockk<AlertDataHandler>()
        val mockColorHandler = mockk<StrikeColorHandler>()
        val mockAlarmView: AlarmView = mockk(relaxed = true)

        val mockDataProvider = mockk<JsonRpcDataProvider>()
        val dataRetrieverSlot = slot<DataProvider.DataRetriever.() -> DataReceived>()

        every { mockDataProvider.retrieveData(capture(dataRetrieverSlot)) } returns DataReceived(
            strikes = null,
            gridParameters = null,
            referenceTime = System.currentTimeMillis(),
            parameters = mockk(relaxed = true),
            flags = mockk(relaxed = true)
        )

        val location = createLocation(51.0, 7.0, 10f)
        val appComponents = createAppComponents(
            mockColorHandler,
            mockAlertHandler,
            mockAlertDataHandler,
            mockDataProvider
        )

        val worker = TestableWidgetUpdateWorker(context, workerParams)
        worker.testFetchStrikeData(appComponents, location, mockAlarmView)

        // Execute the captured lambda to verify parameters are correct
        val mockDataRetriever = mockk<DataProvider.DataRetriever>()
        val capturedParamsSlot = slot<org.blitzortung.android.data.Parameters>()

        every { mockDataRetriever.getStrikesGrid(capture(capturedParamsSlot), any(), any()) } returns mockk()

        dataRetrieverSlot.captured.invoke(mockDataRetriever)

        val params = capturedParamsSlot.captured
        assertThat(params.region).isEqualTo(org.blitzortung.android.data.provider.LOCAL_REGION)
        assertThat(params.gridSize).isEqualTo(5000)
        assertThat(params.interval.duration).isEqualTo(60)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun isDisclosureNeeded_returnsTrueWhenNotDisclosedAndPermissionNotGranted() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test", android.content.Context.MODE_PRIVATE)
        val worker = TestableWidgetUpdateWorker(context, workerParams)

        assertThat(worker.testIsDisclosureNeeded(prefs)).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun isDisclosureNeeded_returnsFalseWhenDisclosureAlreadyShown() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test", android.content.Context.MODE_PRIVATE)
        prefs.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }
        val worker = TestableWidgetUpdateWorker(context, workerParams)

        assertThat(worker.testIsDisclosureNeeded(prefs)).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun isDisclosureNeeded_returnsFalseWhenPermissionAlreadyGranted() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test", android.content.Context.MODE_PRIVATE)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(ACCESS_BACKGROUND_LOCATION)
        val worker = TestableWidgetUpdateWorker(context, workerParams)

        assertThat(worker.testIsDisclosureNeeded(prefs)).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun isDisclosureNeeded_returnsFalseOnAndroidBelowQ() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test", android.content.Context.MODE_PRIVATE)
        val worker = TestableWidgetUpdateWorker(context, workerParams)

        assertThat(worker.testIsDisclosureNeeded(prefs)).isFalse()
    }

    private fun createAppComponents(
        colorHandler: StrikeColorHandler,
        alertHandler: AlertHandler,
        alertDataHandler: AlertDataHandler,
        dataProvider: JsonRpcDataProvider
    ): WidgetUpdateWorkerTest.TestAppComponents {
        return TestAppComponents(
            colorHandler = colorHandler,
            alertHandler = alertHandler,
            alertDataHandler = alertDataHandler,
            locationManager = mockk(relaxed = true),
            dataProvider = dataProvider,
            preferences = mockk(relaxed = true)
        )
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

        fun testGetLastKnownLocationFromProvider(provider: String): Location? {
            return getLastKnownLocationFromProvider(mockLocationManager!!, provider)
        }

        fun testGetManualLocation(preferences: SharedPreferences): Location? {
            return getManualLocation(preferences)
        }

        fun testFetchStrikeData(
            appComponents: TestAppComponents,
            location: Location?,
            alarmView: AlarmView
        ): Pair<String?, Any?> {
            val components = AppComponents(
                colorHandler = appComponents.colorHandler,
                alertHandler = appComponents.alertHandler,
                alertDataHandler = appComponents.alertDataHandler,
                locationManager = appComponents.locationManager,
                dataProvider = appComponents.dataProvider,
                preferences = appComponents.preferences
            )
            return fetchStrikeData(components, location, alarmView)
        }

        fun testIsDisclosureNeeded(preferences: SharedPreferences): Boolean =
            isDisclosureNeeded(preferences)

        override fun getAppWidgetManager(): android.appwidget.AppWidgetManager {
            return mockk(relaxed = true)
        }
    }

    data class TestAppComponents(
        val colorHandler: StrikeColorHandler,
        val alertHandler: AlertHandler,
        val alertDataHandler: AlertDataHandler,
        val locationManager: LocationManager,
        val dataProvider: JsonRpcDataProvider,
        val preferences: SharedPreferences
    )
}
