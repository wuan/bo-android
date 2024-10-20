package org.blitzortung.android.map.overlay.color

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StrikeColorHandlerTest {

    lateinit var strikeColorHandler: StrikeColorHandler

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        strikeColorHandler = StrikeColorHandler(preferences)

    }

    @Test
    fun shouldReturnProperSatelliteColors() {
        val colors = strikeColorHandler.getColors(ColorTarget.SATELLITE)

        assertThat(colors).isEqualTo(ColorScheme.BLITZORTUNG.strikeColors)
    }

    @Test
    fun shouldReturnProperSatelliteTextColor() {
        val textColor = strikeColorHandler.getTextColor(ColorTarget.SATELLITE)

        assertThat(textColor).isEqualTo(0xff000000.toInt())
    }

    @Test
    fun shouldReturnProperStreetmapColors() {
        val colors = strikeColorHandler.getColors(ColorTarget.STREETMAP)

        assertThat(colors).containsExactly(
            0xffcccccc.toInt(),
            0xffcccc8d.toInt(),
            0xffccb361.toInt(),
            0xffcc8d4d.toInt(),
            0xffc0664d.toInt(),
            0xffb34040.toInt(),
        )
    }

    @Test
    fun shouldReturnProperStreetmapTextColor() {
        val textColor = strikeColorHandler.getTextColor(ColorTarget.STREETMAP)

        assertThat(textColor).isEqualTo(0xffffffff.toInt())
    }
}