package org.blitzortung.android.data

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TimeIntervalTest {

    private lateinit var interval : TimeInterval

    @Test
    fun testRewindInterval() {
        interval = TimeInterval()
        val timeIncrement = 15

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(-15)

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(-30)

        for (i in 0 until 23 * 4 - 2 - 1) {
            Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        }
        assertThat(interval.offset).isEqualTo(-23 * 60 + 15)

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(-23 * 60)

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isFalse()
        assertThat(interval.offset).isEqualTo(-23 * 60)
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        interval = TimeInterval()
        val timeIncrement = 45

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(-45)

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(-90)

        for (i in 0 until 23 / 3 * 4) {
            Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isTrue()
        }
        assertThat(interval.offset).isEqualTo(-23 * 60 + 30)

        Assertions.assertThat(update { it.rewInterval(timeIncrement) }).isFalse()
        assertThat(interval.offset).isEqualTo(-23 * 60 + 30)
    }

    @Test
    fun testFastforwardInterval() {
        interval = TimeInterval()
        val timeIncrement = 30

        update { it.rewInterval(timeIncrement) }

        Assertions.assertThat(update { it.ffwdInterval(timeIncrement) }).isTrue()
        assertThat(interval.offset).isEqualTo(0)

        Assertions.assertThat(update { it.ffwdInterval(timeIncrement) }).isFalse()
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun testGoRealtime() {
        interval = TimeInterval()
        val timeIncrement = 30

        Assertions.assertThat(update { it.goRealtime() }).isFalse()
        assertThat(interval.offset).isEqualTo(0)

        update { it.rewInterval(timeIncrement) }

        Assertions.assertThat(update { it.goRealtime() }).isTrue()
        assertThat(interval.offset).isEqualTo(0)
    }

    private fun update(updater: (TimeInterval) -> TimeInterval): Boolean {
        val oldParameters = interval
        interval = updater.invoke(interval)
        return interval != oldParameters
    }
}