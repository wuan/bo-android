package org.blitzortung.android.data

import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.SharedPreferences
import android.os.PowerManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.createLocationEvent
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.LocalData
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ServiceDataHandlerTest {
    private class DummyActivity : Activity()

    private lateinit var preferences: SharedPreferences

    @MockK
    private lateinit var dataProviderFactory: DataProviderFactory

    private lateinit var shadowPowerManager: ShadowPowerManager

    private lateinit var wakeLock: PowerManager.WakeLock

    @MockK
    private lateinit var localData: LocalData

    private lateinit var activity: Activity

    @MockK
    private lateinit var dataProvider: DataProvider

    private lateinit var receivedEvents: MutableList<DataEvent>

    private lateinit var uut: ServiceDataHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        activity = Robolectric.buildActivity(DummyActivity::class.java).create().get()
        val pm = activity.getSystemService(POWER_SERVICE) as PowerManager
        shadowPowerManager = shadowOf(pm)

        val preferences = activity.getSharedPreferences(activity.packageName, Context.MODE_PRIVATE)

        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            BOApplication.WAKE_LOCK_TAG,
        )

        this.preferences = preferences
        uut = ServiceDataHandler(activity, wakeLock, dataProviderFactory, localData)

        receivedEvents = mutableListOf<DataEvent>()
        val eventConsumer: (DataEvent) -> Unit = { event -> receivedEvents.add(event) }
        uut.requestUpdates(eventConsumer)
    }

    @Test
    fun spass() {
        every { dataProviderFactory.getDataProviderForType(DataProviderType.RPC) } returns dataProvider

        uut.locationEventConsumer.invoke(createLocationEvent(11.0, 49.0))

        uut.updateData()

        assertThat(receivedEvents).hasSize(1)

//        verify { dataProvider.retrieveData(any()) }
    }
}
