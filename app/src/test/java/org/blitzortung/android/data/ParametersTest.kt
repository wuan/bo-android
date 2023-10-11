package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ParametersTest {

    @Test
    fun sliderStartPosition() {
        val parameters = Parameters()

        assertThat(parameters.intervalPosition).isEqualTo(0)
        assertThat(parameters.intervalMaxPosition).isEqualTo(46)
    }

    @Test
    fun sliderOneOffPosition() {
        val parameters = Parameters().rewInterval()

        assertThat(parameters.intervalPosition).isEqualTo(1)
        assertThat(parameters.intervalMaxPosition).isEqualTo(46)
    }
}