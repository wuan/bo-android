package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TimeIntervalTest {
    private val history = History(30, 120, true)
    private val historyUnlimited = History(30, 120, false)
    private lateinit var interval: TimeInterval

    @Test
    fun isRealtime() {
        interval = TimeInterval()

        assertThat(interval.isRealtime()).isTrue()

        update { it.withOffset(-30, history) }

        assertThat(interval.isRealtime()).isFalse()
    }

    @Test
    fun testRewindInterval() {
        interval = TimeInterval()

        assertThat(update { it.rewInterval(history) }).isTrue()
        assertThat(interval.offset).isEqualTo(-30)

        assertThat(update { it.rewInterval(history) }).isTrue()
        assertThat(interval.offset).isEqualTo(-60)

        assertThat(update { it.rewInterval(history) }).isFalse()
        assertThat(interval.offset).isEqualTo(-60)
    }

    @Test
    fun testRewindIntervalWithAlignment() {
        interval = TimeInterval()
        val history = History(45, 120, true)

        assertThat(update { it.rewInterval(history) }).isTrue()
        assertThat(interval.offset).isEqualTo(-45)

        assertThat(update { it.rewInterval(history) }).isFalse()
        assertThat(interval.offset).isEqualTo(-45)
    }

    @Test
    fun testFastforwardInterval() {
        interval = TimeInterval()

        update { it.rewInterval(history) }

        assertThat(update { it.ffwdInterval(history) }).isTrue()
        assertThat(interval.offset).isEqualTo(0)

        assertThat(update { it.ffwdInterval(history) }).isFalse()
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun testGoRealtime() {
        interval = TimeInterval()

        assertThat(update { it.goRealtime() }).isFalse()
        assertThat(interval.offset).isEqualTo(0)

        update { it.rewInterval(history) }

        assertThat(update { it.goRealtime() }).isTrue()
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun testWithOffsetInRange() {
        interval = TimeInterval()

        update { it.withOffset(-50, history) }
        assertThat(interval.offset).isEqualTo(-50)
    }

    fun testWithPositiveOffset() {
        interval = TimeInterval()

        update { it.withOffset(1, history) }
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun testWithOffsetInLimitedRange() {
        interval = TimeInterval()

        update { it.withOffset(-61, history) }
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun testWithOffsetInUnlimitedRange() {
        interval = TimeInterval()

        update { it.withOffset(-120, historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-120)
    }

    @Test
    fun testWithOffsetOutsideRange() {
        interval = TimeInterval()

        update { it.withOffset(-121, historyUnlimited) }
        assertThat(interval.offset).isEqualTo(0)
    }

    @Test
    fun animationSteps() {
        interval = TimeInterval()

        update { it.animationStep(history) }
        assertThat(interval.offset).isEqualTo(-60)

        update { it.animationStep(history) }
        assertThat(interval.offset).isEqualTo(-30)

        update { it.animationStep(history) }
        assertThat(interval.offset).isEqualTo(0)

        update { it.animationStep(history) }
        assertThat(interval.offset).isEqualTo(-60)
    }

    @Test
    fun animationStepsUnlimited() {
        interval = TimeInterval()

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-120)

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-90)

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-60)

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-30)

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(0)

        update { it.animationStep(historyUnlimited) }
        assertThat(interval.offset).isEqualTo(-120)
    }

    private fun update(updater: (TimeInterval) -> TimeInterval): Boolean {
        val oldParameters = interval
        interval = updater.invoke(interval)
        return interval != oldParameters
    }
}