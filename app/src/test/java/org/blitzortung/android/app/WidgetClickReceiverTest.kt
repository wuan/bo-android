package org.blitzortung.android.app

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class WidgetClickReceiverTest {

    @get:Rule
    val mockKRule = MockKRule(this)

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun receiverClassIsPublic() {
        val receiverClass = WidgetClickReceiver::class.java
        val modifiers = receiverClass.modifiers
        val isPublic = java.lang.reflect.Modifier.isPublic(modifiers)
        assertThat(isPublic).isTrue()
    }

    @Test
    fun actionWidgetClickIsDefined() {
        assertThat(WidgetClickReceiver.ACTION_WIDGET_CLICK).isEqualTo("org.blitzortung.android.app.ACTION_WIDGET_CLICK")
    }
}
