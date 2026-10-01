package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ClusterParametersTest {
    @Test
    fun defaultsToTenMinuteIntervalCoveringOneHour() {
        val parameters = ClusterParameters()

        assertThat(parameters.minuteLength).isEqualTo(10)
        assertThat(parameters.intervalCount).isEqualTo(6)
        assertThat(parameters.minuteOffset).isEqualTo(0)
        assertThat(parameters.global).isTrue()
    }

    @Test
    fun derivesIntervalCountFromMinuteLength() {
        assertThat(ClusterParameters.intervalCountFor(5)).isEqualTo(12)
        assertThat(ClusterParameters.intervalCountFor(10)).isEqualTo(6)
        assertThat(ClusterParameters.intervalCountFor(0)).isEqualTo(1)

        assertThat(ClusterParameters(minuteLength = 5).intervalCount).isEqualTo(12)
        assertThat(ClusterParameters(minuteLength = 10).intervalCount).isEqualTo(6)
    }

    @Test
    fun appliesNegativeHistoryOffset() {
        val parameters = ClusterParameters(minuteLength = 10).withMinuteOffset(-30)

        assertThat(parameters.minuteOffset).isEqualTo(-30)
        assertThat(parameters.minuteLength).isEqualTo(10)
        assertThat(parameters.intervalCount).isEqualTo(6)
    }

    @Test
    fun selectsLocalWhenDataAreaIsSet() {
        val dataArea = DataArea(4, 9, 5)

        val parameters = ClusterParameters().withDataArea(dataArea)

        assertThat(parameters.global).isFalse()
        assertThat(parameters.dataArea).isEqualTo(dataArea)
    }
}
