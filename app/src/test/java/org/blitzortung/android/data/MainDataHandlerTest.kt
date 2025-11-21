package org.blitzortung.android.data

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.MainDataHandler.Companion.REQUEST_STARTED_EVENT
import org.blitzortung.android.data.cache.DataCache
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.LocalData
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.util.Period
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MainDataHandlerTest {
    private lateinit var preferences: SharedPreferences

    @MockK
    private lateinit var dataProviderFactory: DataProviderFactory

    @MockK
    private lateinit var handler: Handler

    @MockK
    private lateinit var localData: LocalData

    @MockK
    private lateinit var period: Period

    private lateinit var receivedEvents: MutableList<DataEvent>

    private lateinit var uut: MainDataHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        this.preferences = preferences
        uut = MainDataHandler(context, dataProviderFactory, preferences, handler, DataCache(), localData, period)

        receivedEvents = mutableListOf<DataEvent>()
        val eventConsumer: (DataEvent) -> Unit = { event -> receivedEvents.add(event) }
        uut.requestUpdates(eventConsumer)
    }

    @Test
    fun updateGridSize() {
        val result1 = uut.updateAutoGridSize(1.0, true)
        assertThat(result1).isTrue

        val result2 = uut.updateAutoGridSize(5.0, true)

        assertThat(result2).isTrue
        assertThat(uut.parameters.gridSize).isEqualTo(10000)
    }

    @Test
    fun SharedPreferencesChangedForDataSource() {
        preferences.edit()
            .putString(PreferenceKey.DATA_SOURCE.toString(), DataProviderType.HTTP.toString())
            .commit()

        uut.onSharedPreferenceChanged(preferences, PreferenceKey.DATA_SOURCE)

        verify { dataProviderFactory.getDataProviderForType(DataProviderType.HTTP) }
        assertThat(receivedEvents).contains(REQUEST_STARTED_EVENT)
    }

    @Test
    fun SharedPreferencesChangedForGridSize() {
        preferences.edit()
            .putString(PreferenceKey.GRID_SIZE.toString(), "5000")
            .commit()

        uut.onSharedPreferenceChanged(preferences, PreferenceKey.GRID_SIZE)

        assertThat(uut.parameters.gridSize).isEqualTo(5000)
        assertThat(receivedEvents).contains(REQUEST_STARTED_EVENT)
    }

    @Test
    fun tryDataModeRun() {
        uut.run()

        assertThat(receivedEvents).contains(StatusEvent("0/60"))
        verify { handler.postDelayed(uut, 1000) }
    }

    @Test
    fun onScrollWithoutEvent() {
        val result = uut.onScroll(null)

        assertThat(result).isFalse
    }

    @Test
    fun reactOnScrollWithinDataArea() {
        val mapView = mockk<OwnMapView>()
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 6.0
        every { localData.update(boundingBox, false) } returns false

        val event = ScrollEvent(mapView, 100, 100)

        val result = uut.onScroll(event)

        assertThat(result).isFalse
    }

    @Test
    fun reactOnScrollLeavingDataArea() {
        val mapView = mockk<OwnMapView>()
        every { mapView.isAnimating } returns false
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 6.0
        every { localData.update(boundingBox, false) } returns true
        val event = ScrollEvent(mapView, 100, 100)

        val result = uut.onScroll(event)

        assertThat(result).isTrue
    }

    @Test
    fun reactOnScrollLeavingDataAreaWithAnimation() {
        val mapView = mockk<OwnMapView>()
        val animator = mockk<ValueAnimator>(relaxed = true)
        every { animator.listeners } returns ArrayList<Animator.AnimatorListener>()
        every { mapView.animator() } returns animator
        every { mapView.isAnimating } returns true
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 6.0
        every { localData.update(boundingBox, false) } returns true
        val event = ScrollEvent(mapView, 100, 100)

        val result = uut.onScroll(event)

        assertThat(result).isTrue

        val animatorListenerSlot = slot<Animator.AnimatorListener>()
        verify { animator.addListener(capture(animatorListenerSlot)) }
        val captured = animatorListenerSlot.captured

        assertThat(receivedEvents).isEmpty()
        captured.onAnimationEnd(animator)
        assertThat(receivedEvents).contains(REQUEST_STARTED_EVENT)
    }

    @Test
    fun onZoomWithoutEvent() {
        val result = uut.onZoom(null)

        assertThat(result).isFalse
    }

    @Test
    fun reactOnZoomWithinDataArea() {
        val mapView = mockk<OwnMapView>()
        every { mapView.isAnimating } returns false
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 6.0
        every { localData.update(boundingBox, false) } returns false
        val event = ZoomEvent(mapView, 6.0)

        val result = uut.onZoom(event)

        assertThat(result).isFalse
    }

    @Test
    fun reactOnZoomlLeavingDataArea() {
        val mapView = mockk<OwnMapView>()
        every { mapView.isAnimating } returns false
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 6.0
        every { localData.update(boundingBox, false) } returns true
        val event = ZoomEvent(mapView, 6.0)

        val result = uut.onZoom(event)

        assertThat(result).isTrue
    }

    @Test
    fun reactOnZoomlUpdateGridSize() {
        val mapView = mockk<OwnMapView>()
        every { mapView.isAnimating } returns false
        val boundingBox = BoundingBox(45.0, 15.0, 40.0, 10.0)
        every { mapView.boundingBox } returns boundingBox
        every { mapView.zoomLevelDouble } returns 8.0
        every { localData.update(boundingBox, false) } returns false
        val event = ZoomEvent(mapView, 8.0)

        val result = uut.onZoom(event)

        assertThat(result).isTrue
    }
}
