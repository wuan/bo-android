package org.blitzortung.android.app

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetProviderTest {

    @get:Rule
    val mockKRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var workManager: androidx.work.WorkManager

    private lateinit var context: Context
    private lateinit var widgetProvider: TestableWidgetProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widgetProvider = TestableWidgetProvider()
    }

    @Test
    fun onDisabledCancelsWork() {
        widgetProvider.onDisabled(context)

        verify { workManager.cancelUniqueWork(WidgetProvider.WIDGET_UPDATE_WORK_NAME) }
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

    private inner class TestableWidgetProvider : WidgetProvider() {
        override fun getWorkManager(context: Context): androidx.work.WorkManager = workManager
    }
}
