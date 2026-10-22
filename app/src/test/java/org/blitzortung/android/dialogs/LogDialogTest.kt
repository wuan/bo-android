package org.blitzortung.android.dialogs

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.dialogs.log.LogProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LogDialogTest {

    private lateinit var activity: Activity
    private lateinit var buildVersion: BuildVersion

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        buildVersion = mockk(relaxed = true) {
            every { versionName } returns "1.2.3"
            every { versionCode } returns 42
        }
    }

    private fun createDialog(logProvider: LogProvider): LogDialog =
        LogDialog(
            activity,
            CacheSize(entries = 2, strikes = 7),
            buildVersion,
            logProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun `onStart loads log lines using the provided provider and dispatcher`() {
        val logProvider = mockk<LogProvider>()
        every { logProvider.getLogLines() } returns listOf("line one", "line two")

        val dialog = createDialog(logProvider)
        dialog.show()

        val logView = dialog.findViewById<android.widget.TextView>(org.blitzortung.android.app.R.id.log_text)

        assertThat(logView).isNotNull()
        assertThat(logView!!.text.toString()).contains("line one")
        assertThat(logView.text.toString()).contains("line two")
        assertThat(logView.text.toString()).contains("Cache size: 2 entries, 7 strikes")
        assertThat(logView.text.toString()).contains("Version 1.2.3 (42)")

        dialog.dismiss()
    }

    @Test
    fun `onStop cancels the running load job`() {
        val logProvider = mockk<LogProvider>()
        every { logProvider.getLogLines() } returns emptyList()

        val dialog = createDialog(logProvider)
        dialog.show()
        dialog.hide()

        dialog.dismiss()
    }
}
