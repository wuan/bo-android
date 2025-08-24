package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ParametersTest {

    private val parameters = Parameters()
    private val history = History()

    @Test
    fun returnsDuration() {
        assertThat(parameters.intervalDuration).isEqualTo(60)
    }

    @Test
    fun returnsOffset() {
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }

    @Test
    fun isRealtime() {
        assertThat(parameters.isRealtime()).isTrue()
    }

    @Test
    fun returnsNoRealtime() {
        val result = parameters.animationStep(history)

        assertThat(result.isRealtime()).isFalse()
    }

    @Test
    fun goRealtime() {
        val result = parameters.animationStep(history).goRealtime()

        assertThat(result.isRealtime()).isTrue()
    }


    @Test
    fun goToHistory() {
        val result = parameters.animationStep(history)
        assertThat(result.isRealtime()).isFalse()
        assertThat(parameters.intervalOffset).isEqualTo(0)
    }

    @Test
    fun defaultSliderEndPosition() {
        assertThat(parameters.intervalPosition(history)).isEqualTo(46)
        assertThat(parameters.intervalMaxPosition(history)).isEqualTo(46)
    }

    @Test
    fun sliderEndPositionForUnlimitedHistory() {
        val unlimitedHistory = History(5, 120, false)

        assertThat(parameters.intervalPosition(unlimitedHistory)).isEqualTo(24)
        assertThat(parameters.intervalMaxPosition(unlimitedHistory)).isEqualTo(24)
    }

    @Test
    fun sliderStartPosition() {
        val result = parameters.animationStep(history)

        assertThat(result.intervalPosition(history)).isEqualTo(0)
        assertThat(result.intervalMaxPosition(history)).isEqualTo(46)
    }

    @Test
    fun withPositionAtStart() {
        val result = parameters.withPosition(0, history)

        assertThat(result.intervalOffset).isEqualTo(-1380)
    }

    @Test
    fun withPositionAtEnd() {
        val result = parameters.withPosition(46, history)

        assertThat(result.intervalOffset).isEqualTo(0)
    }

    @Test
    fun withIntervalDurationUpdate() {
        val result = parameters.withIntervalDuration(120)

        assertThat(result.intervalMaxPosition(history)).isEqualTo(44)
    }

    @Test
    fun withLocalReference() {
        val localReference = LocalReference(5, 6, dataArea = 5)
        val result = parameters.copy(localReference = localReference)

        assertThat(result.localReference).isEqualTo(localReference)
    }
}
